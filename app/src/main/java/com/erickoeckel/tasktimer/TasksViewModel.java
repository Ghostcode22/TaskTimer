package com.erickoeckel.tasktimer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.List;

public class TasksViewModel extends ViewModel {
    private final MutableLiveData<List<Task>> tasks = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<Task>> getTasks() { return tasks; }

    public void addTask(Task t) {
        List<Task> cur = new ArrayList<>(tasks.getValue());
        cur.add(0, t);
        tasks.setValue(cur);
    }

    public void toggleDone(String id, boolean done) {
        List<Task> cur = new ArrayList<>(tasks.getValue());
        for (Task t : cur) if (t.id.equals(id)) { t.done = done; break; }
        tasks.setValue(cur);
    }
}
