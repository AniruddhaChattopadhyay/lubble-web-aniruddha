package in.lubble.app.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import in.lubble.app.R;
import in.lubble.app.firebase.RealtimeDbHelper;


public class PinnedMessageBottomSheet extends BottomSheetDialogFragment {
    TextView pinnedMessageContent;
    RelativeLayout pinnedMessageContainer;

    String groupId;
    public PinnedMessageBottomSheet(String gid){
        this.groupId = gid;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootview = inflater.inflate(R.layout.pinned_message_bottom_sheet_layout, container, false);
        pinnedMessageContent = rootview.findViewById(R.id.pinned_message_content);
        pinnedMessageContainer = rootview.findViewById(R.id.pinned_message_container);


        RealtimeDbHelper.getLubbleGroupsRef().child(groupId).child("pinned_message").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String message = snapshot.getValue(String.class);
                    pinnedMessageContainer.setVisibility(View.VISIBLE);
                    pinnedMessageContent.setText(message);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        return rootview;
    }
}