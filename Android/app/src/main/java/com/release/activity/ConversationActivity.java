package com.release.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.applozic.mobicomkit.Applozic;
import com.applozic.mobicomkit.api.conversation.ApplozicConversation;
import com.applozic.mobicomkit.api.conversation.Message;
import com.applozic.mobicomkit.api.conversation.MessageBuilder;
import com.applozic.mobicomkit.api.conversation.MobiComConversationService;
import com.applozic.mobicomkit.channel.database.ChannelDatabaseService;
import com.applozic.mobicomkit.channel.service.ChannelService;
import com.applozic.mobicomkit.contact.AppContactService;
import com.applozic.mobicomkit.contact.database.ContactDatabase;
import com.applozic.mobicomkit.exception.ApplozicException;
import com.applozic.mobicomkit.listners.ApplozicUIListener;
import com.applozic.mobicomkit.listners.MediaUploadProgressHandler;
import com.applozic.mobicomkit.listners.MessageListHandler;
import com.applozic.mobicommons.commons.core.utils.DateUtils;
import com.applozic.mobicommons.json.GsonUtils;
import com.applozic.mobicommons.people.channel.Channel;
import com.applozic.mobicommons.people.contact.Contact;
import com.release.R;
//import com.release.adapters.ClickedItem;
import com.release.adapters.ConversationAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * This is an activity to show detailed conversation for any user or group.
 */
public class ConversationActivity extends AppCompatActivity implements ApplozicUIListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ConversationAdapter conversationAdapter;
    private EditText sendMessageContent;
    private ImageButton sendTextButton;
    private List<Message> messageList;
    private String type;
    private LinearLayout layout;
    private ImageButton sendAttachmentButton;
    private static final int REQUEST_CODE = 7;
    private static final int PICK_FILE = 4;

    private Toolbar toolbar;
    private TextView toolbarTitle;
    private TextView toolbarStatus;

    private static final String TAG = "CONVERSATION";

    private boolean isGroup = false;
    private Channel mChannel;
    private Contact mContact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);
        toolbar = findViewById(R.id.conversation_activity_toolbar);
        toolbarTitle = findViewById(R.id.conversation_activity_toolbar_title);
        toolbarStatus = findViewById(R.id.conversation_activity_toolbar_status);
        toolbarStatus.setVisibility(View.GONE);
        setSupportActionBar(toolbar);

        processIntent();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
//        type = getIntent().getStringExtra("TYPE");

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        layout = findViewById(R.id.footer_snackbar);
        recyclerView = findViewById(R.id.recyclerview_message_list);
        recyclerView.setHasFixedSize(true);

        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setStackFromEnd(true);
        //	manager.setReverseLayout(true);
        recyclerView.setLayoutManager(manager);

        recyclerView.setItemAnimator(new DefaultItemAnimator());
