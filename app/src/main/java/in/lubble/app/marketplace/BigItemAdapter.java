package in.lubble.app.marketplace;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import in.lubble.app.GlideRequests;
import in.lubble.app.R;

public class BigItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


    private static final String TAG = "MsgReceiptAdapter";

    private final List<String> msgInfoList;
    private final GlideRequests glide;

    public BigItemAdapter(GlideRequests glide) {
        msgInfoList = new ArrayList<>();
        this.glide = glide;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new BigItemAdapter.ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.big_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        final BigItemAdapter.ViewHolder viewHolder = (BigItemAdapter.ViewHolder) holder;
        viewHolder.itemIv.setImageResource(R.drawable.blue_circle);
        viewHolder.nameTv.setText(msgInfoList.get(position));
        viewHolder.priceTv.setText(msgInfoList.get(position));

    }

    @Override
    public int getItemCount() {
        return msgInfoList.size();
    }

    public void addData(String str) {
        msgInfoList.add(str);
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView itemIv;
        final TextView nameTv;
        final TextView priceTv;

        ViewHolder(View view) {
            super(view);
            itemIv = view.findViewById(R.id.iv_item);
            nameTv = view.findViewById(R.id.tv_name);
            priceTv = view.findViewById(R.id.tv_price);
        }
    }


}
