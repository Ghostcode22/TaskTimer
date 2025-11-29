package com.erickoeckel.tasktimer;

import android.content.Context;
import android.content.SharedPreferences;

public final class Onboarding {
    private Onboarding() {}
    private static SharedPreferences p(Context c){ return c.getSharedPreferences("onboarding", Context.MODE_PRIVATE); }
    public static boolean shouldShow(Context c, String key){ return !p(c).getBoolean(key, false); }
    public static void markShown(Context c, String key){ p(c).edit().putBoolean(key, true).apply(); }
}
