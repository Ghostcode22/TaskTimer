package com.erickoeckel.tasktimer;

import android.content.Context;
import android.util.Log;
import androidx.annotation.Nullable;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import java.util.HashMap;
import java.util.Map;

public final class AiCoach {
    private AiCoach() {}
    public static void generateAndNotify(Context ctx,
                                         String event,
                                         @Nullable Map<String, Object> extra,
                                         String channelId,
                                         String title) {

        Map<String, Object> payload = new HashMap<>();
        payload.put("event", event);
        if (extra != null) payload.put("extra", extra);

        FirebaseFunctions.getInstance("us-central1")
                .getHttpsCallable("coachMessage")
                .call(payload)
                .addOnSuccessListener((HttpsCallableResult r) -> {
                    String msg = null;
                    Object data = r.getData();
                    if (data instanceof Map) {
                        Object m = ((Map<?, ?>) data).get("message");
                        if (m != null) msg = String.valueOf(m);
                    } else if (data != null) {
                        msg = String.valueOf(data);
                    }
                    if (msg == null || msg.trim().isEmpty()) {
                        msg = "Nice! One more step forward."; // ultra-fallback
                    }
                    Log.d("AiCoach", "success: " + msg);
                    Notify.show(ctx.getApplicationContext(), channelId, title, msg);
                })
                .addOnFailureListener(e -> {
                    Log.e("AiCoach", "failed", e);
                    String fallback =
                            "TASK_MISSED".equals(event) ? "No sweat—start with the tiniest step."
                                    : "HABIT_COMPLETED".equals(event) ? "Great consistency—keep it up!"
                                    : "Nice! One more task down.";
                    Notify.show(ctx.getApplicationContext(), channelId, title, fallback);
                });
    }

    public static com.google.android.gms.tasks.Task<String> ask(
            Context ctx,
            String question,
            @Nullable Map<String, Object> extra) {

        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "ASK");
        payload.put("question", question);
        if (extra != null) payload.put("extra", extra);

        return FirebaseFunctions.getInstance("us-central1")
                .getHttpsCallable("coachMessage")
                .call(payload)
                .continueWith(task -> {
                    Object data = task.getResult() != null ? task.getResult().getData() : null;
                    if (data instanceof Map) {
                        Object m = ((Map<?, ?>) data).get("message");
                        return m != null ? String.valueOf(m) : "";
                    }
                    return data != null ? String.valueOf(data) : "";
                });
    }
}
