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
import android.text.TextUtils;
import androidx.lifecycle.LiveData;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;


public class TimerFragment extends Fragment {

    private TimerViewModel vm;
    private TextView tvTime, tvPhase;
    private Button btnStart, btnPause, btnReset, btnCancel;
    private TextView tvWorkingOn;
    private Button btnComplete;
    @Nullable private String sourceType, sourceId, sourceTitle;
    private boolean showCompleteAfterBreak = false;


    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        View v = inf.inflate(R.layout.fragment_timer, c, false);

        tvTime  = v.findViewById(R.id.tvTime);
        tvPhase = v.findViewById(R.id.tvPhase);
        btnStart = v.findViewById(R.id.btnStart);
        btnPause = v.findViewById(R.id.btnPause);
        btnReset = v.findViewById(R.id.btnReset);
        tvWorkingOn = v.findViewById(R.id.tvWorkingOn);
        btnComplete = v.findViewById(R.id.btnComplete);
        btnCancel = v.findViewById(R.id.btnCancel);

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
        btnCancel.setOnClickListener(x -> {
            vm.pause();
            vm.resetToFocus();
            setTabsEnabled(true);
            sourceType = sourceId = sourceTitle = null;
            NavHostFragment.findNavController(TimerFragment.this).popBackStack();
        });

        final TimerViewModel.Phase[] lastPhase = new TimerViewModel.Phase[]{null};

        vm.getPhase().observe(getViewLifecycleOwner(), p -> {
            if (lastPhase[0] == TimerViewModel.Phase.FOCUS && p == TimerViewModel.Phase.BREAK) {
                Rewards.awardFocusSession(db)
                        .addOnFailureListener(e ->
                                System.out.println("awardFocusSession failed: " + e.getMessage()));
                if (sourceType != null && sourceId != null) {
                    if ("task".equals(sourceType)) {
                        TasksViewModel tVm = new ViewModelProvider(requireActivity()).get(TasksViewModel.class);
                        tVm.completeTask(sourceId);
                    } else if ("habit".equals(sourceType)) {
                        HabitsViewModel hVm = new ViewModelProvider(requireActivity()).get(HabitsViewModel.class);
                        hVm.completeTodayWithInfo(sourceId);
                    }
                }

            }
            if (lastPhase[0] == TimerViewModel.Phase.BREAK
                    && p == TimerViewModel.Phase.FOCUS
                    && showCompleteAfterBreak
                    && !TextUtils.isEmpty(sourceType)
                    && !TextUtils.isEmpty(sourceId)) {
                btnComplete.setVisibility(View.VISIBLE);
            }
            lastPhase[0] = p;
        });

        View infoBtn = v.findViewById(R.id.btnInfoTimer);
        if (infoBtn != null) {
            infoBtn.setOnClickListener(x -> HelpSheetFragment.show(
                    getChildFragmentManager(),
                    "Timer",
                    "Focus & break cycles:",
                    java.util.Arrays.asList(
                            "Start: begins a focus session; Pause/Reset as needed.",
                            "When Focus ends, a Break starts and rewards are granted.",
                            "If you came from a task/habit, you can Complete it after break.",
                            "Cancel exits back to your list."
                    )));
        }
        if (Onboarding.shouldShow(requireContext(), "help_timer_v1")) {
            if (infoBtn != null) infoBtn.performClick();
            Onboarding.markShown(requireContext(), "help_timer_v1");
        }

        btnComplete.setOnClickListener(x -> {
            btnComplete.setEnabled(false);

            if ("task".equals(sourceType) && !TextUtils.isEmpty(sourceId)) {
                TasksViewModel tVm = new ViewModelProvider(requireActivity()).get(TasksViewModel.class);
                tVm.completeTask(sourceId);

                int remainingAfter = 0;
                LiveData<java.util.List<Task>> live = tVm.getTasks();
                java.util.List<Task> current = (live != null) ? live.getValue() : null;
                if (current != null) {
                    for (Task t : current) {
                        if (!t.isDone() && !sourceId.equals(t.getId())) remainingAfter++;
                    }
                }
                java.util.Map<String, Object> extra = new java.util.HashMap<>();
                extra.put("taskTitle", TextUtils.isEmpty(sourceTitle) ? "Task" : sourceTitle);
                extra.put("tasksLeft", remainingAfter);
                AiCoach.generateAndNotify(
                        requireContext(),
                        AiCoach.EVENT_TASK_COMPLETED,
                        extra,
                        Notify.CH_REWARDS,
                        "Task completed!"
                );

            } else if ("habit".equals(sourceType) && !TextUtils.isEmpty(sourceId)) {
                HabitsViewModel hVm = new ViewModelProvider(requireActivity()).get(HabitsViewModel.class);
                hVm.completeTodayWithInfo(sourceId)
                        .addOnSuccessListener(info -> {
                            java.util.Map<String, Object> extra = new java.util.HashMap<>();
                            extra.put("habitTitle", info.habitTitle);
                            extra.put("streak", info.streak);
                            extra.put("habitsLeft", info.habitsLeft);
                            AiCoach.generateAndNotify(
                                    requireContext(),
                                    AiCoach.EVENT_HABIT_COMPLETED,
                                    extra,
                                    Notify.CH_REWARDS,
                                    "Habit logged!"
                            );
                        });
            }

            btnComplete.setVisibility(View.GONE);
            btnComplete.setEnabled(true);
            showCompleteAfterBreak = false;
            tvWorkingOn.setVisibility(View.GONE);

            setTabsEnabled(true);
            NavHostFragment.findNavController(TimerFragment.this).popBackStack();

            sourceType = sourceId = sourceTitle = null;
        });

        Bundle args = getArguments();
        if (args != null) {
            sourceType  = args.getString("sourceType");
            sourceId    = args.getString("sourceId");
            sourceTitle = args.getString("sourceTitle");
            boolean autoStart = args.getBoolean("autoStart", true);

            if (!TextUtils.isEmpty(sourceTitle)) {
                tvWorkingOn.setText(getString(R.string.working_on, sourceTitle));
                tvWorkingOn.setVisibility(View.VISIBLE);
            } else {
                tvWorkingOn.setVisibility(View.GONE);
            }

            if (sourceType != null && sourceId != null) {
                vm.resetToFocus();
                if (autoStart) {
                    vm.start();
                    setTabsEnabled(false);
                }
            }
            showCompleteAfterBreak = true;
        }
        return v;
    }

    private void setTabsEnabled(boolean enabled) {
        View v = requireActivity().findViewById(R.id.bottomNav);
        if (v instanceof BottomNavigationView) {
            BottomNavigationView b = (BottomNavigationView) v;
            b.setEnabled(enabled);
            b.setClickable(enabled);
            for (int i = 0; i < b.getMenu().size(); i++) {
                b.getMenu().getItem(i).setEnabled(enabled);
            }
        }
    }

    private String format(long ms) {
        long totalSec = Math.max(0, ms / 1000);
        long m = totalSec / 60;
        long s = totalSec % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", m, s);
    }
}

