package com.erickoeckel.tasktimer;

import com.google.firebase.firestore.DocumentSnapshot;
import java.util.HashMap;
import java.util.Map;

public class Avatar {
    public String skinTone = "#F2D3B1";
    public String hairStyle = "short";
    public String hairColor = "#2E2E2E";
    public String eyeColor = "#2E2E2E";
    public String shirtColor = "#D4AF37";
    public String accessory = "none";

    public static Avatar fromUserSnap(DocumentSnapshot user) {
        Avatar a = new Avatar();
        Object obj = user.get("avatar");
        if (obj instanceof Map) {
            Map<?,?> m = (Map<?,?>) obj;
            a.skinTone   = str(m.get("skinTone"), a.skinTone);
            a.hairStyle  = str(m.get("hairStyle"), a.hairStyle);
            a.hairColor  = str(m.get("hairColor"), a.hairColor);
            a.eyeColor   = str(m.get("eyeColor"), a.eyeColor);
            a.shirtColor = str(m.get("shirtColor"), a.shirtColor);
            a.accessory  = str(m.get("accessory"), a.accessory);
        }
        return a;
    }

    public Map<String,Object> toMap() {
        Map<String,Object> m = new HashMap<>();
        m.put("skinTone", skinTone);
        m.put("hairStyle", hairStyle);
        m.put("hairColor", hairColor);
        m.put("eyeColor", eyeColor);
        m.put("shirtColor", shirtColor);
        m.put("accessory", accessory);
        return m;
    }

    private static String str(Object v, String def) { return v == null ? def : String.valueOf(v); }
}
