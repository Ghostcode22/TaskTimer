package com.erickoeckel.tasktimer;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import java.util.List;

public class FriendsFragment extends Fragment {
    private FriendsViewModel vm;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        View v = inf.inflate(R.layout.fragment_friends, c, false);
        vm = new ViewModelProvider(requireActivity()).get(FriendsViewModel.class);

        v.findViewById(R.id.btnAddFriend).setOnClickListener(x -> showAddDialog());

        ListView lvIncoming = v.findViewById(R.id.lvIncoming);
        ListView lvOutgoing = v.findViewById(R.id.lvOutgoing);
        ListView lvFriends  = v.findViewById(R.id.lvFriends);

        vm.incoming().observe(getViewLifecycleOwner(), list -> {
            lvIncoming.setAdapter(new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_list_item_1,
                    labelsForRequests(list, true)));
            lvIncoming.setOnItemClickListener((a,view,pos,id) -> {
                var r = vm.incoming().getValue().get(pos);
                new android.app.AlertDialog.Builder(requireContext())
                        .setItems(new String[]{"Accept","Decline"}, (d,which)->{
                            if (which==0) vm.accept(r.id, r.fromUid);
                            else vm.decline(r.id);
                        }).show();
            });
        });

        vm.outgoing().observe(getViewLifecycleOwner(), list -> {
            lvOutgoing.setAdapter(new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_list_item_1,
                    labelsForRequests(list, false)));
            lvOutgoing.setOnItemClickListener((a,view,pos,id) -> {
                var r = vm.outgoing().getValue().get(pos);
                new android.app.AlertDialog.Builder(requireContext())
                        .setItems(new String[]{"Cancel"}, (d,which)-> vm.cancel(r.id)).show();
            });
        });

        vm.friends().observe(getViewLifecycleOwner(), ids -> {
            lvFriends.setAdapter(new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_list_item_1, ids));
            lvFriends.setOnItemClickListener((a,view,pos,id) -> {
                String fid = vm.friends().getValue().get(pos);
                new android.app.AlertDialog.Builder(requireContext())
                        .setItems(new String[]{"Unfriend"}, (d,which)-> vm.unfriend(fid)).show();
            });
        });

        return v;
    }

    private void showAddDialog() {
        final EditText et = new EditText(requireContext());
        et.setHint("friend@email.com");
        et.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(
                requireContext(), R.style.ThemeOverlay_TaskTimer_Dialog)
                .setTitle("Add friend by email")
                .setView(et)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Send", (d, w) -> {
                    String email = String.valueOf(et.getText()).trim();
                    if (!email.isEmpty()) {
                        vm.sendRequestToEmail(email)
                                .addOnFailureListener(e ->
                                        Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show());
                    }
                })
                .show();
    }


    private static String[] labelsForRequests(List<FriendRequest> reqs, boolean incoming) {
        String[] arr = new String[reqs.size()];
        for (int i=0;i<reqs.size();i++) {
            FriendRequest r = reqs.get(i);
            arr[i] = (incoming ? "From: " + r.fromUid : "To: " + r.toUid) + " (" + r.status + ")";
        }
        return arr;
    }
}
