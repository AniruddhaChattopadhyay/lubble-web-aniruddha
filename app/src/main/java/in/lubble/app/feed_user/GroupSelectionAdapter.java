package in.lubble.app.feed_user;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.radiobutton.MaterialRadioButton;

import java.util.ArrayList;
import java.util.List;

import in.lubble.app.R;
import in.lubble.app.models.FeedGroupData;

public class GroupSelectionAdapter extends RecyclerView.Adapter<GroupSelectionAdapter.GroupSelectionViewHolder> {

    private int lastCheckedPos = 0;
    private final List<FeedGroupData> stringList;
    private final List<FeedGroupData> stringListCopy;

    public GroupSelectionAdapter(List<FeedGroupData> stringList) {
        this.stringList = stringList;
        stringListCopy = new ArrayList<>();
        stringListCopy.addAll(stringList);
    }

    @NonNull
    @Override
    public GroupSelectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group_selection, parent, false);
        return new GroupSelectionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupSelectionViewHolder holder, int position) {
        FeedGroupData feedGroupData = stringList.get(position);

        holder.titleTv.setText(feedGroupData.getName());

        holder.selectionRb.setChecked(position == lastCheckedPos);
        holder.titleTv.setOnClickListener(v -> holder.selectionRb.performClick());
        holder.selectionRb.setOnClickListener(v -> {
            int copyOfLastCheckedPosition = lastCheckedPos;
            lastCheckedPos = holder.getAdapterPosition();
            notifyItemChanged(copyOfLastCheckedPosition);
            notifyItemChanged(lastCheckedPos);
        });
    }

    int getLastCheckedPos() {
        return lastCheckedPos;
    }

    public void filter(String text) {
        stringList.clear();
        if (text.isEmpty()) {
            stringList.addAll(stringListCopy);
        } else {
            text = text.toLowerCase();
            for (FeedGroupData groupData : stringListCopy) {
                if (groupData.getName().toLowerCase().contains(text) || groupData.getName().toLowerCase().contains(text)) {
                    stringList.add(groupData);
                }
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return stringList.size();
    }

    public static class GroupSelectionViewHolder extends RecyclerView.ViewHolder {
        final View mView;
        final TextView titleTv;
        final MaterialRadioButton selectionRb;

        GroupSelectionViewHolder(View view) {
            super(view);
            mView = view;
            titleTv = view.findViewById(R.id.tv_group_name);
            selectionRb = view.findViewById(R.id.rb_group_selection);
        }

    }

}
