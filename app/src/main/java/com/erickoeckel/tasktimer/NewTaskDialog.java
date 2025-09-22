package com.erickoeckel.tasktimer;

import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class NewTaskDialog extends DialogFragment {

    public interface Callback {
        void onCreate(String title, @Nullable String dueYmd);
    }

    private final Callback cb;
    public NewTaskDialog(Callback cb) { this.cb = cb; }

    private Long selectedUtcMidnight = null;

    @NonNull @Override
    public Dialog onCreateDialog(@Nullable android.os.Bundle savedInstanceState) {
        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_new_task, null, false);

        TextInputEditText etTitle = v.findViewById(R.id.etTaskTitle);
        TextView tvDue = v.findViewById(R.id.tvDueValue);
        View btnPick = v.findViewById(R.id.btnPickDue);
        View btnClear = v.findViewById(R.id.btnClearDue);

        btnPick.setOnClickListener(view -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select due date")
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .setTheme(R.style.ThemeOverlay_TaskTimer_DatePicker)
                    .build();
            picker.addOnPositiveButtonClickListener(sel -> {
                selectedUtcMidnight = sel;
                String ymd = toLocalYMD(sel);
                tvDue.setText(pretty(ymd));
            });
            picker.show(getParentFragmentManager(), "duePicker");
        });


        btnClear.setOnClickListener(view -> {
            selectedUtcMidnight = null;
            tvDue.setText("None");
        });

        return new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_TaskTimer_Dialog)
                .setTitle("New Task")
                .setView(v)
                .setNegativeButton("Cancel", (d,w) -> {})
                .setPositiveButton("Create", (d,w) -> {
                    String title = String.valueOf(etTitle.getText()).trim();
                    if (title.isEmpty()) return;
                    String dueYmd = (selectedUtcMidnight == null) ? null : toLocalYMD(selectedUtcMidnight);
                    if (cb != null) cb.onCreate(title, dueYmd);
                })
                .create();
    }

    private static String toLocalYMD(long utcMs) {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(utcMs);
        Calendar local = Calendar.getInstance();
        local.clear();
        local.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH));
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(local.getTime());
    }

    private static String pretty(String ymd) {
        try {
            var in = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(ymd);
            return "Due " + new SimpleDateFormat("MMM d", Locale.US).format(in);
        } catch (Exception e) { return "Due " + ymd; }
    }
}
