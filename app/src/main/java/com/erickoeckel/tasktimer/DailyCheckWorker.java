package com.erickoeckel.tasktimer;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class DailyCheckWorker extends Worker {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String uid;

    public DailyCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.uid = (FirebaseAuth.getInstance().getCurrentUser() != null)
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
    }

    @NonNull @Override
    public Result doWork() {
        if (uid == null) return Result.success();

        try {
            int dueHabits = countHabitsDueToday();
            if (dueHabits > 0) {
                Map<String, Object> extra = new HashMap<>();
                extra.put("dueCount", dueHabits);
                AiCoach.generateAndNotify(
                        getApplicationContext(),
                        AiCoach.EVENT_HABITS_DUE,
                        extra,
                        Notify.CH_REMINDERS,
                        (dueHabits == 1) ? "1 habit due" : (dueHabits + " habits due")
                );
            }

            for (Map<String, Object> ex : getOverdueTasks()) {
                AiCoach.generateAndNotify(
                        getApplicationContext(),
                        AiCoach.EVENT_TASK_MISSED,
                        ex,
                        Notify.CH_MISSED,
                        "Coach nudge"
                );
            }

            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.retry();
        }
    }

    private int countHabitsDueToday() throws Exception {
        QuerySnapshot qs = Tasks.await(
                db.collection("users").document(uid).collection("habits").get()
        );
        String today = ymd(0);
        int count = 0;
        for (DocumentSnapshot d : qs.getDocuments()) {
            String lastDone = asString(d.get("lastDone"));
            @SuppressWarnings("unchecked")
            java.util.List<Boolean> days = (java.util.List<Boolean>) d.get("days");
            boolean activeToday = isActiveToday(days);
            boolean alreadyDone = today.equals(lastDone);
            if (activeToday && !alreadyDone) count++;
        }
        return count;
    }

    private static boolean isActiveToday(java.util.List<Boolean> days) {
        if (days == null || days.size() < 7) return false;
        int dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK); // 1=Sun..7=Sat
        int idx = dow - Calendar.SUNDAY; // 0..6
        Boolean v = (idx >= 0 && idx < days.size()) ? days.get(idx) : Boolean.FALSE;
        return Boolean.TRUE.equals(v);
    }

    private List<Map<String, Object>> getOverdueTasks() throws Exception {
        QuerySnapshot qs = Tasks.await(
                db.collection("users").document(uid).collection("tasks").get()
        );
        String today = ymd(0);

        List<Map<String, Object>> out = new ArrayList<>();
        for (DocumentSnapshot d : qs.getDocuments()) {
            Boolean done = (d.get("done") instanceof Boolean) ? (Boolean) d.get("done") : Boolean.FALSE;
            String due = asString(d.get("dueDate"));
            if (Boolean.TRUE.equals(done)) continue;
            if (due == null || due.isEmpty()) continue;
            if (due.compareTo(today) < 0) {
                Map<String, Object> ex = new HashMap<>();
                ex.put("title", asString(d.get("title")));
                out.add(ex);
            }
        }
        return out;
    }

    private static String ymd(int offsetDays) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, offsetDays);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());
    }
    private static String asString(Object o) { return (o == null) ? null : String.valueOf(o); }
}
