package in.lubble.app.feed_groups.SingleGroupFeed;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.emoji.widget.EmojiTextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cooltechworks.views.shimmer.ShimmerRecyclerView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.net.MalformedURLException;
import java.util.List;

import in.lubble.app.R;
import in.lubble.app.feed_user.AddPostForFeed;
import in.lubble.app.feed_user.FeedAdaptor;
import in.lubble.app.network.Endpoints;
import in.lubble.app.network.ServiceGenerator;
import in.lubble.app.services.FeedServices;
import io.getstream.cloud.CloudFlatFeed;
import io.getstream.core.exceptions.StreamException;
import io.getstream.core.models.Activity;
import io.getstream.core.options.Limit;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.app.Activity.RESULT_OK;

public class SingleGroupFeed extends Fragment {

    private ExtendedFloatingActionButton postBtn;
    private ShimmerRecyclerView feedRV;
    private EmojiTextView joinGroupTv;
    private List<Activity> activities = null;
    private static final int REQUEST_CODE_POST = 800;
    private static final String FEED_NAME_BUNDLE = "FEED_NAME";
    private String feedName = null;

    public SingleGroupFeed() {
        // Required empty public constructor
    }

    public static SingleGroupFeed newInstance(String feedName) {
        SingleGroupFeed fragment = new SingleGroupFeed();
        Bundle args = new Bundle();
        args.putString(FEED_NAME_BUNDLE, feedName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            feedName = getArguments().getString(FEED_NAME_BUNDLE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_single_group_feed, container, false);
        joinGroupTv = rootView.findViewById(R.id.tv_join_group);
        postBtn = rootView.findViewById(R.id.btn_new_post);
        feedRV = rootView.findViewById(R.id.feed_recyclerview);

        //todo hide joinGroupTv btn if group is already followed by user
        joinGroupTv.setVisibility(View.VISIBLE);

        postBtn.setOnClickListener(v -> {
            startActivityForResult(new Intent(getContext(), AddPostForFeed.class), REQUEST_CODE_POST);
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        feedRV.setLayoutManager(layoutManager);
        feedRV.showShimmerAdapter();
        getCredentials();
        return rootView;
    }

    void getCredentials() {
        final Endpoints endpoints = ServiceGenerator.createService(Endpoints.class);
        Call<Endpoints.StreamCredentials> call = endpoints.getStreamCredentials(feedName);//feedName
        call.enqueue(new Callback<Endpoints.StreamCredentials>() {
            @Override
            public void onResponse(Call<Endpoints.StreamCredentials> call, Response<Endpoints.StreamCredentials> response) {
                if (response.isSuccessful()) {
                    //Toast.makeText(getContext(), R.string.upload_success, Toast.LENGTH_SHORT).show();
                    assert response.body() != null;
                    final Endpoints.StreamCredentials credentials = response.body();
                    try {
                        FeedServices.init(credentials.getApi_key(), credentials.getUser_token());

                        initRecyclerView();

                    } catch (MalformedURLException | StreamException e) {
                        e.printStackTrace();
                    }

                } else {
                    Toast.makeText(getContext(), R.string.all_try_again, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Endpoints.StreamCredentials> call, Throwable t) {
                Toast.makeText(getContext(), R.string.all_try_again, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initRecyclerView() throws StreamException {
        CloudFlatFeed groupFeed = FeedServices.client.flatFeed("group", feedName);
        activities = groupFeed
                .getActivities(new Limit(25))
                .join();
        Log.d("hey", "hey");
        if (feedRV.getActualAdapter() != feedRV.getAdapter()) {
            // recycler view is currently holding shimmer adapter so hide it
            feedRV.hideShimmerAdapter();
        }
        FeedAdaptor adapter = new FeedAdaptor(getContext(), activities);
        feedRV.setAdapter(adapter);

        joinGroupTv.setOnClickListener(v -> {
            CloudFlatFeed timeline = FeedServices.getTimelineClient().flatFeed("timeline", FeedServices.uid);
            try {
                timeline.follow(groupFeed).join();
                Toast.makeText(requireContext(), "Followed", Toast.LENGTH_SHORT).show();
            } catch (StreamException e) {
                FirebaseCrashlytics.getInstance().recordException(e);
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_POST && resultCode == RESULT_OK) {
            try {
                initRecyclerView();
            } catch (StreamException e) {
                e.printStackTrace();
            }
        }
    }
}