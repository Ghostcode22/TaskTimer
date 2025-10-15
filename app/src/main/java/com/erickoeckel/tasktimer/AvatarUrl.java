package com.erickoeckel.tasktimer;

import android.net.Uri;
import android.util.Log;

final class AvatarUrl {
    private static final String TAG  = "Avatar";
    private static final String BASE = "https://avataaars.io/";

    static String build(AvatarConfig c) {
        if (c == null) c = new AvatarConfig();
        log("---- BUILD START ----");
        log("cfg=" + c.toMap());

        String avatarStyle = c.background ? "Circle" : "Transparent";

        Uri.Builder b = Uri.parse(BASE).buildUpon();
        b.appendQueryParameter("avatarStyle", avatarStyle);

        if (nz(c.top)) {
            b.appendQueryParameter("topType", c.top);
        }

        if (isHairTop(c.top) && nz(c.hairColor)) {
            b.appendQueryParameter("hairColor", c.hairColor);
        }

        String accessories = (c.accessoriesOn && nz(c.accessories)) ? c.accessories : "Blank";
        b.appendQueryParameter("accessoriesType", accessories);

        if (nz(c.clothing)) {
            b.appendQueryParameter("clotheType", c.clothing);
            if (!isBlazer(c.clothing) && nz(c.clothesColor)) {
                b.appendQueryParameter("clotheColor", c.clothesColor);
            }
            if ("GraphicShirt".equals(c.clothing) && nz(c.clothingGraphic)) {
                b.appendQueryParameter("graphicType", c.clothingGraphic);
            }
        }

        if (nz(c.eyes))     b.appendQueryParameter("eyeType", c.eyes);
        if (nz(c.eyebrows)) b.appendQueryParameter("eyebrowType", c.eyebrows);
        if (nz(c.mouth))    b.appendQueryParameter("mouthType", c.mouth);

        String facialType = (c.facialHairOn && nz(c.facialHair)) ? c.facialHair : "Blank";
        b.appendQueryParameter("facialHairType", facialType);
        if (!"Blank".equals(facialType) && nz(c.facialHairColor)) {
            b.appendQueryParameter("facialHairColor", c.facialHairColor);
        }

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

    static boolean isBlazer(String clotheType) {
        if (clotheType == null) return false;
        return clotheType.startsWith("Blazer");
    }
}
