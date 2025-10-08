package com.erickoeckel.tasktimer;

import android.content.Intent;
import android.graphics.drawable.PictureDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import java.util.HashMap;
import java.util.Map;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.DocumentSnapshot;

public class ProfileFragment extends Fragment {

    private TextView tvEmail, tvWelcome, tvLevel, tvXp, tvCoins, tvAvatarSummary;
    private ImageView ivAvatar;
    private FirebaseAuth auth;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        tvWelcome        = v.findViewById(R.id.tvWelcome);
        tvEmail          = v.findViewById(R.id.tvEmail);
        tvLevel          = v.findViewById(R.id.tvLevel);
        tvXp             = v.findViewById(R.id.tvXp);
        tvCoins          = v.findViewById(R.id.tvCoins);
        tvAvatarSummary  = v.findViewById(R.id.tvAvatarSummary);
        ivAvatar         = v.findViewById(R.id.ivAvatar);
        MaterialButton btnFriends         = v.findViewById(R.id.btnFriends);
        MaterialButton btnCustomizeAvatar = v.findViewById(R.id.btnCustomizeAvatar);
        MaterialButton btnSignOut         = v.findViewById(R.id.btnSignOut);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            tvWelcome.setText("Welcome!");
            tvEmail.setText(user.getEmail() == null ? "" : user.getEmail());

            ensureUserDoc(db, user);

            DocumentReference userDoc = db.collection("users").document(user.getUid());
            userDoc.addSnapshotListener(new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@Nullable DocumentSnapshot snap,
                                    @Nullable FirebaseFirestoreException e) {
                    if (!isAdded() || getView() == null) return;

                    if (e != null || snap == null || !snap.exists()) return;
                    bindStatsAndAvatar(snap);
                }
            });

            btnFriends.setOnClickListener(view -> {
                NavController nav = NavHostFragment.findNavController(ProfileFragment.this);
                nav.navigate(R.id.action_profileFragment_to_friendsFragment);
            });

            btnCustomizeAvatar.setOnClickListener(view -> {
                NavController nav = NavHostFragment.findNavController(ProfileFragment.this);
                nav.navigate(R.id.action_profile_to_avatarEditor);
            });

        } else {
            tvWelcome.setText("Not signed in");
            tvEmail.setText("");
        }

        btnSignOut.setOnClickListener(view -> {
            auth.signOut();
            Intent i = new Intent(requireContext(), AuthActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            requireActivity().finish();
        });

        return v;
    }

    private void bindStatsAndAvatar(@NonNull DocumentSnapshot snap) {
        Long xpVal    = snap.getLong("xp");
        Long coinsVal = snap.getLong("coins");
        long xp    = xpVal == null ? 0L : xpVal;
        long coins = coinsVal == null ? 0L : coinsVal;

        int level = Rewards.levelForXp(xp);
        long next  = Rewards.xpForLevel(level + 1);

        tvLevel.setText("Level " + level);
        tvXp.setText(xp + " / " + next +" XP");
        tvCoins.setText(coins + " Coins");

        AvatarConfig cfg = AvatarConfig.from(snap);
        tvAvatarSummary.setText(
                "Hair: " + cfg.hair + ", Eyes: " + cfg.eyes
        );

        String url = AvatarUrl.build(cfg, 256);
        Glide.with(this)
                .as(PictureDrawable.class)
                .listener(new SvgSoftwareLayerSetter())
                .load(url)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_edit_24)
                .into(ivAvatar);

    }

    private void ensureUserDoc(FirebaseFirestore db, FirebaseUser user) {
        DocumentReference doc = db.collection("users").document(user.getUid());
        doc.get().addOnSuccessListener(s -> {
            if (s.exists()) return;
            Map<String, Object> init = new HashMap<>();
            init.put("xp", 0L);
            init.put("coins", 0L);
            init.put("avatarConfig", new AvatarConfig().toMap());
            doc.set(init, SetOptions.merge());
        });
    }
}
