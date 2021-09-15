package in.lubble.app.feed_user;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import androidx.emoji.widget.EmojiTextView;
import androidx.paging.LoadState;
import androidx.paging.LoadStateAdapter;
import androidx.paging.PagingDataAdapter;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.curios.textformatter.FormatText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import in.lubble.app.GlideRequests;
import in.lubble.app.LubbleSharedPrefs;
import in.lubble.app.R;
import in.lubble.app.analytics.Analytics;
import in.lubble.app.analytics.AnalyticsEvents;
import in.lubble.app.chat.CustomURLSpan;
import in.lubble.app.models.FeedGroupData;
import in.lubble.app.profile.ProfileActivity;
import in.lubble.app.receivers.ShareSheetReceiver;
import in.lubble.app.services.FeedServices;
import in.lubble.app.utils.FeedUtils;
import in.lubble.app.utils.RoundedCornersTransformation;
import in.lubble.app.utils.UiUtils;
import io.getstream.core.exceptions.StreamException;
import io.getstream.core.models.EnrichedActivity;
import io.getstream.core.models.FeedID;
import io.getstream.core.models.Reaction;
import it.sephiroth.android.library.xtooltip.ClosePolicy;
import it.sephiroth.android.library.xtooltip.Tooltip;

import static android.view.View.GONE;
import static in.lubble.app.utils.DateTimeUtils.SERVER_DATE_TIME;
import static in.lubble.app.utils.DateTimeUtils.stringTimeToEpoch;
import static in.lubble.app.utils.UiUtils.dpToPx;

public class FeedAdaptor extends PagingDataAdapter<EnrichedActivity, FeedAdaptor.MyViewHolder> {

    private static final String TAG = "FeedAdaptor";
    private Context context = null;
    private int itemWidth, displayHeight;
    private FeedListener feedListener;
    private GlideRequests glide;
    private final HashMap<Integer, String> likedMap = new HashMap<>();
    private final String userId = FirebaseAuth.getInstance().getUid();
    private String photoLink = null;
    private GestureDetector gestureDetector;

    public FeedAdaptor(@NotNull DiffUtil.ItemCallback<EnrichedActivity> diffCallback) {
        super(diffCallback);
    }

    public void setVars(Context context, int displayWidth, int displayHeight, GlideRequests glide, FeedListener feedListener) {
        this.context = context;
        this.glide = glide;
        this.feedListener = feedListener;
        this.itemWidth = displayWidth - UiUtils.dpToPx(32);
        this.displayHeight = displayHeight - UiUtils.dpToPx(172);
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.feed_list_row, parent, false);

        return new MyViewHolder(itemView);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        EnrichedActivity activity = getItem(position);
        if (activity == null) {
            return;
        }
        String postDateDisplay = getPostDateDisplay(activity.getTime());
        Map<String, Object> extras = activity.getExtra();
        holder.photoContentIv.setVisibility(View.GONE);
        holder.groupNameTv.setVisibility(View.GONE);
        holder.lubbleNameTv.setVisibility(View.GONE);
        if (extras != null) {
            holder.textContentTv.setVisibility(View.VISIBLE);
            final String message = String.valueOf(extras.get("message") == null ? "" : extras.get("message"));
            holder.textContentTv.setText(FormatText.boldAndItalics(message));
            holder.textContentTv.setLinkTextColor(ContextCompat.getColor(context, R.color.colorAccent));

            Linkify.addLinks(holder.textContentTv, Linkify.ALL);
            CustomURLSpan.clickifyTextView(holder.textContentTv, () -> {
                Bundle bundle = new Bundle();
                bundle.putString("group_id", String.valueOf(extras.get("feed_name")));
                bundle.putString("post_id", activity.getID());
                bundle.putString("author_uid", activity.getActor().getID());
                Analytics.triggerEvent(AnalyticsEvents.POST_LINK_CLICKED, bundle, context);
            });
            if (extras.containsKey("aspectRatio") && extras.get("aspectRatio") instanceof Double) {
                float aspectRatio = ((Double) extras.get("aspectRatio")).floatValue();
                if (aspectRatio > 0) {
                    holder.itemView.measure(
                            View.MeasureSpec.makeMeasureSpec(itemWidth, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                    int itemViewHeight = holder.itemView.getMeasuredHeight();
                    float targetHeight = Math.min(displayHeight - itemViewHeight - dpToPx(80), itemWidth / aspectRatio); //80->sum of heights for joined groups & new post btns
                    holder.photoContentIv.setVisibility(View.VISIBLE);
                    ViewGroup.LayoutParams lp = holder.photoContentIv.getLayoutParams();
                    if (targetHeight < 300) {
                        float delta = 300 - targetHeight;
                        int linesToPurge = (int) Math.ceil(delta / UiUtils.spToPx(14));
                        holder.textContentTv.setMaxLines(Math.max(9 - linesToPurge, 5));
                        targetHeight = 300;
                    } else {
                        holder.textContentTv.setMaxLines(9);
                    }
                    lp.height = Math.round(targetHeight);
                    holder.photoContentIv.setLayoutParams(lp);
                    holder.photoContentIv.setBackgroundColor(ContextCompat.getColor(context, R.color.md_grey_200));
                }
            } else {
                // photoContentIv.visibility = GONE already at top
                holder.textContentTv.setMaxLines(9);
            }

            if (extras.containsKey("photoLink")) {
                holder.photoContentIv.setVisibility(View.VISIBLE);
                photoLink = extras.get("photoLink").toString();
                glide
                        .load(photoLink)
                        .transform(new RoundedCornersTransformation(dpToPx(8), 0))
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                holder.photoContentIv.setBackgroundResource(0);//removes bg
                                return false;
                            }
                        })
                        .into(holder.photoContentIv);
            }

