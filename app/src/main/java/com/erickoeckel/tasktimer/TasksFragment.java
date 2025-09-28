package com.erickoeckel.tasktimer;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;

public class TasksFragment extends Fragment {
    private TasksViewModel vm;
    private TasksAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inf, ViewGroup c, Bundle b) {
        View v = inf.inflate(R.layout.fragment_tasks, c, false);

        vm = new ViewModelProvider(requireActivity()).get(TasksViewModel.class);

        RecyclerView rv = v.findViewById(R.id.rvTasks);
        adapter = new TasksAdapter(new ArrayList<>(), (String id, boolean ignored) -> {
            vm.completeTask(id)
                    .addOnSuccessListener(unused -> {
                        Rewards.awardTaskCompleted(FirebaseFirestore.getInstance())
                                .addOnSuccessListener(v2 -> AiCoach.generateAndNotify(
                                        requireContext().getApplicationContext(),
                                        "TASK_COMPLETED",
                                        null,
                                        Notify.CH_REWARDS,
                                        "Task completed!"
                                ))
                                .addOnFailureListener(e -> Log.e("Rewards", "awardTaskCompleted failed", e));


                    })
                    .addOnFailureListener(e -> Log.e("Tasks","completeTask failed", e));
        });

        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        v.findViewById(R.id.btnAdd).setOnClickListener(view -> {
            new NewTaskDialog((taskTitle, dueYmd) -> {
                String id = java.util.UUID.randomUUID().toString();
                Task t = new Task(id, taskTitle);
                t.setDueDate(dueYmd);
                vm.addTask(t);
            }).show(getChildFragmentManager(), "newTask");
        });

        vm.getTasks().observe(getViewLifecycleOwner(), adapter::submit);
        return v;
    }
}


