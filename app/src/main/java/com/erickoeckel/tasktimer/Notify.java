package com.erickoeckel.tasktimer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import java.util.Random;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;


public final class Notify {
    private Notify() {}

    public static final String CH_REWARDS   = "rewards";
    public static final String CH_REMINDERS = "reminders";
    public static final String CH_MISSED    = "missed";
    public static final String CH_SOCIAL = "social";

    private static final String[] MSG_FRIEND_REQ = {
            "sent you a friend request.",
            "wants to connect with you!",
            "would like to be friends."
    };

    private static final String[] MSG_FRIEND_ACCEPT = {
            "accepted your friend request!",
            "is now your friend 🎉",
            "and you are connected!"
    };

    private static final Random R = new Random();

    public static void createChannels(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);

        NotificationChannel rewards = new NotificationChannel(
                CH_REWARDS, "Achievements", NotificationManager.IMPORTANCE_DEFAULT);
        rewards.setDescription("Notifications when you complete tasks and habits.");
        rewards.enableLights(true); rewards.setLightColor(Color.parseColor("#D4AF37"));

        NotificationChannel reminders = new NotificationChannel(
                CH_REMINDERS, "Reminders", NotificationManager.IMPORTANCE_DEFAULT);
        reminders.setDescription("Daily nudges for due habits and tasks.");

        NotificationChannel missed = new NotificationChannel(
                CH_MISSED, "Missed", NotificationManager.IMPORTANCE_DEFAULT);
        missed.setDescription("Encouragement when you miss scheduled items.");

        NotificationChannel social = new NotificationChannel(
                CH_SOCIAL, "Social", NotificationManager.IMPORTANCE_DEFAULT);
        social.setDescription("Friend request notifications.");

        nm.createNotificationChannel(rewards);
        nm.createNotificationChannel(reminders);
        nm.createNotificationChannel(missed);
        nm.createNotificationChannel(social);
    }

    public static void friendRequest(Context ctx, @Nullable String fromLabel) {
        String tail = MSG_FRIEND_REQ[new java.util.Random().nextInt(MSG_FRIEND_REQ.length)];
        String text = (fromLabel == null ? "Someone " : (fromLabel + " ")) + tail;
        push(ctx, (int) System.currentTimeMillis(),
                base(ctx, CH_SOCIAL, "New friend request", text));
    }

    public static void friendAccepted(Context ctx, @Nullable String who) {
        String tail = MSG_FRIEND_ACCEPT[new java.util.Random().nextInt(MSG_FRIEND_ACCEPT.length)];
        String text = (who == null ? "They " : (who + " ")) + tail;
        push(ctx, (int) System.currentTimeMillis(),
                base(ctx, CH_SOCIAL, "Friend request accepted", text));
    }

    private static NotificationCompat.Builder base(Context ctx, String channelId, String title, String text) {
        return new NotificationCompat.Builder(ctx, channelId)
                .setSmallIcon(com.erickoeckel.tasktimer.R.drawable.ic_stat_check)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setColor(Color.parseColor("#0A2740"))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

    }

    private static void push(Context ctx, int id, NotificationCompat.Builder b) {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        NotificationManagerCompat.from(ctx.getApplicationContext()).notify(id, b.build());
    }
}
