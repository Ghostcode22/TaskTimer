package com.erickoeckel.tasktimer;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AvatarEditorFragment extends Fragment {

    private static final String TAG = "Avatar";

    private static final String[] TOPS = {
            "NoHair","Eyepatch","Hat","Hijab","Turban",
            "LongHairBigHair","LongHairBob","LongHairBun","LongHairCurly","LongHairCurvy","LongHairDreads",
            "LongHairFrida","LongHairFro","LongHairFroBand","LongHairMiaWallace","LongHairNotTooLong",
            "LongHairShavedSides","LongHairStraight","LongHairStraight2","LongHairStraightStrand",
            "ShortHairDreads01","ShortHairDreads02","ShortHairFrizzle","ShortHairShaggyMullet",
            "ShortHairShortCurly","ShortHairShortFlat","ShortHairShortRound","ShortHairShortWaved",
            "ShortHairSides","ShortHairTheCaesar","ShortHairTheCaesarSidePart"
    };

    private static final String[] ACCESSORIES = {
            "Blank","Kurt","Prescription01","Prescription02","Round","Sunglasses","Wayfarers"
    };

    private static final String[] FACIAL_HAIR = {
            "Blank","BeardLight","BeardMajestic","BeardMedium","MoustacheFancy","MoustacheMagnum"
    };

    private static final String[] CLOTHES = {
            "BlazerShirt","BlazerSweater","CollarSweater","GraphicShirt","Hoodie","Overall",
            "ShirtCrewNeck","ShirtScoopNeck","ShirtVNeck"
    };

    private static final String[] GRAPHICS = {
            "Bat","Bear","Cumbia","Deer","Diamond","Hola","Pizza","Resist","Selena","Skull","SkullOutline"
    };

    private static final String[] EYES = {
            "Close","Cry","Default","Dizzy","EyeRoll","Happy","Hearts","Side","Squint","Surprised","Wink","WinkWacky"
    };

    private static final String[] EYEBROWS = {
            "Angry","AngryNatural","Default","DefaultNatural","FlatNatural","RaisedExcited",
            "RaisedExcitedNatural","SadConcerned","SadConcernedNatural","UnibrowNatural","UpDown","UpDownNatural"
    };

    private static final String[] MOUTHS = {
            "Concerned","Default","Disbelief","Eating","Grimace","Sad","ScreamOpen","Serious","Smile","Tongue","Twinkle","Vomit"
    };

    private static final String[] HAIR_COLORS = {
            "Auburn","Black","Blonde","BlondeGolden","Brown","BrownDark","PastelPink","Platinum","Red","SilverGray"
    };

    private static final String[] FACIAL_HAIR_COLORS = {
            "Auburn","Black","Blonde","BlondeGolden","Brown","BrownDark","Platinum","Red"
    };

    private static final String[] CLOTHES_COLORS = {
            "Black","Blue01","Blue02","Blue03","Gray01","Gray02","Heather",
            "PastelBlue","PastelGreen","PastelOrange","PastelRed","PastelYellow","Pink","Red","White"
    };

    private static final String[] SKINS = {
            "Tanned","Yellow","Pale","Light","Brown","DarkBrown","Black"
    };

    private AvatarConfig cfg = new AvatarConfig();
    private ImageView ivPreview;

    private TabLayout tabs;
    private View groupHair, groupClothes, groupAccessories, groupFace;

    private Map<String, Boolean> unlocks = new HashMap<>();
    private ListenerRegistration unlocksReg;

    private MaterialAutoCompleteTextView ddTopType, ddAccessoriesType, ddClotheType, ddClotheColor,
            ddGraphicType, ddHairColor, ddFacialHairType, ddFacialHairColor, ddEyeType, ddEyebrowType,
            ddMouthType, ddSkinColor;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_avatar_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);

        MaterialToolbar toolbar = root.findViewById(R.id.toolbarAvatar);
        if (toolbar != null) toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        tabs = root.findViewById(R.id.tabs);
        if (tabs != null) {
            if (tabs.getTabCount() != 4) {
                tabs.removeAllTabs();
                tabs.addTab(tabs.newTab().setText("Hair"));
                tabs.addTab(tabs.newTab().setText("Clothes"));
                tabs.addTab(tabs.newTab().setText("Accessories"));
                tabs.addTab(tabs.newTab().setText("Face"));
            }
            tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override public void onTabSelected(TabLayout.Tab tab)   { showSection(tab.getPosition()); }
                @Override public void onTabUnselected(TabLayout.Tab tab) { }
                @Override public void onTabReselected(TabLayout.Tab tab) { showSection(tab.getPosition()); }
            });
        }

        ivPreview = root.findViewById(R.id.ivPreview);

        groupHair        = root.findViewById(R.id.groupHair);
        groupClothes     = root.findViewById(R.id.groupClothes);
        groupAccessories = root.findViewById(R.id.groupAccessories);
        groupFace        = root.findViewById(R.id.groupFace);

        MaterialButton btnAvatarStyle = root.findViewById(R.id.btnAvatarStyle);
        MaterialButton btnSave        = root.findViewById(R.id.btnSave);

        if (btnAvatarStyle != null) {
            btnAvatarStyle.setOnClickListener(v -> {
                cfg.background = !cfg.background;
                refresh();
            });
        }
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                Log.d(TAG, "SAVE avatarConfig: " + cfg.toMap());
                saveAvatarToFirestore(cfg);
            });
        }

        ddTopType         = root.findViewById(R.id.ddTopType);
        ddAccessoriesType = root.findViewById(R.id.ddAccessoriesType);
        ddClotheType      = root.findViewById(R.id.ddClotheType);
        ddClotheColor     = root.findViewById(R.id.ddClotheColor);
        ddGraphicType     = root.findViewById(R.id.ddGraphicType);
        ddHairColor       = root.findViewById(R.id.ddHairColor);
        ddFacialHairType  = root.findViewById(R.id.ddFacialHairType);
        ddFacialHairColor = root.findViewById(R.id.ddFacialHairColor);
        ddEyeType         = root.findViewById(R.id.ddEyeType);
        ddEyebrowType     = root.findViewById(R.id.ddEyebrowType);
        ddMouthType       = root.findViewById(R.id.ddMouthType);
        ddSkinColor       = root.findViewById(R.id.ddSkinColor);

        startUnlocksListener();
        buildAllDropdowns();

        loadAvatarFromFirestore();
        if (tabs != null) tabs.selectTab(tabs.getTabAt(0));
        showSection(0);
    }

    private void showSection(int index) {
        setVisible(groupHair,        index == 0);
        setVisible(groupClothes,     index == 1);
        setVisible(groupAccessories, index == 2);
        setVisible(groupFace,        index == 3);
    }

    private void setVisible(View v, boolean visible) {
        if (v != null) v.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void loadAvatarFromFirestore() {
        String uid = (FirebaseAuth.getInstance().getCurrentUser() != null)
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "local";

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(this::applyLoadedSnapshot)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load avatarConfig", e);
                    applyHairHatExclusivity();
                    refresh();
                });
    }

    private void applyLoadedSnapshot(DocumentSnapshot snap) {
        AvatarConfig loaded = AvatarConfig.from(snap);
        if (loaded == null) loaded = new AvatarConfig();
        cfg = loaded;
        applyHairHatExclusivity();
        buildAllDropdowns();
        refresh();
    }

    private void saveAvatarToFirestore(AvatarConfig raw) {
        AvatarConfig clean = new AvatarConfig();

        clean.seed            = safe(raw.seed, "tasktimer");

        clean.top             = raw.top;
        clean.accessories     = raw.accessories;
        clean.clothing        = raw.clothing;
        clean.eyes            = raw.eyes;
        clean.eyebrows        = raw.eyebrows;
        clean.mouth           = raw.mouth;
        clean.facialHair      = raw.facialHair;

        clean.hairColor       = raw.hairColor;
        clean.facialHairColor = raw.facialHairColor;
        clean.clothesColor    = isBlazer(raw.clothing) ? null : raw.clothesColor;
        clean.skinColor       = raw.skinColor;

        clean.clothingGraphic = "GraphicShirt".equals(raw.clothing) ? raw.clothingGraphic : null;

        clean.background      = raw.background;

        clean.accessoriesOn   = !"Blank".equals(raw.accessories);
        clean.facialHairOn    = raw.facialHairOn && !"Blank".equals(raw.facialHair);

        applyHairHatExclusivityFor(clean);

        String uid = (FirebaseAuth.getInstance().getCurrentUser() != null)
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "local";

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("avatarConfig", clean.toMap())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Saved avatarConfig to Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save avatarConfig", e));
    }

    private void refresh() {
        sanitizeForAvataaars(cfg);
        updateDropdownEnables();
        if (ivPreview != null) {
            AvatarSvgLoader.load(ivPreview, cfg, "EDITOR");
        }
    }

    private void sanitizeForAvataaars(AvatarConfig c) {
        applyHairHatExclusivity();

        if (c.accessories == null) {
            c.accessories = "Blank";
            c.accessoriesOn = false;
        } else {
            c.accessoriesOn = !"Blank".equals(c.accessories);
        }

        if (!"GraphicShirt".equals(c.clothing)) c.clothingGraphic = null;
        if (c.skinColor == null) c.skinColor = "Light";
    }

    private void applyHairHatExclusivity() {
        if (isHatTop(cfg.top) || isHeadCover(cfg.top) || "NoHair".equals(cfg.top)) {
            cfg.hairColor = null;
        }
    }

    private void applyHairHatExclusivityFor(AvatarConfig c) {
        if (isHatTop(c.top) || isHeadCover(c.top) || "NoHair".equals(c.top)) {
            c.hairColor = null;
        }
    }

    private static boolean isBlazer(String clotheType) {
        return clotheType != null && clotheType.startsWith("Blazer");
    }

    private static boolean isHeadCover(String top) {
        return "Hijab".equals(top) || "Turban".equals(top);
    }

    private static boolean isHairTop(String topType) {
        if (topType == null) return false;
        return topType.startsWith("ShortHair") || topType.startsWith("LongHair");
    }

    private static boolean isHatTop(String topType) {
        return "Hat".equals(topType) || (topType != null && topType.startsWith("WinterHat"));
    }

    private static void setEnabledWithAlpha(View v, boolean enabled) {
        if (v == null) return;
        v.setEnabled(enabled);
        v.setAlpha(enabled ? 1f : 0.35f);
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        if (unlocksReg != null) { unlocksReg.remove(); unlocksReg = null; }
    }

    private void startUnlocksListener() {
        String uid = (FirebaseAuth.getInstance().getCurrentUser() != null)
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        DocumentReference userRef = FirebaseFirestore.getInstance()
                .collection("users").document(uid);

        unlocksReg = userRef.addSnapshotListener((snap, e) -> {
            if (e != null || snap == null) return;
            Object u = snap.get("unlocks");
            if (u instanceof Map) {
                unlocks = (Map<String, Boolean>) u;
            } else unlocks = new HashMap<>();
            buildAllDropdowns();
        });
    }

    private void buildAllDropdowns() {
        makeReadOnly(ddTopType);
        makeReadOnly(ddAccessoriesType);
        makeReadOnly(ddClotheType);
        makeReadOnly(ddClotheColor);
        makeReadOnly(ddGraphicType);
        makeReadOnly(ddHairColor);
        makeReadOnly(ddFacialHairType);
        makeReadOnly(ddFacialHairColor);
        makeReadOnly(ddEyeType);
        makeReadOnly(ddEyebrowType);
        makeReadOnly(ddMouthType);
        makeReadOnly(ddSkinColor);

        setDropdown(ddTopType, TOPS,
                (map, slug) -> Unlocks.hairUnlocked(map, slug) || isHatTop(slug) || "NoHair".equals(slug),
                picked -> {
                    cfg.top = picked;
                    applyHairHatExclusivity();
                    refresh();
                });

        setDropdown(ddAccessoriesType, ACCESSORIES,
                Unlocks::glassesUnlocked,
                picked -> {
                    cfg.accessories  = picked;
                    cfg.accessoriesOn = !"Blank".equals(picked);
                    refresh();
                });

        setDropdown(ddClotheType, CLOTHES,
                Unlocks::clothesUnlocked,
                picked -> {
                    cfg.clothing = picked;
                    if (!"GraphicShirt".equals(picked)) cfg.clothingGraphic = null;
                    if (isBlazer(picked)) cfg.clothesColor = null;
                    refresh();
                });

        setDropdown(ddGraphicType, GRAPHICS,
                Unlocks::graphicUnlocked,
                picked -> {
                    cfg.clothingGraphic = picked;
                    refresh();
                });

        setDropdown(ddHairColor, HAIR_COLORS,
                Unlocks::hairColorUnlocked,
                picked -> {
                    cfg.hairColor = picked;
                    refresh();
                });

        setDropdown(ddFacialHairColor, FACIAL_HAIR_COLORS,
                Unlocks::facialHairColorUnlocked,
                picked -> {
                    cfg.facialHairColor = picked;
                    refresh();
                });

        setDropdown(ddClotheColor, CLOTHES_COLORS,
                Unlocks::clothesColorUnlocked,
                picked -> {
                    cfg.clothesColor = picked;
                    refresh();
                });

        setDropdown(ddSkinColor, SKINS,
                Unlocks::skinUnlocked,
                picked -> {
                    cfg.skinColor = picked;
                    refresh();
                });

        setDropdown(ddEyeType, EYES,
                Unlocks::eyesUnlocked,
                picked -> { cfg.eyes = picked; });

        simpleDropdown(ddEyebrowType, EYEBROWS, sel -> { cfg.eyebrows = sel; });

        setDropdown(ddMouthType, MOUTHS,
                Unlocks::mouthUnlocked,
                picked -> { cfg.mouth = picked; });

        setDropdown(ddFacialHairType, FACIAL_HAIR,
                (map, slug) -> "Blank".equals(slug) || Unlocks.facialHairUnlocked(map, slug),
                picked -> {
                    cfg.facialHair   = picked;
                    cfg.facialHairOn = !"Blank".equals(picked);
                });

        selectIn(ddTopType, cfg.top);
        selectIn(ddAccessoriesType, cfg.accessories == null ? "Blank" : cfg.accessories);
        selectIn(ddClotheType, cfg.clothing);
        selectIn(ddGraphicType, cfg.clothingGraphic);
        selectIn(ddHairColor, cfg.hairColor);
        selectIn(ddFacialHairColor, cfg.facialHairColor);
        selectIn(ddClotheColor, cfg.clothesColor);
        selectIn(ddSkinColor, cfg.skinColor);
        selectIn(ddEyeType, cfg.eyes);
        selectIn(ddEyebrowType, cfg.eyebrows);
        selectIn(ddMouthType, cfg.mouth);

        updateDropdownEnables();
    }


    private void updateDropdownEnables() {
        final boolean hairColorAllowed = isHairTop(cfg.top);
        final boolean facialHairColorAllowed = cfg.facialHairOn && cfg.facialHair != null && !"Blank".equals(cfg.facialHair);
        final boolean clothesColorAllowed = !isBlazer(cfg.clothing);
        final boolean graphicAllowed = "GraphicShirt".equals(cfg.clothing);

        setEnabledWithAlpha(ddHairColor, hairColorAllowed);
        if (!hairColorAllowed) cfg.hairColor = null;

        setEnabledWithAlpha(ddFacialHairColor, facialHairColorAllowed);
        if (!facialHairColorAllowed) cfg.facialHairColor = null;

        setEnabledWithAlpha(ddClotheColor, clothesColorAllowed);
        if (!clothesColorAllowed) cfg.clothesColor = null;

        setEnabledWithAlpha(ddGraphicType, graphicAllowed);
        if (!graphicAllowed) cfg.clothingGraphic = null;
    }

    private void setDropdown(@Nullable MaterialAutoCompleteTextView dd,
                             @NonNull String[] values,
                             @NonNull UnlockCheck checker,
                             @NonNull OnPick<String> onPick) {
        if (dd == null) return;

        List<String> display = new ArrayList<>(values.length);
        for (String v : values) {
            boolean ok = checker.isUnlocked(unlocks, v);
            display.add(ok ? v : (v + "  🔒"));
        }

        ArrayAdapter<String> ad = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, display);
        dd.setAdapter(ad);

        dd.setOnItemClickListener((parent, view, position, id) -> {
            String picked = values[position];
            boolean ok = checker.isUnlocked(unlocks, picked);
            if (!ok) {
                Toast.makeText(requireContext(), "Locked — buy in Shop", Toast.LENGTH_SHORT).show();
                selectIn(dd, currentFor(dd));
                return;
            }
            onPick.accept(picked);
            refresh();
        });
    }

    private interface UnlockCheck {
        boolean isUnlocked(Map<String, Boolean> unlocks, String slug);
    }

    private interface OnPick<T> {
        void accept(T value);
    }

    private void simpleDropdown(@Nullable MaterialAutoCompleteTextView dd,
                                @NonNull String[] values,
                                @NonNull java.util.function.Consumer<String> apply) {
        if (dd == null) return;
        ArrayAdapter<String> ad = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, values);
        dd.setAdapter(ad);
        dd.setOnItemClickListener((p, v, pos, id) -> {
            apply.accept(values[pos]);
            refresh();
        });
    }

    private void selectIn(@Nullable MaterialAutoCompleteTextView dd, @Nullable String value) {
        if (dd == null || value == null) return;
        dd.setText(value, false);
    }

    private void makeReadOnly(@Nullable com.google.android.material.textfield.MaterialAutoCompleteTextView dd) {
        if (dd == null) return;
        dd.setInputType(android.text.InputType.TYPE_NULL);
        dd.setKeyListener(null);
        dd.setCursorVisible(false);
        dd.setFocusable(false);
        dd.setClickable(true);
        dd.setOnClickListener(v -> dd.showDropDown());
        dd.setOnFocusChangeListener((v, has) -> { if (has) dd.showDropDown(); });
    }



    @Nullable private String currentFor(@NonNull MaterialAutoCompleteTextView dd) {
        if (dd == ddTopType) return cfg.top;
        if (dd == ddAccessoriesType) return cfg.accessories;
        if (dd == ddClotheType) return cfg.clothing;
        if (dd == ddGraphicType) return cfg.clothingGraphic;
        if (dd == ddHairColor) return cfg.hairColor;
        if (dd == ddFacialHairColor) return cfg.facialHairColor;
        if (dd == ddClotheColor) return cfg.clothesColor;
        if (dd == ddSkinColor) return cfg.skinColor;
        if (dd == ddEyeType) return cfg.eyes;
        if (dd == ddEyebrowType) return cfg.eyebrows;
        if (dd == ddMouthType) return cfg.mouth;
        return null;
    }

    private static String safe(String s, String d) { return (s == null || s.isEmpty()) ? d : s; }
}
