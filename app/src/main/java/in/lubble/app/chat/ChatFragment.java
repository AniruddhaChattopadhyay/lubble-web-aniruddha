package in.lubble.app.chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import in.lubble.app.R;
import in.lubble.app.UploadFileService;
import in.lubble.app.models.ChatData;
import in.lubble.app.models.GroupData;

import static android.app.Activity.RESULT_OK;
import static in.lubble.app.UploadFileService.EXTRA_FILE_URI;
import static in.lubble.app.firebase.RealtimeDbHelper.getLubbleGroupsRef;
import static in.lubble.app.firebase.RealtimeDbHelper.getMessagesRef;
import static in.lubble.app.utils.FileUtils.createImageFile;
import static in.lubble.app.utils.FileUtils.getFileFromInputStreamUri;
import static in.lubble.app.utils.FileUtils.getPickImageIntent;

public class ChatFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "ChatFragment";
    private static final int REQUEST_CODE_IMG = 789;
    private static final String KEY_GROUP_ID = "CHAT_GROUP_ID";

    private RecyclerView chatRecyclerView;
    private EditText newMessageEt;
    private Button sendBtn;
    private Button attachMediaBtn;
    private DatabaseReference groupReference;
    private DatabaseReference messagesReference;
    private String currentPhotoPath;
    private String groupId;

    public ChatFragment() {
        // Required empty public constructor
    }

    public static ChatFragment newInstance(String groupId) {

        Bundle args = new Bundle();
        args.putString(KEY_GROUP_ID, groupId);
        ChatFragment fragment = new ChatFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        groupId = getArguments().getString(KEY_GROUP_ID);

        groupReference = getLubbleGroupsRef().child(groupId);
        messagesReference = getMessagesRef().child(groupId);

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        registerMediaUploadCallback();

        syncGroupInfo();
    }

    private void syncGroupInfo() {
        groupReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final GroupData groupData = dataSnapshot.getValue(GroupData.class);
                if (groupData != null) {
                    ((ChatActivity) getActivity()).setGroupMeta(groupData.getTitle(), groupData.getThumbnail());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_chat, container, false);

        chatRecyclerView = view.findViewById(R.id.rv_chat);
        newMessageEt = view.findViewById(R.id.et_new_message);
        sendBtn = view.findViewById(R.id.btn_send_message);
        attachMediaBtn = view.findViewById(R.id.btn_attach_media);

        chatRecyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (bottom < oldBottom) {
                    chatRecyclerView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            final int pos = Math.max(chatRecyclerView.getAdapter().getItemCount() - 1, 0);
                            chatRecyclerView.smoothScrollToPosition(pos);
                        }
                    }, 100);
                }
            }
        });

        setupTogglingOfSendBtn();
        sendBtn.setOnClickListener(this);
        attachMediaBtn.setOnClickListener(this);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        final ChatAdapter chatAdapter = new ChatAdapter(getActivity(), getContext(), new ArrayList<ChatData>());
        chatRecyclerView.setAdapter(chatAdapter);

        messagesReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Log.d(TAG, "onChildAdded: ");
                final ChatData chatData = dataSnapshot.getValue(ChatData.class);
                if (chatData != null) {
                    chatData.setId(dataSnapshot.getKey());
                    chatAdapter.addChatData(chatData);
                }
                chatRecyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Log.d(TAG, "onChildChanged: ");
                final ChatData chatData = dataSnapshot.getValue(ChatData.class);
                if (chatData != null) {
                    chatData.setId(dataSnapshot.getKey());
                    chatAdapter.updateChatData(chatData);
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        return view;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_send_message:

                final ChatData chatData = new ChatData();
                chatData.setAuthorUid(FirebaseAuth.getInstance().getCurrentUser().getUid());
                chatData.setMessage(newMessageEt.getText().toString());

                messagesReference.push().setValue(chatData);

                newMessageEt.setText("");
                break;
            case R.id.btn_attach_media:
                startPhotoPicker(REQUEST_CODE_IMG);
                break;
        }
    }

    private void startPhotoPicker(int REQUEST_CODE) {
        try {
            File cameraPic = createImageFile(getContext());
            currentPhotoPath = cameraPic.getAbsolutePath();
            Intent pickImageIntent = getPickImageIntent(getContext(), cameraPic);
            startActivityForResult(pickImageIntent, REQUEST_CODE);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            getContext().startService(new Intent(getContext(), UploadFileService.class)
                    .putExtra(UploadFileService.EXTRA_BUCKET, UploadFileService.BUCKET_CONVO)
                    .putExtra(UploadFileService.EXTRA_FILE_NAME, fileUri.getLastPathSegment())
                    .putExtra(EXTRA_FILE_URI, fileUri)
                    .putExtra(UploadFileService.EXTRA_UPLOAD_PATH, "lubbles/0/groups/0")
                    .setAction(UploadFileService.ACTION_UPLOAD));

        } else {
            Toast.makeText(getContext(), "Failed to get photo", Toast.LENGTH_SHORT).show();
        }
    }

    private void registerMediaUploadCallback() {
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //todo hideProgressDialog();

                switch (intent.getAction()) {
                    case UploadFileService.UPLOAD_COMPLETED:
                        //todo show pic to user

                        break;
                    case UploadFileService.UPLOAD_ERROR:

                        break;
                }
            }
        }, UploadFileService.getIntentFilter());
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
                sendBtn.setEnabled(editable.length() > 0);
            }
        });
    }
}
