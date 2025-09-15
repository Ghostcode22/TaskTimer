package com.erickoeckel.tasktimer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

public class ProfileFragment extends Fragment {
    private TextView tvEmail, tvWelcome;
    private FirebaseAuth auth;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);
        tvWelcome = v.findViewById(R.id.tvWelcome);
        tvEmail = v.findViewById(R.id.tvEmail);
        MaterialButton btnSignOut = v.findViewById(R.id.btnSignOut);
        TextView tvLevel = v.findViewById(R.id.tvLevel);
        TextView tvXp = v.findViewById(R.id.tvXp);
        TextView tvCoins = v.findViewById(R.id.tvCoins);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            tvWelcome.setText("Welcome");
            tvEmail.setText(user.getEmail());

            db.collection("users").document(user.getUid())
                    .addSnapshotListener(requireActivity(), new EventListener<DocumentSnapshot>() {
                        @Override
                        public void onEvent(@Nullable DocumentSnapshot snap,
                                            @Nullable FirebaseFirestoreException e) {
                            if (e != null || snap == null || !snap.exists()) return;

                            Long xp    = snap.getLong("xp");
                            Long coins = snap.getLong("coins");
                            long curXp    = xp == null ? 0L : xp;
                            long curCoins = coins == null ? 0L : coins;

                            int level      = Rewards.levelForXp(curXp);
                            long nextLevel = Rewards.xpForLevel(level + 1);

                            tvLevel.setText("Level " + level);
                            tvXp.setText(curXp + " / " + nextLevel + " XP");
                            tvCoins.setText(curCoins + " Coins");
                        }
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
}

