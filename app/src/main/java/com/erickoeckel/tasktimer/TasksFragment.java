package com.erickoeckel.tasktimer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TasksFragment extends Fragment {
    private TasksViewModel vm;
    private TasksAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inf, ViewGroup c, Bundle b) {
        View v = inf.inflate(R.layout.fragment_tasks, c, false);

        vm = new ViewModelProvider(requireActivity()).get(TasksViewModel.class);

        RecyclerView rv = v.findViewById(R.id.rvTasks);
        adapter = new TasksAdapter(new ArrayList<>(), vm::toggleDone);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        EditText et = v.findViewById(R.id.etTask);
        v.findViewById(R.id.btnAdd).setOnClickListener(x -> {
            String t = et.getText().toString().trim();
            if (!t.isEmpty()) {
                vm.addTask(new Task(UUID.randomUUID().toString(), t, false));
                et.setText("");
            }
        });

        vm.getTasks().observe(getViewLifecycleOwner(), adapter::submit);
        return v;
    }
}


