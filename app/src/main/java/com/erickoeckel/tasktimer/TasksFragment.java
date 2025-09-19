package com.erickoeckel.tasktimer;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.UUID;

public class TasksFragment extends Fragment {
    private TasksViewModel vm;
    private TasksAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inf, ViewGroup c, Bundle b) {
        View v = inf.inflate(R.layout.fragment_tasks, c, false);

        vm = new ViewModelProvider(requireActivity()).get(TasksViewModel.class);

        RecyclerView rv = v.findViewById(R.id.rvTasks);
        adapter = new TasksAdapter(new ArrayList<>(), (String id, boolean ignored) -> {
            vm.completeTask(id) // always sets done=true
                    .addOnSuccessListener(unused -> Rewards.awardTaskCompleted(FirebaseFirestore.getInstance()))
                    .addOnFailureListener(e -> Log.e("Tasks","completeTask failed", e));
        });

        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        EditText et = v.findViewById(R.id.etTask);
        v.findViewById(R.id.btnAdd).setOnClickListener(view -> {
            String title = et.getText().toString().trim();
            if (!title.isEmpty()) {
                vm.addTask(new Task(UUID.randomUUID().toString(), title, false));
                et.setText("");
            }
        });

        vm.getTasks().observe(getViewLifecycleOwner(), adapter::submit);
        return v;
    }
}


