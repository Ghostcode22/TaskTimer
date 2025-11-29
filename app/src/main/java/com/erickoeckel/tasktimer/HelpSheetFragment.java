package com.erickoeckel.tasktimer;

import android.os.Bundle;
import android.view.*;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class HelpSheetFragment extends BottomSheetDialogFragment {
    private static final String ARG_TITLE   = "title";
    private static final String ARG_BODY    = "body";
    private static final String ARG_BULLETS = "bullets";

    public static void show(androidx.fragment.app.FragmentManager fm,
                            String title, String body, List<String> bullets) {
        Bundle b = new Bundle();
        b.putString(ARG_TITLE, title);
        b.putString(ARG_BODY, body);
        b.putStringArrayList(ARG_BULLETS, new ArrayList<>(bullets == null ? List.of() : bullets));
        HelpSheetFragment f = new HelpSheetFragment();
        f.setArguments(b);
        f.show(fm, "help_sheet");
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_help_sheet, container, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        String title = getArguments() == null ? "Help" : getArguments().getString(ARG_TITLE, "Help");
        String body  = getArguments() == null ? ""    : getArguments().getString(ARG_BODY, "");
        ArrayList<String> bullets = getArguments() == null ? new ArrayList<>() :
                getArguments().getStringArrayList(ARG_BULLETS);

        ((TextView) v.findViewById(R.id.tvTitle)).setText(title);
        ((TextView) v.findViewById(R.id.tvBody)).setText(body == null ? "" : body);

        LinearLayout host = v.findViewById(R.id.bulletHost);
        host.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(requireContext());
        if (bullets != null) {
            for (String b : bullets) {
                View row = inf.inflate(R.layout.item_help_bullet, host, false);
                ((TextView) row.findViewById(R.id.tvBullet)).setText(b);
                host.addView(row);
            }
        }

        ((MaterialButton) v.findViewById(R.id.btnOk)).setOnClickListener(x -> dismiss());
    }

    @Override public int getTheme() { return R.style.ThemeOverlay_TaskTimer_BottomSheet; }
}
