package com.erickoeckel.tasktimer;

import android.content.Context;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

public final class ReminderScheduler {
    private ReminderScheduler(){}

    private static final String UNIQUE_NAME = "daily_check";

    public static void scheduleDaily(Context ctx) {
        PeriodicWorkRequest req = new PeriodicWorkRequest.Builder(
                DailyCheckWorker.class, 24, TimeUnit.HOURS)
                .build();

        WorkManager.getInstance(ctx.getApplicationContext())
                .enqueueUniquePeriodicWork(UNIQUE_NAME,
                        ExistingPeriodicWorkPolicy.UPDATE, req);
    }
}
