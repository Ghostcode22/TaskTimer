package com.erickoeckel.tasktimer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AuthActivity extends AppCompatActivity {
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnSubmit;
    private MaterialButtonToggleGroup toggle;
    private ProgressBar progress;
    private TextView tvError;

    private boolean isRegister = false;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSubmit = findViewById(R.id.btnSubmit);
        toggle = findViewById(R.id.toggleMode);
        progress = findViewById(R.id.progress);
        tvError = findViewById(R.id.tvError);

        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            isRegister = (checkedId == R.id.btnRegisterMode);
            btnSubmit.setText(isRegister ? "Create Account" : "Login");
            tvError.setVisibility(View.GONE);
        });

        btnSubmit.setOnClickListener(v -> submit());
    }

    private void submit() {
        String email = Objects.toString(etEmail.getText(), "").trim();
        String pass = Objects.toString(etPassword.getText(), "").trim();

        if (email.isEmpty() || pass.isEmpty()) {
            showError("Email and password required.");
            return;
        }

        setLoading(true);

        if (isRegister) {
            auth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null) {
                                Map<String,Object> base = new HashMap<>();
                                base.put("email", user.getEmail());
                                base.put("xp", 0L);
                                base.put("coins", 0L);
                                base.put("createdAt", FieldValue.serverTimestamp());
                                db.collection("users").document(user.getUid()).set(base, SetOptions.merge());


                                db.collection("users").document(user.getUid())
                                        .set(base, SetOptions.merge())
                                        .addOnSuccessListener(unused -> goToApp())
                                        .addOnFailureListener(e -> {
                                            showError("Account created, but profile save failed: " + e.getMessage());
                                            goToApp(); // still proceed
                                        });
                            } else {
                                goToApp();
                            }
                        } else {
                            showError(Objects.requireNonNull(task.getException()).getMessage());
                            setLoading(false);
                        }
                    });
        } else {
            auth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            goToApp();
                        } else {
                            showError(Objects.requireNonNull(task.getException()).getMessage());
                            setLoading(false);
                        }
                    });
        }
    }

    private void goToApp() {
        setLoading(false);
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean b) {
        progress.setVisibility(b ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!b);
        toggle.setEnabled(!b);
        etEmail.setEnabled(!b);
        etPassword.setEnabled(!b);
    }
}

