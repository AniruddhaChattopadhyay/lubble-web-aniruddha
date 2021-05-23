package in.lubble.app.feed_user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import in.lubble.app.R;
import in.lubble.app.feed_groups.FeedGroupsFrag;

import static in.lubble.app.utils.UiUtils.reduceDragSensitivity;

public class FeedCombinedFragment extends Fragment {

    public FeedCombinedFragment() {
        // Required empty public constructor
    }

    public static FeedCombinedFragment newInstance() {
        return new FeedCombinedFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.frag_feed_combined, container, false);
        TabLayout tabLayout = view.findViewById(R.id.tabLayout_feed);
        ViewPager2 viewPager = view.findViewById(R.id.tab_pager);

        MyTabPagerAdapter tabPager = new MyTabPagerAdapter(requireActivity());
        viewPager.setAdapter(tabPager);
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) ->
                        tab.setText(position == 0 ? "Nearby Feed" : "Groups")
        ).attach();
        reduceDragSensitivity(viewPager);

        return view;
    }

    static class MyTabPagerAdapter extends FragmentStateAdapter {
        MyTabPagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @Override
        public int getItemCount() {
            return 2;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new FeedFrag();
                case 1:
                    return new FeedGroupsFrag();
                default:
                    throw new IllegalArgumentException();
            }
        }
    }

}