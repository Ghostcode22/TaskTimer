package com.erickoeckel.tasktimer;

import androidx.annotation.NonNull;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;

public final class Rewards {
    public static final long XP_PER_TASK_DONE    = 15;
    public static final long COINS_PER_TASK_DONE = 5;
    public static final long XP_PER_FOCUS        = 25;
    public static final long COINS_PER_FOCUS     = 10;
    public static final long XP_PER_HABIT_CHECK    = 10;
    public static final long COINS_PER_HABIT_CHECK = 3;

    private Rewards() {}

    public static int levelForXp(long xp) {
        int level = 1;
        while (xp >= xpForLevel(level)) level++;
        return Math.max(1, level - 1);
    }
    public static long xpForLevel(int level) {
        return Math.round(100.0 * Math.pow(level, 1.5));
    }

    @NonNull
    private static String uidOrThrow() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null)
            throw new IllegalStateException("No signed-in user.");
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    private static Task<Void> apply(FirebaseFirestore db, long xpInc, long coinInc) {
        String uid = uidOrThrow();
        Map<String, Object> updates = new HashMap<>();
        updates.put("xp", FieldValue.increment(xpInc));
        updates.put("coins", FieldValue.increment(coinInc));
        updates.put("updatedAt", FieldValue.serverTimestamp());
        return db.collection("users").document(uid).set(updates, SetOptions.merge());
    }

    public static Task<Void> awardFocusSession(FirebaseFirestore db) {
        return apply(db, XP_PER_FOCUS, COINS_PER_FOCUS);
    }

    public static Task<Void> awardTaskCompleted(FirebaseFirestore db) {
        return apply(db, XP_PER_TASK_DONE, COINS_PER_TASK_DONE);
    }

    public static com.google.android.gms.tasks.Task<Void> awardHabitCheck(FirebaseFirestore db) {
        return apply(db, XP_PER_HABIT_CHECK, COINS_PER_HABIT_CHECK);
    }
}

