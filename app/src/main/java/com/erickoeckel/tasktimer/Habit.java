package com.erickoeckel.tasktimer;

import java.util.ArrayList;
import java.util.List;

public class Habit {
    private String id;
    private String title;
    private List<Boolean> days;
    private boolean archived;
    private int streak;
    private String lastCompleted;

    public Habit() {}

    public Habit(String id, String title, List<Boolean> days) {
        this.id = id;
        this.title = title;
        this.days = (days == null) ? new ArrayList<>() : days;
        this.archived = false;
        this.streak = 0;
        this.lastCompleted = null;
    }

    public static List<Boolean> fromArray(boolean[] arr) {
        List<Boolean> out = new ArrayList<>(7);
        if (arr != null) for (boolean b : arr) out.add(b);
        return out;
    }

    public static boolean isActiveToday(java.util.List<Boolean> days) {
        if (days == null || days.size() < 7) return true; // default to active if not configured
        java.util.Calendar c = java.util.Calendar.getInstance();
        int dow = c.get(java.util.Calendar.DAY_OF_WEEK); // 1=Sun..7=Sat
        int idx = (dow == java.util.Calendar.SUNDAY) ? 0 : (dow - 1);
        Boolean b = days.get(idx);
        return b != null && b;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<Boolean> getDays() { return days; }
    public void setDays(List<Boolean> days) { this.days = days; }

    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }

    public int getStreak() { return streak; }
    public void setStreak(int streak) { this.streak = streak; }

    public String getLastCompleted() { return lastCompleted; }
    public void setLastCompleted(String lastCompleted) { this.lastCompleted = lastCompleted; }
}
