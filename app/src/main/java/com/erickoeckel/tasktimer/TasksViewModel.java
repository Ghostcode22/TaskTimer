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

    private final String uid = (FirebaseAuth.getInstance().getCurrentUser() != null)
            ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "local";

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

        java.util.Collections.sort(list, (a, b) -> {
            boolean aDone = isDone(a);
            boolean bDone = isDone(b);
            if (aDone != bDone) return aDone ? 1 : -1;

            if (!aDone) {
                long ad = dueKey(a), bd = dueKey(b);
                boolean aHasDue = ad != Long.MAX_VALUE;
                boolean bHasDue = bd != Long.MAX_VALUE;
                if (aHasDue && bHasDue) {
                    return Long.compare(ad, bd);
                } else if (aHasDue != bHasDue) {
                    return aHasDue ? -1 : 1;
                } else {
                    long ac = createdKey(a), bc = createdKey(b);
                    return Long.compare(ac, bc);
                }
            } else {
                long ac = completedAtKey(a), bc = completedAtKey(b);
                if (ac != bc) return Long.compare(bc, ac);
                long ac2 = createdKey(a), bc2 = createdKey(b);
                return Long.compare(bc2, ac2);
            }
        });

        tasks.setValue(list);
    }

    @androidx.annotation.Nullable
    private static Object callGetter(Object bean, String... methodNames) {
        for (String name : methodNames) {
            try {
                java.lang.reflect.Method m = bean.getClass().getMethod(name);
                Object v = m.invoke(bean);
                if (v != null) return v;
            } catch (Exception ignore) {}
        }
        return null;
    }

    private static long toMillis(Object v) {
        if (v == null) return -1L;
        if (v instanceof com.google.firebase.Timestamp) {
            return ((com.google.firebase.Timestamp) v).toDate().getTime();
        }
        if (v instanceof java.util.Date) {
            return ((java.util.Date) v).getTime();
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        if (v instanceof String) {
            String s = (String) v;
            try {
                java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
                f.setLenient(false);
                return f.parse(s).getTime();
            } catch (Exception ignored) {}
            try {
                return java.time.OffsetDateTime.parse(s).toInstant().toEpochMilli();
            } catch (Exception ignored) {}
            try {
                return Long.parseLong(s.trim());
            } catch (Exception ignored) {}
        }
        return -1L;
    }

    private static long dueKey(Task t) {
        Object raw = callGetter(t, "getDueAt", "getDueDate", "getDue");
        long ms = toMillis(raw);
        return (ms > 0) ? ms : Long.MAX_VALUE;
    }

    private static long createdKey(Task t) {
        Object raw = callGetter(t, "getCreatedAt", "getCreated", "getAddedAt");
        long ms = toMillis(raw);
        return (ms > 0) ? ms : Long.MAX_VALUE;
    }

    private static long completedAtKey(Task t) {
        Object raw = callGetter(t, "getCompletedAt", "getDoneAt", "getFinishedAt", "getCompletedOn");
        long ms = toMillis(raw);
        return (ms > 0) ? ms : -1L;
    }

    private static boolean isDone(Task t) {
        Object v = callGetter(t, "isDone", "getDone", "isCompleted", "getCompleted");
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof Number)  return ((Number) v).intValue() != 0;
        if (v instanceof String)  return "true".equalsIgnoreCase((String) v) || "1".equals(v);
        return false;
    }

    public void addTask(@NonNull Task t) {
        userTasksRef().document(t.getId())
                .set(t, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(v -> Log.d(TAG, "addTask OK"))
                .addOnFailureListener(e -> Log.e(TAG, "addTask FAILED", e));
    }

    public com.google.android.gms.tasks.Task<Void> completeTask(@NonNull String id) {
        Rewards.awardTaskCompleted(db);
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
        doc.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
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

    public com.google.android.gms.tasks.Task<Void> deleteTask(@androidx.annotation.NonNull String id) {
        return db.collection("users").document(uid)
                .collection("tasks").document(id)
                .delete()
                .continueWithTask(t -> db.collection("users").document(uid)
                        .collection("tasks").get()
                        .addOnSuccessListener(this::applySnapshot)
                        .continueWith(tt -> null));
    }

}