            if (extras.containsKey("authorName")) {
                holder.authorNameTv.setText(extras.get("authorName").toString());
            }
            if (extras.containsKey("group")) {
                holder.groupNameTv.setVisibility(View.VISIBLE);
                holder.groupNameTv.setText(extras.get("group").toString());
            }
            if (extras.containsKey("lubble_id")) {
                holder.lubbleNameTv.setVisibility(View.VISIBLE);
                if (extras.containsKey("lubble_name"))
                    holder.lubbleNameTv.setText(extras.get("lubble_name").toString());
                else
                    holder.lubbleNameTv.setText(extras.get("lubble_id").toString());
            }
        }
        Map<String, Object> actorMap = activity.getActor().getData();
        if (actorMap.containsKey("name")) {
            holder.authorNameTv.setText(String.valueOf(actorMap.get("name")));
            if (actorMap.containsKey("profile_picture") && actorMap.get("profile_picture") != null) {
                Glide.with(context)
                        .load(actorMap.get("profile_picture").toString())
                        .placeholder(R.drawable.ic_account_circle_black_no_padding)
                        .error(R.drawable.ic_account_circle_black_no_padding)
                        .circleCrop()
                        .into(holder.authorPhotoIv);
            } else {
                holder.authorPhotoIv.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_account_circle_black_no_padding));
            }
        }
        holder.timePostedTv.setText(postDateDisplay);

        List<Reaction> userLikes = activity.getOwnReactions().get("like");
        if (userLikes != null && userLikes.size() > 0) {
            holder.likeIv.setImageResource(R.drawable.ic_favorite_24dp);
            likedMap.put(position, userLikes.get(0).getId());
        } else {
            holder.likeIv.setImageResource(R.drawable.ic_favorite_border_24dp);
            likedMap.remove(position);
        }
        handleReactionStats(activity, holder);
        initCommentRecyclerView(holder, activity);
        handleCommentEditText(activity, holder);
        handleLinkPreview(activity, holder);

        //setting the tooltip for the first post
        if (holder.getAbsoluteAdapterPosition() == 0 && !LubbleSharedPrefs.getInstance().getFEED_DOUBLE_TAP_LIKE_TOOLTIP_FLAG()) {
            prepareDoubleTapToLikeTooltip(holder, extras);
        }
    }

    private void prepareDoubleTapToLikeTooltip(MyViewHolder holder, Map<String, Object> extras) {
        try {
            View v;
            if (extras.containsKey("photoLink")) {
                v = holder.photoContentIv;
            } else {
                v = holder.textContentTv;
            }
            v.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (v.getWindowToken() != null) {
                        setToolTipForDoubleTapLike(v);
                        v.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                }
            });
        } catch (Exception e) {
            // Avoid crashing the app just coz showing the tooltip broke
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    private void setToolTipForDoubleTapLike(View view) {
        if (view == null || context == null)
            return;

        Tooltip tooltip = new Tooltip.Builder(context)
                .anchor(view, 0, -dpToPx(24), true)
                .closePolicy(ClosePolicy.Companion.getTOUCH_NONE())
                .showDuration(5000)
                .overlay(false)
                .text("Double tap anywhere to like")
                .create();

        tooltip.show(view, Tooltip.Gravity.BOTTOM, true);
        LubbleSharedPrefs.getInstance().setFEED_DOUBLE_TAP_LIKE_TOOLTIP_FLAG();

    }

    private void startShareFlow(Intent sharingIntent) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 21,
                new Intent(context, ShareSheetReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            context.startActivity(Intent.createChooser(sharingIntent, context.getString(R.string.refer_share_title), pendingIntent.getIntentSender()));
        } else {
            context.startActivity(Intent.createChooser(sharingIntent, context.getString(R.string.refer_share_title)));
        }
        Analytics.triggerEvent(AnalyticsEvents.POST_SHARED, context);
    }

    private void handleLinkPreview(EnrichedActivity activity, MyViewHolder holder) {
        if (activity.getExtra().containsKey("linkUrl")) {
            String linkUrl = ((String) activity.getExtra().get("linkUrl")).toLowerCase();
            if (!TextUtils.isEmpty(linkUrl)) {
                holder.linkPreviewContainer.setVisibility(View.VISIBLE);
                if (activity.getExtra().containsKey("linkTitle")) {
                    holder.linkTitleTv.setText((String) activity.getExtra().get("linkTitle"));
                }
                if (activity.getExtra().containsKey("linkDesc")) {
                    holder.linkDescTv.setText((String) activity.getExtra().get("linkDesc"));
                }
                if (activity.getExtra().containsKey("linkPicUrl")) {
                    String linkPicUrl = (String) activity.getExtra().get("linkPicUrl");
                    glide
                            .load(linkPicUrl)
                            .error(R.drawable.ic_public_black_24dp)
                            .placeholder(R.drawable.ic_public_black_24dp)
                            .transform(new RoundedCornersTransformation(dpToPx(8), 0))
                            .into(holder.linkImageIv);
                }
            } else {
                holder.linkPreviewContainer.setVisibility(GONE);
            }
        } else {
            holder.linkPreviewContainer.setVisibility(GONE);
        }
    }

    private void handleCommentEditText(EnrichedActivity activity, MyViewHolder holder) {
        glide
                .load(LubbleSharedPrefs.getInstance().getProfilePicUrl())
                .apply(new RequestOptions().override(dpToPx(24), dpToPx(24)))
                .circleCrop()
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE) //caches final image after transformations
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        holder.commentEdtText.setCompoundDrawablesWithIntrinsicBounds(resource, null, null, null);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        holder.commentEdtText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_account_circle_grey_24dp, 0, 0, 0);
                    }
                });
    }

    public interface FeedListener {
        void onReplyClicked(String activityId, String foreignId, String postActorUid, int position);

        void onImageClicked(String imgPath, ImageView imageView);

        void onLiked(String foreignID);

        void onRefreshLoading(@NotNull LoadState refresh);

        void openPostActivity(@NotNull String activityId);

        void openGroupFeed(@NotNull FeedGroupData feedGroupData);

        void showEmptyView(boolean show);
    }

    private void initCommentRecyclerView(MyViewHolder holder, EnrichedActivity activity) {
        List<Reaction> commentList = activity.getLatestReactions().get("comment");
        if (commentList != null && commentList.size() > 0) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
            holder.commentRecyclerView.setVisibility(View.VISIBLE);
            holder.viewAllRepliesTv.setVisibility(View.VISIBLE);
            holder.commentRecyclerView.setLayoutManager(layoutManager);
            holder.commentRecyclerView.setNestedScrollingEnabled(false);
            sortComments(commentList);
            FeedCommentAdaptor adapter = new FeedCommentAdaptor(commentList, activity.getID(), feedListener);
            holder.commentRecyclerView.setAdapter(adapter);
        } else {
            holder.commentRecyclerView.setVisibility(GONE);
            holder.viewAllRepliesTv.setVisibility(GONE);
        }
    }

    private void sortComments(List<Reaction> commentList) {
        Collections.sort(commentList, (o1, o2) ->
                Long.compare(
                        stringTimeToEpoch((String) o1.getExtra().get("created_at"), SERVER_DATE_TIME),
                        stringTimeToEpoch((String) o2.getExtra().get("created_at"), SERVER_DATE_TIME)
                )
        );
    }

    private void handleReactionStats(EnrichedActivity enrichedActivity, MyViewHolder holder) {
        extractReactionCount(enrichedActivity, "like", holder.likeTv, 0);
        extractReactionCount(enrichedActivity, "comment", holder.replyTv, 0);
    }

    private void extractReactionCount(EnrichedActivity enrichedActivity, @NotNull String reaction, TextView statsTv, int change) {
        Number reactionNumber = enrichedActivity.getReactionCounts().get(reaction);
        if (reactionNumber == null) {
            reactionNumber = 0;
        }
        int reactionCount = reactionNumber.intValue();
        if (change != 0) {
            reactionCount += change;
            enrichedActivity.getReactionCounts().put(reaction, reactionCount);
        }
        if (reactionCount > 0) {
            statsTv.setVisibility(View.VISIBLE);
            statsTv.setText(String.valueOf(reactionCount));
        } else {
            statsTv.setText("");
        }
    }

    private void toggleLike(MyViewHolder holder, int position) {
        EnrichedActivity activity = getItem(position);
        if (!likedMap.containsKey(position)) {
            // like
            Reaction like = new Reaction.Builder()
                    .kind("like")
                    .id(userId + activity.getID())
                    .activityID(activity.getID())
                    .build();
            try {
                String notificationUserFeedId = "notification:" + activity.getActor().getID();
                FeedServices.getTimelineClient().reactions().add(like, new FeedID(notificationUserFeedId)).whenComplete((reaction, throwable) -> {
                    if (throwable != null) {
                        FirebaseCrashlytics.getInstance().recordException(throwable);
                    }
                });
                holder.likeIv.setImageResource(R.drawable.ic_favorite_24dp);
                likedMap.put(position, like.getId());
                extractReactionCount(activity, "like", holder.likeTv, 1);
                feedListener.onLiked(activity.getForeignID());
            } catch (StreamException e) {
                e.printStackTrace();
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        } else {
            // unlike
            try {
                FeedServices.getTimelineClient().reactions().delete(likedMap.get(position)).whenComplete((aVoid, throwable) -> {
                    if (throwable != null) {
                        FirebaseCrashlytics.getInstance().recordException(throwable);
                    }
                });
                holder.likeIv.setImageResource(R.drawable.ic_favorite_border_24dp);
                likedMap.remove(position);
                extractReactionCount(activity, "like", holder.likeTv, -1);
            } catch (StreamException e) {
                e.printStackTrace();
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        }
    }


    private void toggleLike2(ImageView likeIv, TextView likeTv, int position) {
        EnrichedActivity activity = getItem(position);
        if (!likedMap.containsKey(position)) {
            // like
            Reaction like = new Reaction.Builder()
                    .kind("like")
                    .id(userId + activity.getID())
                    .activityID(activity.getID())
                    .build();
            try {
                String notificationUserFeedId = "notification:" + activity.getActor().getID();
                FeedServices.getTimelineClient().reactions().add(like, new FeedID(notificationUserFeedId)).whenComplete((reaction, throwable) -> {
                    if (throwable != null) {
                        FirebaseCrashlytics.getInstance().recordException(throwable);
                    }
                });
                likeIv.setImageResource(R.drawable.ic_favorite_24dp);
                likedMap.put(position, like.getId());
                extractReactionCount(activity, "like", likeTv, 1);
                feedListener.onLiked(activity.getForeignID());
            } catch (StreamException e) {
                e.printStackTrace();
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        } else {
            // unlike
            try {
                FeedServices.getTimelineClient().reactions().delete(likedMap.get(position)).whenComplete((aVoid, throwable) -> {
                    if (throwable != null) {
                        FirebaseCrashlytics.getInstance().recordException(throwable);
                    }
                });
                likeIv.setImageResource(R.drawable.ic_favorite_border_24dp);
                likedMap.remove(position);
                extractReactionCount(activity, "like", likeTv, -1);
            } catch (StreamException e) {
                e.printStackTrace();
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        }
    }

    public ConcatAdapter withLoadStateAdapters(LoadStateAdapter footer) {
        addLoadStateListener(combinedLoadStates -> {
            footer.setLoadState(combinedLoadStates.getAppend());
            feedListener.onRefreshLoading(combinedLoadStates.getRefresh());
            if (combinedLoadStates.getSource().getRefresh() instanceof LoadState.NotLoading
                    && combinedLoadStates.getAppend().getEndOfPaginationReached()
                    && getItemCount() < 1) {
                feedListener.showEmptyView(true);
            } else {
                feedListener.showEmptyView(false);
            }
            return null;
        });
        return new ConcatAdapter(FeedAdaptor.this, footer);
    }

    private String getPostDateDisplay(Date timePosted) {
        Date timeNow = new Date(System.currentTimeMillis());
        long duration = timeNow.getTime() - timePosted.getTime();

        long diffInSeconds = TimeUnit.MILLISECONDS.toSeconds(duration);
        long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(duration);
        long diffInHours = TimeUnit.MILLISECONDS.toHours(duration);
        long diffInDays = TimeUnit.MILLISECONDS.toDays(duration);

        if (diffInDays > 0) {
            return diffInDays + "d";
        } else if (diffInHours > 0) {
            return diffInHours + "hr";
        } else if (diffInMinutes > 0) {
            return diffInMinutes + "min";
        } else if (diffInSeconds > 0) {
            return "just now";
        }
        return "Just Now";
    }

    public void addUserReply(String activityId, Reaction reaction) {
        int pos = getActivityPosById(activityId);
        if (pos >= 0) {
            EnrichedActivity updatedActivity = getItem(pos);
            // add reply to comment RV
            List<Reaction> latestCommentList = updatedActivity.getLatestReactions().get("comment");
            if (latestCommentList == null)
                latestCommentList = new ArrayList<>();
            latestCommentList.add(0, reaction);
            updatedActivity.getLatestReactions().put("comment", latestCommentList);
            // update reply count
            Number reactionNumber = updatedActivity.getReactionCounts().get(reaction.getKind());
            if (reactionNumber == null) {
                reactionNumber = 0;
            }
            int reactionCount = reactionNumber.intValue();
            updatedActivity.getReactionCounts().put(reaction.getKind(), ++reactionCount);

            notifyItemChanged(pos);
        }
    }

    private int getActivityPosById(String activityId) {
        for (int i = 0; i < getItemCount(); i++)
            if (activityId.equalsIgnoreCase(getItem(i).getID()))
                return i;
        return -1;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnTouchListener {
        private final EmojiTextView textContentTv;
        private final ImageView photoContentIv;
        private final ImageView authorPhotoIv, linkImageIv;
        private final TextView viewAllRepliesTv, authorNameTv, timePostedTv, groupNameTv, lubbleNameTv, linkTitleTv, linkDescTv;
        private final LinearLayout likeLayout, shareLayout;
        private final ImageView likeIv;
        private final LinearLayout commentLayout;
        private final TextView commentEdtText, likeTv, replyTv;
        private final RecyclerView commentRecyclerView;
        private final RelativeLayout linkPreviewContainer;
        private View touchView;
        private EnrichedActivity activity;
        private Map<String, Object> extras;

        private final GestureDetector gestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                toggleLike2(likeIv, likeTv, getAbsoluteAdapterPosition());
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                activity = getItem(getAbsoluteAdapterPosition());
                if (activity == null)
                    return false;
                extras = activity.getExtra();
                switch (touchView.getId()) {
                    case R.id.feed_photo_content:
                        photoLink = extras.get("photoLink").toString();
                        feedListener.onImageClicked(photoLink, photoContentIv);
                        break;

                    case R.id.feed_text_content:
                        feedListener.openPostActivity(activity.getID());
                        break;

                    case -1:
                        feedListener.openPostActivity(activity.getID());
                        break;

                    case R.id.tv_group_name:
                        if (extras.containsKey("feed_name")) {
                            String groupFeedName = extras.get("feed_name").toString();
                            FeedGroupData feedGroupData = new FeedGroupData(extras.get("group").toString(), groupFeedName, extras.get("lubble_id").toString());
                            feedListener.openGroupFeed(feedGroupData);

                        }
                        break;

                    case R.id.cont_like:
                        toggleLike2(likeIv, likeTv, getAbsoluteAdapterPosition());
                        break;

                    case R.id.cont_reply:
                        feedListener.onReplyClicked(activity.getID(), activity.getForeignID(), activity.getActor().getID(), getAbsoluteAdapterPosition());
                        break;

                    case R.id.feed_author_photo:
                    case R.id.feed_author_name:
                        ProfileActivity.open(context, activity.getActor().getID());
                        break;

                    case R.id.cont_share:
                        FeedUtils.requestPostShareIntent(glide, activity, extras, FeedAdaptor.this::startShareFlow);
                        break;

                    case R.id.cont_link_preview:
                        if (activity.getExtra().containsKey("linkUrl")) {
                            String linkUrl = ((String) activity.getExtra().get("linkUrl")).toLowerCase();
                            CustomTabsIntent.Builder intentBuilder = new CustomTabsIntent.Builder();
                            intentBuilder.setToolbarColor(ContextCompat.getColor(context, R.color.colorAccent));
                            intentBuilder.setSecondaryToolbarColor(ContextCompat.getColor(context, R.color.dk_colorAccent));
                            intentBuilder.enableUrlBarHiding();
                            intentBuilder.setShowTitle(true);
                            CustomTabsIntent customTabsIntent = intentBuilder.build();
                            try {
                                customTabsIntent.launchUrl(context, Uri.parse(linkUrl));
                            } catch (ActivityNotFoundException exception) {
                                Intent i = new Intent(Intent.ACTION_VIEW);
                                i.setData(Uri.parse(linkUrl));
                                context.startActivity(i);
                            }
                        }
                        break;

                    case R.id.comment_edit_text:
                        feedListener.onReplyClicked(activity.getID(), activity.getForeignID(), activity.getActor().getID(), getAbsoluteAdapterPosition());
                        break;

                    case R.id.tv_view_all_replies:
                        feedListener.openPostActivity(activity.getID());
                        break;
                }
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }
        });


        public MyViewHolder(View view) {
            super(view);
            textContentTv = view.findViewById(R.id.feed_text_content);
            photoContentIv = view.findViewById(R.id.feed_photo_content);
            authorNameTv = view.findViewById(R.id.feed_author_name);
            groupNameTv = view.findViewById(R.id.tv_group_name);
            lubbleNameTv = view.findViewById(R.id.tv_lubble_name);
            authorPhotoIv = view.findViewById(R.id.feed_author_photo);
            viewAllRepliesTv = view.findViewById(R.id.tv_view_all_replies);
            timePostedTv = view.findViewById(R.id.feed_post_timestamp);
            likeLayout = view.findViewById(R.id.cont_like);
            shareLayout = view.findViewById(R.id.cont_share);
            likeIv = view.findViewById(R.id.like_imageview);
            likeTv = view.findViewById(R.id.tv_like);
            replyTv = view.findViewById(R.id.tv_reply);
            commentLayout = view.findViewById(R.id.cont_reply);
            commentEdtText = view.findViewById(R.id.comment_edit_text);
            commentRecyclerView = view.findViewById(R.id.comment_recycler_view);
            linkPreviewContainer = view.findViewById(R.id.cont_link_preview);
            linkImageIv = view.findViewById(R.id.iv_link_image);
            linkTitleTv = view.findViewById(R.id.tv_link_title);
            linkDescTv = view.findViewById(R.id.tv_link_desc);
            ImageView moreMenuIv = view.findViewById(R.id.iv_more_menu);
            moreMenuIv.setVisibility(GONE);

            photoContentIv.setOnTouchListener(this);
            textContentTv.setOnTouchListener(this);
            groupNameTv.setOnTouchListener(this);
            likeLayout.setOnTouchListener(this);
            commentLayout.setOnTouchListener(this);
            authorPhotoIv.setOnTouchListener(this);
            shareLayout.setOnTouchListener(this);
            linkPreviewContainer.setOnTouchListener(this);
            commentEdtText.setOnTouchListener(this);
            viewAllRepliesTv.setOnTouchListener(this);
            authorNameTv.setOnTouchListener(this);
            itemView.setOnTouchListener(this);

        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            touchView = v;
            boolean b = gestureDetector.onTouchEvent(event);
            return b;
        }
    }

}
