package com.erickoeckel.tasktimer;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class CoachFragment extends Fragment {
    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        View v = inf.inflate(R.layout.fragment_coach, c, false);
        EditText et = v.findViewById(R.id.etQuestion);
        TextView tv = v.findViewById(R.id.tvAnswer);
        v.findViewById(R.id.btnAsk).setOnClickListener(x -> {
                String q = String.valueOf(et.getText()).trim();
                if (q.isEmpty()) return;

                tv.setText("Thinking...");

                AiCoach.ask(requireContext(), q, null)
                        .addOnSuccessListener(ans -> tv.setText(ans))
                        .addOnFailureListener(e -> tv.setText("Couldn't reach coach."));
        });
        com.google.firebase.functions.FirebaseFunctions.getInstance("us-central1")
                .getHttpsCallable("coachMessage")
                .call(java.util.Map.of("event","ASK","question","Say hi"))
                .addOnSuccessListener(r -> android.util.Log.d("CoachTest","OK: "+r.getData()))
                .addOnFailureListener(e -> android.util.Log.e("CoachTest","FAIL", e));

        return v;
    }
}
