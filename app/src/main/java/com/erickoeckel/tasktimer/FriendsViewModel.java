package com.erickoeckel.tasktimer;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FriendsViewModel extends AndroidViewModel {

    private final Application app;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

    private final MutableLiveData<List<FriendRequest>> _incoming = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<FriendRequest>> _outgoing = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<String>> _friends = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<FriendRequest>> incoming() { return _incoming; }
    public LiveData<List<FriendRequest>> outgoing() { return _outgoing; }
    public LiveData<List<String>> friends() { return _friends; }

    private ListenerRegistration incReg, outReg, frReg, acceptedReg, acceptedCreateReg;

    private boolean incomingInit = false;
    private boolean outgoingAcceptedInit = false;
    private boolean acceptedCreateInit = false;

    public FriendsViewModel(@NonNull Application application) {
        super(application);
        this.app = application;

        incReg = db.collection("friend_requests")
                .whereEqualTo("toUid", uid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;
                    List<FriendRequest> list = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        FriendRequest r = d.toObject(FriendRequest.class);
                        if (r != null) { r.id = d.getId(); list.add(r); }
                    }
                    _incoming.setValue(list);

                    if (!incomingInit) {
                        incomingInit = true;
                    } else {
                        for (DocumentChange dc : snap.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                DocumentSnapshot d = dc.getDocument();
                                String status = d.getString("status");
                                if ("pending".equals(status)) {
                                    String fromUid = d.getString("fromUid");
                                    notifyFriendRequest(fromUid);
                                }
                            }
                        }
                    }
                });

        outReg = db.collection("friend_requests")
                .whereEqualTo("fromUid", uid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;
                    List<FriendRequest> list = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        FriendRequest r = d.toObject(FriendRequest.class);
                        if (r != null) { r.id = d.getId(); list.add(r); }
                    }
                    _outgoing.setValue(list);
                });

        frReg = db.collection("users").document(uid).collection("friendships")
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;
                    List<String> ids = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        ids.add(d.getId());
                    }
                    _friends.setValue(ids);
                });

        acceptedReg = db.collection("friend_requests")
                .whereEqualTo("fromUid", uid)
                .whereEqualTo("status", "accepted")
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;
                    if (!outgoingAcceptedInit) {
                        outgoingAcceptedInit = true;
                        return;
                    }
                    for (DocumentChange dc : snap.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            String toUid = dc.getDocument().getString("toUid");
                            notifyFriendAccepted(toUid);
                        }
                    }
                });

        acceptedCreateReg = db.collection("friend_requests")
                .whereEqualTo("fromUid", uid)
                .whereEqualTo("status", "accepted")
                .addSnapshotListener((snap, e) -> {
                    if (snap == null) return;

                    if (!acceptedCreateInit) {
                        acceptedCreateInit = true;
                    }

                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String other = d.getString("toUid");
                        if (other == null) continue;
                        DocumentReference mine = db.collection("users").document(uid)
                                .collection("friendships").document(other);
                        mine.get().addOnSuccessListener(s -> {
                            if (!s.exists()) {
                                Map<String, Object> f = new HashMap<>();
                                f.put("friendUid", other);
                                f.put("since", FieldValue.serverTimestamp());
                                mine.set(f, SetOptions.merge());
                            }
                        });
                    }
                });
    }

    public Task<Void> sendRequestToEmail(String email) {
        return db.collection("users").whereEqualTo("email", email).limit(1).get()
                .continueWithTask(task -> {
                    QuerySnapshot qs = task.getResult();
                    if (qs == null || qs.isEmpty())
                        throw new IllegalStateException("No user with that email.");
                    String toUid = qs.getDocuments().get(0).getId();
                    if (toUid.equals(uid)) throw new IllegalStateException("You cannot add yourself.");

                    Map<String, Object> req = new HashMap<>();
                    req.put("fromUid", uid);
                    req.put("toUid", toUid);
                    req.put("status", "pending");
                    req.put("createdAt", FieldValue.serverTimestamp());
                    req.put("updatedAt", FieldValue.serverTimestamp());
                    return db.collection("friend_requests").add(req).continueWith(t -> null);
                });
    }

    public Task<Void> accept(String requestId, String fromUid) {
        DocumentReference req = db.collection("friend_requests").document(requestId);
        DocumentReference myFriendDoc = db.collection("users").document(uid)
                .collection("friendships").document(fromUid);

        com.google.firebase.firestore.WriteBatch b = db.batch();
        b.update(req, "status", "accepted", "updatedAt", FieldValue.serverTimestamp());
        Map<String, Object> me = new HashMap<>();
        me.put("friendUid", fromUid);
        me.put("since", FieldValue.serverTimestamp());
        b.set(myFriendDoc, me, SetOptions.merge());
        return b.commit();
    }

    public Task<Void> decline(String requestId) {
        return db.collection("friend_requests").document(requestId)
                .update("status", "declined", "updatedAt", FieldValue.serverTimestamp());
    }

    public Task<Void> cancel(String requestId) {
        return db.collection("friend_requests").document(requestId)
                .update("status", "canceled", "updatedAt", FieldValue.serverTimestamp());
    }

    public Task<Void> unfriend(String friendUid) {
        return db.collection("users").document(uid)
                .collection("friendships").document(friendUid).delete();
    }

    private void notifyFriendRequest(String fromUid) {
        if (fromUid == null) {
            Notify.friendRequest(app.getApplicationContext(), null);
            return;
        }
        db.collection("users").document(fromUid).get()
                .addOnSuccessListener(u -> {
                    String label = safeLabel(u);
                    Notify.friendRequest(app.getApplicationContext(), label);
                })
                .addOnFailureListener(e ->
                        Notify.friendRequest(app.getApplicationContext(), null));
    }

    private void notifyFriendAccepted(String otherUid) {
        if (otherUid == null) {
            Notify.friendAccepted(app.getApplicationContext(), null);
            return;
        }
        db.collection("users").document(otherUid).get()
                .addOnSuccessListener(u -> {
                    String label = safeLabel(u);
                    Notify.friendAccepted(app.getApplicationContext(), label);
                })
                .addOnFailureListener(e ->
                        Notify.friendAccepted(app.getApplicationContext(), null));
    }

    private static String safeLabel(DocumentSnapshot u) {
        String label = u.getString("displayName");
        if (label == null || label.isEmpty()) label = u.getString("email");
        if (label == null || label.isEmpty()) label = u.getId();
        return label;
    }

    @Override
    protected void onCleared() {
        if (incReg != null) incReg.remove();
        if (outReg != null) outReg.remove();
        if (frReg != null) frReg.remove();
        if (acceptedReg != null) acceptedReg.remove();
        if (acceptedCreateReg != null) acceptedCreateReg.remove();
    }
}
