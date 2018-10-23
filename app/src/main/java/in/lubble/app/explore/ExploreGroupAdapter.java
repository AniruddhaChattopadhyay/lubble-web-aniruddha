package in.lubble.app.explore;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.request.RequestOptions;
import in.lubble.app.GlideRequests;
import in.lubble.app.R;
import in.lubble.app.utils.RoundedCornersTransformation;
import in.lubble.app.utils.UiUtils;

import java.util.List;

import static in.lubble.app.utils.RoundedCornersTransformation.CornerType.TOP;

public class ExploreGroupAdapter extends RecyclerView.Adapter<ExploreGroupAdapter.ViewHolder> {

    private final List<ExploreGroupData> mValues;
    private final OnListFragmentInteractionListener mListener;
    private final GlideRequests glide;

    public ExploreGroupAdapter(List<ExploreGroupData> items, OnListFragmentInteractionListener listener, GlideRequests glide) {
        mValues = items;
        mListener = listener;
        this.glide = glide;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_explore_group, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.groupItem = mValues.get(position);
        holder.titleTv.setText(mValues.get(position).getTitle());
        RequestOptions requestOptions = new RequestOptions();
        requestOptions = requestOptions.transforms(new CenterCrop(), new RoundedCornersTransformation(UiUtils.dpToPx(8), 0, TOP));
        glide.load(mValues.get(position).getPhotoUrl())
                .placeholder(R.drawable.rounded_rect_gray)
                .error(R.drawable.rounded_rect_gray)
                .apply(requestOptions)
                .into(holder.imageView);

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListFragmentInteraction(holder.groupItem);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final View mView;
        final ImageView imageView;
        final TextView titleTv;
        public ExploreGroupData groupItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            imageView = view.findViewById(R.id.iv_group_pic);
            titleTv = view.findViewById(R.id.tv_group_title);
        }

    }

    public interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        void onListFragmentInteraction(ExploreGroupData item);
    }
}