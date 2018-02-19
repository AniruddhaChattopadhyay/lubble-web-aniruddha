package in.lubble.app.user_search;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import in.lubble.app.GlideApp;
import in.lubble.app.R;
import in.lubble.app.models.ProfileInfo;

import static in.lubble.app.firebase.RealtimeDbHelper.getUserInfoRef;

public class SelectedUserAdapter extends RecyclerView.Adapter<SelectedUserAdapter.ViewHolder> {

    private final List<String> selectedUsersList;
    private final OnUserSelectedListener mListener;

    public SelectedUserAdapter(OnUserSelectedListener listener) {
        selectedUsersList = new ArrayList<>();
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_selected_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final String userId = selectedUsersList.get(position);

        getUserInfoRef(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final ProfileInfo profileInfo = dataSnapshot.getValue(ProfileInfo.class);
                if (profileInfo != null) {
                    holder.nameTv.setText(profileInfo.getName().split(" ")[0]);
                    GlideApp.with(holder.itemView.getContext())
                            .load(profileInfo.getThumbnail())
                            .placeholder(R.drawable.ic_account_circle_black_no_padding)
                            .circleCrop()
                            .into(holder.iconIv);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    mListener.onUserDeSelected(selectedUsersList.get(holder.getAdapterPosition()));
                }
            }
        });
    }

    public void addUser(String uid) {
        if (selectedUsersList.indexOf(uid) == -1) {
            // only add if user not in list already
            selectedUsersList.add(uid);
            notifyItemInserted(selectedUsersList.size());
        }
    }

    public void removeUser(String uid) {
        final int position = selectedUsersList.indexOf(uid);
        if (position != -1) {
            // only remove if user exists in list already
            selectedUsersList.remove(uid);
            notifyItemRemoved(position);
        }
    }

    @Override
    public int getItemCount() {
        return selectedUsersList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        final View mView;
        final ImageView iconIv;
        final TextView nameTv;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            iconIv = view.findViewById(R.id.iv_icon);
            nameTv = view.findViewById(R.id.tv_title);
        }
    }
}
