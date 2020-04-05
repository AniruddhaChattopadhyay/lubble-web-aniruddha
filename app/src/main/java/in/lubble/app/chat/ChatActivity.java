package in.lubble.app.chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.crashlytics.android.Crashlytics;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.util.HashMap;
import java.util.Map;
import java.util.MissingFormatArgumentException;

import in.lubble.app.BaseActivity;
import in.lubble.app.GlideApp;
import in.lubble.app.LubbleSharedPrefs;
import in.lubble.app.R;
import in.lubble.app.analytics.Analytics;
import in.lubble.app.analytics.AnalyticsEvents;
import in.lubble.app.firebase.RealtimeDbHelper;
import in.lubble.app.models.ChatData;
import in.lubble.app.models.GroupData;
import in.lubble.app.models.NotifData;
import in.lubble.app.models.search.Hit;
import in.lubble.app.models.search.SearchResultData;
import in.lubble.app.user_search.UserSearchActivity;
import in.lubble.app.utils.StringUtils;
import in.lubble.app.utils.UiUtils;

import static in.lubble.app.Constants.NEW_CHAT_ACTION;
import static in.lubble.app.utils.AppNotifUtils.TRACK_NOTIF_ID;
import static in.lubble.app.utils.NotifUtils.sendNotifAnalyticEvent;

public class ChatActivity extends BaseActivity implements ChatMoreFragment.FlairUpdateListener, SearchView.OnQueryTextListener {

    public static final String EXTRA_GROUP_ID = "chat_activ_group_id";
    public static final String EXTRA_MSG_ID = "chat_activ_msg_id";
    public static final String EXTRA_CHAT_DATA = "chat_activ_chat_data";
    public static final String EXTRA_IMG_URI_DATA = "EXTRA_IMG_URI_DATA";
    public static final String EXTRA_TAB_POS = "EXTRA_TAB_POS";
    // if we need to show joining progress dialog
    public static final String EXTRA_IS_JOINING = "chat_activ_is_joining";
    // for when DM exists
    public static final String EXTRA_DM_ID = "EXTRA_DM_ID";
    // for new DMs
    public static final String EXTRA_RECEIVER_ID = "EXTRA_RECEIVER_ID";
    public static final String EXTRA_RECEIVER_NAME = "EXTRA_RECEIVER_NAME";
    public static final String EXTRA_RECEIVER_DP_URL = "EXTRA_RECEIVER_DP_URL";
    public static final String EXTRA_ITEM_TITLE = "EXTRA_ITEM_TITLE";
    private ImageView toolbarIcon, toolbarLockIcon;
    private Toolbar toolbar;
    private TextView toolbarTv, toolbarInviteHint;
    private LinearLayout inviteContainer;
    private ImageView searchIv, searchBackIv, searchUpIv, searchDownIv;
    private SearchView searchView;
    private ChatFragment targetFrag = null;
    private String groupId;
    private ViewPager viewPager;
    private TabLayout tabLayout;
    private String dmId;
    private SearchResultData searchResultData = null;
    private int currSearchCursorPos = 0;

    public static void openForGroup(@NonNull Context context, @NonNull String groupId, boolean isJoining, @Nullable String msgId) {
        final Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra(EXTRA_GROUP_ID, groupId);
        intent.putExtra(EXTRA_IS_JOINING, isJoining);
        intent.putExtra(EXTRA_MSG_ID, msgId);
        context.startActivity(intent);
    }

    public static void openGroupMore(@NonNull Context context, @NonNull String groupId) {
        final Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra(EXTRA_GROUP_ID, groupId);
        intent.putExtra(EXTRA_TAB_POS, 1);
        context.startActivity(intent);
    }

    public static void openForGroup(@NonNull Context context, @NonNull String groupId, boolean isJoining, @Nullable String msgId, ChatData chatData, @Nullable Uri imgUri) {
        final Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra(EXTRA_GROUP_ID, groupId);
        intent.putExtra(EXTRA_IS_JOINING, isJoining);
        intent.putExtra(EXTRA_MSG_ID, msgId);
        intent.putExtra(EXTRA_CHAT_DATA, chatData);
        intent.putExtra(EXTRA_IMG_URI_DATA, imgUri);
        Log.d("mime type chat", "&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&" + imgUri);
        context.startActivity(intent);
    }

