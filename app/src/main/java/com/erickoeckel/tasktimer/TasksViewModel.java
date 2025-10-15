package com.erickoeckel.tasktimer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;

public class TasksViewModel extends ViewModel {
    private static final String TAG = "TasksVM";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private final MutableLiveData<List<Task>> tasks = new MutableLiveData<>(new ArrayList<>());
    private ListenerRegistration reg;

    private CollectionReference userTasksRef() {
        if (auth.getCurrentUser() == null) throw new IllegalStateException("No signed-in user");
        return db.collection("users")
                .document(auth.getCurrentUser().getUid())
                .collection("tasks");
    }

    public LiveData<List<Task>> getTasks() {
        if (reg == null) {
            reg = userTasksRef().addSnapshotListener((QuerySnapshot snap, FirebaseFirestoreException e) -> {
                if (e != null || snap == null) {
                    Log.e(TAG, "listen failed", e);
                    return;
                }
                applySnapshot(snap);
            });
        }
        return tasks;
    }

    private void applySnapshot(@NonNull QuerySnapshot snap) {
        List<Task> list = new ArrayList<>();
        for (DocumentSnapshot d : snap.getDocuments()) {
            Task t = d.toObject(Task.class);
            if (t != null) {
                if (t.getId() == null || t.getId().isEmpty()) t.setId(d.getId());
                list.add(t);
            }
        }
        tasks.setValue(list);
    }

    public void addTask(@NonNull Task t) {
        userTasksRef().document(t.getId())
                .set(t, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(v -> Log.d(TAG, "addTask OK"))
                .addOnFailureListener(e -> Log.e(TAG, "addTask FAILED", e));
    }

    public com.google.android.gms.tasks.Task<Void> completeTask(@NonNull String id) {
        return userTasksRef().document(id).update("done", true);
    }

    public com.google.android.gms.tasks.Task<Void> createQuickTask(
            @NonNull String title,
            @Nullable String dueYmd,
            @Nullable Integer priority
    ) {

        String uid = (FirebaseAuth.getInstance().getCurrentUser() != null)
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "local";

        java.util.Map<String, Object> doc = new java.util.HashMap<>();
        doc.put("title", title.trim());
        doc.put("done", false);
        doc.put("createdAt", com.google.firebase.Timestamp.now());
        if (dueYmd != null && !dueYmd.isEmpty()) doc.put("dueDate", dueYmd);
        if (priority != null) doc.put("priority", priority);

        return db.collection("users").document(uid)
                .collection("tasks").document()
                .set(doc)
                .addOnFailureListener(e -> android.util.Log.e("TasksVM", "createQuickTask failed", e));
    }

    @Override
    protected void onCleared() {
        if (reg != null) { reg.remove(); reg = null; }
    }
}
