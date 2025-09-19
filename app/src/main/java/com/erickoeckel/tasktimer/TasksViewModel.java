package com.erickoeckel.tasktimer;

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
                    Log.e("TasksVM", "listen failed", e);
                    return;
                }
                List<Task> list = new ArrayList<>();
                for (DocumentSnapshot d : snap.getDocuments()) {
                    Task t = d.toObject(Task.class);
                    if (t != null) {
                        // ensure id matches doc id
                        if (t.getId() == null || t.getId().isEmpty()) t.setId(d.getId());
                        list.add(t);
                    }
                }
                tasks.setValue(list);
            });
        }
        return tasks;
    }

    public void addTask(Task t) {
        userTasksRef().document(t.getId()).set(t)
                .addOnSuccessListener(v -> Log.d("TasksVM", "addTask ok: " + t.getId()))
                .addOnFailureListener(e -> Log.e("TasksVM", "addTask failed", e));
    }

    public com.google.android.gms.tasks.Task<Void> toggleDone(String id, boolean done) {
        return userTasksRef().document(id).update("done", done);
    }

    @Override
    protected void onCleared() {
        if (reg != null) { reg.remove(); reg = null; }
    }

    public com.google.android.gms.tasks.Task<Void> completeTask(String id) {
        return userTasksRef().document(id).update("done", true);
    }

}

