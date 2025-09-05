package com.erickoeckel.tasktimer;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class TimerFragment extends Fragment {
    private TextView tvTime;
    private Button btnStart, btnPause, btnReset;
    private CountDownTimer timer;
    private long focusMillis = 25 * 60 * 1000;
    private long remaining = focusMillis;
    private boolean running = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inf.inflate(R.layout.fragment_timer, container, false);

        tvTime   = v.findViewById(R.id.tvTime);
        btnStart = v.findViewById(R.id.btnStart);
        btnPause = v.findViewById(R.id.btnPause);
        btnReset = v.findViewById(R.id.btnReset);

        updateClock();

        btnStart.setOnClickListener(view -> startTimer());
        btnPause.setOnClickListener(view -> pauseTimer());
        btnReset.setOnClickListener(view -> resetTimer());

        return v;
    }

    private void startTimer() {
        if (running) return;
        running = true;
        timer = new CountDownTimer(remaining, 1000) {
            public void onTick(long ms) {
                remaining = ms;
                updateClock();
            }
            public void onFinish() {
                running = false;
                remaining = 0;
                updateClock();
            }
        }.start();
    }

    private void pauseTimer() {
        if (timer != null) timer.cancel();
        running = false;
    }

    private void resetTimer() {
        if (timer != null) timer.cancel();
        running = false;
        remaining = focusMillis;
        updateClock();
    }

    private void updateClock() {
        long s = remaining / 1000;
        long m = s / 60;
        long ss = s % 60;
        tvTime.setText(String.format("%02d:%02d", m, ss));
    }
}
