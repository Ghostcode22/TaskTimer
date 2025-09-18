package com.erickoeckel.tasktimer;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class NewHabitDialog extends DialogFragment {

    public static class Payload { public final String title; public final boolean[] days;
        public Payload(String t, boolean[] d){ title=t; days=d; } }
    public interface Callback { void onCreate(Payload payload); }

    private final Callback cb;
    public NewHabitDialog(Callback cb) { this.cb = cb; }

    @NonNull @Override public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        var v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_new_habit, null, false);
        EditText et = v.findViewById(R.id.etHabitTitle);
        CheckBox sun=v.findViewById(R.id.d0), mon=v.findViewById(R.id.d1), tue=v.findViewById(R.id.d2),
                wed=v.findViewById(R.id.d3), thu=v.findViewById(R.id.d4), fri=v.findViewById(R.id.d5),
                sat=v.findViewById(R.id.d6);

        sun.setChecked(true); mon.setChecked(true); tue.setChecked(true); wed.setChecked(true);
        thu.setChecked(true); fri.setChecked(true); sat.setChecked(true);

        return new AlertDialog.Builder(requireContext())
                .setTitle("New Habit")
                .setView(v)
                .setPositiveButton("Create", (d, w) -> {
                    String title = et.getText().toString().trim();
                    if (title.isEmpty()) {
                        et.setError("Title required");
                        return;
                    }
                    boolean[] days = new boolean[]{
                            sun.isChecked(), mon.isChecked(), tue.isChecked(),
                            wed.isChecked(), thu.isChecked(), fri.isChecked(), sat.isChecked()
                    };
                    if (cb != null) cb.onCreate(new Payload(title, days));
                })

                .setNegativeButton("Cancel", null)
                .create();
    }
}
