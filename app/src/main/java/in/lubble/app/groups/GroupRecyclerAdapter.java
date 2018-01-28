package in.lubble.app.groups;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import in.lubble.app.GlideApp;
import in.lubble.app.R;
import in.lubble.app.models.GroupData;

public class GroupRecyclerAdapter extends RecyclerView.Adapter<GroupRecyclerAdapter.GroupViewHolder> {

    private final List<GroupData> groupDataList;
    private final OnListFragmentInteractionListener mListener;

    public GroupRecyclerAdapter(OnListFragmentInteractionListener listener) {
        groupDataList = new ArrayList<>();
        mListener = listener;
    }

    @Override
    public GroupViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_group_list, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final GroupViewHolder holder, int position) {
        final GroupData groupData = groupDataList.get(position);
        holder.groupData = groupData;

        GlideApp.with(holder.mView)
                .load(groupData.getIconUrl())
                .circleCrop()
                .centerCrop()
                .into(holder.iconIv);

        holder.titleTv.setText(groupData.getTitle());
        holder.subtitleTv.setText(groupData.getDescription());

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListFragmentInteraction(holder.groupData);
                }
            }
        });
    }

    public void addGroup(GroupData groupData) {
        groupDataList.add(groupData);
        notifyItemInserted(getItemCount());
    }

    public void updateGroup(GroupData newGroupData) {
        for (GroupData groupData : groupDataList) {
            if (newGroupData.equals(groupData)) {
                final int pos = groupDataList.indexOf(groupData);
                groupDataList.set(pos, newGroupData);
                notifyItemChanged(pos);
                return;
            }
        }
    }

    public void clearGroups() {
        groupDataList.clear();
    }

    @Override
    public int getItemCount() {
        return groupDataList.size();
    }

    class GroupViewHolder extends RecyclerView.ViewHolder {
        final View mView;
        final ImageView iconIv;
        final TextView titleTv;
        final TextView subtitleTv;
        GroupData groupData;

        public GroupViewHolder(View view) {
            super(view);
            mView = view;
            iconIv = view.findViewById(R.id.iv_group_pic);
            titleTv = view.findViewById(R.id.tv_title);
            subtitleTv = view.findViewById(R.id.tv_subtitle);
        }
    }
}