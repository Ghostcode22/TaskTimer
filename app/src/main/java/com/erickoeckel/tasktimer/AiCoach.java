package com.erickoeckel.tasktimer;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public final class AiCoach {

    private AiCoach() {}

    public static final String EVENT_TASK_COMPLETED  = "TASK_COMPLETED";
    public static final String EVENT_HABIT_COMPLETED = "HABIT_COMPLETED";
    public static final String EVENT_TASK_MISSED     = "TASK_MISSED";
    public static final String EVENT_HABITS_DUE      = "HABITS_DUE";

    private static final Random RNG = new Random();

    public static void generateAndNotify(
            @NonNull Context ctx,
            @NonNull String event,
            @Nullable Map<String, Object> extra,
            @NonNull String channelId,
            @NonNull String title
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", event);

        Map<String, Object> x = (extra == null) ? new HashMap<>() : new HashMap<>(extra);
        x.put("locale", Locale.getDefault().toLanguageTag());
        x.put("timeOfDay", timeOfDay());
        payload.put("extra", x);

        FirebaseFunctions.getInstance("us-central1")
                .getHttpsCallable("coachMessage")
                .call(payload)
                .addOnSuccessListener((HttpsCallableResult r) -> {
                    String msg = extractMessage(r);
                    if (TextUtils.isEmpty(msg)) {
                        msg = coachText(event, extra);
                    }
                    showNotification(ctx, channelId, title, msg);
                })
                .addOnFailureListener(e -> {
                    String msg = coachText(event, extra);
                    showNotification(ctx, channelId, title, msg);
                });
    }

    @NonNull
    private static String coachText(@NonNull String event, @Nullable Map<String, Object> extra) {
        Map<String, Object> x = (extra == null) ? new HashMap<>() : extra;

        if (EVENT_TASK_COMPLETED.equals(event)) {
            String task = s(x.get("taskTitle"), "task");
            int left = i(x.get("tasksLeft"), 0);
            String leftPart = (left <= 0) ? "All tasks done—nice finish!"
                    : String.format(Locale.US, "%d %s left today.", left, left == 1 ? "task" : "tasks");
            return pick(
                    "Done with \"" + task + "\"—crushed it! " + leftPart,
                    "Great work on \"" + task + "\". " + leftPart,
                    "Checked off \"" + task + "\". " + leftPart
            );

        } else if (EVENT_HABIT_COMPLETED.equals(event)) {
            String habit = s(x.get("habitTitle"), "habit");
            int streak = i(x.get("streak"), 1);
            int left = i(x.get("habitsLeft"), 0);
            String streakPart = (streak <= 1) ? "Streak started!" :
                    String.format(Locale.US, "%d-day streak 🔥", streak);
            String leftPart = (left <= 0) ? "All habits for today done." :
                    String.format(Locale.US, "%d %s left today.", left, left == 1 ? "habit" : "habits");
            return pick(
                    "Logged \"" + habit + "\" — " + streakPart + " " + leftPart,
                    "Nice! \"" + habit + "\" completed. " + streakPart + " " + leftPart,
                    "Kept it up with \"" + habit + "\". " + streakPart + " " + leftPart
            );

        } else if (EVENT_HABITS_DUE.equals(event)) {
            int due = i(x.get("dueCount"), 0);
            if (due <= 0) return "Nothing due right now—enjoy the win.";
            return String.format(Locale.US, "%d %s scheduled today. You got this 💪",
                    due, due == 1 ? "habit is" : "habits are");

        } else if (EVENT_TASK_MISSED.equals(event)) {
            String t = s(x.get("title"), "A task");
            return pick(
                    t + " slipped by yesterday—want to reschedule?",
                    "Missed: \"" + t + "\". No worries, plan it again and keep rolling.",
                    "Yesterday's \"" + t + "\" is still waiting. Small step to restart."
            );
        }
        return "You're doing great—keep going!";
    }

    private static void showNotification(Context ctx, String channelId, String title, String body) {
        Intent launch = new Intent(ctx, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                ctx, 0, launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(pi);

        Notification n = b.build();

        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            NotificationManagerCompat.from(ctx)
                    .notify((int) (System.currentTimeMillis() & 0xFFFFFFF), n);
        } catch (SecurityException ignored) {}
    }

    private static String extractMessage(HttpsCallableResult r) {
        if (r == null) return null;
        Object data = r.getData();
        if (data instanceof Map) {
            Object m = ((Map<?, ?>) data).get("message");
            return (m != null) ? String.valueOf(m) : null;
        }
        return (data != null) ? String.valueOf(data) : null;
    }

    private static String timeOfDay() {
        int h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (h < 12) return "morning";
        if (h < 17) return "afternoon";
        return "evening";
    }

    private static String pick(String... variants) { return variants[RNG.nextInt(variants.length)]; }
    private static String s(Object o, String d) {
        String v = (o == null) ? null : String.valueOf(o);
        return TextUtils.isEmpty(v) ? d : v;
    }
    private static int i(Object o, int d) {
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception ignore) { return d; }
    }
}
