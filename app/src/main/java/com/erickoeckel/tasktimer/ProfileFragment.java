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

        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            tvWelcome.setText("Welcome");
            tvEmail.setText(user.getEmail());
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

