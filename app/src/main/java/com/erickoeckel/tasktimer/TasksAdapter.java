package com.erickoeckel.tasktimer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.VH> {

    public interface Toggle {
        void onToggle(String id, boolean done);
    }

    private final List<Task> data;
    private final Toggle toggle;

    public TasksAdapter(List<Task> data, Toggle toggle) {
        this.data = (data == null) ? new ArrayList<>() : data;
        this.toggle = toggle;
    }

    public void submit(List<Task> newData) {
        this.data.clear();
        if (newData != null) this.data.addAll(newData);
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        CheckBox cb;
        TextView title;
        VH(View v) {
            super(v);
            cb = v.findViewById(R.id.cb);
            title = v.findViewById(R.id.title);
        }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_task, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Task t = data.get(pos);

        // Avoid triggering the listener while we set state
        h.cb.setOnCheckedChangeListener(null);

        h.title.setText(t.getTitle());
        h.cb.setChecked(t.isDone());

        h.cb.setOnCheckedChangeListener((CompoundButton b, boolean isChecked) -> {
            if (toggle != null) toggle.onToggle(t.getId(), isChecked);
        });
    }

    @Override
    public int getItemCount() { return data.size(); }
}

