package in.lubble.app.feed_groups;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import in.lubble.app.R;
import in.lubble.app.feed_groups.SingleGroupFeed.SingleGroupFeed;
import in.lubble.app.models.FeedGroupData;
import in.lubble.app.network.Endpoints;
import in.lubble.app.network.ServiceGenerator;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class FeedGroupsFrag extends Fragment {

    private RecyclerView groupRecycleView;
    public List<FeedGroupData> feedGroupData;


    public FeedGroupsFrag() {
        // Required empty public constructor
    }

    public static FeedGroupsFrag newInstance() {
        FeedGroupsFrag fragment = new FeedGroupsFrag();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_feed_groups, container, false);
//
        groupRecycleView = rootView.findViewById(R.id.feed_group_recyclerview);
        getFeedGroups();

        return rootView;
    }

    private void getFeedGroups() {
        Endpoints endpoints = ServiceGenerator.createService(Endpoints.class);
        Call<List<FeedGroupData>> call = endpoints.getFeedGroupList();
        call.enqueue(new Callback<List<FeedGroupData>>() {
            @Override
            public void onResponse(Call<List<FeedGroupData>> call, Response<List<FeedGroupData>> response) {
                if (response.isSuccessful() ) {
                    feedGroupData = response.body();
                    initRecyclerView();
                }
            }

            @Override
            public void onFailure(Call<List<FeedGroupData>> call, Throwable t) {

            }

        });
    }

    private void initRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        groupRecycleView.setVisibility(View.VISIBLE);
        groupRecycleView.setLayoutManager(layoutManager);
        FeedGroupAdapter adapter = new FeedGroupAdapter(getContext(), feedGroupData);
        groupRecycleView.setAdapter(adapter);
        groupRecycleView.addItemDecoration(new MyDividerItemDecoration(getContext(), LinearLayoutManager.VERTICAL,40));
        groupRecycleView.addOnItemTouchListener(new RecyclerTouchListener(getContext(), groupRecycleView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Fragment fragment = SingleGroupFeed.newInstance(feedGroupData.get(position).getFeedName());
                FragmentManager fm = getActivity().getSupportFragmentManager();
                fm.beginTransaction().replace(R.id.tv_book_author, fragment).commitAllowingStateLoss();
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));
    }
}