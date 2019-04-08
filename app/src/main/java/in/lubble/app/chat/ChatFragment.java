package in.lubble.app.chat;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.Fragment;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.gson.Gson;
import in.lubble.app.Constants;
import in.lubble.app.GlideApp;
import in.lubble.app.LubbleSharedPrefs;
import in.lubble.app.R;
import in.lubble.app.analytics.Analytics;
import in.lubble.app.analytics.AnalyticsEvents;
import in.lubble.app.chat.chat_info.MsgInfoActivity;
import in.lubble.app.events.EventPickerActiv;
import in.lubble.app.firebase.RealtimeDbHelper;
import in.lubble.app.groups.group_info.ScrollingGroupInfoActivity;
import in.lubble.app.models.*;
import in.lubble.app.network.LinkMetaAsyncTask;
import in.lubble.app.network.LinkMetaListener;
import in.lubble.app.notifications.UnreadChatsSharedPrefs;
import in.lubble.app.utils.AppNotifUtils;
import in.lubble.app.utils.DateTimeUtils;
import permissions.dispatcher.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static android.app.Activity.RESULT_OK;
import static in.lubble.app.firebase.RealtimeDbHelper.*;
import static in.lubble.app.models.ChatData.*;
import static in.lubble.app.utils.FileUtils.*;
import static in.lubble.app.utils.NotifUtils.deleteUnreadMsgsForGroupId;
import static in.lubble.app.utils.StringUtils.extractFirstLink;
import static in.lubble.app.utils.StringUtils.isValidString;
import static in.lubble.app.utils.UiUtils.dpToPx;
import static in.lubble.app.utils.UiUtils.showBottomSheetAlert;
import static in.lubble.app.utils.YoutubeUtils.extractYoutubeId;

@RuntimePermissions
public class ChatFragment extends Fragment implements View.OnClickListener, AttachmentClickListener {

    private static final String TAG = "ChatFragment";
    private static final int REQUEST_CODE_IMG = 789;
    private static final int REQUEST_CODE_GROUP_PICK = 917;
    private static final int REQUEST_CODE_EVENT_PICK = 922;
    private static final String KEY_GROUP_ID = "CHAT_GROUP_ID";
    private static final String KEY_MSG_ID = "CHAT_MSG_ID";
    private static final String KEY_IS_JOINING = "KEY_IS_JOINING";
    private static final String KEY_IMG_URI = "KEY_IMG_URI";
    private static final String KEY_DM_ID = "KEY_DM_ID";
    private static final String KEY_RECEIVER_ID = "KEY_RECEIVER_ID";
    private static final String KEY_RECEIVER_NAME = "KEY_RECEIVER_NAME";
    private static final String KEY_RECEIVER_DP_URL = "KEY_RECEIVER_DP_URL";
    private static final String KEY_ITEM_TITLE = "KEY_ITEM_TITLE";
    private static final String KEY_CHAT_DATA = "KEY_CHAT_DATA";

    @Nullable
    private GroupData groupData;
    private RelativeLayout joinContainer;
    private TextView joinDescTv;
    private Button joinBtn;
    private ImageView declineIv;
    private RelativeLayout composeContainer;
    private Group linkMetaContainer;
    private RecyclerView chatRecyclerView;
    private EditText newMessageEt;
    private ImageView sendBtn;
    private ImageView attachMediaBtn;
    private ImageView linkPicIv;
    private TextView linkTitle;
    private TextView linkDesc;
    private ImageView linkCancel;
    @Nullable
    private DatabaseReference groupReference;
    @Nullable
    private DatabaseReference dmInfoReference;
    @Nullable
    private DatabaseReference messagesReference;
    private String currentPhotoPath;
    @Nullable
    private String groupId;
    @Nullable
    private String dmId;
    @Nullable
    private String receiverId;
    @Nullable
    private String receiverName;
    @Nullable
    private String receiverDpUrl;
    @Nullable
    private String itemTitle;
    @Nullable
    private String msgIdToOpen;
    private boolean isJoining;
    private ChildEventListener msgChildListener;
    private ValueEventListener groupInfoListener;
    private HashMap<String, ProfileInfo> groupMembersMap;
    private ValueEventListener dmEventListener;
    private String prevUrl = "";
    private boolean foundFirstUnreadMsg;
    private RelativeLayout bottomContainer;
    private View pvtSystemMsg;
    private ProgressDialog joiningProgressDialog;
    private ProgressBar sendBtnProgressBtn;
    private ProgressBar chatProgressBar;
    private ProgressBar paginationProgressBar;
    @Nullable
    private ValueEventListener bottomBarListener;
    @Nullable
    private String replyMsgId = null;
    @Nullable
    private Parcelable recyclerViewState;
    private ChatAdapter chatAdapter;
    private String authorId = FirebaseAuth.getInstance().getUid();
    private boolean isCurrUserSeller;
    private LinkMetaAsyncTask linkMetaAsyncTask;
    private boolean isLoadingMoreChats;
    private boolean isLastPage;
    private long endAtTimestamp;
    private final static int PAGE_SIZE = 20;
    private int unreadCount = 0;
    private String attachedGroupId;
    private String attachedEventId;
    private String attachedGroupPicUrl;
    private String attachedEventPicUrl;
    private Uri sharedImageUri;
    private ConstraintLayout introPromptContainer;
    private ImageView introPromptCloseIv;
    private ImageView bunnyHandsIv;

    public ChatFragment() {
        // Required empty public constructor
    }

