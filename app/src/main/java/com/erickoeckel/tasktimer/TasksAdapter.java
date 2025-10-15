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
        void onToggle(@NonNull String taskId, boolean checked);
    }

    private final Toggle toggle;
    private final List<Task> data = new ArrayList<>();

    public TasksAdapter(@NonNull Toggle toggle) {
        this.toggle = toggle;
    }

    public static class VH extends RecyclerView.ViewHolder {
        CheckBox cb;
        TextView title;
        public VH(@NonNull View v) {
            super(v);
            cb = v.findViewById(R.id.cb);
            title = v.findViewById(R.id.title);
        }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_task, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Task t = data.get(pos);

        h.cb.setOnCheckedChangeListener(null);
        h.title.setText(t.getTitle());
        h.cb.setChecked(t.isDone());

        boolean canToggle = !t.isDone();
        h.cb.setEnabled(canToggle);
        h.cb.setAlpha(canToggle ? 1f : 0.4f);

        h.cb.setOnCheckedChangeListener((CompoundButton b, boolean checked) -> {
            if (canToggle && checked && toggle != null) {
                toggle.onToggle(t.getId(), true);
            }
        });
        TextView tvDue = h.itemView.findViewById(R.id.tvDue);
        String ymd = t.getDueDate(); // must be "yyyy-MM-dd"
        if (ymd == null || ymd.trim().isEmpty()) {
            tvDue.setVisibility(View.GONE);
        } else {
            tvDue.setVisibility(View.VISIBLE);
            tvDue.setText("Due " + pretty(ymd));

            String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    .format(new java.util.Date());

            int color;
            if (ymd.equals(today)) {
                color = androidx.core.content.ContextCompat.getColor(tvDue.getContext(), R.color.gold);
            } else if (ymd.compareTo(today) > 0) {
                color = androidx.core.content.ContextCompat.getColor(tvDue.getContext(), R.color.light_gray);
            } else {
                color = androidx.core.content.ContextCompat.getColor(tvDue.getContext(), R.color.charcoal);
            }
            tvDue.setTextColor(color);
        }
    }

    private static String pretty(String ymd) {
        try {
            java.text.SimpleDateFormat in = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            java.util.Date d = in.parse(ymd);
            java.text.SimpleDateFormat out = new java.text.SimpleDateFormat("MMM d", java.util.Locale.US);
            return out.format(d);
        } catch (Exception e) {
            return ymd;
        }
    }


    @Override
    public int getItemCount() { return data.size(); }

    public void submitList(@NonNull List<Task> tasks) {
        data.clear();
        data.addAll(tasks);
        notifyDataSetChanged();
    }

    public int countUndone() {
        int n = 0; for (Task t : data) if (!t.isDone()) n++; return n;
    }

    public Task findById(@NonNull String id) {
        for (Task t : data) if (id.equals(t.getId())) return t;
        return null;
    }
}
