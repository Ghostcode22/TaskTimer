package com.erickoeckel.tasktimer;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.*;

public class DailyCheckWorker extends Worker {

    public DailyCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull @Override
    public Result doWork() {
        try {
            var user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return Result.success();

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String uid = user.getUid();
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            String yesterday = prevDate(today);

            var habitsSnap = Tasks.await(db.collection("users").document(uid)
                    .collection("habits").get());

            int dueCount = 0;
            for (DocumentSnapshot d : habitsSnap.getDocuments()) {
                List<Boolean> days = (List<Boolean>) d.get("days");
                String lastCompleted = d.getString("lastCompleted");
                if (isActiveToday(days) && !today.equals(lastCompleted)) {
                    dueCount++;
                }
            }
            if (dueCount > 0) {
                AiCoach.generateAndNotify(
                        getApplicationContext(),
                        "HABITS_DUE",
                        null,
                        Notify.CH_REMINDERS,
                        dueCount + " habit" + (dueCount==1?"":"s") + " due"
                );
            }else {
            AiCoach.generateAndNotify(
                    getApplicationContext(),
                    "TASK_MISSED",
                    null,
                    Notify.CH_REMINDERS,
                    "Missed task"
            );}


            var tasksSnap = Tasks.await(db.collection("users").document(uid)
                    .collection("tasks").get());

            for (DocumentSnapshot d : tasksSnap.getDocuments()) {
                Boolean done = d.getBoolean("done");
                String due = d.getString("dueDate");
                if (done != null && done) continue;
                if (due == null || due.isEmpty()) continue;
                if (due.compareTo(yesterday) <= 0) {
                    String title = d.getString("title");
                    java.util.Map<String, Object> extra2 = new java.util.HashMap<>();
                    extra2.put("title", title);

                    AiCoach.generateAndNotify(
                            getApplicationContext(),
                            "TASK_MISSED",
                            extra2,
                            Notify.CH_MISSED,
                            "Coach nudge"
                    );

                }
            }

            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }

    private static boolean isActiveToday(List<Boolean> days) {
        if (days == null || days.size() < 7) return true;
        Calendar c = Calendar.getInstance();
        int dow = c.get(Calendar.DAY_OF_WEEK);
        int idx = (dow == Calendar.SUNDAY) ? 0 : (dow - 1);
        Boolean b = days.get(idx);
        return b != null && b;
    }

    private static String prevDate(String yyyyMmDd) {
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Calendar cal = Calendar.getInstance();
            cal.setTime(f.parse(yyyyMmDd));
            cal.add(Calendar.DATE, -1);
            return f.format(cal.getTime());
        } catch (Exception e) { return yyyyMmDd; }
    }
}
