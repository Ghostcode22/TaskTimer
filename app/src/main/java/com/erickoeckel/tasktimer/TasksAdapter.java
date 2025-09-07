package com.erickoeckel.tasktimer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.VH> {
    interface Toggle { void onToggle(String id, boolean done); }
    private final List<Task> data;
    private final Toggle toggle;

    public TasksAdapter(List<Task> data, Toggle toggle) { this.data = data; this.toggle = toggle; }
    public void submit(List<Task> newData) { data.clear(); data.addAll(newData); notifyDataSetChanged(); }

    static class VH extends RecyclerView.ViewHolder {
        CheckBox cb; TextView title;
        VH(View v) { super(v); cb = v.findViewById(R.id.cb); title = v.findViewById(R.id.title); }
    }

    @NonNull
    @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int viewType) {
        View v = LayoutInflater.from(p.getContext()).inflate(R.layout.row_task, p, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Task t = data.get(pos);
        h.title.setText(t.title);
        h.cb.setOnCheckedChangeListener(null);
        h.cb.setChecked(t.done);
        h.cb.setOnCheckedChangeListener((b, isChecked) -> toggle.onToggle(t.id, isChecked));
    }

    @Override public int getItemCount() { return data.size(); }
}
