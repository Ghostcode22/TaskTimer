package com.erickoeckel.tasktimer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class HabitsFragment extends Fragment {

    private HabitsViewModel vm;
    private HabitsAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_habits, container, false);

        vm = new ViewModelProvider(this).get(HabitsViewModel.class);

        RecyclerView rv = v.findViewById(R.id.rvHabits);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new HabitsAdapter((String id, boolean checked) -> {
            if (!checked) return;

            vm.completeTodayWithInfo(id)
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
        });

        rv.setAdapter(adapter);


        vm.getHabits().observe(getViewLifecycleOwner(), list -> {
            adapter.submit(list);
            boolean isEmpty = (list == null || list.isEmpty());
            final TextView tvEmpty = v.findViewById(R.id.tvEmpty);
            tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        });

        v.findViewById(R.id.fabAddHabit).setOnClickListener(btn -> {
            new NewHabitDialog(payload -> {
                String id = java.util.UUID.randomUUID().toString();
                java.util.List<Boolean> days = Habit.fromArray(payload.days);
                vm.addHabit(new Habit(id, payload.title, days));
            }).show(getChildFragmentManager(), "newHabit");
        });

        return v;
    }
}
