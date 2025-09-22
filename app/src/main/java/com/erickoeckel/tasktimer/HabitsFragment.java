package com.erickoeckel.tasktimer;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;


public class HabitsFragment extends Fragment {

    private HabitsViewModel vm;
    private HabitsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_habits, container, false);

        vm = new ViewModelProvider(requireActivity()).get(HabitsViewModel.class);

        String uid = (FirebaseAuth.getInstance().getCurrentUser() != null)
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "null";
        String path = "users/" + uid + "/habits";
        Log.d("HabitsDebug", "UID=" + uid + " path=" + path);

        FirebaseFirestore.getInstance().collection("users")
                .document(uid)
                .collection("habits")
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> Log.d("HabitsDebug", "probe GET OK; docs=" + snap.size()))
                .addOnFailureListener(e -> Log.e("HabitsDebug", "probe GET FAILED", e));

        RecyclerView rv = v.findViewById(R.id.rvHabits);
        adapter = new HabitsAdapter((id, checked) -> {
            if (checked) vm.toggleToday(requireContext(), id, true);
        });
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        final TextView tvEmpty = v.findViewById(R.id.tvEmpty);

        vm.getHabits().observe(getViewLifecycleOwner(), list -> {
            android.util.Log.d("HabitsUI", "render size=" + (list == null ? 0 : list.size()));
            adapter.submit(list);
            boolean isEmpty = (list == null || list.isEmpty());
            tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        });

        v.findViewById(R.id.fabAddHabit).setOnClickListener(btn -> {
            new NewHabitDialog(payload -> {
                String id = java.util.UUID.randomUUID().toString();
                java.util.List<Boolean> days = Habit.fromArray(payload.days);
                vm.addHabit(new Habit(id, payload.title, days));
            }).show(getChildFragmentManager(), "newHabit");
        });

        return v;
    }
}
