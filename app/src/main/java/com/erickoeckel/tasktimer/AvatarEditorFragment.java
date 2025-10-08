package com.erickoeckel.tasktimer;

import android.graphics.drawable.PictureDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import java.util.Collections;
import java.util.Objects;

public class AvatarEditorFragment extends Fragment {

    private final String TAG = "AvatarEditor";
    private AvatarConfig cfg = new AvatarConfig();

    private ImageView iv;
    private FirebaseFirestore db;
    private String uid;


    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup container, @Nullable Bundle s) {
        View v = inf.inflate(R.layout.fragment_avatar_editor, container, false);

        iv = v.findViewById(R.id.ivPreview);

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        uid = Objects.requireNonNull(user).getUid();

        MaterialToolbar tb = v.findViewById(R.id.toolbarAvatar);
        tb.setNavigationOnClickListener(view ->
                NavHostFragment.findNavController(AvatarEditorFragment.this).navigateUp()
        );

        db.collection("users").document(uid).get()
                .addOnSuccessListener(snap -> {
                    cfg = AvatarConfig.from(snap);
                    Log.d(TAG, "Loaded cfg: " + cfg.toMap());
                    wireUi(v);
                    refresh();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "No cfg, using defaults", e);
                    wireUi(v);
                    refresh();
                });

        return v;
    }

    private void wireUi(View v) {
        final String[] SKIN_HEX = {"ffe1cc","f2dbb1","f1c27d","c68642","8d5524"};
        final String[] HAIR_HEX = {"000000","5a3e2b","2f2f2f","c0c0c0","d4af37","8b4513"};
        final String[] EYES_V7 = {"default","happy","wink","squint","surprised","hearts","side","xDizzy"};
        final String[] SHIRT_HEX = {"3d85c6","e06666","93c47d","ffd966","8e7cc3"};
        final String[] CLOTHES = {"shirtCrewNeck","shirtVNeck","shirtScoopNeck","hoodie"};

        Chip chHairShort = v.findViewById(R.id.chHairShort);
        Chip chHairLong  = v.findViewById(R.id.chHairLong);
        Chip chHairBuzz  = v.findViewById(R.id.chHairBuzz);
        if (chHairShort != null) chHairShort.setOnClickListener(x -> {
            cfg.hair = "shortRound";
            applyCheckedStates(v);
            refresh();
        });
        if (chHairLong  != null) chHairLong.setOnClickListener(x -> {
            cfg.hair = "straight01";
            applyCheckedStates(v);
            refresh();
        });
        if (chHairBuzz  != null) chHairBuzz.setOnClickListener(x -> {
            cfg.hair = "shavedSides";
            applyCheckedStates(v);
            refresh();
        });

        Chip chAccNone    = v.findViewById(R.id.chAccNone);
        Chip chAccGlasses = v.findViewById(R.id.chAccGlasses);
        Chip chAccHat     = v.findViewById(R.id.chAccHat);
        if (chAccNone != null) chAccNone.setOnClickListener(x -> {
            cfg.glasses = "blank";
            applyCheckedStates(v);
            refresh();
        });
        if (chAccGlasses != null) chAccGlasses.setOnClickListener(x -> {
            cfg.glasses = "round";
            applyCheckedStates(v);
            refresh();
        });
        if (chAccHat != null) chAccHat.setOnClickListener(x -> {
            cfg.hair = "hat";
            applyCheckedStates(v);
            refresh();
        });

        MaterialButton btnSkin  = v.findViewById(R.id.btnSkin);
        MaterialButton btnHair  = v.findViewById(R.id.btnHair);
        MaterialButton btnEyes  = v.findViewById(R.id.btnEyes);
        MaterialButton btnShirt = v.findViewById(R.id.btnShirt);
        MaterialButton btnSave  = v.findViewById(R.id.btnSave);

        if (btnSkin != null) btnSkin.setOnClickListener(x -> {
            cfg.skinColor  = nextOf(SKIN_HEX, cfg.skinColor );
            refresh();
        });
        if (btnHair != null) btnHair.setOnClickListener(x -> {
            cfg.hairColor = nextOf(HAIR_HEX, cfg.hairColor);
            refresh();
        });
        if (btnEyes != null) btnEyes.setOnClickListener(x -> {
            cfg.eyes = nextOf(EYES_V7, cfg.eyes);
            refresh();
        });

        if (btnShirt != null) btnShirt.setOnClickListener(x -> {
            cfg.clothesColor = nextOf(SHIRT_HEX, cfg.clothesColor);
            cfg.clothes      = nextOf(CLOTHES,   cfg.clothes);
            refresh();
        });

        if (btnSave != null) btnSave.setOnClickListener(x -> save());

        applyCheckedStates(v);
    }

    private void applyCheckedStates(View v) {
        setChecked(v, R.id.chHairShort, "shortHairShortRound".equals(cfg.hair));
        setChecked(v, R.id.chHairLong,  "longHairStraight".equals(cfg.hair));
        setChecked(v, R.id.chHairBuzz,  "longHairBob".equals(cfg.hair));
        setChecked(v, R.id.chAccHat,    "hat".equals(cfg.hair));

        boolean noGlasses = "blank".equalsIgnoreCase(cfg.glasses) || cfg.glasses == null;
        setChecked(v, R.id.chAccNone,    noGlasses);
        setChecked(v, R.id.chAccGlasses, !noGlasses);
    }
    private void setChecked(View v, int id, boolean checked) {
        Chip c = v.findViewById(id);
        if (c != null) c.setChecked(checked);
    }

    private String nextOf(String[] arr, String cur) {
        if (cur == null) return arr[0];
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equalsIgnoreCase(cur)) return arr[(i + 1) % arr.length];
        }
        return arr[0];
    }

    private void refresh() {
        String url = AvatarUrl.build(cfg, 512);
        Glide.with(this)
                .as(PictureDrawable.class)
                .listener(new SvgSoftwareLayerSetter())
                .load(url)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_edit_24)
                .into(iv);
    }

    private void save() {
        db.collection("users").document(uid)
                .set(Collections.singletonMap("avatarConfig", cfg.toMap()), SetOptions.merge())
                .addOnSuccessListener(v -> {
                    Toast.makeText(requireContext(), "Avatar saved!", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(AvatarEditorFragment.this).navigateUp();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}
