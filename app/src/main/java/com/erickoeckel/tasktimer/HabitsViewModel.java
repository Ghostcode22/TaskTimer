package com.erickoeckel.tasktimer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import java.text.SimpleDateFormat;
import java.util.*;
import android.content.Context;

public class HabitsViewModel extends ViewModel {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final MutableLiveData<List<Habit>> habits = new MutableLiveData<>(new ArrayList<>());
    private ListenerRegistration reg;

    private String uid() {
        if (auth.getCurrentUser() == null) throw new IllegalStateException("No signed-in user");
        return auth.getCurrentUser().getUid();
    }

    private CollectionReference habitsRef() {
        return db.collection("users").document(uid()).collection("habits");
    }

    public LiveData<List<Habit>> getHabits() {
        if (reg == null) {
            reg = habitsRef()
                    .addSnapshotListener((snap, e) -> {
                        if (e != null) {
                            Log.e("HabitsVM", "listen failed", e);
                            return;
                        }
                        if (snap == null) return;
                        List<Habit> list = new ArrayList<>();
                        for (DocumentSnapshot d : snap.getDocuments()) {
                            Habit h = d.toObject(Habit.class);
                            if (h != null) {
                                if (h.getId() == null) h.setId(d.getId());
                                list.add(h);
                            }
                        }
                        habits.setValue(list);
                    });

        }
        return habits;
    }

    public void addHabit(Habit h) {
        habitsRef().document(h.getId()).set(h, SetOptions.merge())
                .addOnSuccessListener(v -> android.util.Log.d("HabitsVM", "addHabit OK: " + h.getId()))
                .addOnFailureListener(e -> android.util.Log.e("HabitsVM", "addHabit FAILED", e));
    }

    public void toggleToday(Context ctx, String habitId, boolean checked) {
        final String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        final DocumentReference habitDoc = habitsRef().document(habitId);

        if (!checked) return;

        db.runTransaction(trx -> {
            DocumentSnapshot hSnap = trx.get(habitDoc);
            if (!hSnap.exists()) return null;

            String last = hSnap.getString("lastCompleted");
            if (today.equals(last)) return null;

            Integer streak = (hSnap.getLong("streak") == null) ? 0 : hSnap.getLong("streak").intValue();
            List<Boolean> days = (List<Boolean>) hSnap.get("days");
            if (!Habit.isActiveToday(days)) return null;

            String prevScheduled = previousActiveDate(days, today);
            int newStreak = (prevScheduled != null && prevScheduled.equals(last)) ? streak + 1 : 1;

            Map<String,Object> upd = new HashMap<>();
            upd.put("streak", newStreak);
            upd.put("lastCompleted", today);
            trx.set(habitDoc, upd, SetOptions.merge());
            return null;
        }).addOnSuccessListener(unused -> {
                    Rewards.awardHabitCheck(db)
                            .addOnSuccessListener(v -> Notify.habitCompleted(ctx.getApplicationContext()));
        }).addOnFailureListener(e ->
                android.util.Log.e("HabitsVM","toggleToday failed", e)
        );

        habitDoc.collection("completions").document(today)
                .set(new HashMap<String,Object>() {{ put("completed", true); }},
                        SetOptions.merge());
    }

    private static String previousActiveDate(java.util.List<Boolean> days, String yyyyMmDd) {
        if (days == null || days.size() < 7) return null;
        try {
            java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(f.parse(yyyyMmDd));
            for (int i = 1; i <= 7; i++) {
                cal.add(java.util.Calendar.DATE, -1);
                int dow = cal.get(java.util.Calendar.DAY_OF_WEEK); // 1..7
                int idx = (dow == java.util.Calendar.SUNDAY) ? 0 : (dow - 1);
                Boolean active = days.get(idx);
                if (active != null && active) return f.format(cal.getTime());
            }
        } catch (Exception ignore) {}
        return null;
    }

    @Override protected void onCleared() {
        if (reg != null) { reg.remove(); reg = null; }
    }
}