    public static void openForDm(@NonNull Context context, @NonNull String dmId, @Nullable String msgId, @Nullable String itemTitle) {
        final Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra(EXTRA_DM_ID, dmId);
        intent.putExtra(EXTRA_MSG_ID, msgId);
        intent.putExtra(EXTRA_ITEM_TITLE, itemTitle);
        context.startActivity(intent);
    }

    public static void openForDm(@NonNull Context context, @NonNull String dmId, @Nullable String msgId, @Nullable String itemTitle, ChatData chatData, @Nullable Uri imgUri) {
        final Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra(EXTRA_DM_ID, dmId);
        intent.putExtra(EXTRA_MSG_ID, msgId);
        intent.putExtra(EXTRA_ITEM_TITLE, itemTitle);
        intent.putExtra(EXTRA_CHAT_DATA, chatData);
        intent.putExtra(EXTRA_IMG_URI_DATA, imgUri);
        context.startActivity(intent);
    }

    public static void openForEmptyDm(@NonNull Context context, @Nullable String receiverId, @Nullable String receiverName, @Nullable String receiverDpUrl,
                                      @Nullable String itemTitle) {
        final Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra(EXTRA_RECEIVER_ID, receiverId);
        intent.putExtra(EXTRA_RECEIVER_NAME, receiverName);
        intent.putExtra(EXTRA_RECEIVER_DP_URL, receiverDpUrl);
        intent.putExtra(EXTRA_ITEM_TITLE, itemTitle);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        toolbar = findViewById(R.id.icon_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbarIcon = toolbar.findViewById(R.id.iv_toolbar);
        toolbarLockIcon = toolbar.findViewById(R.id.iv_lock_icon);
        toolbarInviteHint = toolbar.findViewById(R.id.tv_invite_hint);
        toolbarTv = toolbar.findViewById(R.id.tv_toolbar_title);
        inviteContainer = toolbar.findViewById(R.id.container_invite);
        searchIv = toolbar.findViewById(R.id.iv_toolbar_search);
        searchBackIv = toolbar.findViewById(R.id.iv_search_back);
        searchView = toolbar.findViewById(R.id.search_view);
        searchUpIv = toolbar.findViewById(R.id.iv_search_up);
        searchDownIv = toolbar.findViewById(R.id.iv_search_down);
        searchView.setOnQueryTextListener(this);
        setTitle("");

        viewPager = findViewById(R.id.viewpager_chat);
        tabLayout = findViewById(R.id.tablayout_chat);
        tabLayout.setupWithViewPager(viewPager);

        toolbarIcon.setImageResource(R.drawable.ic_circle_group_24dp);

        groupId = getIntent().getStringExtra(EXTRA_GROUP_ID);
        int tabPosition = getIntent().getIntExtra(EXTRA_TAB_POS, 0);
        final String msgId = getIntent().getStringExtra(EXTRA_MSG_ID);
        final boolean isJoining = getIntent().getBooleanExtra(EXTRA_IS_JOINING, false);
        dmId = getIntent().getStringExtra(EXTRA_DM_ID);

        if (!TextUtils.isEmpty(dmId)) {
            // for DMs
            tabLayout.setVisibility(View.GONE);
            inviteContainer.setVisibility(View.GONE);
            toolbarInviteHint.setText("Personal Chat");
        } else {
            tabLayout.setVisibility(View.VISIBLE);
            inviteContainer.setVisibility(View.VISIBLE);
        }

        ChatViewPagerAdapter adapter = new ChatViewPagerAdapter(getSupportFragmentManager(), msgId, isJoining);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(tabPosition, true);

        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (targetFrag != null) {
                    targetFrag.openGroupInfo();
                }
            }
        });
        try {
            Intent intent = this.getIntent();
            if (intent != null && intent.getExtras() != null && intent.getExtras().containsKey(TRACK_NOTIF_ID)
                    && (intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0) {
                String notifId = this.getIntent().getExtras().getString(TRACK_NOTIF_ID);

                if (!TextUtils.isEmpty(notifId)) {
                    final Bundle bundle = new Bundle();
                    bundle.putString("notifId", String.valueOf(notifId));
                    bundle.putString("groupId", String.valueOf(groupId));
                    bundle.putString("dmId", String.valueOf(dmId));
                    bundle.putString("msgId", String.valueOf(msgId));
                    Analytics.triggerEvent(AnalyticsEvents.NOTIF_OPENED, bundle, this);
                }
            }
        } catch (Exception e) {
            Crashlytics.logException(e);
        }
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                final Bundle bundle = new Bundle();
                bundle.putString("groupid", groupId);
                if (tab.getPosition() == 0) {
                    Analytics.triggerEvent(AnalyticsEvents.GROUP_CHAT_FRAG, bundle, ChatActivity.this);
                } else if (tab.getPosition() == 1) {
                    Analytics.triggerEvent(AnalyticsEvents.GROUP_MORE_FRAG, bundle, ChatActivity.this);
                    final View customView = tab.getCustomView();
                    if (customView != null) {
                        ((TextView) customView.findViewById(android.R.id.text1)).setTextColor(ContextCompat.getColor(ChatActivity.this, R.color.black));
                        customView.findViewById(R.id.badge).setVisibility(View.GONE);
                        LubbleSharedPrefs.getInstance().setIsBookExchangeOpened(true);
                    }
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                if (tab.getPosition() == 1) {
                    final View customView = tab.getCustomView();
                    if (customView != null) {
                        ((TextView) customView.findViewById(android.R.id.text1)).setTextColor(ContextCompat.getColor(ChatActivity.this, R.color.default_text_color));
                    }
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        inviteContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UserSearchActivity.newInstance(ChatActivity.this, groupId);
            }
        });

        searchIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // empty to consume clicks; late init after chats are loaded, logic in startChatSearch()
            }
        });

        searchBackIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeSearch();
            }
        });

        searchUpIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UiUtils.hideKeyboard(ChatActivity.this);
                if (searchResultData != null) {
                    if (currSearchCursorPos < searchResultData.getHits().size()) {
                        currSearchCursorPos++;
                    }
                    Hit searchHit = searchResultData.getHits().get(currSearchCursorPos);
                    targetFrag.scrollToChatId(searchHit.getChatId());
                } else {
                    onQueryTextSubmit(searchView.getQuery().toString());
                }
            }
        });

        searchDownIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UiUtils.hideKeyboard(ChatActivity.this);
                if (searchResultData != null) {
                    if (currSearchCursorPos > 0) {
                        currSearchCursorPos--;
                    }
                    Hit searchHit = searchResultData.getHits().get(currSearchCursorPos);
                    targetFrag.scrollToChatId(searchHit.getChatId());
                } else {
                    onQueryTextSubmit(searchView.getQuery().toString());
                }
            }
        });
    }

    private void closeSearch() {
        UiUtils.hideKeyboard(ChatActivity.this);
        searchView.setQuery("", false);
        searchView.clearFocus();
        searchResultData = null;
        currSearchCursorPos = 0;
        toolbar.clearFocus();
        toggleSearchViewVisibility(false);
    }

    private GroupData groupData;

    private ChatFragment getTargetChatFrag(String msgId, boolean isJoining) {
        if (!TextUtils.isEmpty(groupId)) {
            return targetFrag = ChatFragment.newInstanceForGroup(
                    groupId, isJoining, msgId, (ChatData) getIntent().getSerializableExtra(EXTRA_CHAT_DATA), (Uri) getIntent().getParcelableExtra(EXTRA_IMG_URI_DATA)
            );
        } else if (!TextUtils.isEmpty(dmId)) {
            return targetFrag = ChatFragment.newInstanceForDm(
                    dmId, msgId,
                    getIntent().getStringExtra(EXTRA_ITEM_TITLE), (ChatData) getIntent().getSerializableExtra(EXTRA_CHAT_DATA), (Uri) getIntent().getParcelableExtra(EXTRA_IMG_URI_DATA)
            );
        } else if (getIntent().hasExtra(EXTRA_RECEIVER_ID) && getIntent().hasExtra(EXTRA_RECEIVER_NAME)) {
            return targetFrag = ChatFragment.newInstanceForEmptyDm(
                    getIntent().getStringExtra(EXTRA_RECEIVER_ID),
                    getIntent().getStringExtra(EXTRA_RECEIVER_NAME),
                    getIntent().getStringExtra(EXTRA_RECEIVER_DP_URL),
                    getIntent().getStringExtra(EXTRA_ITEM_TITLE)
            );
        } else {
            throw new RuntimeException("Invalid Args, see the valid factory methods by searching for this error string");
        }
    }

    /**
     * ensures that notifs for this chat do not appear when activity is in foreground
     */
    private BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Since we will process the message and update the UI, we don't need to show a notif in Status Bar
            // To do this, we call abortBroadcast()
            Log.d("NotificationResultRecei", "onReceive: in activity");
            if (intent != null && intent.hasExtra("remoteMessage")) {
                final RemoteMessage remoteMessage = intent.getParcelableExtra("remoteMessage");
                Gson gson = new Gson();
                final Map<String, String> dataMap = remoteMessage.getData();
                JsonElement jsonElement = gson.toJsonTree(dataMap);
                NotifData notifData = gson.fromJson(jsonElement, NotifData.class);
                String type = dataMap.get("type");
                if ("chat".equalsIgnoreCase(type)) {
                    if (notifData.getGroupId().equalsIgnoreCase(groupId)) {
                        abortBroadcast();
                        sendNotifAnalyticEvent(AnalyticsEvents.NOTIF_ABORTED, dataMap, ChatActivity.this);
                    }
                } else if ("dm".equalsIgnoreCase(type)) {
                    if (notifData.getGroupId().equalsIgnoreCase(dmId)) {
                        abortBroadcast();
                        sendNotifAnalyticEvent(AnalyticsEvents.NOTIF_ABORTED, dataMap, ChatActivity.this);
                    }
                } else {
                    Crashlytics.logException(new IllegalArgumentException("chatactiv: notif recvd with illegal type"));
                }
            } else {
                Crashlytics.logException(new MissingFormatArgumentException("chatactiv: notif broadcast recvd with no intent data"));
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(NEW_CHAT_ACTION);
        filter.setPriority(1);
        registerReceiver(notificationReceiver, filter);

    }

    public void chatLoadingComplete() {
        searchIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startChatSearch();
            }
        });
    }

    private void startChatSearch() {
        toggleSearchViewVisibility(true);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (!TextUtils.isEmpty(query)) {
            HashMap<Object, Object> map = new HashMap<>();
            map.put("query", query);
            map.put("lubbleId", LubbleSharedPrefs.getInstance().getLubbleId());
            map.put("groupId", groupId);
            DatabaseReference pushQueryRef = RealtimeDbHelper.getSearchQueryRef().push();
            pushQueryRef.setValue(map);
            initSearchResultListener(pushQueryRef.getKey());
        }
        return false;
    }

    private void initSearchResultListener(String searchKey) {
        RealtimeDbHelper.getSearchResultRef().child(searchKey).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                searchResultData = dataSnapshot.getValue(SearchResultData.class);
                if (searchResultData != null) {
                    if (searchResultData.getNbHits() > 0) {
                        Hit searchHit = searchResultData.getHits().get(0);
                        targetFrag.scrollToChatId(searchHit.getChatId());
                        currSearchCursorPos = 0;
                    } else {
                        Toast.makeText(ChatActivity.this, "No results found", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    public class ChatViewPagerAdapter extends FragmentPagerAdapter {

        private String title[] = {"Chats", "Collections"};
        private String msgId;
        private boolean isJoining;

        public ChatViewPagerAdapter(FragmentManager manager, String msgId, boolean isJoining) {
            super(manager);
            this.msgId = msgId;
            this.isJoining = isJoining;
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return getTargetChatFrag(msgId, isJoining);
            } else {
                return ChatMoreFragment.newInstance(groupId);
            }
        }

        @Override
        public int getCount() {
            return dmId == null ? title.length : 1;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return title[position];
        }
    }

    public void setGroupMeta(String title, String thumbnailUrl, boolean isPrivate, int memberCount) {
        toolbarTv.setText(title);
        if (StringUtils.isValidString(thumbnailUrl)) {
            GlideApp.with(this).load(thumbnailUrl).circleCrop().into(toolbarIcon);
        }
        toolbarLockIcon.setVisibility(isPrivate ? View.VISIBLE : View.GONE);

        groupData = new GroupData();
        groupData.setId(groupId);
        groupData.setTitle(title);
        groupData.setThumbnail(thumbnailUrl);
        groupData.setIsPrivate(isPrivate);

        if (dmId != null) {
            toolbarInviteHint.setText(getString(R.string.personal_chat));
        } else if (memberCount > 10) {
            toolbarInviteHint.setText(String.format(getString(R.string.members_count), memberCount));
        } else {
            toolbarInviteHint.setText(getString(R.string.click_group_info));
        }
    }

    private void toggleSearchViewVisibility(boolean show) {
        if (show) {
            searchView.setVisibility(View.VISIBLE);
            searchView.setFocusable(true);
            searchView.requestFocusFromTouch();
            searchView.setIconifiedByDefault(false);
            searchView.setIconified(false);
            searchBackIv.setVisibility(View.VISIBLE);
            searchUpIv.setVisibility(View.VISIBLE);
            searchDownIv.setVisibility(View.VISIBLE);
            toolbarIcon.setVisibility(View.GONE);
            toolbarLockIcon.setVisibility(View.GONE);
            toolbarTv.setVisibility(View.GONE);
            searchIv.setVisibility(View.GONE);
            toolbarInviteHint.setVisibility(View.GONE);
            inviteContainer.setVisibility(View.GONE);
            tabLayout.setVisibility(View.GONE);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        } else {
            searchView.setOnQueryTextListener(null);
            searchView.setVisibility(View.GONE);
            searchBackIv.setVisibility(View.GONE);
            searchUpIv.setVisibility(View.GONE);
            searchDownIv.setVisibility(View.GONE);
            toolbarIcon.setVisibility(View.VISIBLE);
            toolbarTv.setVisibility(View.VISIBLE);
            toolbarLockIcon.setVisibility(groupData.getIsPrivate() ? View.VISIBLE : View.GONE);
            searchIv.setVisibility(View.VISIBLE);
            inviteContainer.setVisibility(View.VISIBLE);
            tabLayout.setVisibility(View.VISIBLE);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    public void showNewBadge() {
        tabLayout.getTabAt(1).setCustomView(R.layout.tab_with_badge);
        final View customView = tabLayout.getTabAt(1).getCustomView();
        if (customView != null) {
            customView.findViewById(R.id.badge).setVisibility(View.VISIBLE);
        }
    }

    private void blockAccount() {
        String title = "Block this person?";
        if (!TextUtils.isEmpty(groupData.getTitle())) {
            title = "Block " + groupData.getTitle() + "?";
        }
        UiUtils.showBottomSheetAlertLight(ChatActivity.this, getLayoutInflater(), title,
                "They will no longer be able to message you. You can unblock anytime from the navigation.",
                R.drawable.ic_block_black_24dp, "BLOCK", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        targetFrag.setBlockedStatus("BLOCKED");
                        Analytics.triggerEvent(AnalyticsEvents.DM_BLOCKED, ChatActivity.this);
                        finish();
                    }
                });
    }

    private void reportAccount() {
        String title = "Report this person to Lubble?";
        if (!TextUtils.isEmpty(groupData.getTitle())) {
            title = "Report " + groupData.getTitle() + " to Lubble?";
        }
        UiUtils.showBottomSheetAlertLight(ChatActivity.this, getLayoutInflater(), title,
                "We will investigate their profile for spam, abusive, or inappropriate behaviour & take necessary action.\nThis will also block them.",
                R.drawable.ic_error_outline_black_24dp, "REPORT", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        targetFrag.setBlockedStatus("REPORTED");
                        // push DM id to firebase, which triggers an email for the report
                        DatabaseReference pushRef = FirebaseDatabase.getInstance().getReference("dm_reports").push();
                        final HashMap<Object, Object> map = new HashMap<>();
                        map.put("dm_id", dmId);
                        pushRef.setValue(map);
                        Analytics.triggerEvent(AnalyticsEvents.DM_REPORTED, ChatActivity.this);
                        finish();
                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(notificationReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(notificationReceiver);
        } catch (IllegalArgumentException e) {
            //already unregistered
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(notificationReceiver);
        } catch (IllegalArgumentException e) {
            //already unregistered
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        int menuId = dmId != null ? R.menu.chat_menu : R.menu.group_chat_menu;
        getMenuInflater().inflate(menuId, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.block:
                blockAccount();
                return true;
            case R.id.report:
                reportAccount();
                return true;
            case R.id.group_info:
                if (targetFrag != null) {
                    targetFrag.openGroupInfo();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (searchView.getVisibility() == View.VISIBLE) {
            closeSearch();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onFlairUpdated() {
        ChatFragment frag = (ChatFragment)
                getSupportFragmentManager().findFragmentByTag(makeFragmentName(viewPager.getId(), 0));
        frag.updateThisUserFlair();
    }

    private static String makeFragmentName(int viewPagerId, int index) {
        return "android:switcher:" + viewPagerId + ":" + index;
    }

}
