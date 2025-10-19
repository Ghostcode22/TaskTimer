package com.erickoeckel.tasktimer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.HashMap;
import java.util.Map;

public class ShopViewModel extends ViewModel {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private final MutableLiveData<Integer> _coins = new MutableLiveData<>(0);
    private final MutableLiveData<Map<String, Boolean>> _unlocks = new MutableLiveData<>(new HashMap<>());
    private ListenerRegistration reg;

    public LiveData<Integer> coins()   { return _coins; }
    public LiveData<Map<String, Boolean>> unlocks() { return _unlocks; }

    public void start() {
        if (auth.getCurrentUser() == null) return;
        if (reg != null) return;
        reg = db.collection("users").document(auth.getCurrentUser().getUid())
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    applyUserSnapshot(snap);
                });
    }

    private void applyUserSnapshot(DocumentSnapshot snap) {
        Number c = (Number) snap.get("coins");
        _coins.setValue(c == null ? 0 : c.intValue());

        Map<String, Boolean> u = new HashMap<>();
        Object raw = snap.get("unlocks");
        if (raw instanceof Map) {
            u.putAll((Map<String, Boolean>) raw);
        }
        _unlocks.setValue(u);
    }

    @Override protected void onCleared() {
        if (reg != null) { reg.remove(); reg = null; }
    }

    public com.google.android.gms.tasks.Task<Void> purchase(@androidx.annotation.NonNull ShopItem item) {
        com.google.firebase.auth.FirebaseUser u = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) return com.google.android.gms.tasks.Tasks.forException(new IllegalStateException("No user"));

        com.google.firebase.firestore.DocumentReference userRef =
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users").document(u.getUid());

        return com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .runTransaction(tx -> {
                    com.google.firebase.firestore.DocumentSnapshot snap = tx.get(userRef);
                    long coins = snap.contains("coins") ? snap.getLong("coins") : 0L;
                    @SuppressWarnings("unchecked")
                    java.util.Map<String,Boolean> uMap = (java.util.Map<String,Boolean>) snap.get("unlocks");
                    if (uMap == null) uMap = new java.util.HashMap<>();

                    if (Boolean.TRUE.equals(uMap.get(item.slug))) {
                        return null;
                    }
                    if (coins < item.price) {
                        throw new IllegalStateException("Not enough coins");
                    }
                    java.util.Map<String,Object> updates = new java.util.HashMap<>();
                    updates.put("coins", coins - item.price);
                    updates.put("unlocks." + item.slug, true);
                    tx.update(userRef, updates);
                    return null;
                });
    }



}
