package com.erickoeckel.tasktimer;

import android.os.CountDownTimer;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class TimerViewModel extends ViewModel {

    public enum Phase { FOCUS, BREAK }
    public static final long FOCUS_MILLIS = 25L * 60L * 1000L;
    public static final long BREAK_MILLIS = 5L  * 60L * 1000L;
    private final MutableLiveData<Long> remaining = new MutableLiveData<>(FOCUS_MILLIS);
    private final MutableLiveData<Phase> phase = new MutableLiveData<>(Phase.FOCUS);
    private final MutableLiveData<Boolean> running = new MutableLiveData<>(false);
    private CountDownTimer timer;
    private boolean transitionInFlight = false;
    public LiveData<Long> getRemaining() { return remaining; }
    public LiveData<Phase> getPhase() { return phase; }
    public LiveData<Boolean> isRunning() { return running; }

    public void start() {
        Long ms = remaining.getValue();
        if (ms == null) ms = (phase.getValue() == Phase.FOCUS) ? FOCUS_MILLIS : BREAK_MILLIS;
        startNew(ms);
    }

    private void startNew(long durationMs) {
        if (Boolean.TRUE.equals(running.getValue())) return;
        cancelTimerIfAny();

        remaining.setValue(durationMs);
        running.setValue(true);

        timer = new CountDownTimer(durationMs, 1000) {
            @Override public void onTick(long millisUntilFinished) {
                remaining.setValue(millisUntilFinished);
            }
            @Override public void onFinish() {
                if (transitionInFlight) return;
                transitionInFlight = true;

                running.setValue(false);
                remaining.setValue(0L);

                cancelTimerIfAny();

                if (phase.getValue() == Phase.FOCUS) {
                    phase.setValue(Phase.BREAK);
                    startNew(BREAK_MILLIS);
                } else {
                    phase.setValue(Phase.FOCUS);
                    remaining.setValue(FOCUS_MILLIS);
                }

                transitionInFlight = false;
            }

        }.start();
    }

    public void pause() {
        cancelTimerIfAny();
        running.setValue(false);
    }

    public void resetToFocus() {
        cancelTimerIfAny();
        running.setValue(false);
        phase.setValue(Phase.FOCUS);
        remaining.setValue(FOCUS_MILLIS);
    }

    private void cancelTimerIfAny() {
        if (timer != null) { timer.cancel(); timer = null; }
    }

    @Override protected void onCleared() { cancelTimerIfAny(); }
}

