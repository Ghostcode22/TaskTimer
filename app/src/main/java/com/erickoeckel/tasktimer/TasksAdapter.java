package com.erickoeckel.tasktimer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.VH> {

    public interface Toggle {
        void onToggle(@NonNull String taskId, boolean checked);
    }
    public interface Opener {
        void onOpen(@NonNull Task task);
    }
    public interface Remover {
        void onRemove(@NonNull String taskId);
    }

    private final Toggle toggle;
    @Nullable private final Opener opener;
    @Nullable private final Remover remover;

    private final List<Task> data = new ArrayList<>();

    public TasksAdapter(@NonNull Toggle toggle) { this(toggle, null, null); }

    public TasksAdapter(@NonNull Toggle toggle, @Nullable Opener opener) {
        this(toggle, opener, null);
    }

    public TasksAdapter(@NonNull Toggle toggle, @Nullable Opener opener, @Nullable Remover remover) {
        this.toggle = toggle;
        this.opener = opener;
        this.remover = remover;
    }

    public static class VH extends RecyclerView.ViewHolder {
        CheckBox cb;
        TextView title;
        ImageButton btnDelete;
        TextView tvDue;

        public VH(@NonNull View v) {
            super(v);
            cb = v.findViewById(R.id.cb);
            title = v.findViewById(R.id.title);
            tvDue = v.findViewById(R.id.tvDue);
            btnDelete = v.findViewById(R.id.btnDelete);
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
        boolean isDone = t.isDone();

        h.title.setText(t.getTitle());
        h.cb.setOnCheckedChangeListener(null);
        h.cb.setChecked(isDone);
        h.cb.setEnabled(false);
        h.cb.setAlpha(1f);

        if (opener != null) {
            if (!isDone) {
                h.itemView.setOnClickListener(v -> opener.onOpen(t));
                h.itemView.setClickable(true);
                h.itemView.setEnabled(true);
                h.title.setAlpha(1f);
            } else {
                h.itemView.setOnClickListener(null);
                h.itemView.setClickable(false);
                h.itemView.setEnabled(false);
                h.title.setAlpha(0.5f);
            }
        }

        // Due date color/visibility
        String ymd = t.getDueDate();
        if (h.tvDue != null) {
            if (ymd == null || ymd.trim().isEmpty()) {
                h.tvDue.setVisibility(View.GONE);
            } else {
                h.tvDue.setVisibility(View.VISIBLE);
                h.tvDue.setText("Due " + pretty(ymd));

                String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        .format(new java.util.Date());

                int color;
                if (ymd.equals(today)) {
                    color = androidx.core.content.ContextCompat.getColor(h.tvDue.getContext(), R.color.gold);
                } else if (ymd.compareTo(today) > 0) {
                    color = androidx.core.content.ContextCompat.getColor(h.tvDue.getContext(), R.color.light_gray);
                } else {
                    color = androidx.core.content.ContextCompat.getColor(h.tvDue.getContext(), R.color.charcoal);
                }
                h.tvDue.setTextColor(color);
            }
        }

        // Delete button
        if (h.btnDelete != null) {
            h.btnDelete.setOnClickListener(v -> {
                if (remover != null && t.getId() != null) {
                    remover.onRemove(t.getId());
                }
            });
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
        int n = 0;
        for (Task t : data) if (!t.isDone()) n++;
        return n;
    }

    public Task findById(@NonNull String id) {
        for (Task t : data) if (id.equals(t.getId())) return t;
        return null;
    }
}