//        Log.v("Haha",String.valueOf(ClickedItem.getMessage().size()));
//        Log.v("asdsa",ClickedItem.getMessage().get(0).getMessage());
//        messageList = ClickedItem.getMessage();
//        conversationAdapter = new ConversationAdapter(this, messageList);
//        recyclerView.setAdapter(conversationAdapter);
//        conversationAdapter.notifyDataSetChanged();

        sendMessageContent = findViewById(R.id.send_message_content);
        sendTextButton = findViewById(R.id.message_send_button);
        sendAttachmentButton = findViewById(R.id.attachment_send_button);

        sendTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TextUtils.isEmpty(sendMessageContent.getText().toString().trim())) {
                    Toast.makeText(ConversationActivity.this, "Empty Text", Toast.LENGTH_SHORT).show();
                } else {
                    //
                    if (type.equalsIgnoreCase("Contact")) {
                        new MessageBuilder(ConversationActivity.this).setMessage(sendMessageContent.getText().toString().trim()).setTo(messageList.get(0).getContactIds()).send();
                    } else {
                        new MessageBuilder(ConversationActivity.this).setMessage(sendMessageContent.getText().toString().trim()).setGroupId(messageList.get(0).getGroupId()).send();
                    }
                }
            }
        });


        /**
         * Send attachment Open gallary to select an image
         */
        sendAttachmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendAttachment();
            }
        });

        /**
         * Pagination.
         */
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (type.equalsIgnoreCase("Contact")) {
                    Message message = messageList.get(0);
                    loadNextContactList(message);
                } else {
                    Message message = messageList.get(0);
                    loadNextChannelList(message);
                }
            }
        });

        /**
         * Ask for permission
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            showRunTimePermission();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Log.d("Coming Here", "Check Yes");
        processIntent();
    }

    private void processIntent() {
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            for (String key : bundle.keySet()) {
                Object value = bundle.get(key);
                Log.d(TAG + " CHECKING", String.format("%s %s (%s)", key,
                        value.toString(), value.getClass().getName()));
            }

            if (intent.hasExtra("CHECK_INTENT")) {
                //from activity
                type = intent.getStringExtra("TYPE");
                if (type.equals("CHANNEL")) {
                    int channelId = intent.getIntExtra("ID", 12345);
                    isGroup = true;
                    getMessageListForChannel(channelId);
                } else {
                    String contactId = intent.getStringExtra("ID");
                    getMessageListForContact(contactId);
                }
            } else {
                //from notification
                Message message = (Message) GsonUtils.getObjectFromJson(intent.getStringExtra("message_json"), Message.class);
//
                if (message.isGroupMessage()) {
                    getMessageListForChannel(message.getGroupId());
                } else {
                    getMessageListForContact(message.getContactIds());
                }
            }
        }
    }

    public void setMessageList(List<Message> messages) {
        messageList = messages;
        conversationAdapter = new ConversationAdapter(this, messageList);
        recyclerView.setAdapter(conversationAdapter);
        conversationAdapter.notifyDataSetChanged();
    }

    public void getMessageListForContact(String contactId) {
        final Contact contact = new AppContactService(ConversationActivity.this).getContactById(contactId);
        mContact = contact;
        toolbarTitle.setText(contact.getDisplayName());
        toolbarStatus.setVisibility(View.VISIBLE);
        toolbarStatus.setText(mContact.isOnline()?"ONLINE":"Last seen: "+DateUtils.getDateAndTimeForLastSeen(mContact.getLastSeenAt()));
        ApplozicConversation.getMessageListForContact(ConversationActivity.this, (new ContactDatabase(ConversationActivity.this)).getContactById(contactId), null, new MessageListHandler() {
            @Override
            public void onResult(List<Message> messageList, ApplozicException e) {
                if (e != null) {
                }
                MobiComConversationService mobiComConversationService = new MobiComConversationService(ConversationActivity.this);
                mobiComConversationService.read(contact, null);
                setMessageList(messageList);
            }
        });
    }

    public void getMessageListForChannel(int channelId) {
        final Channel channel = ChannelService.getInstance(ConversationActivity.this).getChannelInfo(channelId);
        mChannel = channel;
        toolbarTitle.setText(channel.getName());
        ApplozicConversation.getMessageListForChannel(ConversationActivity.this, ChannelDatabaseService.getInstance(ConversationActivity.this).getChannelByChannelKey(channelId), null, new MessageListHandler() {
            @Override
            public void onResult(List<Message> messageList, ApplozicException e) {
                if (e != null) {
                }
                MobiComConversationService mobiComConversationService = new MobiComConversationService(ConversationActivity.this);
                mobiComConversationService.read(null, channel);
                setMessageList(messageList);
            }
        });
    }

    public void sendAttachment() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);

        //for other gallery apps
        Intent otherIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        otherIntent.setType("image/* video/* audio/*");

        Intent chooserIntent = Intent.createChooser(intent, "Select Picture");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{otherIntent});

        startActivityForResult(chooserIntent, PICK_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_FILE && resultCode == RESULT_OK && data != null) {
            Log.d("Checking Attachment ", "Is being called");

            Uri selectedUri = data.getData();
            String[] columns = {MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.MIME_TYPE};

            Cursor cursor1 = getContentResolver().query(selectedUri, columns, null, null, null);
            cursor1.moveToFirst();

            int pathColumnIndex = cursor1.getColumnIndex(columns[0]);
            int mimeTypeColumnIndex = cursor1.getColumnIndex(columns[1]);

            String contentPath = cursor1.getString(pathColumnIndex);
            String mimeType = cursor1.getString(mimeTypeColumnIndex);
            cursor1.close();

            String filePath = "";
            Uri selectedFile = data.getData();

            if (mimeType.startsWith("image")) {
                Log.d("Checking Attachment ", "Is IMAGE");
                String filePathColumn[] = {MediaStore.Images.Media.DATA};
                filePath = getFilePathFromChoosenFile(selectedFile, filePathColumn);
            } else if (mimeType.startsWith("video")) {
                Log.d("Checking Attachment ", "Is VIDEO");
                String filePathColumn[] = {MediaStore.Video.Media.DATA};
                filePath = getFilePathFromChoosenFile(selectedFile, filePathColumn);
            } else if (mimeType.startsWith("audio")) {
                Log.d("Checking Attachment ", "Is AUDIO");
                String filePathColumn[] = {MediaStore.Audio.Media.DATA};
                filePath = getFilePathFromChoosenFile(selectedFile, filePathColumn);
            }

            Log.d("Attachment ", filePath);
            if (type.equalsIgnoreCase("Contact")) {
                //This is a contact
                sendAttachmentToContact(filePath);
            } else {
                //This is a group
                sendAttachmentToGroup(filePath);
            }
        }
    }

    private String getFilePathFromChoosenFile(Uri selectedFile, String filePathColumn[]) {
        Cursor cursor = getContentResolver().query(selectedFile, filePathColumn, null, null, null);
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String filePath = cursor.getString(columnIndex);
        cursor.close();
        return filePath;
    }

    private void sendAttachmentToContact(String filePath) {
        new MessageBuilder(ConversationActivity.this)
                .setContentType(Message.ContentType.ATTACHMENT.getValue())
                .setTo(messageList.get(0).getContactIds())
                .setFilePath(filePath)
                .send(new MediaUploadProgressHandler() {
                    @Override
                    public void onUploadStarted(ApplozicException e) {

                    }

                    @Override
                    public void onProgressUpdate(int percentage, ApplozicException e) {

                    }

                    @Override
                    public void onCancelled(ApplozicException e) {

                    }

                    @Override
                    public void onCompleted(ApplozicException e) {

                    }

                    @Override
                    public void onSent(Message message) {

                    }
                });
    }

    private void sendAttachmentToGroup(String filePath) {
        new MessageBuilder(ConversationActivity.this)
                .setContentType(Message.ContentType.ATTACHMENT.getValue())
                .setGroupId(messageList.get(0).getGroupId())
                .setFilePath(filePath)
                .send();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent parentIntent = NavUtils.getParentActivityIntent(this);
                parentIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(parentIntent);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method checks permission for writing to external storage at runtime if device is using android version above Marshmellow.
     * This checking is done at runtime, if permission is already given nothing happens otherwise we ask for permission.
     */
    public void showRunTimePermission() {
        Log.d("Permission", "Checinkg");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestStoragePermission();
        }
    }

    /**
     * This method asks permission for writing to external storage at runtime.
     */
    public void requestStoragePermission() {
        final String permission[] = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Snackbar.make(layout, "External Storage Permission", Snackbar.LENGTH_INDEFINITE).
                    setAction("YES", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(ConversationActivity.this, permission, REQUEST_CODE);
                        }
                    }).show();
        } else {
            ActivityCompat.requestPermissions(this, permission, REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE) {
            Snackbar.make(layout, "permission granted", Snackbar.LENGTH_SHORT).show();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * This method implements Pagination, load next 60 messages for a particular contact.
     * It calls onItemsLoadComplete after fetching messages.
     *
     * @param message Contains information about client id.
     */
    public void loadNextContactList(final Message message) {
        ApplozicConversation.getMessageListForContact(this, (new ContactDatabase(this)).getContactById(message.getContactIds()), messageList.get(0).getCreatedAtTime(), new MessageListHandler() {
            @Override
            public void onResult(List<Message> listMessage, ApplozicException e) {
                if (e == null) {
                    messageList.addAll(0, listMessage);
                    conversationAdapter.notifyItemRangeInserted(0, listMessage.size());
                    conversationAdapter.notifyItemChanged(listMessage.size());
//                    conversationAdapter.notifyDataSetChanged();
                }
            }
        });
        swipeRefreshLayout.setRefreshing(false);
    }

    /**
     * This method implements Pagination, load next 60 messages for channel.
     * It calls onItemsLoadComplete after fetching messages.
     *
     * @param message It contains information about the group.
     */
    public void loadNextChannelList(Message message) {
        ApplozicConversation.getMessageListForChannel(this, ChannelDatabaseService.getInstance(this).getChannelByChannelKey(message.getGroupId()), messageList.get(0).getCreatedAtTime(), new MessageListHandler() {
            @Override
            public void onResult(List<Message> listMessage, ApplozicException e) {
                if (e == null) {
                    messageList.addAll(0, listMessage);
                    conversationAdapter.notifyItemRangeInserted(0, listMessage.size());
                    conversationAdapter.notifyItemChanged(listMessage.size());
                }
            }
        });
        swipeRefreshLayout.setRefreshing(false);
    }

    public boolean isMessageForAdapter(Message message) {
        if (message.isGroupMessage()) {
            if (messageList.get(0).isGroupMessage()) {
                if (message.getGroupId().equals(messageList.get(0).getGroupId())) {
                    return true;

                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            if (messageList.get(0).isGroupMessage()) {
                return false;
            } else {
                if (message.getTo().equals(messageList.get(0).getTo())) {
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    /**
     * This method is used to add a new message to adapter when it is sent from device.
     *
     * @param message This is the message sent by user
     */
    public void updateAdapterOnSent(Message message) {
        if (isMessageForAdapter(message)) {
            messageList.add(message);
            conversationAdapter.notifyDataSetChanged();
            sendMessageContent.getText().clear();
            recyclerView.scrollToPosition(messageList.size() - 1);
        }
    }

    public void updateAdapterOnDelivered(Message message) {
        //check message in message list
        if(isMessageForAdapter(message)){
            int index = messageList.indexOf(message);
            if (index != -1) {
                messageList.set(index, message);
                conversationAdapter.notifyDataSetChanged();
            }
        }
       /* if(isMessageForAdapter(message)) {
            for (int i = messageList.size() - 1; i >= 0; i--) {
                if (message.equals(messageList.get(i))) {
                    Log.d("Yes Found Message ", message.getMessage());
                    Log.d("yes Foud Status", message.isSentToServer()?"Yes":"NO");
                    messageList.set(i, message);
                    break;
                }else{
                    Log.d("Message ", message.getMessage() + "\n Already Present "+ messageList.get(i).getMessage());
                }
            }
            conversationAdapter.notifyDataSetChanged();
        }*/
    }

    public void updateSeenStatus(String userId) {
        Log.d("Check", "STarts HERE");
        if (userId.equals(messageList.get(0).getTo())) {
//            Log.d()
            for (int i = messageList.size() - 1; i >= 0; i--) {
                if (messageList.get(i).getStatus() != Message.Status.DELIVERED_AND_READ.getValue()) {
                    messageList.get(i).setStatus(Message.Status.DELIVERED_AND_READ.getValue());
                } else {
                    Log.d("Check ", "Message " + messageList.get(i).getMessage() + " Status " + messageList.get(i).getStatus());
                    break;
                }
            }
            conversationAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Applozic.connectPublish(this);
        if(isGroup){
            Applozic.subscribeToTyping(ConversationActivity.this,mChannel, null);
        }else{
            Applozic.subscribeToTyping(ConversationActivity.this, null, mContact);
        }

        Applozic.getInstance(this).registerUIListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(isGroup){
            Applozic.unSubscribeToTyping(ConversationActivity.this,mChannel, null);
        }else{
            Applozic.unSubscribeToTyping(ConversationActivity.this, null, mContact);
        }

        Applozic.disconnectPublish(this);
        Applozic.getInstance(this).unregisterUIListener();
    }

    @Override
    public void onMessageSent(Message message) {
//        updateAdapter(message);
        Log.d("Checking Sent", ".................................Sending...................................");
        Log.d("Checking Sent", "Message " + message);
        Log.d("Checking Sent", "Status " + message.getStatus());
        message.setSentToServer(true);
        updateAdapterOnDelivered(message);
    }

    @Override
    public void onMessageReceived(Message message) {
        Log.d("Checking Reception", "..................................Receiving...............................");
        Log.d("Checking Reception", "Message " + message.getMessage());
        Log.d("Checking Reception", "Status " + message.getStatus());
        updateAdapterOnSent(message);
    }


    @Override
    public void onLoadMore(boolean loadMore) {
        Log.d("Checking ", "..................................." + "LOAD MORE CALLED" + "...................................");
    }

    @Override
    public void onMessageSync(Message message, String key) {
        Log.d("Checking Syncing", "............................Syncing.........................................");
        Log.d("Checking Syncing", "Message " + message.toString());
        Log.d("Checking Syncing", "Status " + message.getStatus());
        if (message.isTypeOutbox()) {
            updateAdapterOnSent(message);
        }
//        updateAdapterOnSent(message);
    }

    @Override
    public void onMessageDeleted(String messageKey, String userId) {
        Log.d("Checking ", "..................................." + "DELETED CALLED" + "...................................");
    }

    @Override
    public void onMessageDelivered(Message message, String userId) {
        Log.d("Checking Delivery", ".................Delivered...........................");
        Log.d("Checking Delivery", "Delivered " + message.getMessage());
        Log.d("Checking Delivery", "Status " + message.getStatus());
        updateAdapterOnDelivered(message);
    }

    @Override
    public void onAllMessagesDelivered(String userId) {
        Log.d("Checking ", "..................................." + "ALL DELIVERED CALLED" + "...................................");
    }

    @Override
    public void onAllMessagesRead(String userId) {
        //message read
        updateSeenStatus(userId);
        Log.d("Checking ", "..................................." + "READ CALLED" + "...................................");
    }

    @Override
    public void onConversationDeleted(String userId, Integer channelKey, String response) {
        Log.d("Checking ", "..................................." + "CONVO DEL CALLED" + "...................................");
    }
    @Override
    public void onUpdateTypingStatus(String userId, String isTyping) {
        Log.d("Checking","..................................." + "TYPING STATUS" + "...................................");
        Log.d("Checking", "Typing Status "+isTyping);
        if(isGroup){
            Log.d("Checking", "Typing yess ");
            if(ChannelService.getInstance(ConversationActivity.this).isUserAlreadyPresentInChannel(mChannel.getKey(),userId)){
                Log.d("Checking", "Typing yesssssss "+isTyping);
                if (isTyping.equals("1")) {
                    toolbarStatus.setVisibility(View.VISIBLE);
                    toolbarStatus.setText(userId+" TYPING");
                } else {
                    toolbarStatus.setText(null);
                    toolbarStatus.setVisibility(View.GONE);
                }
            }
        }else {
            if (userId.equals(mContact.getContactIds())) {
                if (isTyping.equals("1")) {
                    toolbarStatus.setVisibility(View.VISIBLE);
                    toolbarStatus.setText("TYPING.....");
                } else {
                    toolbarStatus.setText(mContact.isOnline()?"ONLINE":"Last seen: "+DateUtils.getDateAndTimeForLastSeen(mContact.getLastSeenAt()));
                }
            }
        }
//        ChannelService.getInstance(this).isUserAlreadyPresentInChannel(mChannel.getKey(),userId);
    }

    @Override
    public void onUpdateLastSeen(String userId) {
        Log.d("Checking","..................................." + "LAST SEEN STATUS" + "...................................");
        if(!isGroup){
            if(userId.equals(mContact.getUserId())){
                if(mContact.isOnline()){
                    toolbarStatus.setVisibility(View.VISIBLE);
                    toolbarStatus.setText("ONLINE");
                }else{
                    toolbarStatus.setVisibility(View.VISIBLE);
                    toolbarStatus.setText("Last seen: " + DateUtils.getDateAndTimeForLastSeen(mContact.getLastSeenAt()));
                }
            }
        }
    }

    @Override
    public void onMqttDisconnected() {
        Log.d("Checking","..................................." + "MQQQT DISCONNECTED" + "...................................");
        Applozic.connectPublish(ConversationActivity.this);
    }

    @Override
    public void onChannelUpdated() {
        Log.d("Checking ", "..................................." + "CAHNNEL UPDATED CALLED" + "...................................");
    }

    @Override
    public void onConversationRead(String userId, boolean isGroup) {
        Log.d("Checking ", "..................................." + "CONVO READ CALLED" + "...................................");
    }

    @Override
    public void onUserDetailUpdated(String userId) {
        Log.d("Checking ", "..................................." + "USER DETAIL CALLED" + "...................................");
    }

    @Override
    public void onMessageMetadataUpdated(String keyString) {
        Log.d("Checking ", "..................................." + "META DATA CALLED" + "...................................");
    }

//    public void scrollToBottom(){
// 		recyclerView.scrollVerticallyTo(0);
//    }

}
