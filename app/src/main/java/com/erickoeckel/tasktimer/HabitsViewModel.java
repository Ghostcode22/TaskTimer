package com.erickoeckel.tasktimer;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.*;

public class HabitsViewModel extends ViewModel {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String uid = (FirebaseAuth.getInstance().getCurrentUser() != null)
            ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "local";

    private final MutableLiveData<List<Habit>> _habits = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<Habit>> getHabits() { return _habits; }

    public HabitsViewModel() { load(); }

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

    public Task<Void> addHabit(@NonNull Habit h) {
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

    public static final class HabitCompletedInfo {
        public final String habitTitle;
        public final int streak;
        public final int habitsLeft;
        HabitCompletedInfo(String habitTitle, int streak, int habitsLeft) {
            this.habitTitle = habitTitle;
            this.streak = streak;
            this.habitsLeft = habitsLeft;
        }
    }

    @SuppressWarnings("unchecked")
    private static Habit habitFrom(DocumentSnapshot d) {
        Habit h = new Habit();
        h.setId(d.getId());
        h.setTitle(safe((String) d.get("name"), "Habit"));
        h.setStreak(intOf(d.get("streak")));
        h.setBestStreak(intOf(d.get("bestStreak")));
        h.setLastCompleted(safe((String) d.get("lastDone"), null));
        Object daysObj = d.get("days");
        if (daysObj instanceof java.util.List) {
            h.setDays((java.util.List<Boolean>) daysObj);
        }
        return h;
    }

    private static int countScheduledLeftToday(java.util.List<Habit> list, String today) {
        if (list == null) return 0;
        int left = 0;
        for (Habit h : list) {
            if (Habit.isActiveToday(h.getDays()) && !today.equals(h.getLastCompleted())) left++;
        }
        return left;
    }

    public com.google.android.gms.tasks.Task<HabitCompletedInfo> completeTodayWithInfo(@NonNull String habitId) {
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());
        java.util.Map<String, Object> up = new java.util.HashMap<>();
        up.put("lastDone", today);
        up.put("streak", com.google.firebase.firestore.FieldValue.increment(1));
        Rewards.awardHabitCheck(db);

        return db.collection("users").document(uid)
                .collection("habits").document(habitId)
                .update(up)
                .continueWithTask(t -> db.collection("users").document(uid).collection("habits").get())
                .continueWith(task -> {
                    com.google.firebase.firestore.QuerySnapshot qs = task.getResult();
                    java.util.List<Habit> list = new java.util.ArrayList<>();
                    for (DocumentSnapshot d : qs.getDocuments()) list.add(habitFrom(d));

                    _habits.postValue(list);

                    Habit target = null;
                    for (Habit h : list) if (habitId.equals(h.getId())) { target = h; break; }

                    int streak = (target != null) ? target.getStreak() : 1;
                    String title = (target != null) ? target.getTitle() : "habit";
                    int left = countScheduledLeftToday(list, today); // AFTER completion

                    return new HabitCompletedInfo(title, streak, left);
                });
    }

    private static int intOf(Object o) { return (o instanceof Number) ? ((Number) o).intValue() : 0; }
    private static String safe(String s, String d) { return (s == null || s.isEmpty()) ? d : s; }

    public com.google.android.gms.tasks.Task<Void> deleteHabit(@androidx.annotation.NonNull String id) {
        return db.collection("users").document(uid)
                .collection("habits").document(id)
                .delete()
                .continueWithTask(t -> db.collection("users").document(uid)
                        .collection("habits").get()
                        .addOnSuccessListener(this::applySnapshot)
                        .continueWith(tt -> null));
    }

}
