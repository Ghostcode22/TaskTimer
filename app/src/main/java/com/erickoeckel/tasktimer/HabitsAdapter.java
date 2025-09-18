package com.erickoeckel.tasktimer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.*;

public class HabitsAdapter extends RecyclerView.Adapter<HabitsAdapter.VH> {

    public interface Toggle { void onToggle(String id, boolean checked); }

    private final List<Habit> data = new ArrayList<>();
    private final Toggle toggle;

    public HabitsAdapter(Toggle toggle) { this.toggle = toggle; }
    public void submit(List<Habit> list) { data.clear(); if (list!=null) data.addAll(list); notifyDataSetChanged(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, streak;
        CheckBox today;
        VH(View v) { super(v);
            title = v.findViewById(R.id.habitTitle);
            streak = v.findViewById(R.id.habitStreak);
            today  = v.findViewById(R.id.habitToday);
        }
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
        View v = LayoutInflater.from(p.getContext()).inflate(R.layout.row_habit, p, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Habit hb = data.get(pos);
        h.title.setText(hb.getTitle());
        h.streak.setText("🔥 " + hb.getStreak());

        boolean activeToday = Habit.isActiveToday(hb.getDays());
        h.today.setEnabled(activeToday);
        h.today.setAlpha(activeToday ? 1f : 0.4f);

        boolean isToday = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date())
                .equals(hb.getLastCompleted());
        h.today.setOnCheckedChangeListener(null);
        h.today.setChecked(isToday);
        h.today.setOnCheckedChangeListener((CompoundButton b, boolean checked) ->
                toggle.onToggle(hb.getId(), checked));
    }

    @Override public int getItemCount() { return data.size(); }
}
