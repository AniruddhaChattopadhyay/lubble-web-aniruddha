package in.lubble.app.feed_user;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.transition.platform.MaterialArcMotion;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.JsonObject;
import com.segment.analytics.internal.Private;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import in.lubble.app.R;
import in.lubble.app.network.Endpoints;
import in.lubble.app.network.ServiceGenerator;
import in.lubble.app.services.FeedServices;
import in.lubble.app.utils.RoundedCornersTransformation;
import io.getstream.client.Feed;
import io.getstream.core.LookupKind;
import io.getstream.core.exceptions.StreamException;
import io.getstream.core.models.Activity;
import io.getstream.core.models.Reaction;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static in.lubble.app.Constants.MEDIA_TYPE;
import static in.lubble.app.utils.UiUtils.dpToPx;

public class FeedAdaptor extends RecyclerView.Adapter<FeedAdaptor.MyViewHolder> {

    private static final String TAG = "FeedAdaptor";
    private List<Activity> activityList;
    private Context context;
    private ArrayList<Reaction> currUserReactionList = new ArrayList<>();

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView textContentTv;
        private ImageView photoContentIv;
        private TextView authorNameTv;
        private TextView timePostedTv;
        private LinearLayout likeLayout;
        private TextView likeStatsTv;
        private ImageView likeIv;
        private int likeCount = 0;
        private LinearLayout commentLayout;
        private RelativeLayout commentViewLayout;
        private MaterialButton postCommentBtn;
        private EditText commentEdtText;
        private RecyclerView commentRecyclerView;
        public MyViewHolder(View view) {
            super(view);
            textContentTv = view.findViewById(R.id.feed_text_content);
            photoContentIv = view.findViewById(R.id.feed_photo_content);
            authorNameTv = view.findViewById(R.id.feed_author_name);
            timePostedTv = view.findViewById(R.id.feed_post_timestamp);
            likeLayout = view.findViewById(R.id.cont_like);
            likeStatsTv = view.findViewById(R.id.tv_like_stats);
            likeIv = view.findViewById(R.id.like_imageview);
            commentLayout = view.findViewById(R.id.cont_reply);
            commentViewLayout = view.findViewById(R.id.comment_section_layout);
            postCommentBtn = view.findViewById(R.id.comment_post_btn);
            commentEdtText = view.findViewById(R.id.comment_edit_text);
            commentRecyclerView = view.findViewById(R.id.comment_recycler_view);
        }
    }


    public FeedAdaptor(Context context,List<Activity> moviesList) {
        this.activityList = moviesList;
        this.context = context;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.feed_list_row, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Activity activity = activityList.get(position);
        String postDateDisplay = getPostDateDisplay(activity.getTime());
        Map<String,Object> extras = activity.getExtra();

        try {
            handleLikes(activity,holder,position);
            handleComments(activity,holder,position);
        } catch (StreamException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(extras.containsKey("message")){
            holder.textContentTv.setVisibility(View.VISIBLE);
            holder.textContentTv.setText(extras.get("message").toString());
        }
        if(extras.containsKey("photoLink")){
            holder.photoContentIv.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(extras.get("photoLink").toString())
                    .transform(new RoundedCornersTransformation(dpToPx(8), 0))
                    .into(holder.photoContentIv);
        }

        if(extras.containsKey("authorName")){
            holder.authorNameTv.setText(extras.get("authorName").toString());
        }
        holder.timePostedTv.setText(postDateDisplay);
    }

    private void handleComments(Activity activity, MyViewHolder holder, int position) {
        toogleCommentVisibility(activity,holder,position);

    }

    private void toogleCommentVisibility(Activity activity, MyViewHolder holder, int position)  {
        holder.commentLayout.setOnClickListener(v -> {
            if(holder.commentViewLayout.getVisibility() == View.GONE){
                holder.commentViewLayout.setVisibility(View.VISIBLE);
                try {
                    initCommentRecyclerView(holder,activity);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (StreamException e) {
                    e.printStackTrace();
                }
                holder.postCommentBtn.setOnClickListener(view->{
                    if(!TextUtils.isEmpty(holder.commentEdtText.getText().toString())){
                        Reaction comment = new Reaction.Builder()
                                .kind("comment")
                                .activityID(activity.getID())
                                .extraField("text", holder.commentEdtText.getText().toString())
                                .extraField("userId",FirebaseAuth.getInstance().getUid())
                                .build();
                        try {
                            comment = FeedServices.client.reactions().add(comment).get();
                            Toast.makeText(context,"Comment posted!",Toast.LENGTH_LONG).show();
                            holder.commentEdtText.setText("");
                            //holder.postCommentBtn.setOnClickListener(null);
                            //holder.commentViewLayout.setVisibility(View.GONE);
                            initCommentRecyclerView(holder,activity);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        } catch (StreamException e) {
                            e.printStackTrace();
                        }
                    }
                    else{
                        Toast.makeText(context,"Comment can't be empty. Please write something!",Toast.LENGTH_LONG).show();
                    }

                });
            }
            else{
                holder.postCommentBtn.setOnClickListener(null);
                holder.commentViewLayout.setVisibility(View.GONE);
            }
        });
    }

    private void initCommentRecyclerView(MyViewHolder holder, Activity activity) throws StreamException, ExecutionException, InterruptedException {
        List<Reaction> reactions = FeedServices.client.reactions().filter(LookupKind.ACTIVITY, activity.getID(),"comment").get();
        if(reactions.size()>0) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
            holder.commentRecyclerView.setVisibility(View.VISIBLE);
            holder.commentRecyclerView.setLayoutManager(layoutManager);
            FeedCommentAdaptor adapter = new FeedCommentAdaptor(context, reactions);
            holder.commentRecyclerView.setAdapter(adapter);
        }
    }


    private void handleLikes(Activity activity, MyViewHolder holder, int position) throws StreamException, ExecutionException, InterruptedException {

        List<Reaction> reactions = FeedServices.client.reactions().filter(LookupKind.ACTIVITY, activity.getID(),"like").get();
        holder.likeCount = reactions.size();
        holder.likeStatsTv.setText(Integer.toString(holder.likeCount));
        Reaction currUserReaction = null;
        for (Reaction reaction : reactions) {
            if (reaction.getUserID().equals(FirebaseAuth.getInstance().getUid())) {
                holder.likeIv.setImageResource(R.drawable.ic_favorite_24dp);
                currUserReaction = reaction;
            }
        }
        currUserReactionList.add(currUserReaction);


        holder.likeLayout.setOnClickListener(v -> {
            if(currUserReactionList.get(position)==null) {
                Reaction like = new Reaction.Builder()
                        .kind("like")
                        .activityID(activity.getID())
                        .build();
                try {
                    like = FeedServices.client.reactions().add(like).get();
                    holder.likeStatsTv.setText(Integer.toString(holder.likeCount+1));
                    holder.likeCount += 1;
                    holder.likeIv.setImageResource(R.drawable.ic_favorite_24dp);
                    currUserReactionList.set(position,like);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (StreamException e) {
                    e.printStackTrace();
                }
            }
            else {
                try {
                    FeedServices.client.reactions().delete(currUserReactionList.get(position).getId()).join();
                    holder.likeIv.setImageResource(R.drawable.ic_favorite_border_24dp);
                    holder.likeStatsTv.setText(Integer.toString(holder.likeCount-1));
                    holder.likeCount -= 1;
                    currUserReactionList.set(position,null);
                } catch (StreamException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    private String getPostDateDisplay(Date timePosted){
        Date timeNow = new Date(System.currentTimeMillis());
        long duration  = timeNow.getTime() - timePosted.getTime();

        long diffInSeconds = TimeUnit.MILLISECONDS.toSeconds(duration);
        long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(duration);
        long diffInHours = TimeUnit.MILLISECONDS.toHours(duration);
        long diffInDays = TimeUnit.MILLISECONDS.toDays(duration);

        if(diffInDays>0){
            return diffInDays + "D";
        }
        else if(diffInHours>0){
            return diffInHours + "Hr";
        }
        else if(diffInMinutes>0){
            return diffInMinutes + "Min";
        }
        else if(diffInSeconds>0){
            return "Just Now";
        }
        return "some time ago";
    }

    @Override
    public int getItemCount() {
        return activityList.size();
    }
}
