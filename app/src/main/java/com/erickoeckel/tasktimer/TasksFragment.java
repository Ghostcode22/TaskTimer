package com.erickoeckel.tasktimer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.Map;

public class TasksFragment extends Fragment {

    private TasksViewModel vm;
    private TasksAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, ViewGroup c, Bundle b) {
        View v = inf.inflate(R.layout.fragment_tasks, c, false);

        vm = new ViewModelProvider(this).get(TasksViewModel.class);

        RecyclerView rv = v.findViewById(R.id.rvTasks);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new TasksAdapter((id, checked) -> {
            // compute remaining AFTER this one is checked
            int remainingAfter = Math.max(0, adapter.countUndone() - 1);

            String taskTitle = "Task";
            Task tapped = adapter.findById(id);
            if (tapped != null && tapped.getTitle() != null && !tapped.getTitle().isEmpty()) {
                taskTitle = tapped.getTitle();
            }

            vm.completeTask(id); // your existing implementation

            Map<String, Object> extra = new HashMap<>();
            extra.put("taskTitle", taskTitle);
            extra.put("tasksLeft", remainingAfter);

            AiCoach.generateAndNotify(
                    requireContext(),
                    AiCoach.EVENT_TASK_COMPLETED,
                    extra,
                    Notify.CH_REWARDS,
                    "Task completed!"
            );
        });

        rv.setAdapter(adapter);

        v.findViewById(R.id.btnAdd).setOnClickListener(view -> {
            new NewTaskDialog((taskTitle, dueYmd) -> {
                String id = java.util.UUID.randomUUID().toString();
                Task t = new Task(id, taskTitle);
                t.setDueDate(dueYmd);
                vm.addTask(t);
            }).show(getChildFragmentManager(), "newTask");
        });

        vm.getTasks().observe(getViewLifecycleOwner(), adapter::submitList);
        return v;
    }
}
