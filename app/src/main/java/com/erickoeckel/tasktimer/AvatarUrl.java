package com.erickoeckel.tasktimer;

import android.net.Uri;
import android.util.Log;

final class AvatarUrl {
    private static final String TAG  = "Avatar";
    private static final String BASE = "https://avataaars.io/"; // <- avataaars.io root

    static String build(AvatarConfig c) {
        if (c == null) c = new AvatarConfig();
        log("---- BUILD START ----");
        log("cfg=" + c.toMap());

        // avatarStyle: Circle (background) or Transparent (no background)
        String avatarStyle = c.background ? "Circle" : "Transparent";

        Uri.Builder b = Uri.parse(BASE).buildUpon();
        b.appendQueryParameter("avatarStyle", avatarStyle);

        // --- TOP / HAIR / HAT ---
        // For Avataaars, your cfg.top should already be in PascalCase (e.g., LongHairBigHair, ShortHairShortRound, NoHair, Hat, Hijab, Turban, etc.)
        if (nz(c.top)) {
            b.appendQueryParameter("topType", c.top);
        }

        // Hair color: MUST be one of the Avataaars named enums (NOT hex)
        // Valid hairColor names: Auburn, Black, Blonde, BlondeGolden, Brown, BrownDark, PastelPink, Platinum, Red, SilverGray
        // Only include hairColor if it’s a hair style (not NoHair, Hat/Hijab/Turban/WinterHat)
        if (isHairTop(c.top) && nz(c.hairColor)) {
            b.appendQueryParameter("hairColor", c.hairColor);
        }

        // --- ACCESSORIES ---
        // Valid: Blank, Kurt, Prescription01, Prescription02, Round, Sunglasses, Wayfarers
        String accessories = (c.accessoriesOn && nz(c.accessories)) ? c.accessories : "Blank";
        b.appendQueryParameter("accessoriesType", accessories);

        // --- CLOTHES ---
        // clotheType: BlazerShirt, BlazerSweater, GraphicShirt, Hoodie, Overall, ShirtCrewNeck, ShirtScoopNeck, ShirtVNeck
        if (nz(c.clothing)) {
            b.appendQueryParameter("clotheType", c.clothing);
            // clotheColor (Avataaars *names*, not hex) — but NOT for Blazer types
            if (!isBlazer(c.clothing) && nz(c.clothesColor)) {
                b.appendQueryParameter("clotheColor", c.clothesColor);
            }
            // graphicType only if GraphicShirt
            if ("GraphicShirt".equals(c.clothing) && nz(c.clothingGraphic)) {
                b.appendQueryParameter("graphicType", c.clothingGraphic);
            }
        }

        // --- FACE ---
        if (nz(c.eyes))     b.appendQueryParameter("eyeType", c.eyes);
        if (nz(c.eyebrows)) b.appendQueryParameter("eyebrowType", c.eyebrows);
        if (nz(c.mouth))    b.appendQueryParameter("mouthType", c.mouth);

        // --- FACIAL HAIR ---
        // facialHairType: Blank when off; otherwise one of the valid types
        String facialType = (c.facialHairOn && nz(c.facialHair)) ? c.facialHair : "Blank";
        b.appendQueryParameter("facialHairType", facialType);
        // facialHairColor uses same NAMED palette as hair (NOT hex)
        if (!"Blank".equals(facialType) && nz(c.facialHairColor)) {
            b.appendQueryParameter("facialHairColor", c.facialHairColor);
        }

        // --- SKIN ---
        // Valid skinColor names: Tanned, Yellow, Pale, Light, Brown, DarkBrown, Black
        if (nz(c.skinColor)) {
            b.appendQueryParameter("skinColor", c.skinColor);
        }

        String url = b.build().toString();
        Log.d(TAG, "URL vAvataaars = " + url);
        log("---- BUILD END ----");
        return url;
    }

    private static void log(String s) { Log.d(TAG, "[BUILD] " + s); }
    private static boolean nz(String s) { return s != null && !s.isEmpty(); }

    static boolean isHairTop(String top) {
        if (top == null) return false;
        return top.startsWith("LongHair") || top.startsWith("ShortHair");
    }

    /** Blazers don’t accept clotheColor in Avataaars. */
    static boolean isBlazer(String clotheType) {
        if (clotheType == null) return false;
        return clotheType.startsWith("Blazer");
    }
}