    public static ChatFragment newInstanceForGroup(@NonNull String groupId, boolean isJoining, @Nullable String msgId, @Nullable ChatData chatData, @Nullable Uri imgUri) {
        Bundle args = new Bundle();
        args.putString(KEY_GROUP_ID, groupId);
        args.putString(KEY_MSG_ID, msgId);
        args.putSerializable(KEY_CHAT_DATA, chatData);
        args.putBoolean(KEY_IS_JOINING, isJoining);
        args.putParcelable(KEY_IMG_URI, imgUri);
        ChatFragment fragment = new ChatFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static ChatFragment newInstanceForDm(@NonNull String dmId, @Nullable String msgId, @Nullable String itemName, @Nullable ChatData chatData, @Nullable Uri imgUri) {
        Bundle args = new Bundle();
        args.putString(KEY_MSG_ID, msgId);
        args.putString(KEY_DM_ID, dmId);
        args.putString(KEY_ITEM_TITLE, itemName);
        args.putSerializable(KEY_CHAT_DATA, chatData);
        args.putParcelable(KEY_IMG_URI, imgUri);
        ChatFragment fragment = new ChatFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static ChatFragment newInstanceForEmptyDm(@NonNull String receiverId, @NonNull String receiverName, @Nullable String receiverDpUrl, @Nullable String itemName) {
        Bundle args = new Bundle();
        args.putString(KEY_RECEIVER_ID, receiverId);
        args.putString(KEY_RECEIVER_NAME, receiverName);
        args.putString(KEY_RECEIVER_DP_URL, receiverDpUrl);
        args.putString(KEY_ITEM_TITLE, itemName);
        ChatFragment fragment = new ChatFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        groupId = getArguments().getString(KEY_GROUP_ID);
        msgIdToOpen = getArguments().getString(KEY_MSG_ID);
        dmId = getArguments().getString(KEY_DM_ID);
        receiverId = getArguments().getString(KEY_RECEIVER_ID);
        receiverName = getArguments().getString(KEY_RECEIVER_NAME);
        receiverDpUrl = getArguments().getString(KEY_RECEIVER_DP_URL);
        itemTitle = getArguments().getString(KEY_ITEM_TITLE);
        isJoining = getArguments().getBoolean(KEY_IS_JOINING);

        if (groupId != null) {
            groupReference = getLubbleGroupsRef().child(groupId);
            messagesReference = getMessagesRef().child(groupId);
        } else if (dmId != null) {
            dmInfoReference = getDmsRef().child(dmId);
            messagesReference = getDmMessagesRef().child(dmId);
        } else if (receiverId != null) {
            // no refs need to be init here
        } else {
            throw new RuntimeException("khuch to params dega bhai?");
        }
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    private void populateChatData(ChatData chatData) {
        switch (chatData.getType()) {
            case GROUP:
                attachedGroupId = chatData.getAttachedGroupId();
                fetchAndShowAttachedGroupInfo();
            case EVENT:
                attachedEventId = chatData.getAttachedGroupId();
                fetchAndShowAttachedEventInfo();
                break;
        }
        if (!TextUtils.isEmpty(chatData.getMessage())) {
            newMessageEt.setText(chatData.getMessage());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_chat, container, false);

        composeContainer = view.findViewById(R.id.compose_container);
        joinContainer = view.findViewById(R.id.relativeLayout_join_container);
        joinDescTv = view.findViewById(R.id.tv_join_desc);
        joinBtn = view.findViewById(R.id.btn_join);
        declineIv = view.findViewById(R.id.iv_decline_cross);
        chatRecyclerView = view.findViewById(R.id.rv_chat);
        newMessageEt = view.findViewById(R.id.et_new_message);
        sendBtn = view.findViewById(R.id.iv_send_btn);
        attachMediaBtn = view.findViewById(R.id.iv_attach);
        linkMetaContainer = view.findViewById(R.id.group_link_meta);
        linkPicIv = view.findViewById(R.id.iv_link_pic);
        linkTitle = view.findViewById(R.id.tv_link_title);
        linkDesc = view.findViewById(R.id.tv_link_desc);
        linkCancel = view.findViewById(R.id.iv_link_cancel);
        bottomContainer = view.findViewById(R.id.bottom_container);
        pvtSystemMsg = view.findViewById(R.id.view_pvt_sys_msg);
        sendBtnProgressBtn = view.findViewById(R.id.progress_bar_send);
        chatProgressBar = view.findViewById(R.id.progressbar_chat);
        paginationProgressBar = view.findViewById(R.id.progressbar_pagination);
        introPromptContainer = view.findViewById(R.id.container_intro_prompt);
        introPromptCloseIv = view.findViewById(R.id.iv_intro_prompt_close);
        bunnyHandsIv = view.findViewById(R.id.iv_bunny_hands);

        groupMembersMap = new HashMap<>();

        if (isJoining) {
            showJoiningDialog();
        }
        sendBtn.setEnabled(false);
        setupTogglingOfSendBtn();
        sendBtn.setOnClickListener(this);
        attachMediaBtn.setOnClickListener(this);
        joinBtn.setOnClickListener(this);
        declineIv.setOnClickListener(this);
        linkCancel.setOnClickListener(this);

        if (!TextUtils.isEmpty(itemTitle)) {
            // new DM chat, pre-fill help text in editText
            newMessageEt.setText("Hi! I am interested in \"" + itemTitle + "\"");
            newMessageEt.selectAll();

            newMessageEt.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (getContext() != null) {
                        InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputMethodManager.showSoftInput(newMessageEt, InputMethodManager.SHOW_IMPLICIT);
                    }
                }
            }, 1000);
            newMessageEt.requestFocus();
        }
        init();
        ChatData chatData = (ChatData) getArguments().getSerializable(KEY_CHAT_DATA);
        if (getArguments().getParcelable(KEY_IMG_URI) != null) {
            sharedImageUri = getArguments().getParcelable(KEY_IMG_URI);
            if (TextUtils.isEmpty(dmId)) {
                // not a DM
                AttachImageActivity.open(getContext(), sharedImageUri, groupId, false, isCurrUserSeller, authorId);
                sharedImageUri = null;
            }
        }
        if (chatData != null) {
            populateChatData(chatData);
        }

        introPromptCloseIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideIntroPrompt();
                Analytics.triggerEvent(AnalyticsEvents.GROUP_QUES_DISMISSED, getContext());
            }
        });

        return view;
    }

    private void hideIntroPrompt() {
        getUserGroupsRef().child(groupId).child("isIntroPromptDismissed").setValue(true);
        introPromptContainer.animate()
                .translationY(introPromptContainer.getHeight())
                .setInterpolator(new FastOutSlowInInterpolator())
                .setDuration(200)
                .setListener(new AnimatorListenerAdapter() {

                    @Override
                    public void onAnimationStart(Animator animation) {
                        introPromptContainer.setVisibility(View.VISIBLE);
                        bunnyHandsIv.setVisibility(View.GONE);
                        chatRecyclerView.setPadding(0, 0, 0, 0);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        introPromptContainer.setVisibility(View.GONE);
                    }
                });
    }

    private void showJoiningDialog() {
        joiningProgressDialog = new ProgressDialog(getContext());
        joiningProgressDialog.setTitle(getString(R.string.joining_group));
        joiningProgressDialog.setMessage(getString(R.string.all_please_wait));
        joiningProgressDialog.show();
    }

    private void init() {
        endAtTimestamp = 0L;
        syncGroupInfo();
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        chatRecyclerView.setLayoutManager(layoutManager);
        chatAdapter = new ChatAdapter(
                getActivity(),
                getContext(),
                groupId,
                chatRecyclerView,
                this,
                GlideApp.with(getContext()));
        chatRecyclerView.setAdapter(chatAdapter);
        calcUnreadCount();
        if (messagesReference != null) {
            msgChildListener = msgListener(messagesReference);
            initMsgListenerToKnowWhenSyncComplete();
        } else {
            chatProgressBar.setVisibility(View.GONE);
        }
        foundFirstUnreadMsg = false;
        chatRecyclerView.setOnFlingListener(new RecyclerView.OnFlingListener() {
            @Override
            public boolean onFling(int velocityX, int velocityY) {
                recyclerViewState = null;
                msgIdToOpen = null;
                return false;
            }
        });
        chatAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                if (!isLoadingMoreChats) {
                    int msgCount = chatAdapter.getItemCount();
                    int lastVisiblePosition =
                            layoutManager.findLastCompletelyVisibleItemPosition();
                    if (recyclerViewState != null) {
                        chatRecyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);
                        if (lastVisiblePosition != -1 && (positionStart >= (msgCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                            // If the user is at the bottom of the list, scroll to the bottom
                            // of the list to show the newly added message.
                            recyclerViewState = null;
                            chatRecyclerView.scrollToPosition(positionStart);
                        } else {
                            chatRecyclerView.scrollToPosition(positionStart);
                        }
                    } else if (msgIdToOpen != null) {
                        final int indexOfChatMsg = chatAdapter.getIndexOfChatMsg(msgIdToOpen);
                        if (indexOfChatMsg != -1) {
                            chatRecyclerView.scrollToPosition(indexOfChatMsg);
                            chatAdapter.setPosToFlash(indexOfChatMsg);
                            if (lastVisiblePosition != -1 && (positionStart >= (msgCount - 1) &&
                                    lastVisiblePosition == (positionStart - 1))) {
                                // If the user is at the bottom of the list, scroll to the bottom
                                // of the list to show the newly added message.
                                msgIdToOpen = null;
                                chatRecyclerView.scrollToPosition(positionStart);
                            }
                        }
                    } else {
                        // If the recycler view is initially being loaded
                        if (lastVisiblePosition == -1 && !foundFirstUnreadMsg) {
                            final int pos = msgCount - 1;
                            final ChatData chatMsg = chatAdapter.getChatMsgAt(pos);
                            if (chatMsg.getReadReceipts().get(authorId) == null) {
                                // unread msg found
                                foundFirstUnreadMsg = true;
                                final ChatData unreadChatData = new ChatData();
                                unreadChatData.setType(UNREAD);
                                chatAdapter.addChatData(pos, unreadChatData);
                                chatRecyclerView.scrollToPosition(pos - 1);
                            } else {
                                // all msgs read, scroll to last msg
                                chatRecyclerView.scrollToPosition(positionStart);
                            }
                        } else if (lastVisiblePosition != -1 && (positionStart >= (msgCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                            // If the user is at the bottom of the list, scroll to the bottom
                            // of the list to show the newly added message.
                            chatRecyclerView.scrollToPosition(positionStart);
                        } else if (isValidString(chatAdapter.getChatMsgAt(positionStart).getAuthorUid()) &&
                                chatAdapter.getChatMsgAt(positionStart).getAuthorUid().equalsIgnoreCase(authorId)) {
                            chatRecyclerView.scrollToPosition(positionStart);
                        } else {
                            chatRecyclerView.scrollToPosition(positionStart);
                        }
                    }
                }
            }
        });

        chatRecyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (bottom < oldBottom) {
                    int position = chatAdapter.getItemCount() - 1;
                    if (position != -1) {
                        // scrollToPosition() doesn't work here. why?
                        // opening keyboard will now shift recyclerview above
                        chatRecyclerView.smoothScrollToPosition(position);
                    }
                }
            }
        });

        chatRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (firstVisibleItemPosition == 0 && !isLoadingMoreChats && !isLastPage && totalItemCount != visibleItemCount) {
                    paginationProgressBar.setVisibility(View.VISIBLE);
                    moreMsgListener(messagesReference);
                }
            }
        });

        resetActionBar();
    }

    @Override
    public void onResume() {
        super.onResume();
        deleteUnreadMsgsForGroupId(groupId, getContext());
        AppNotifUtils.deleteAppNotif(getContext(), groupId);
        resetUnreadCount();
    }

    private void calcUnreadCount() {
        final SharedPreferences chatSharedPrefs = UnreadChatsSharedPrefs.getInstance().getPreferences();
        final Map<String, String> chatsMap = (Map<String, String>) chatSharedPrefs.getAll();
        for (String json : chatsMap.values()) {
            final NotifData notifData = new Gson().fromJson(json, NotifData.class);
            if (notifData.getGroupId().equalsIgnoreCase(groupId)) {
                ++unreadCount;
            }
        }
    }

    private void initMsgListenerToKnowWhenSyncComplete() {
        messagesReference.orderByChild("serverTimestamp").limitToLast(PAGE_SIZE + unreadCount).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // this is only called after all chats have been synced
                // use this to hide the progressbar
                if (paginationProgressBar.getVisibility() == View.VISIBLE) {
                    paginationProgressBar.setVisibility(View.GONE);
                }
                if (chatProgressBar != null && chatProgressBar.getVisibility() == View.VISIBLE) {
                    chatProgressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void resetActionBar() {
        final ActionMode actionMode = ((AppCompatActivity) getContext()).startSupportActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.menu_chat, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {

            }
        });
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    // For populating the toolbar with DP and Title
    private void syncGroupInfo() {
        if (!TextUtils.isEmpty(groupId)) {
            groupInfoListener = groupReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    groupData = dataSnapshot.getValue(GroupData.class);
                    // fetchMembersProfile(groupData.getMembers()); to be used for tagging
                    if (groupData != null && getActivity() != null) {
                        if (!groupData.isJoined() && groupData.getIsPrivate()) {
                            chatRecyclerView.setVisibility(View.GONE);
                            pvtSystemMsg.setVisibility(View.VISIBLE);
                            ((TextView) pvtSystemMsg.findViewById(R.id.tv_system_msg)).setText(R.string.pvt_group_msgs_hidden);
                        } else {
                            chatRecyclerView.setVisibility(View.VISIBLE);
                            pvtSystemMsg.setVisibility(View.GONE);
                        }
                        ((ChatActivity) getActivity()).setGroupMeta(groupData.getTitle(), groupData.getThumbnail(), groupData.getIsPrivate());
                        resetUnreadCount();
                        showBottomBar(groupData);
                        showPublicGroupWarning();
                    } else {
                        Crashlytics.logException(new NullPointerException("groupdata is null for group id: " + groupId));
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Crashlytics.logException(databaseError.toException());
                }
            });
        } else if (!TextUtils.isEmpty(dmId)) {
            chatRecyclerView.setVisibility(View.VISIBLE);
            deleteUnreadMsgsForGroupId(dmId, getContext());
            AppNotifUtils.deleteAppNotif(getContext(), dmId);
            dmEventListener = dmInfoReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    final DmData dmData = dataSnapshot.getValue(DmData.class);
                    if (dmData != null) {
                        dmData.setId(dataSnapshot.getKey());

                        final HashMap<String, Object> members = dmData.getMembers();
                        for (String profileId : members.keySet()) {
                            final String sellerId = String.valueOf(LubbleSharedPrefs.getInstance().getSellerId());
                            if (authorId.equalsIgnoreCase(profileId) || sellerId.equalsIgnoreCase(profileId)) {
                                // this person's profile ID, could be a seller or a user
                                final HashMap<String, Object> profileMap = (HashMap<String, Object>) members.get(profileId);
                                if (profileMap != null) {
                                    isCurrUserSeller = (boolean) profileMap.get("isSeller");
                                    authorId = profileId;
                                    chatAdapter.setAuthorId(authorId);
                                    chatAdapter.setDmId(dmId);
                                    if (sharedImageUri != null) {
                                        String chatId = groupId;
                                        if (!TextUtils.isEmpty(dmId)) {
                                            chatId = dmId;
                                        }
                                        AttachImageActivity.open(getContext(), sharedImageUri, chatId, !TextUtils.isEmpty(dmId), isCurrUserSeller, authorId);
                                        sharedImageUri = null;
                                    }
                                }
                            } else {
                                // other person's profile ID, could be a seller or a user
                                final HashMap<String, Object> profileMap = (HashMap<String, Object>) members.get(profileId);
                                if (profileMap != null) {
                                    final boolean isSeller = (boolean) profileMap.get("isSeller");
                                    if (isSeller) {
                                        fetchSellerProfileFrom(profileId);
                                    } else {
                                        fetchProfileFrom(profileId);
                                    }
                                }
                            }
                        }
                        resetUnreadCount();
                    } else {
                        Crashlytics.logException(new NullPointerException("dmData is null for dm id: " + dmId));
                    }
                }

                private void fetchProfileFrom(String profileId) {
                    getUserInfoRef(profileId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            HashMap<String, String> map = (HashMap<String, String>) dataSnapshot.getValue();
                            if (map != null) {
                                final ProfileInfo profileInfo = dataSnapshot.getValue(ProfileInfo.class);
                                if (profileInfo != null) {
                                    profileInfo.setId(dataSnapshot.getRef().getParent().getKey()); // this works. Don't touch.
                                    ((ChatActivity) getActivity()).setGroupMeta(profileInfo.getName(), profileInfo.getThumbnail(), true);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }

                private synchronized void fetchSellerProfileFrom(String profileId) {
                    getSellerRef().child(profileId).child("info").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            final ProfileInfo profileInfo = dataSnapshot.getValue(ProfileInfo.class);
                            if (profileInfo != null) {
                                profileInfo.setId(dataSnapshot.getRef().getParent().getKey()); // this works. Don't touch.
                                ((ChatActivity) getActivity()).setGroupMeta(profileInfo.getName(), profileInfo.getThumbnail(), true);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Crashlytics.logException(databaseError.toException());
                }
            });
        } else if (!TextUtils.isEmpty(receiverId)) {
            chatRecyclerView.setVisibility(View.VISIBLE);
            ((ChatActivity) getActivity()).setGroupMeta(receiverName, receiverDpUrl, true);
        }
    }

    private void fetchMembersProfile(HashMap<String, Object> membersMap) {
        for (String uid : membersMap.keySet()) {
            ValueEventListener valueEventListener = getUserInfoRef(uid).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    final ProfileInfo profileInfo = dataSnapshot.getValue(ProfileInfo.class);
                    if (profileInfo != null) {
                        profileInfo.setId(dataSnapshot.getRef().getParent().getKey()); // this works. Don't touch.
                        groupMembersMap.put(dataSnapshot.getRef().getParent().getKey(), profileInfo);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    private void showBottomBar(final GroupData groupData) {
        if (!isJoining) {
            bottomContainer.setVisibility(View.VISIBLE);
        }

        bottomBarListener = RealtimeDbHelper.getUserGroupsRef().child(groupId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (groupData.isJoined()) {
                    composeContainer.setVisibility(View.VISIBLE);
                    joinContainer.setVisibility(View.GONE);
                    if (joiningProgressDialog != null && isJoining) {
                        bottomContainer.setVisibility(View.VISIBLE);
                        joiningProgressDialog.dismiss();
                        isJoining = false;
                    }
                    final UserGroupData userGroupData = dataSnapshot.getValue(UserGroupData.class);
                    showIntroPrompt(userGroupData);
                } else {
                    final UserGroupData userGroupData = dataSnapshot.getValue(UserGroupData.class);
                    if (userGroupData != null && userGroupData.getInvitedBy() != null && userGroupData.getInvitedBy().size() != 0) {
                        final HashMap<String, Boolean> invitedBy = userGroupData.getInvitedBy();
                        String inviter = (String) invitedBy.keySet().toArray()[0];
                        if (inviter.equalsIgnoreCase(LubbleSharedPrefs.getInstance().getSupportUid())) {
                            joinDescTv.setText(getString(R.string.ready_to_join));
                            declineIv.setVisibility(View.GONE);
                        } else {
                            RealtimeDbHelper.getUserInfoRef(inviter).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (isAdded()) {
                                        final ProfileInfo profileInfo = dataSnapshot.getValue(ProfileInfo.class);
                                        joinDescTv.setText(String.format(getString(R.string.invited_by), profileInfo.getName()));
                                        declineIv.setVisibility(View.VISIBLE);
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                        }
                        composeContainer.setVisibility(View.GONE);
                        joinContainer.setVisibility(View.VISIBLE);
                    } else {
                        joinDescTv.setText(R.string.join_group_to_chat);
                        declineIv.setVisibility(View.GONE);
                        composeContainer.setVisibility(View.GONE);
                        joinContainer.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void showIntroPrompt(UserGroupData userGroupData) {
        if (groupData != null && !TextUtils.isEmpty(groupData.getQuestion()) && !userGroupData.getIsIntroPromptDismissed()) {
            final TranslateAnimation translateAnimation = new TranslateAnimation(0, 0, dpToPx(48), 0);
            translateAnimation.setDuration(200);
            translateAnimation.setInterpolator(new FastOutSlowInInterpolator());
            translateAnimation.setAnimationListener(new Animation.AnimationListener() {

                @Override
                public void onAnimationStart(Animation animation) {
                    introPromptContainer.setVisibility(View.VISIBLE);
                    chatRecyclerView.setPadding(0, 0, 0, dpToPx(48));
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

            });
            introPromptContainer.startAnimation(translateAnimation);
            bunnyHandsIv.setVisibility(View.VISIBLE);
            final TextView msgTv = introPromptContainer.findViewById(R.id.tv_intro_prompt);
            msgTv.setText(groupData.getQuestion());
        }
    }

    private void showPublicGroupWarning() {
        if (!LubbleSharedPrefs.getInstance().getIsDefaultGroupInfoShown() && groupId.equalsIgnoreCase(Constants.DEFAULT_GROUP)) {
            RealtimeDbHelper.getLubbleRef().addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String lubbleName = dataSnapshot.child("title").getValue(String.class);
                    showBottomSheetAlert(getContext(), getLayoutInflater(),
                            String.format(getString(R.string.lubble_group_warning_title), lubbleName),
                            String.format(getString(R.string.lubble_group_warning_subtitle), lubbleName),
                            R.drawable.ic_public_black_24dp, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    LubbleSharedPrefs.getInstance().setIsDefaultGroupInfoShown(true);
                                }
                            });
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        } else if (groupData != null && !LubbleSharedPrefs.getInstance().getIsPublicGroupInfoShown()
                && !groupData.getIsPrivate() && !groupId.equalsIgnoreCase(Constants.DEFAULT_GROUP)) {
            showBottomSheetAlert(getContext(), getLayoutInflater(),
                    getString(R.string.public_group_warning_title),
                    getString(R.string.public_group_warning_subtitle),
                    R.drawable.ic_public_black_24dp, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            LubbleSharedPrefs.getInstance().setIsPublicGroupInfoShown(true);
                        }
                    });
        }
    }

    private void resetUnreadCount() {
        if (!TextUtils.isEmpty(groupId) && groupData != null && groupData.isJoined()) {
            RealtimeDbHelper.getUserGroupsRef().child(groupId)
                    .child("unreadCount").setValue(0);
        } else if (!TextUtils.isEmpty(dmId)) {
            if (isCurrUserSeller) {
                RealtimeDbHelper.getSellerDmsRef().child(dmId)
                        .child("unreadCount").setValue(0);
            } else {
                RealtimeDbHelper.getUserDmsRef().child(dmId)
                        .child("unreadCount").setValue(0);
            }
        }
    }

    private ChildEventListener msgListener(@NonNull DatabaseReference messagesReference) {
        final ArrayList<ChatData> tempChatList = new ArrayList<>();
        return messagesReference.orderByChild("serverTimestamp").limitToLast(PAGE_SIZE + unreadCount).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Log.d(TAG, "onChildAdded: ");
                final ChatData chatData = dataSnapshot.getValue(ChatData.class);
                if (chatData != null) {
                    sendBtnProgressBtn.setVisibility(View.GONE);
                    Log.d(TAG, "onChildAdded: " + dataSnapshot.getKey());
                    chatData.setId(dataSnapshot.getKey());
                    tempChatList.add(chatData);
                    sendReadReceipt(chatData);
                    final int lastPos = chatAdapter.getItemCount() - 1;
                    ChatData lastMsg = null;
                    if (lastPos > -1) {
                        lastMsg = chatAdapter.getChatMsgAt(lastPos);
                    }
                    checkAndInsertDate(chatData, lastMsg, chatAdapter.getItemCount());
                    chatAdapter.addChatData(chatData);
                    if (tempChatList.size() == PAGE_SIZE) {
                        endAtTimestamp = tempChatList.get(0).getServerTimestampInLong();
                        /*for (int i = 0; i < tempChatList.size(); i++) {
                            final ChatData currChatData = tempChatList.get(i);
                        }*/
                    }
                } else {
                    Crashlytics.logException(new NullPointerException("chat data is null for chat ID: " + dataSnapshot.getKey() + " and group ID: " + groupId));
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                if (isAdded()) {
                    Log.d(TAG, "onChildChanged: ");
                    final ChatData chatData = dataSnapshot.getValue(ChatData.class);
                    if (chatData != null) {
                        chatData.setId(dataSnapshot.getKey());
                        chatAdapter.updateChatData(chatData);
                    }
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onChildRemoved: ");
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Crashlytics.logException(databaseError.toException());
            }
        });
    }

    private void moreMsgListener(@NonNull DatabaseReference messagesReference) {
        isLoadingMoreChats = true;
        final Query query = messagesReference.orderByChild("serverTimestamp").endAt(endAtTimestamp).limitToLast(PAGE_SIZE);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            final ArrayList<ChatData> newChatDataList = new ArrayList<>();

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getChildrenCount() == 0) {
                    isLoadingMoreChats = false;
                    isLastPage = true;
                    paginationProgressBar.setVisibility(View.GONE);
                    return;
                }
                for (DataSnapshot childDataSnapshot : dataSnapshot.getChildren()) {
                    final ChatData chatData = childDataSnapshot.getValue(ChatData.class);
                    if (chatData != null) {
                        Log.d(TAG, "onChildAdded: " + childDataSnapshot.getKey());
                        chatData.setId(childDataSnapshot.getKey());
                        sendReadReceipt(chatData);
                        newChatDataList.add(chatData);
                        if (newChatDataList.size() == PAGE_SIZE) {
                            endAtTimestamp = newChatDataList.get(0).getServerTimestampInLong();
                            newChatDataList.remove(newChatDataList.size() - 1);
                            Collections.reverse(newChatDataList);
                            for (int i = 0; i < newChatDataList.size(); i++) {
                                final ChatData currChatData = newChatDataList.get(i);
                                chatAdapter.addChatData(0, currChatData);
                                checkAndInsertDate(currChatData, i + 1 >= newChatDataList.size() ? null : newChatDataList.get(i + 1), 0);
                            }
                            newChatDataList.clear();
                            isLoadingMoreChats = false;
                            paginationProgressBar.setVisibility(View.GONE);
                            chatRecyclerView.scrollBy(0, -dpToPx(40));
                        } else if (endAtTimestamp == chatData.getServerTimestampInLong() && newChatDataList.size() >= 1) {
                            // last page
                            isLastPage = true;
                            if (newChatDataList.size() == 1) {
                                // for edge case wherein prev page was the last one
                                checkAndInsertDate(newChatDataList.get(0), null, 0);
                            } else {
                                newChatDataList.remove(newChatDataList.size() - 1);
                                Collections.reverse(newChatDataList);
                                for (int i = 0; i < newChatDataList.size(); i++) {
                                    final ChatData currChatData = newChatDataList.get(i);
                                    chatAdapter.addChatData(0, currChatData);
                                    checkAndInsertDate(currChatData, i + 1 >= newChatDataList.size() ? null : newChatDataList.get(i + 1), 0);
                                }
                            }
                            newChatDataList.clear();
                            isLoadingMoreChats = false;
                            paginationProgressBar.setVisibility(View.GONE);
                            chatRecyclerView.scrollBy(0, -dpToPx(40));
                        }
                    } else {
                        Crashlytics.logException(new NullPointerException("chat data is null for chat ID: " + childDataSnapshot.getKey() + " and group ID: " + groupId));
                        isLoadingMoreChats = false;
                        paginationProgressBar.setVisibility(View.GONE);
                        chatRecyclerView.scrollBy(0, -dpToPx(40));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void checkAndInsertDate(ChatData chatData, @Nullable ChatData prevChatData, int posToInsert) {
        ChatData lastMsg = prevChatData;
        if ((lastMsg == null && isLastPage) || (lastMsg != null && lastMsg.getServerTimestampInLong() != null &&
                !DateTimeUtils.getDateFromLong(lastMsg.getServerTimestampInLong()).equalsIgnoreCase(DateTimeUtils.getDateFromLong(chatData.getServerTimestampInLong())))) {
            // different date, insert date divider
            final ChatData dateChatData = new ChatData();
            dateChatData.setMessage(DateTimeUtils.getDateFromLong(chatData.getServerTimestampInLong()));
            dateChatData.setType(SYSTEM);
            final HashMap<String, Long> readMap = new HashMap<>();
            readMap.put(authorId, 0L);
            dateChatData.setReadReceipts(readMap);
            chatAdapter.addChatData(posToInsert, dateChatData);
        }
    }

    private void sendReadReceipt(ChatData chatData) {
        if (chatData.getReadReceipts().get(authorId) == null) {
            if (!TextUtils.isEmpty(groupId)) {
                getMessagesRef().child(groupId).child(chatData.getId())
                        .child("readReceipts")
                        .child(authorId)
                        .setValue(System.currentTimeMillis());
            } else {
                getDmMessagesRef().child(dmId).child(chatData.getId())
                        .child("readReceipts")
                        .child(authorId)
                        .setValue(System.currentTimeMillis());
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_send_btn:

                if (introPromptContainer.getVisibility() == View.VISIBLE) {
                    hideIntroPrompt();
                    Analytics.triggerEvent(AnalyticsEvents.GROUP_QUES_ANSWERED, getContext());
                }

                final ChatData chatData = new ChatData();
                chatData.setAuthorUid(authorId);
                chatData.setAuthorIsSeller(isCurrUserSeller);
                chatData.setMessage(newMessageEt.getText().toString());
                chatData.setCreatedTimestamp(System.currentTimeMillis());
                chatData.setServerTimestamp(ServerValue.TIMESTAMP);
                chatData.setIsDm(TextUtils.isEmpty(groupId));

                if (isValidString(attachedGroupId)) {
                    chatData.setType(GROUP);
                    chatData.setAttachedGroupId(attachedGroupId);
                    chatData.setLinkTitle(linkTitle.getText().toString());
                    chatData.setLinkDesc(linkDesc.getText().toString());
                    chatData.setLinkPicUrl(attachedGroupPicUrl);
                } else if (isValidString(attachedEventId)) {
                    chatData.setType(EVENT);
                    chatData.setAttachedGroupId(attachedEventId);
                    chatData.setLinkTitle(linkTitle.getText().toString());
                    chatData.setLinkDesc(linkDesc.getText().toString());
                    chatData.setLinkPicUrl(attachedEventPicUrl);
                } else if (isValidString(replyMsgId)) {
                    chatData.setType(REPLY);
                    chatData.setReplyMsgId(replyMsgId);
                } else if (isValidString(linkTitle.getText().toString())) {
                    chatData.setType(LINK);
                    chatData.setLinkTitle(linkTitle.getText().toString());
                    chatData.setLinkDesc(linkDesc.getText().toString());
                }

                if (TextUtils.isEmpty(groupId) && TextUtils.isEmpty(dmId)) {
                    // first msg in a new DM, create new DM chat
                    sendBtnProgressBtn.setVisibility(View.VISIBLE);
                    final DatabaseReference pushRef = RealtimeDbHelper.getCreateDmRef().push();

                    final HashMap<String, Object> userMap = new HashMap<>();
                    final HashMap<Object, Object> map2 = new HashMap<>();
                    map2.put("isSeller", true);
                    userMap.put(receiverId, map2);

                    HashMap<String, Object> sellerMap = new HashMap<>();
                    sellerMap.put("isSeller", false);
                    sellerMap.put("otherUser", receiverId);
                    userMap.put(authorId, sellerMap);

                    final HashMap<String, Object> map = new HashMap<>();
                    map.put("members", userMap);
                    map.put("message", chatData);
                    pushRef.setValue(map);
                    dmId = pushRef.getKey();
                    // new DM chat created with dmId. Start listeners.
                    dmInfoReference = getDmsRef().child(dmId);
                    messagesReference = getDmMessagesRef().child(dmId);
                    msgChildListener = msgListener(messagesReference);
                    syncGroupInfo();

                } else if (!TextUtils.isEmpty(groupId)) {
                    messagesReference.push().setValue(chatData);
                    final Bundle bundle = new Bundle();
                    bundle.putString("group_id", groupId);
                    Analytics.triggerEvent(AnalyticsEvents.SEND_GROUP_CHAT, bundle, getContext());
                } else if (!TextUtils.isEmpty(dmId)) {
                    messagesReference.push().setValue(chatData);
                }
                newMessageEt.setText("");
                linkTitle.setText("");
                linkDesc.setText("");
                linkMetaContainer.setVisibility(View.GONE);
                replyMsgId = null;
                attachedGroupId = null;
                attachedEventId = null;
                if (linkMetaAsyncTask != null) {
                    linkMetaAsyncTask.cancel(true);
                }
                break;
            case R.id.iv_attach:
                if (TextUtils.isEmpty(groupId) && TextUtils.isEmpty(dmId)) {
                    Toast.makeText(getContext(), "Please send a text message first", Toast.LENGTH_SHORT).show();
                    break;
                }
                ChatFragmentPermissionsDispatcher
                        .showAttachmentBottomSheetWithPermissionCheck(ChatFragment.this);
                break;
            case R.id.btn_join:
                getCreateOrJoinGroupRef().child(groupId).setValue(true);
                isJoining = true;
                showJoiningDialog();
                break;
            case R.id.iv_decline_cross:
                RealtimeDbHelper.getUserGroupsRef().child(groupId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        getActivity().finish();
                    }
                });
                break;
            case R.id.iv_link_cancel:
                linkTitle.setText("");
                linkDesc.setText("");
                prevUrl = "";
                linkMetaContainer.setVisibility(View.GONE);
                replyMsgId = null;
                attachedGroupId = null;
                attachedEventId = null;
                break;
        }
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public void showAttachmentBottomSheet() {
        AttachmentListDialogFrag.newInstance().show(getChildFragmentManager(), null);
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public void getWritePerm() {
        chatAdapter.writePermGranted();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_IMG && resultCode == RESULT_OK) {
            File imageFile;
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                imageFile = getFileFromInputStreamUri(getContext(), uri);
            } else {
                // from camera
                imageFile = new File(currentPhotoPath);
            }

            final Uri fileUri = Uri.fromFile(imageFile);
            String chatId = groupId;
            if (!TextUtils.isEmpty(dmId)) {
                chatId = dmId;
            }
            AttachImageActivity.open(getContext(), fileUri, chatId, !TextUtils.isEmpty(dmId), isCurrUserSeller, authorId);
        } else if (requestCode == REQUEST_CODE_GROUP_PICK && resultCode == RESULT_OK) {
            String chosenGroupId = data.getStringExtra("group_id");
            if (!TextUtils.isEmpty(chosenGroupId)) {
                attachedGroupId = chosenGroupId;
                fetchAndShowAttachedGroupInfo();
            }
        } else if (requestCode == REQUEST_CODE_EVENT_PICK && resultCode == RESULT_OK) {
            String chosenEventId = data.getStringExtra("event_id");
            if (!TextUtils.isEmpty(chosenEventId)) {
                attachedEventId = chosenEventId;
                fetchAndShowAttachedEventInfo();
            }
        }
    }

    private void fetchAndShowAttachedGroupInfo() {
        if (!TextUtils.isEmpty(attachedGroupId)) {
            RealtimeDbHelper.getLubbleGroupsRef().child(attachedGroupId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot != null) {
                        linkMetaContainer.setVisibility(View.VISIBLE);
                        final GroupData groupData = dataSnapshot.getValue(GroupData.class);
                        linkTitle.setText(groupData.getTitle());
                        linkDesc.setText(groupData.getDescription());
                        GlideApp.with(getContext())
                                .load(groupData.getThumbnail())
                                .circleCrop()
                                .placeholder(R.drawable.ic_circle_group_24dp)
                                .error(R.drawable.ic_circle_group_24dp)
                                .into(linkPicIv);
                        attachedGroupPicUrl = groupData.getThumbnail();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        } else {
            linkMetaContainer.setVisibility(View.GONE);
        }
    }

    private void fetchAndShowAttachedEventInfo() {
        if (!TextUtils.isEmpty(attachedEventId)) {
            RealtimeDbHelper.getEventsRef().child(attachedEventId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.getValue() != null) {
                        linkMetaContainer.setVisibility(View.VISIBLE);
                        final EventData eventData = dataSnapshot.getValue(EventData.class);
                        linkTitle.setText(eventData.getTitle());
                        linkDesc.setText(DateTimeUtils.getTimeFromLong(eventData.getStartTimestamp(), DateTimeUtils.APP_DATE_NO_YEAR) + ": " + eventData.getDesc());
                        GlideApp.with(getContext())
                                .load(eventData.getProfilePic())
                                .circleCrop()
                                .placeholder(R.drawable.ic_event)
                                .error(R.drawable.ic_event)
                                .into(linkPicIv);
                        attachedEventPicUrl = eventData.getProfilePic();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        } else {
            linkMetaContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public void onAttachmentClicked(int position) {
        switch (position) {
            case 0:
                startCameraIntent();
                break;
            case 1:
                NewPollActiv.open(getContext(), groupId);
                break;
            case 2:
                startGalleryPicker();
                break;
            case 3:
                startActivityForResult(GroupPickerActiv.getIntent(getContext()), REQUEST_CODE_GROUP_PICK);
                break;
            case 4:
                startActivityForResult(EventPickerActiv.getIntent(getContext()), REQUEST_CODE_EVENT_PICK);
                break;
        }
    }

    private void startCameraIntent() {
        try {
            File cameraPic = createImageFile(getContext());
            currentPhotoPath = cameraPic.getAbsolutePath();
            Intent pickImageIntent = getTakePhotoIntent(getContext(), cameraPic);
            startActivityForResult(pickImageIntent, REQUEST_CODE_IMG);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startGalleryPicker() {
        try {
            File cameraPic = createImageFile(getContext());
            currentPhotoPath = cameraPic.getAbsolutePath();
            Intent pickImageIntent = getGalleryIntent(getContext());
            startActivityForResult(pickImageIntent, REQUEST_CODE_IMG);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupTogglingOfSendBtn() {
        newMessageEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                sendBtn.setEnabled(editable.length() > 0 && editable.toString().trim().length() > 0);
                final String extractedUrl = extractFirstLink(editable.toString());
                if (extractedUrl != null && !prevUrl.equalsIgnoreCase(extractedUrl) && extractYoutubeId(extractedUrl) == null) {
                    // ignore youtube URLs
                    prevUrl = extractedUrl;
                    linkMetaAsyncTask = new LinkMetaAsyncTask(prevUrl, getLinkMetaListener());
                    linkMetaAsyncTask.execute();
                } else if (extractedUrl == null && linkMetaContainer.getVisibility() == View.VISIBLE && !isValidString(replyMsgId) && !isValidString(attachedGroupId) && !isValidString(attachedEventId)) {
                    linkMetaContainer.setVisibility(View.GONE);
                    prevUrl = "";
                    linkTitle.setText("");
                    linkDesc.setText("");
                }
            }
        });
    }

    @NonNull
    private LinkMetaListener getLinkMetaListener() {
        return new LinkMetaListener() {
            @Override
            public void onMetaFetched(final String title, final String desc) {
                if (isAdded() && isVisible() && getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            linkMetaContainer.setVisibility(View.VISIBLE);
                            linkTitle.setText(title);
                            linkDesc.setText(desc);
                            linkPicIv.setImageResource(R.drawable.ic_public_black_24dp);
                        }
                    });
                }
            }

            @Override
            public void onMetaFailed() {
                if (isAdded() && isVisible() && getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            linkMetaContainer.setVisibility(View.GONE);
                        }
                    });
                }
            }
        };
    }

    public void addReplyFor(@NonNull String selectedChatId) {
        linkMetaContainer.setVisibility(View.VISIBLE);
        replyMsgId = selectedChatId;

        messagesReference.child(selectedChatId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final ChatData quotedChatData = dataSnapshot.getValue(ChatData.class);
                RealtimeDbHelper.getUserInfoRef(quotedChatData.getAuthorUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        ProfileInfo profileInfo = dataSnapshot.getValue(ProfileInfo.class);
                        linkPicIv.setImageResource(R.drawable.ic_reply_black_24dp);
                        linkTitle.setText(profileInfo.getName());
                        String desc = "";
                        if (isValidString(quotedChatData.getImgUrl())) {
                            desc = desc.concat("\uD83D\uDCF7 ");
                            if (!isValidString(quotedChatData.getMessage())) {
                                // add the word photo if there is no caption
                                desc = desc.concat("Photo ");
                            }
                        }
                        desc = desc.concat(quotedChatData.getMessage());
                        linkDesc.setText(desc);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void openGroupInfo() {
        if (groupData != null && (groupData.isJoined() || !groupData.getIsPrivate())) {
            ScrollingGroupInfoActivity.open(getContext(), groupId);
        }
    }

    public void openChatInfo(String chatId, boolean showReadReceipts) {
        if (chatId != null) {
            if (!TextUtils.isEmpty(groupId)) {
                startActivity(MsgInfoActivity.getIntent(getContext(), groupId, chatId, showReadReceipts, false, authorId));
            } else {
                startActivity(MsgInfoActivity.getIntent(getContext(), dmId, chatId, showReadReceipts, true, authorId));
            }
        } else {
            Crashlytics.logException(new NullPointerException("chatId is null when trying to open msg info"));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        recyclerViewState = chatRecyclerView.getLayoutManager().onSaveInstanceState();
        prevUrl = "";
        if (groupReference != null && groupInfoListener != null) {
            groupReference.removeEventListener(groupInfoListener);
        }
        if (dmInfoReference != null && dmEventListener != null) {
            dmInfoReference.removeEventListener(dmEventListener);
        }
        if (bottomBarListener != null) {
            RealtimeDbHelper.getUserGroupsRef().child(groupId).removeEventListener(bottomBarListener);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // NOTE: delegate the permission handling to generated method
        ChatFragmentPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @OnShowRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showRationaleForExtStorage(final PermissionRequest request) {
        showStoragePermRationale(getContext(), request);
    }

    @OnPermissionDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showDeniedForExtStorage() {
        Toast.makeText(getContext(), getString(R.string.write_storage_perm_denied_text), Toast.LENGTH_SHORT).show();
    }

    @OnNeverAskAgain(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showNeverAskForExtStorage() {
        Toast.makeText(getContext(), R.string.write_storage_perm_never_text, Toast.LENGTH_LONG).show();
    }
}
