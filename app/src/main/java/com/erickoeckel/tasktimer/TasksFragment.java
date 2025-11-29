package com.erickoeckel.tasktimer;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.*;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.*;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class TasksFragment extends Fragment {

    private TasksViewModel vm;
    private TasksAdapter adapter;
    private ImageButton fabAddTask, fabVoiceTask;
    private ActivityResultLauncher<String> micPermLauncher;
    private ActivityResultLauncher<Intent> speechLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup parent, @Nullable Bundle b) {
        View v = inf.inflate(R.layout.fragment_tasks, parent, false);

        vm = new ViewModelProvider(this).get(TasksViewModel.class);

        RecyclerView rv = v.findViewById(R.id.rvTasks);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new TasksAdapter((id, checked) -> {
            int remainingAfter = Math.max(0, adapter.countUndone() - 1);

            String taskTitle = "Task";
            Task tapped = adapter.findById(id);
            if (tapped != null && !android.text.TextUtils.isEmpty(tapped.getTitle())) {
                taskTitle = tapped.getTitle();
            }

            vm.completeTask(id);

            java.util.Map<String, Object> extra = new java.util.HashMap<>();
            extra.put("taskTitle", taskTitle);
            extra.put("tasksLeft", remainingAfter);
            AiCoach.generateAndNotify(
                    requireContext(),
                    AiCoach.EVENT_TASK_COMPLETED,
                    extra,
                    Notify.CH_REWARDS,
                    "Task completed!"
            );
        }, task -> {
            Bundle args = new Bundle();
            args.putString("sourceType", "task");
            args.putString("sourceId", task.getId());
            args.putString("sourceTitle", task.getTitle());
            args.putBoolean("autoStart", true);
            NavHostFragment.findNavController(TasksFragment.this)
                    .navigate(R.id.timerFragment, args);
        }, id -> {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(
                    requireContext(), R.style.ThemeOverlay_TaskTimer_Alert)
                    .setTitle("Delete task?")
                    .setMessage("This cannot be undone.")
                    .setPositiveButton("Delete", (d, w) -> vm.deleteTask(id))
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        rv.setAdapter(adapter);

        View infoBtn = v.findViewById(R.id.btnInfoTasks);
        if (infoBtn != null) {
            infoBtn.setOnClickListener(x -> HelpSheetFragment.show(
                    getChildFragmentManager(),
                    "Tasks",
                    "Quick tips to manage tasks:",
                    java.util.Arrays.asList(
                            "Tap a task to start a focus session.",
                            "Mic button = add by voice (e.g., “Buy milk tomorrow”).",
                            "Due dates color code: gold=today, gray=future, charcoal=past.",
                            "Tap the trash icon to delete a task."
                    )));
        }

        if (Onboarding.shouldShow(requireContext(), "help_tasks_v1")) {
            infoBtn.performClick();
            Onboarding.markShown(requireContext(), "help_tasks_v1");
        }

        fabAddTask = v.findViewById(R.id.fabAddTask);
        if (fabAddTask != null) {
            fabAddTask.setOnClickListener(view -> {
                new NewTaskDialog((taskTitle, dueYmd) -> {
                    String id = UUID.randomUUID().toString();
                    Task t = new Task(id, taskTitle);
                    t.setDueDate(dueYmd);
                    vm.addTask(t);
                }).show(getChildFragmentManager(), "newTask");
            });
        }

        fabVoiceTask = v.findViewById(R.id.fabVoiceTask);
        initVoiceLaunchers();
        if (fabVoiceTask != null) {
            fabVoiceTask.setOnClickListener(btn -> {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED) {
                    startVoiceInput();
                } else {
                    micPermLauncher.launch(Manifest.permission.RECORD_AUDIO);
                }
            });
        }

        vm.getTasks().observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(list);
            TextView tvEmpty = v.findViewById(R.id.tvEmpty);
            if (tvEmpty != null) {
                tvEmpty.setVisibility((list == null || list.isEmpty()) ? View.VISIBLE : View.GONE);
            }
        });

        return v;
    }

    private void initVoiceLaunchers() {
        micPermLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) startVoiceInput();
                    else Toast.makeText(requireContext(), "Microphone permission denied", Toast.LENGTH_SHORT).show();
                });

        speechLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
                        Toast.makeText(requireContext(), "No speech recognized", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ArrayList<String> matches =
                            result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (matches == null || matches.isEmpty()) {
                        Toast.makeText(requireContext(), "No speech recognized", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String spoken = matches.get(0);

                    ParsedTask pt = parseSpokenTask(spoken);
                    vm.createQuickTask(pt.title, pt.dueYmd, pt.priority)
                            .addOnSuccessListener(v -> {
                                Toast.makeText(
                                        requireContext(),
                                        "Added: " + pt.title + (pt.dueYmd != null ? " (due " + pt.dueYmd + ")" : ""),
                                        Toast.LENGTH_SHORT
                                ).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(requireContext(), "Voice task failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                            );
                });
    }

    private void startVoiceInput() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a task and optional due date");
        speechLauncher.launch(i);
    }

    private static final SimpleDateFormat YMD = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private static final Map<String, Integer> WORD_NUMS = new HashMap<>();
    static {
        String[][] pairs = {
                {"a","1"},{"an","1"},{"one","1"},{"two","2"},{"three","3"},{"four","4"},
                {"five","5"},{"six","6"},{"seven","7"},{"eight","8"},{"nine","9"},
                {"ten","10"},{"eleven","11"},{"twelve","12"}
        };
        for (String[] p : pairs) WORD_NUMS.put(p[0], Integer.parseInt(p[1]));
    }
    private static int wordOrInt(String token) {
        Integer w = WORD_NUMS.get(token.toLowerCase(Locale.US));
        if (w != null) return w;
        return Integer.parseInt(token);
    }

    private static class ParsedTask {
        final String title;
        final String dueYmd;
        final Integer priority;
        ParsedTask(String t, String d, Integer p) { title = t; dueYmd = d; priority = p; }
    }

    private ParsedTask parseSpokenTask(String raw) {
        if (raw == null) raw = "";
        String s = " " + raw.trim().toLowerCase(Locale.US) + " ";

        Integer priority = null;
        if (s.contains(" high priority")) { priority = 3; s = s.replace(" high priority", " "); }
        else if (s.contains(" medium priority")) { priority = 2; s = s.replace(" medium priority", " "); }
        else if (s.contains(" low priority")) { priority = 1; s = s.replace(" low priority", " "); }

        s = s.replace(" due on ", " due ")
                .replace(" due by ", " due ")
                .replace(" for ", " ")
                .replace(" on ", " ")
                .replaceAll("\\s+", " ");

        Date due = null;

        if (due == null && (s.contains(" tonight ") || s.contains(" this evening "))) {
            due = addDays(0);
            s = s.replace(" tonight ", " ").replace(" this evening ", " ");
        }

        if (due == null && s.contains(" today "))  { due = addDays(0); s = s.replace(" today ", " "); }
        if (due == null && s.contains(" tomorrow ")) { due = addDays(1); s = s.replace(" tomorrow ", " "); }

        if (due == null && s.contains(" this weekend ")) {
            due = upcomingSaturday(0);
            s = s.replace(" this weekend ", " ");
        }
        if (due == null && s.contains(" next weekend ")) {
            due = upcomingSaturday(1);
            s = s.replace(" next weekend ", " ");
        }

        if (due == null) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("\\b(?:in|within)\\s+(a|an|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|\\d{1,3})\\s+(day|days|week|weeks|month|months|year|years)\\b",
                            java.util.regex.Pattern.CASE_INSENSITIVE)
                    .matcher(s);
            if (m.find()) {
                int n = wordOrInt(m.group(1));
                String unit = m.group(2).toLowerCase(Locale.US);

                if (unit.startsWith("day")) {
                    due = addDays(n);
                } else if (unit.startsWith("week")) {
                    due = addDays(7 * n);
                } else if (unit.startsWith("month")) {
                    due = addMonths(n);
                } else { // year
                    Calendar c = Calendar.getInstance();
                    c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
                    c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
                    c.add(Calendar.YEAR, n);
                    due = c.getTime();
                }

                s = (s.substring(0, m.start()) + " " + s.substring(m.end()))
                        .replaceAll("\\s+", " ");
            }
        }

        String[] wds = {"sunday","monday","tuesday","wednesday","thursday","friday","saturday"};
        if (due == null) {
            for (String wd : wds) {
                if (s.contains(" next " + wd + " ")) {
                    due = nextWeekday(wdToCal(wd), true);
                    s = s.replace(" next " + wd + " ", " ");
                    break;
                }
            }
        }
        if (due == null) {
            for (String wd : wds) {
                if (s.contains(" " + wd + " ")) {
                    due = nextWeekday(wdToCal(wd), false);
                    s = s.replace(" " + wd + " ", " ");
                    break;
                }
            }
        }

        if (due == null) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("\\b(\\d{1,2})/(\\d{1,2})(?:/(\\d{2,4}))?\\b")
                    .matcher(s);
            if (m.find()) {
                int a = Integer.parseInt(m.group(1));
                int b = Integer.parseInt(m.group(2));
                Integer year = (m.group(3) != null) ? parseYear(m.group(3)) : null;

                int month, day;
                if (a > 12) {
                    day = a; month = b;
                } else if (b > 12) {
                    month = a; day = b;
                } else {
                    month = a; day = b;
                }

                due = (year != null) ? exactOrNextYear(month, day, year) : monthDayThisYearOrNext(month, day);
                s = (s.substring(0, m.start()) + " " + s.substring(m.end())).replaceAll("\\s+", " ");
            }
        }

        if (due == null) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("\\b(jan|january|feb|february|mar|march|apr|april|may|jun|june|jul|july|aug|august|sep|sept|september|oct|october|nov|november|dec|december)\\s+(\\d{1,2})(?:\\s+(\\d{2,4}))?\\b")
                    .matcher(s);
            if (m.find()) {
                int month = monthNameToInt(m.group(1));
                int day   = Integer.parseInt(m.group(2));
                Integer year = (m.group(3) != null) ? parseYear(m.group(3)) : null;
                due = (year != null) ? exactOrNextYear(month, day, year) : monthDayThisYearOrNext(month, day);
                s = (s.substring(0, m.start()) + " " + s.substring(m.end())).replaceAll("\\s+", " ");
            }
        }

        if (due == null) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("\\b(?:the\\s+)?(\\d{1,2})(st|nd|rd|th)(?:\\s+of\\s+(jan|january|feb|february|mar|march|apr|april|may|jun|june|jul|july|aug|august|sep|sept|september|oct|october|nov|november|dec|december))?(?:\\s+(\\d{2,4}))?\\b")
                    .matcher(s);
            if (m.find()) {
                int day = Integer.parseInt(m.group(1));
                String monTok = m.group(3);
                Integer year = (m.group(4) != null) ? parseYear(m.group(4)) : null;

                if (monTok != null) {
                    int month = monthNameToInt(monTok);
                    due = (year != null) ? exactOrNextYear(month, day, year) : monthDayThisYearOrNext(month, day);
                } else {
                    due = futureDateForDayOfMonth(day);
                }
                s = (s.substring(0, m.start()) + " " + s.substring(m.end())).replaceAll("\\s+", " ");
            }
        }

        s = s.replace(" due ", " ").replaceAll("\\s+", " ").trim();
        String title = s.isEmpty() ? "Task" : capitalizeFirst(s);

        String ymd = (due != null) ? YMD.format(due) : null;
        return new ParsedTask(title, ymd, priority);
    }

    private static Date addDays(int days) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.DATE, days);
        return c.getTime();
    }

    private static Date nextWeekday(int targetDow, boolean forceNextWeek) {
        Calendar c = Calendar.getInstance();
        int today = c.get(Calendar.DAY_OF_WEEK);
        int delta = targetDow - today;
        if (delta < 0) delta += 7;
        if (delta == 0 || forceNextWeek) delta += 7;
        c.add(Calendar.DATE, delta);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private static int wdToCal(String wd) {
        switch (wd) {
            case "sunday": return Calendar.SUNDAY;
            case "monday": return Calendar.MONDAY;
            case "tuesday": return Calendar.TUESDAY;
            case "wednesday": return Calendar.WEDNESDAY;
            case "thursday": return Calendar.THURSDAY;
            case "friday": return Calendar.FRIDAY;
            default: return Calendar.SATURDAY;
        }
    }

    private static Date monthDayThisYearOrNext(int month1to12, int dom) {
        Calendar now = Calendar.getInstance();
        Calendar cand = Calendar.getInstance();
        cand.set(Calendar.YEAR, now.get(Calendar.YEAR));
        cand.set(Calendar.MONTH, month1to12 - 1);
        cand.set(Calendar.DAY_OF_MONTH, dom);
        cand.set(Calendar.HOUR_OF_DAY, 0); cand.set(Calendar.MINUTE, 0);
        cand.set(Calendar.SECOND, 0); cand.set(Calendar.MILLISECOND, 0);
        if (cand.before(now)) cand.set(Calendar.YEAR, now.get(Calendar.YEAR) + 1);
        return cand.getTime();
    }

    private static int monthNameToInt(String token) {
        token = token.toLowerCase(Locale.US);
        String[] names = {"jan","feb","mar","apr","may","jun","jul","aug","sep","oct","nov","dec"};
        String t = token.substring(0, Math.min(3, token.length()));
        for (int i = 0; i < names.length; i++) if (names[i].equals(t)) return i + 1;
        return Calendar.getInstance().get(Calendar.MONTH) + 1;
    }

    private static String capitalizeFirst(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static Date addMonths(int months) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.MONTH, months);
        return c.getTime();
    }

    private static Date upcomingSaturday(int weekendsAhead) {
        Calendar c = Calendar.getInstance();
        int dow = c.get(Calendar.DAY_OF_WEEK);
        int delta = Calendar.SATURDAY - dow;
        if (delta < 0) delta += 7;
        c.add(Calendar.DATE, delta + (7 * weekendsAhead));
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private static Integer parseYear(String y) {
        try {
            int v = Integer.parseInt(y);
            if (v < 100) v += 2000;
            return v;
        } catch (Exception ignore) { return null; }
    }

    private static Date exactOrNextYear(int month1to12, int dom, int year) {
        Calendar cand = Calendar.getInstance();
        cand.set(Calendar.YEAR, year);
        cand.set(Calendar.MONTH, month1to12 - 1);
        cand.set(Calendar.DAY_OF_MONTH, dom);
        cand.set(Calendar.HOUR_OF_DAY, 0); cand.set(Calendar.MINUTE, 0);
        cand.set(Calendar.SECOND, 0); cand.set(Calendar.MILLISECOND, 0);
        return cand.getTime();
    }

    private static Date futureDateForDayOfMonth(int dayOfMonth) {
        Calendar now = Calendar.getInstance();
        Calendar cand = Calendar.getInstance();
        cand.set(Calendar.HOUR_OF_DAY, 0); cand.set(Calendar.MINUTE, 0);
        cand.set(Calendar.SECOND, 0); cand.set(Calendar.MILLISECOND, 0);

        cand.set(Calendar.DAY_OF_MONTH, 1);
        int max = cand.getActualMaximum(Calendar.DAY_OF_MONTH);
        cand.set(Calendar.DAY_OF_MONTH, Math.min(dayOfMonth, max));
        if (!cand.before(now)) return cand.getTime();

        cand.add(Calendar.MONTH, 1);
        cand.set(Calendar.DAY_OF_MONTH, 1);
        max = cand.getActualMaximum(Calendar.DAY_OF_MONTH);
        cand.set(Calendar.DAY_OF_MONTH, Math.min(dayOfMonth, max));
        return cand.getTime();
    }
}
