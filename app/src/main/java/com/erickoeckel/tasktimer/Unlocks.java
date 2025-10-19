package com.erickoeckel.tasktimer;

import java.util.*;

public final class Unlocks {

    private Unlocks() {}

    public static final String HAIR_BIG     = "LongHairBigHair";
    public static final String HAIR_FROBAND = "LongHairFroBand";

    public static final String CLOTHES_HOODIE  = "Hoodie";
    public static final String CLOTHES_OVERALL = "Overall";

    public static final String FACIAL_MAJESTIC = "BeardMajestic";
    public static final String FACIAL_FANCY    = "MoustacheFancy";

    public static final String EYES_HEARTS = "Hearts";
    public static final String EYES_DIZZY  = "Dizzy";

    public static final String MOUTH_TWINKLE = "Twinkle";
    public static final String MOUTH_TONGUE  = "Tongue";

    private static final Set<String> LOCKED_HAIR    = setOf(HAIR_BIG, HAIR_FROBAND);
    private static final Set<String> LOCKED_CLOTHES = setOf(CLOTHES_HOODIE, CLOTHES_OVERALL);
    private static final Set<String> LOCKED_FACIAL  = setOf(FACIAL_MAJESTIC, FACIAL_FANCY);
    private static final Set<String> LOCKED_EYES    = setOf(EYES_HEARTS, EYES_DIZZY);
    private static final Set<String> LOCKED_MOUTHS  = setOf(MOUTH_TWINKLE, MOUTH_TONGUE);

    private static <T> Set<T> setOf(T... a) { return new HashSet<>(Arrays.asList(a)); }
    private static boolean has(Map<String, Boolean> m, String k) {
        return m != null && Boolean.TRUE.equals(m.get(k));
    }

    public static boolean hairUnlocked(Map<String, Boolean> u, String slug) {
        if (slug == null) return false;
        if (isHeadCover(slug) || isHatTop(slug) || "NoHair".equals(slug)) return true;
        return !LOCKED_HAIR.contains(slug) || has(u, slug);
    }

    public static boolean glassesUnlocked(Map<String, Boolean> u, String slug) {
        return true;
    }

    public static boolean clothesUnlocked(Map<String, Boolean> u, String slug) {
        return slug != null && (!LOCKED_CLOTHES.contains(slug) || has(u, slug));
    }

    public static boolean graphicUnlocked(Map<String, Boolean> u, String slug) { return true; }
    public static boolean hairColorUnlocked(Map<String, Boolean> u, String color) { return true; }
    public static boolean facialHairColorUnlocked(Map<String, Boolean> u, String color) { return true; }
    public static boolean clothesColorUnlocked(Map<String, Boolean> u, String color) { return true; }
    public static boolean skinUnlocked(Map<String, Boolean> u, String skin) { return true; }

    public static boolean eyesUnlocked(Map<String, Boolean> u, String slug) {
        return slug != null && (!LOCKED_EYES.contains(slug) || has(u, slug));
    }

    public static boolean mouthUnlocked(Map<String, Boolean> u, String slug) {
        return slug != null && (!LOCKED_MOUTHS.contains(slug) || has(u, slug));
    }

    public static boolean facialHairUnlocked(Map<String, Boolean> u, String slug) {
        if ("Blank".equals(slug)) return true;
        return slug != null && (!LOCKED_FACIAL.contains(slug) || has(u, slug));
    }

    public static boolean isHatTop(String topType) {
        return "Hat".equals(topType) || (topType != null && topType.startsWith("WinterHat"));
    }
    public static boolean isHeadCover(String topType) {
        return "Hijab".equals(topType) || "Turban".equals(topType);
    }
}
