package com.erickoeckel.tasktimer;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.*;

public class HabitsViewModel extends ViewModel {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String uid = (FirebaseAuth.getInstance().getCurrentUser() != null)
            ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "local";

    private final MutableLiveData<List<Habit>> _habits = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<Habit>> getHabits() { return _habits; }

    public HabitsViewModel() { load(); }

    // ---------- loading ----------

    public void load() {
        db.collection("users").document(uid).collection("habits")
                .get()
                .addOnSuccessListener(this::applySnapshot);
    }

    private void applySnapshot(QuerySnapshot qs) {
        List<Habit> list = new ArrayList<>();
        for (DocumentSnapshot d : qs.getDocuments()) {
            list.add(habitFrom(d));
        }
        _habits.setValue(list);
    }

    /** Called by UI; we only handle checked=true. */
    public void toggleToday(@NonNull String habitId, boolean checked) {
        if (!checked) return;
        completeToday(habitId);
    }
    public com.google.android.gms.tasks.Task<Void> addHabit(@NonNull Habit h) {
        // Persist to Firestore
        Map<String, Object> doc = new HashMap<>();
        doc.put("name",        h.getTitle());
        doc.put("days",        h.getDays() == null ? java.util.Collections.emptyList() : h.getDays());
        doc.put("streak",      0);
        doc.put("bestStreak",  0);
        doc.put("lastDone",    null);

        return db.collection("users").document(uid)
                .collection("habits").document(h.getId())
                .set(doc)
                .continueWithTask(t -> {
                    // Refresh local list
                    return db.collection("users").document(uid)
                            .collection("habits").get()
                            .addOnSuccessListener(this::applySnapshot)
                            .continueWith(tt -> null);
                });
    }

    public Task<Void> completeToday(@NonNull String habitId) {
        final String today = today();
        final DocumentReference doc = db.collection("users").document(uid)
                .collection("habits").document(habitId);

        // Do a small transaction to compute streak/bestStreak correctly
        return db.runTransaction(trx -> {
            DocumentSnapshot snap = trx.get(doc);
            int prevStreak   = intOf(snap.get("streak"));
            int prevBest     = intOf(snap.get("bestStreak"));
            String lastDone  = safe((String) snap.get("lastDone"), null);

            int newStreak;
            if (today.equals(lastDone)) {
                newStreak = prevStreak;           // already counted today
            } else if (yesterday().equals(lastDone)) {
                newStreak = Math.max(1, prevStreak + 1);
            } else {
                newStreak = 1;                    // reset
            }
            int newBest = Math.max(prevBest, newStreak);

            Map<String, Object> up = new HashMap<>();
            up.put("lastDone", today);
            up.put("streak", newStreak);
            up.put("bestStreak", newBest);
            up.put("updatedAt", FieldValue.serverTimestamp());
            trx.update(doc, up);

            // also write a completion marker for the ribbon UI
            DocumentReference comp = doc.collection("completions").document(today);
            Map<String, Object> payload = new HashMap<>();
            payload.put("ts", FieldValue.serverTimestamp());
            trx.set(comp, payload);

            return null;
        }).continueWithTask(t -> {
            // refresh the local cache
            return db.collection("users").document(uid)
                    .collection("habits").get()
                    .addOnSuccessListener(this::applySnapshot)
                    .continueWith(tt -> null);
        });
    }
    // ➊ Add inside HabitsViewModel (top-level members stay as you have them)

    public static final class HabitCompletedInfo {
        public final String habitTitle;
        public final int streak;
        public final int habitsLeft; // scheduled for today, still not logged
        HabitCompletedInfo(String habitTitle, int streak, int habitsLeft) {
            this.habitTitle = habitTitle;
            this.streak = streak;
            this.habitsLeft = habitsLeft;
        }
    }

    // ➋ Make sure habitFrom() also reads days so we can compute "left today"
    @SuppressWarnings("unchecked")
    private static Habit habitFrom(DocumentSnapshot d) {
        Habit h = new Habit();
        h.setId(d.getId());
        h.setTitle(safe((String) d.get("name"), "Habit"));
        h.setStreak(intOf(d.get("streak")));
        h.setBestStreak(intOf(d.get("bestStreak")));
        h.setLastCompleted(safe((String) d.get("lastDone"), null));
        // NEW: keep schedule (Sun..Sat booleans)
        Object daysObj = d.get("days");
        if (daysObj instanceof java.util.List) {
            h.setDays((java.util.List<Boolean>) daysObj);
        }
        return h;
    }

    // ➌ Helper to count only scheduled-left-today
    private static int countScheduledLeftToday(java.util.List<Habit> list, String today) {
        if (list == null) return 0;
        int left = 0;
        for (Habit h : list) {
            if (Habit.isActiveToday(h.getDays()) && !today.equals(h.getLastCompleted())) left++;
        }
        return left;
    }

    // ➍ NEW: completes today, refreshes, and returns fresh info for notifications
    public com.google.android.gms.tasks.Task<HabitCompletedInfo> completeTodayWithInfo(@NonNull String habitId) {
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());
        java.util.Map<String, Object> up = new java.util.HashMap<>();
        up.put("lastDone", today);
        up.put("streak", com.google.firebase.firestore.FieldValue.increment(1));

        return db.collection("users").document(uid)
                .collection("habits").document(habitId)
                .update(up)
                .continueWithTask(t -> db.collection("users").document(uid).collection("habits").get())
                .continueWith(task -> {
                    com.google.firebase.firestore.QuerySnapshot qs = task.getResult();
                    java.util.List<Habit> list = new java.util.ArrayList<>();
                    for (DocumentSnapshot d : qs.getDocuments()) list.add(habitFrom(d));
                    // push fresh list
                    _habits.postValue(list);

                    // find updated habit
                    Habit target = null;
                    for (Habit h : list) if (habitId.equals(h.getId())) { target = h; break; }

                    int streak = (target != null) ? target.getStreak() : 1;
                    String title = (target != null) ? target.getTitle() : "habit";
                    int left = countScheduledLeftToday(list, today); // AFTER completion

                    return new HabitCompletedInfo(title, streak, left);
                });
    }


    // ---------- utils ----------

    private static int intOf(Object o) { return (o instanceof Number) ? ((Number) o).intValue() : 0; }
    private static String safe(String s, String d) { return (s == null || s.isEmpty()) ? d : s; }

    private static String today()     { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()); }
    private static String yesterday() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, -1);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.getTime());
    }
}
