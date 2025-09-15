package com.erickoeckel.tasktimer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class TimerFragment extends Fragment {

    private TimerViewModel vm;
    private TextView tvTime, tvPhase;
    private Button btnStart, btnPause, btnReset;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        View v = inf.inflate(R.layout.fragment_timer, c, false);

        tvTime  = v.findViewById(R.id.tvTime);
        tvPhase = v.findViewById(R.id.tvPhase);
        btnStart = v.findViewById(R.id.btnStart);
        btnPause = v.findViewById(R.id.btnPause);
        btnReset = v.findViewById(R.id.btnReset);

        vm = new ViewModelProvider(requireActivity()).get(TimerViewModel.class);
        final FirebaseFirestore db = FirebaseFirestore.getInstance();

        vm.getRemaining().observe(getViewLifecycleOwner(), ms -> tvTime.setText(format(ms)));
        vm.getPhase().observe(getViewLifecycleOwner(), p -> tvPhase.setText(p == TimerViewModel.Phase.FOCUS ? "FOCUS" : "BREAK"));
        vm.isRunning().observe(getViewLifecycleOwner(), running -> {
            btnStart.setEnabled(!running);
            btnPause.setEnabled(running);
        });

        btnStart.setOnClickListener(x -> vm.start());
        btnPause.setOnClickListener(x -> vm.pause());
        btnReset.setOnClickListener(x -> vm.resetToFocus());

        final TimerViewModel.Phase[] lastPhase = new TimerViewModel.Phase[]{null};

        vm.getPhase().observe(getViewLifecycleOwner(), p -> {
            if (lastPhase[0] == TimerViewModel.Phase.FOCUS && p == TimerViewModel.Phase.BREAK) {
                Rewards.awardFocusSession(db)
                        .addOnFailureListener(e ->
                                System.out.println("awardFocusSession failed: " + e.getMessage()));
            }
            lastPhase[0] = p;
        });
        return v;
    }

    private String format(long ms) {
        long totalSec = Math.max(0, ms / 1000);
        long m = totalSec / 60;
        long s = totalSec % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", m, s);
    }
}

