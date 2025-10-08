package com.erickoeckel.tasktimer;

import androidx.annotation.Nullable;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.HashMap;
import java.util.Map;

public class AvatarConfig {
    public String seed = "tasktimer";
    public String hair = "shortRound";
    public String hairColor = "2f2f2f";
    public String eyes = "default";
    public String glasses = "blank";
    public boolean background = false;
    public String backgroundColor = "0a2740";
    public String skinColor  = "f2dbb1";
    public String clothes = "shirtCrewNeck";
    public String clothesColor = "3d85c6";

    public AvatarConfig() {}

    public static AvatarConfig from(@Nullable DocumentSnapshot snap) {
        AvatarConfig c = new AvatarConfig();
        if (snap != null && snap.exists()) {
            Map<String, Object> m = (Map<String, Object>) snap.get("avatarConfig");
            if (m != null) {
                c.seed          = get(m, "seed", c.seed);
                c.hair          = get(m, "hair", c.hair);
                c.hairColor     = get(m, "hairColor", c.hairColor);
                c.eyes          = get(m, "eyes", c.eyes);
                c.glasses       = get(m, "glasses", c.glasses);
                c.clothes       = get(m, "clothes", c.clothes);
                c.clothesColor  = get(m, "clothesColor", c.clothesColor);
                c.background    = getBool(m, "background", c.background);
                c.backgroundColor = get(m, "backgroundColor", c.backgroundColor);

                String legacySkin = get(m, "skin", null);
                c.skinColor = get(m, "skinColor", (legacySkin != null ? legacySkin : c.skinColor));
            }
        }
        return c;
    }

    private static String get(Map<String,Object> m, String k, String def) {
        Object v = m.get(k);
        return v instanceof String ? (String) v : def;
    }

    private static boolean getBool(Map<String,Object> m, String k, boolean def) {
        Object v = m.get(k);
        return v instanceof Boolean ? (Boolean) v : def;
    }

    public Map<String,Object> toMap() {
        Map<String,Object> out = new HashMap<>();
        out.put("seed", seed);
        out.put("hair", hair);
        out.put("hairColor", hairColor);
        out.put("skinColor", skinColor);
        out.put("eyes", eyes);
        out.put("glasses", glasses);
        out.put("clothes", clothes);
        out.put("clothesColor", clothesColor);
        out.put("background", background);
        out.put("backgroundColor", backgroundColor);
        return out;
    }
}
