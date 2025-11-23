package com.erickoeckel.tasktimer;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.*;

public class HabitsAdapter extends RecyclerView.Adapter<HabitsAdapter.VH> {

    public interface Toggle { void onToggle(String id, boolean checked); }
    public interface Opener { void onOpen(@NonNull Habit habit); }
    public interface Remover { void onRemove(@NonNull String id); }

    private final List<Habit> data = new ArrayList<>();
    private final Toggle toggle;
    @Nullable private final Opener opener;
    @Nullable private final Remover remover;

    private final Map<String, Set<String>> weekCache = new HashMap<>();

    public HabitsAdapter(Toggle toggle) { this(toggle, null, null); }

    public HabitsAdapter(Toggle toggle, @Nullable Opener opener) {
        this(toggle, opener, null);
    }

    public HabitsAdapter(Toggle toggle, @Nullable Opener opener, @Nullable Remover remover) {
        this.toggle = toggle;
        this.opener = opener;
        this.remover = remover;
    }

    public void submit(List<Habit> list) {
        data.clear();
        weekCache.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, streak, days;
        CheckBox today;
        ImageButton btnDelete;
        String boundHabitId;
        VH(View v) {
            super(v);
            title  = v.findViewById(R.id.habitTitle);
            streak = v.findViewById(R.id.habitStreak);
            days   = v.findViewById(R.id.habitDays);
            today  = v.findViewById(R.id.habitToday);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_habit, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Habit hb = data.get(pos);
        h.boundHabitId = hb.getId();
        h.title.setText(hb.getTitle());
        h.streak.setText("🔥 " + hb.getStreak());

        String todayStr = today();
        boolean isTodayChecked = todayStr.equals(hb.getLastCompleted());
        int dow = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK);
        int todayIdx = (dow == java.util.Calendar.SUNDAY) ? 0 : (dow - java.util.Calendar.SUNDAY);

        boolean isScheduledToday = false;
        java.util.List<Boolean> sched = hb.getDays();
        if (sched != null && sched.size() >= 7) {
            Boolean v = sched.get(todayIdx);
            isScheduledToday = (v != null && v);
        }

        if (opener != null) {
            if (isScheduledToday && !isTodayChecked) {
                h.itemView.setOnClickListener(v -> opener.onOpen(hb));
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

        h.today.setOnCheckedChangeListener(null);
        h.today.setChecked(isTodayChecked);

        h.today.setEnabled(false);
        h.today.setAlpha(1f);

        // Delete button
        if (h.btnDelete != null) {
            h.btnDelete.setOnClickListener(v -> {
                if (remover != null && hb.getId() != null) remover.onRemove(hb.getId());
            });
        }

        renderDays(h.days, hb.getDays(), hb.getLastCompleted(), null);

        loadWeekCompletions(hb.getId(), set -> {
            if (!Objects.equals(h.boundHabitId, hb.getId())) return;
            weekCache.put(hb.getId(), set);
            renderDays(h.days, hb.getDays(), hb.getLastCompleted(), set);
        });
    }

    @Override public int getItemCount() { return data.size(); }

    private static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private void renderDays(TextView tv, List<Boolean> schedule, String lastCompleted, Set<String> weekCompleted) {
        final String[] letters = {"S","M","T","W","T","F","S"};

        Calendar cal = startOfWeekCalendar();
        List<String> weekDates = new ArrayList<>(7);
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String todayStr = f.format(new Date());

        for (int i = 0; i < 7; i++) {
            weekDates.add(f.format(cal.getTime()));
            cal.add(Calendar.DATE, 1);
        }

        int colorInactive = ContextCompat.getColor(tv.getContext(), R.color.charcoal);
        int colorScheduled = ContextCompat.getColor(tv.getContext(), R.color.light_gray);
        int colorCompleted = ContextCompat.getColor(tv.getContext(), R.color.gold);
        int colorMissed    = ContextCompat.getColor(tv.getContext(), R.color.red);

        SpannableStringBuilder sb = new SpannableStringBuilder();

        for (int i = 0; i < 7; i++) {
            String date = weekDates.get(i);
            boolean scheduled = (schedule != null && schedule.size() >= 7 && Boolean.TRUE.equals(schedule.get(i)));

            int start = sb.length();
            sb.append(letters[i]);
            int end = sb.length();

            if (!scheduled) {
                sb.setSpan(new ForegroundColorSpan(colorInactive), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                boolean isPast   = date.compareTo(todayStr) < 0;
                boolean isToday  = date.equals(todayStr);
                boolean done     = (weekCompleted != null && weekCompleted.contains(date))
                        || (isToday && todayStr.equals(lastCompleted));

                if (done) {
                    sb.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sb.setSpan(new ForegroundColorSpan(colorCompleted), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else if (isPast) {
                    sb.setSpan(new ForegroundColorSpan(colorMissed), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else {
                    sb.setSpan(new ForegroundColorSpan(colorScheduled), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                if (isToday) {
                    sb.setSpan(new UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            if (i < 6) sb.append(" ");
        }

        tv.setText(sb);
    }

    private static Calendar startOfWeekCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        int daysBack = (dow == Calendar.SUNDAY) ? 0 : (dow - Calendar.SUNDAY);
        cal.add(Calendar.DATE, -daysBack);
        return cal;
    }

    private void loadWeekCompletions(String habitId, java.util.function.Consumer<Set<String>> cb) {
        if (weekCache.containsKey(habitId)) { cb.accept(weekCache.get(habitId)); return; }

        Calendar start = startOfWeekCalendar();
        Calendar end = (Calendar) start.clone(); end.add(Calendar.DATE, 6);
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String startId = f.format(start.getTime());
        String endId   = f.format(end.getTime());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("habits").document(habitId)
                .collection("completions")
                .whereGreaterThanOrEqualTo(FieldPath.documentId(), startId)
                .whereLessThanOrEqualTo(FieldPath.documentId(), endId)
                .get()
                .addOnSuccessListener(snap -> {
                    Set<String> set = new HashSet<>();
                    for (com.google.firebase.firestore.DocumentSnapshot d : snap.getDocuments()) {
                        set.add(d.getId());
                    }
                    cb.accept(set);
                })
                .addOnFailureListener(e -> cb.accept(Collections.emptySet()));
    }
}
