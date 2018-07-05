package com.release.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.applozic.mobicomkit.api.attachment.FileClientService;
import com.applozic.mobicomkit.api.attachment.FileMeta;
import com.applozic.mobicomkit.api.conversation.ApplozicConversation;
import com.applozic.mobicomkit.api.conversation.Message;
import com.applozic.mobicomkit.api.conversation.database.MessageDatabaseService;
import com.applozic.mobicomkit.channel.service.ChannelService;
import com.applozic.mobicomkit.contact.AppContactService;
import com.applozic.mobicomkit.exception.ApplozicException;
import com.applozic.mobicomkit.listners.MediaDownloadProgressHandler;
import com.applozic.mobicommons.commons.image.ImageLoader;
import com.applozic.mobicommons.commons.image.ImageUtils;
import com.applozic.mobicommons.people.channel.Channel;
import com.applozic.mobicommons.people.contact.Contact;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.release.BuildConfig;
import com.release.R;
import com.release.Utility.PhotoFullPopupWindow;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by shivam on 9/12/17.
 */

public class ConversationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context mContext;
    private List<Message> messageList;
    private ImageLoader imageLoader;
    private FileClientService fileClientService;
    private static final int MESSAGE_TYPE_SENT = 1;
    private static final int MESSAGE_TYPE_RECEIVED = 2;
    private boolean isImageFitToScreen;

    private static final String TAG = "CONVERSATION_ADAPTER";

    public ConversationAdapter(Context context, List<Message> messages) {
        this.mContext = context;
        this.messageList = messages;
        Log.v("ow start", String.valueOf(messages.size()));
        fileClientService = new FileClientService(mContext);
        imageLoader = new ImageLoader(mContext, ImageUtils.getLargestScreenDimension((Activity) mContext)) {
            @Override
            protected Bitmap processBitmap(Object data) {
                return fileClientService.loadThumbnailImage(mContext, (Message) data, getImageLayoutParam(false).width, getImageLayoutParam(false).height);
            }
        };
        imageLoader.setImageFadeIn(false);
        imageLoader.addImageCache(((FragmentActivity) context).getSupportFragmentManager(), 0.1f);
    }

    public ViewGroup.LayoutParams getImageLayoutParam(boolean outBoxType) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        float wt_px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, mContext.getResources().getDisplayMetrics());
        ViewGroup.MarginLayoutParams params;
        if (outBoxType) {
            params = new RelativeLayout.LayoutParams(metrics.widthPixels + (int) wt_px * 2, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins((int) wt_px, 0, (int) wt_px, 0);
        } else {
            params = new LinearLayout.LayoutParams(metrics.widthPixels - (int) wt_px * 2, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 0);

        }
        return params;
    }

    /**
     * This method returns ViewHolder either SentMessageHolder or ReceivedMessageHolder
     * @param parent
     * @param viewType
     * @return SentMessageHolder if message is sent and RecievedMessageHolder if message is received.
     */
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType == MESSAGE_TYPE_SENT) {
            view = LayoutInflater.from(mContext).inflate(R.layout.sent_message_item, parent, false);
            return new SentMessageHolder(view);
        } else if (viewType == MESSAGE_TYPE_RECEIVED) {
            view = LayoutInflater.from(mContext).inflate(R.layout.received_message_item, parent, false);
            return new ReceivedMessageHolder(view);
        }
        return null;
    }

    /**
     * Display information in the recycler view depending on type of message.
     * @param mHolder
     * @param position
     */
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder mHolder, int position) {
        final Message message = messageList.get(position);
        switch (mHolder.getItemViewType()) {

            case MESSAGE_TYPE_SENT:
                Log.d("Checking Status","...........................SENT MESSAGE......................");
                Log.d("Checking MEssage",message.getMessage());
                Log.d("Checking Status",String.valueOf(message.getStatus()));
                Log.d("Checking Status","ends");

                final SentMessageHolder holder = (SentMessageHolder) mHolder;

                Glide.with(mContext).clear(holder.imageViewForAttachment);
                Glide.with(mContext).clear(holder.videoViewForAttachment);
                Glide.with(mContext).clear(holder.locationViewForAttachment);
                holder.videoViewForAttachment.setVisibility(View.GONE);
                holder.imageViewForAttachment.setVisibility(View.GONE);
                holder.locationViewForAttachment.setVisibility(View.GONE);
                holder.audioViewForAttachment.setVisibility(View.GONE);
                holder.attachmentView.setVisibility(View.GONE);
                holder.attachmentProgressText.setVisibility(View.GONE);
                holder.messageBody.setVisibility(View.GONE);

                holder.messageTime.setText(com.applozic.mobicommons.commons.core.utils.DateUtils.getFormattedDateAndTime(message.getCreatedAtTime()));

                if(!message.isSentToServer()){
                    holder.status.setImageResource(R.drawable.pending_status);
                } else if(Message.Status.READ.getValue().equals(message.getStatus())){
//                    Log.d("Checking Inside","read");
                    holder.status.setImageResource(R.drawable.send_status);
                }else if(Message.Status.DELIVERED_AND_READ.getValue().equals(message.getStatus())){
//                    Log.d("Checking Inside","sent");
                    holder.status.setImageResource(R.drawable.delivered_and_read_status);
                }else if(Message.Status.DELIVERED.getValue().equals(message.getStatus())){
                    holder.status.setImageResource(R.drawable.delivered_status);
                }

                if(message.hasAttachment()) {
                    Log.d("Attachment ChckMessage ", message.toString());
                    holder.attachmentView.setVisibility(View.VISIBLE);
                    if (message.getFilePaths() == null) {
                        if (message.getAttachmentType().equals(Message.VIDEO)) {
                            downloadMessage(message,holder.videoViewForAttachment,holder.attachmentProgress, holder.attachmentProgressText);
                        } else if (message.getAttachmentType().equals(Message.AUDIO)) {

                        } else if (message.getAttachmentType().equals(Message.CONTACT)) {

                        } else if (message.getAttachmentType().equals(Message.LOCATION)) {

                        } else if (message.getAttachmentType().equals(Message.OTHER)) {

                        } else {
                            //image
                            downloadMessage(message,holder.imageViewForAttachment,holder.attachmentProgress,holder.attachmentProgressText);
                        }
                    } else {
                        if (message.getAttachmentType().equals(Message.VIDEO)) {
                            holder.videoViewForAttachment.setVisibility(View.VISIBLE);

                            final String videoPath = message.getFilePaths().get(0);
                            Glide.with(mContext)
                                    .load(Uri.fromFile(new File(videoPath)))
                                    .into(holder.videoViewForAttachment);
                            holder.videoViewForAttachment.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    final File file = new File(videoPath);
                                    intent.setDataAndType(FileProvider.getUriForFile(mContext,
                                            BuildConfig.APPLICATION_ID + ".provider",
                                            file), "video/*");
                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    mContext.startActivity(intent);
                                }
                            });
                        } else if (message.getAttachmentType().equals(Message.AUDIO)) {
                            holder.audioViewForAttachment.setVisibility(View.VISIBLE);
                            String audioPath = message.getFilePaths().get(0);
                            handleAudio(holder.forwardButton, holder.pauseButton, holder.playButton, holder.rewindButton,
                                    holder.audioName, holder.startTime, holder.finalTime, holder.seekBar, "AUDIO", audioPath);

                        } else if (message.getAttachmentType().equals(Message.CONTACT)) {

                        } else if (message.getAttachmentType().equals(Message.LOCATION)) {

                        } else if (message.getAttachmentType().equals(Message.OTHER)) {

                        } else {
                            //image
                            holder.imageViewForAttachment.setVisibility(View.VISIBLE);
                            final String imgPath = message.getFilePaths().get(0);
                            Glide.with(mContext).load(imgPath).
                                    thumbnail(0.5f).
                                    into(holder.imageViewForAttachment);
                            holder.imageViewForAttachment.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    new PhotoFullPopupWindow(mContext, R.layout.popup_photo_full, holder.imageViewForAttachment, imgPath, null);
                                }
                            });
                        }
                    }
                }else {
                    holder.messageBody.setVisibility(View.VISIBLE);
                    holder.messageBody.setText(message.getMessage());
                }
                break;

            case MESSAGE_TYPE_RECEIVED:

                Log.d("Checking Message","......................RECEIVED MESSAGE.....................");
                Log.d("Checking Data",message.toString());
                final ReceivedMessageHolder receivedHolder = (ReceivedMessageHolder) mHolder;
                //checking attachments


                Glide.with(mContext).clear(receivedHolder.imageViewForAttachment);
                Glide.with(mContext).clear(receivedHolder.videoViewForAttachment);
                Glide.with(mContext).clear(receivedHolder.locationViewForAttachment);
                receivedHolder.videoViewForAttachment.setVisibility(View.GONE);
                receivedHolder.imageViewForAttachment.setVisibility(View.GONE);
                receivedHolder.locationViewForAttachment.setVisibility(View.GONE);
                receivedHolder.audioViewForAttachment.setVisibility(View.GONE);
                receivedHolder.attachmentView.setVisibility(View.GONE);
                receivedHolder.attachmentProgressText.setVisibility(View.GONE);

                if(message.getGroupId() == null){
                    Contact contact = new AppContactService(mContext).getContactById(message.getContactIds());
                    receivedHolder.profileName.setText(contact.getDisplayName());
                }else{
                    Contact contact = new AppContactService(mContext).getContactById(message.getContactIds());
                    receivedHolder.profileName.setText(contact.getDisplayName());
                }

                receivedHolder.messageTime.setText(com.applozic.mobicommons.commons.core.utils.DateUtils.getFormattedDateAndTime(message.getCreatedAtTime()));
//                receivedHolder.profileName.setText(message.getContactIds());
                /**
                 * Download Location From Received Message.
                 */
                if(Message.ContentType.LOCATION.getValue().equals(message.getContentType())){
                    Log.d("Checking Location", message.toString());
                    String latitude = "0";
                    String longitude = "0";
                    receivedHolder.messageBody.setVisibility(View.GONE);
                    receivedHolder.attachmentView.setVisibility(View.VISIBLE);
                    receivedHolder.locationViewForAttachment.setVisibility(View.VISIBLE);
                    try {
                        JSONObject locationObject = new JSONObject(message.getMessage());
                        latitude = locationObject.getString("lat");
                        longitude = locationObject.getString("lon");
                        Log.d("Latitude and Longitude ", latitude+" asd "+longitude);
                        //do something with this lat/long, you could load a static map.
                        String url = "http://maps.google.com/maps/api/staticmap?center=" + latitude + "," + longitude + "&zoom=15&size=200x200&sensor=false";
                        Glide.with(mContext).load(url)
                                .thumbnail(0.5f)
                                .apply(new RequestOptions()
                                .placeholder(R.drawable.location))
                                .into(receivedHolder.locationViewForAttachment);
                        final String finalLatitude = latitude;
                        final String finalLongitude = longitude;
                        receivedHolder.locationViewForAttachment.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                String strUri = "http://maps.google.com/maps?q=loc:" + finalLatitude + "," + finalLongitude + " (" + "SENT_LOCATION" + ")";
                                Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(strUri));
                                intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
                                mContext.startActivity(intent);
                            }
                        });

                    } catch (JSONException e) {
                        Log.d("Latitude Exception"," Not Working");
                        e.printStackTrace();
                    }
                }

                /**
                 *Check if message has attachment
                 */
                else if (message.hasAttachment()) {
                    receivedHolder.attachmentView.setVisibility(View.VISIBLE);
                    /**
                     * If Attachment is VIDEO
                     */
                    if (message.getAttachmentType().equals(Message.VIDEO)) {
                        //VIDEO

                        String fileName = "";
                        fileName = message.getFileMetas().getName();
                        receivedHolder.messageBody.setText(fileName);
                        receivedHolder.videoViewForAttachment.setVisibility(View.VISIBLE);

                        //Check if attachment is downloaded or not.
                        if(message.isAttachmentDownloaded()){
                            final String videoPath = message.getFilePaths().get(0);
                            Glide.with(mContext)
                                    .load(Uri.fromFile(new File(videoPath)))
                                    .into(receivedHolder.videoViewForAttachment);
                            receivedHolder.videoViewForAttachment.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    final File file = new File(videoPath);
                                    intent.setDataAndType( FileProvider.getUriForFile(mContext,
                                            BuildConfig.APPLICATION_ID + ".provider",
                                            file), "video/*");
                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    mContext.startActivity(intent);
                                }
                            });
                        }else{
                            //donwload video
                            receivedHolder.videoViewForAttachment.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    ApplozicConversation.downloadMessage(mContext, message, new MediaDownloadProgressHandler() {
                                        @Override
                                        public void onDownloadStarted() {
                                            Log.d("ProgTest", "Download started");
                                            Toast.makeText(mContext, "Clicked", Toast.LENGTH_SHORT).show();
                                            receivedHolder.videoViewForAttachment.setVisibility(View.INVISIBLE);
                                            receivedHolder.attachmentProgress.setVisibility(View.VISIBLE);
                                            receivedHolder.attachmentProgress.setMax(100);
                                            receivedHolder.attachmentProgress.setProgress(0);
                                            receivedHolder.attachmentProgress.setSecondaryProgress(100);
                                            receivedHolder.attachmentProgress.setProgressDrawable(mContext.getResources().getDrawable(R.drawable.circular_progress_bar));
                                            receivedHolder.attachmentProgressText.setVisibility(View.VISIBLE);
                                        }

                                        @Override
                                        public void onProgressUpdate(int percentage, ApplozicException e) {
                                            Log.d("ProgTest", "Download Progress : " + percentage);
                                            receivedHolder.attachmentProgress.setProgress(percentage);
                                            receivedHolder.attachmentProgressText.setText(percentage+" %");
                                        }

                                        @Override
                                        public void onCompleted(Message message1, ApplozicException e) {
                                            if (e != null) {
                                            }
                                            Log.d("ProgTest", "Download finished : " + message1);
                                            receivedHolder.attachmentProgress.setVisibility(View.GONE);
                                            receivedHolder.attachmentProgressText.setVisibility(View.GONE);
                                            receivedHolder.videoViewForAttachment.setVisibility(View.VISIBLE);
                                            if (e == null && message != null) {
                                                final String videoPath = new MessageDatabaseService(mContext).getMessage(message1.getKeyString()).getFilePaths().get(0);
                                                Glide.with(mContext)
                                                        .load(Uri.fromFile(new File(videoPath)))
                                                        .into(receivedHolder.videoViewForAttachment);
                                                receivedHolder.videoViewForAttachment.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View view) {
                                                        Intent intent = new Intent(Intent.ACTION_VIEW);
                                                        final File file = new File(videoPath);
                                                        intent.setDataAndType( FileProvider.getUriForFile(mContext,
                                                                BuildConfig.APPLICATION_ID + ".provider",
                                                                file), "video/*");
                                                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                                        mContext.startActivity(intent);
                                                    }
                                                });
                                            }
                                        }
                                    });
                                }
                            });
                        }
                    } else if (message.getAttachmentType().equals(Message.AUDIO)) {
                        //AUDIO

                        final String fileName = message.getFileMetas().getName();
                        receivedHolder.messageBody.setVisibility(View.GONE);
                        if(message.isAttachmentDownloaded()){
                            receivedHolder.audioViewForAttachment.setVisibility(View.VISIBLE);
                            String audioPath = message.getFilePaths().get(0);
                            handleAudio(receivedHolder.forwardButton, receivedHolder.pauseButton, receivedHolder.playButton, receivedHolder.rewindButton,
                                    receivedHolder.audioName, receivedHolder.startTime, receivedHolder.finalTime, receivedHolder.seekBar, fileName, audioPath);
                        }else{
                            receivedHolder.imageViewForAttachment.setVisibility(View.VISIBLE);
                            receivedHolder.imageViewForAttachment.setImageResource(R.drawable.audio);
                            receivedHolder.imageViewForAttachment.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    ApplozicConversation.downloadMessage(mContext, message, new MediaDownloadProgressHandler() {
                                        @Override
                                        public void onDownloadStarted() {
                                            Log.d("ProgTest", "Download started");
                                            Toast.makeText(mContext, "Clicked", Toast.LENGTH_SHORT).show();
                                            receivedHolder.imageViewForAttachment.setVisibility(View.VISIBLE);
                                            receivedHolder.attachmentProgress.setVisibility(View.VISIBLE);
                                            receivedHolder.attachmentProgress.setMax(100);
                                            receivedHolder.attachmentProgress.setProgress(0);
                                            receivedHolder.attachmentProgress.setSecondaryProgress(100);
                                            receivedHolder.attachmentProgress.setProgressDrawable(mContext.getResources().getDrawable(R.drawable.circular_progress_bar));
                                            receivedHolder.attachmentProgressText.setVisibility(View.VISIBLE);
                                        }

                                        @Override
                                        public void onProgressUpdate(int percentage, ApplozicException e) {
                                            Log.d("ProgTest", "Download Progress : " + percentage);
                                            receivedHolder.attachmentProgress.setProgress(percentage);
                                            receivedHolder.attachmentProgressText.setText(percentage+" %");
                                        }

                                        @Override
                                        public void onCompleted(Message message1, ApplozicException e) {
                                            if (e != null) {
                                            }
                                            Log.d("ProgTest", "Download finished : " + message1);
                                            receivedHolder.attachmentProgress.setVisibility(View.GONE);
                                            receivedHolder.attachmentProgressText.setVisibility(View.GONE);
                                            receivedHolder.imageViewForAttachment.setVisibility(View.GONE);
                                            receivedHolder.imageViewForAttachment.setOnClickListener(null);
                                            receivedHolder.audioViewForAttachment.setVisibility(View.VISIBLE);
                                            if (e == null && message != null) {
                                                String audioPath = message.getFilePaths().get(0);
                                                handleAudio(receivedHolder.forwardButton, receivedHolder.pauseButton, receivedHolder.playButton, receivedHolder.rewindButton,
                                                        receivedHolder.audioName, receivedHolder.startTime, receivedHolder.finalTime, receivedHolder.seekBar, fileName, audioPath);
                                            }
                                        }
                                    });
                                }
                            });
                        }
                    } else if (message.getAttachmentType().equals(Message.CONTACT)) {

                    } else if (message.getAttachmentType().equals("others")) {
                        //ToDo Change others when issue is resolved
                        Log.d("Checking ","Here");
                        String fileName = "";
                        fileName = message.getFileMetas().getName();
                        receivedHolder.messageBody.setText(fileName);
                        receivedHolder.imageViewForAttachment.setVisibility(View.VISIBLE);

                        if(message.isAttachmentDownloaded()){
                            Log.d("Checking "," Downloaded");
                            receivedHolder.imageViewForAttachment.setVisibility(View.VISIBLE);
                            receivedHolder.imageViewForAttachment.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    String filePath = message.getFilePaths().get(0);
                                    File file = new File(filePath);
                                    Intent intent = new Intent();
                                    intent.setAction(Intent.ACTION_VIEW);

                                    MimeTypeMap mime = MimeTypeMap.getSingleton();
                                    String ext = file.getName().substring(file.getName().indexOf(".") + 1);
                                    String type = mime.getMimeTypeFromExtension(ext);

                                    Log.d("Checking ","Type "+type);
                                    intent.setDataAndType( FileProvider.getUriForFile(mContext,
                                            BuildConfig.APPLICATION_ID + ".provider",
                                            file), type);
                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    mContext.startActivity(intent);
                                }
                            });

                        }else{
                            Log.d("Checking "," Will Download");
                            receivedHolder.imageViewForAttachment.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    ApplozicConversation.downloadMessage(mContext, message, new MediaDownloadProgressHandler() {
                                        @Override
                                        public void onDownloadStarted() {
                                            Log.d("ProgTest", "Download started");
                                            Toast.makeText(mContext, "Clicked", Toast.LENGTH_SHORT).show();
                                            receivedHolder.imageViewForAttachment.setVisibility(View.VISIBLE);
                                            receivedHolder.attachmentProgress.setVisibility(View.VISIBLE);
                                            receivedHolder.attachmentProgress.setMax(100);
                                            receivedHolder.attachmentProgress.setProgress(0);
                                            receivedHolder.attachmentProgress.setSecondaryProgress(100);
                                            receivedHolder.attachmentProgress.setProgressDrawable(mContext.getResources().getDrawable(R.drawable.circular_progress_bar));
                                            receivedHolder.attachmentProgressText.setVisibility(View.VISIBLE);
                                        }

                                        @Override
                                        public void onProgressUpdate(int percentage, ApplozicException e) {
                                            Log.d("ProgTest", "Download Progress : " + percentage);
                                            receivedHolder.attachmentProgress.setProgress(percentage);
                                            receivedHolder.attachmentProgressText.setText(percentage+ " %");
                                        }

                                        @Override
                                        public void onCompleted(Message message1, ApplozicException e) {
                                            if (e != null) {
                                            }
                                            Log.d("ProgTest", "Download finished : " + message1);
                                            receivedHolder.attachmentProgress.setVisibility(View.GONE);
                                            receivedHolder.attachmentProgressText.setVisibility(View.VISIBLE);
                                            receivedHolder.imageViewForAttachment.setVisibility(View.VISIBLE);
                                            if (e == null && message != null) {
                                                String filePath = new MessageDatabaseService(mContext).getMessage(message1.getKeyString()).getFilePaths().get(0);
                                                File file = new File(filePath);
                                                Intent intent = new Intent();
                                                intent.setAction(Intent.ACTION_VIEW);

                                                MimeTypeMap mime = MimeTypeMap.getSingleton();
                                                String ext = file.getName().substring(file.getName().indexOf(".") + 1);
                                                String type = mime.getMimeTypeFromExtension(ext);
                                                intent.setDataAndType( FileProvider.getUriForFile(mContext,
                                                        BuildConfig.APPLICATION_ID + ".provider",
                                                        file), type);
                                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                                mContext.startActivity(intent);
                                            }
                                        }
                                    });
                                }
                            });
                        }
                    } else {
                        Log.d("Checking "," Nooooo " +message.getAttachmentType());
                        FileMeta fileMeta = message.getFileMetas();
                        receivedHolder.imageViewForAttachment.setVisibility(View.VISIBLE);
                        receivedHolder.attachmentView.setVisibility(View.VISIBLE);
                        if (fileMeta.getContentType().contains("image")) {
                            String fileName = "";
                            if (message.getFileMetas() == null && message.getFilePaths() != null) {
                                fileName = message.getFilePaths().get(0).substring(message.getFilePaths().get(0).lastIndexOf("/") + 1);
                            } else if (message.getFileMetas() != null) {
                                fileName = message.getFileMetas().getName();
                            }
                            receivedHolder.messageBody.setText(fileName);
                            receivedHolder.imageViewForAttachment.setVisibility(View.VISIBLE);

                            /**
                             * Check if attachment is already downloaded. If not download it.
                             */
                            if (message.isAttachmentDownloaded()) {
                                final String imgPath = message.getFilePaths().get(0);
                                Glide.with(mContext).load(imgPath).
                                        thumbnail(0.5f).
                                        into(receivedHolder.imageViewForAttachment);
                                receivedHolder.imageViewForAttachment.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        new PhotoFullPopupWindow(mContext, R.layout.popup_photo_full, receivedHolder.imageViewForAttachment, imgPath, null);
                                    }
                                });

                            } else {
                                receivedHolder.imageViewForAttachment.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        ApplozicConversation.downloadMessage(mContext, message, new MediaDownloadProgressHandler() {
                                            @Override
                                            public void onDownloadStarted() {
                                                Toast.makeText(mContext, "Downloading", Toast.LENGTH_SHORT).show();
                                                receivedHolder.imageViewForAttachment.setVisibility(View.VISIBLE);
                                                receivedHolder.attachmentProgress.setVisibility(View.VISIBLE);
                                                receivedHolder.attachmentProgress.setMax(100);
                                                receivedHolder.attachmentProgress.setProgress(0);
                                                receivedHolder.attachmentProgress.setSecondaryProgress(100);
                                                receivedHolder.attachmentProgress.setProgressDrawable(mContext.getResources().getDrawable(R.drawable.circular_progress_bar));
                                                receivedHolder.attachmentProgressText.setVisibility(View.VISIBLE);
                                            }

                                            @Override
                                            public void onProgressUpdate(int percentage, ApplozicException e) {
                                                receivedHolder.attachmentProgress.setProgress(percentage);
                                                receivedHolder.attachmentProgressText.setText(percentage+" %");
                                            }

                                            @Override
                                            public void onCompleted(Message message1, ApplozicException e) {
                                                if (e != null) {
                                                }
                                                Log.d("ProgTest", "Download finished : " + message1);
                                                receivedHolder.attachmentProgress.setVisibility(View.GONE);
                                                receivedHolder.imageViewForAttachment.setVisibility(View.VISIBLE);
                                                receivedHolder.attachmentProgressText.setVisibility(View.GONE);
                                                if (e == null && message != null) {
                                                    String imgPath = new MessageDatabaseService(mContext).getMessage(message1.getKeyString()).getFilePaths().get(0);
                                                    Glide.with(mContext).load(imgPath).
                                                            thumbnail(0.5f).
                                                            into(receivedHolder.imageViewForAttachment);
                                                }
                                                receivedHolder.imageViewForAttachment.setOnClickListener(null);
                                                final String imgPath = new MessageDatabaseService(mContext).getMessage(message1.getKeyString()).getFilePaths().get(0);
                                                receivedHolder.imageViewForAttachment.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View view) {
                                                        new PhotoFullPopupWindow(mContext, R.layout.popup_photo_full, receivedHolder.imageViewForAttachment, imgPath, null);
                                                    }
                                                });
                                            }
                                        });
                                    }
                                });
                            }
                        }
                    }
                }else {
                    receivedHolder.messageBody.setVisibility(View.VISIBLE);
                    receivedHolder.messageBody.setText(message.getMessage());
                }
                break;
        }
    }


    public void downloadMessage(Message message, final ImageView view, final ProgressBar progressBar, final TextView progressText){
        ApplozicConversation.downloadMessage(mContext, message, new MediaDownloadProgressHandler() {
            @Override
            public void onDownloadStarted() {
                Toast.makeText(mContext, "Downloading", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.VISIBLE);
                view.setVisibility(View.VISIBLE);
                progressBar.setMax(100);
                progressBar.setProgress(0);
                progressBar.setSecondaryProgress(100);
                progressBar.setProgressDrawable(mContext.getResources().getDrawable(R.drawable.circular_progress_bar));
                progressText.setVisibility(View.VISIBLE);
            }

            @Override
            public void onProgressUpdate(int percentage, ApplozicException e) {
                progressBar.setProgress(percentage);
                progressText.setText(percentage + " %");
            }

            @Override
            public void onCompleted(Message message, ApplozicException e) {
                if(e==null && message != null){
                    progressBar.setVisibility(View.GONE);
                    progressText.setVisibility(View.GONE);
                    String path = message.getFilePaths().get(0);
                    if(message.getAttachmentType().equals(Message.VIDEO)){
                        Glide.with(mContext).load(Uri.fromFile(new File(path))).thumbnail(0.5f).into(view);
                    }else {
                        Glide.with(mContext).load(path).
                                thumbnail(0.5f).
                                into(view);
                    }
                }
            }
        });
    }



    private static boolean showOnce = false;
    private static double startTime = 0;
    private static double finalTime = 0;
    public void handleAudio(Button forwardButton, final Button pauseButton, final Button playButton, Button rewindButton, TextView audioNameView,
                            final TextView startTimeView, final TextView finalTimeView, final SeekBar seekBar, String audioName, String audioPath){
        final MediaPlayer mediaPlayer = MediaPlayer.create(mContext, Uri.parse(audioPath));
        showOnce = false;
        startTime = 0;
        finalTime = 0;
        final int forwardTime = 5000;
        final int backwardTime = 5000;

        audioNameView.setText(audioName);

        seekBar.setClickable(false);
        pauseButton.setClickable(false);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaPlayer.start();
                finalTime = mediaPlayer.getDuration();
                startTime = mediaPlayer.getCurrentPosition();

                if(!showOnce){
                    showOnce = true;
                    seekBar.setMax((int)finalTime);
                }
                finalTimeView.setText(String.format("%d : %d", TimeUnit.MILLISECONDS.toMinutes((long) finalTime),
                        TimeUnit.MILLISECONDS.toSeconds((long) finalTime) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) finalTime))));

                startTimeView.setText(String.format("%d : %d", TimeUnit.MILLISECONDS.toMinutes((long) startTime),
                        TimeUnit.MILLISECONDS.toSeconds((long) startTime) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) startTime))));

                seekBar.setProgress((int)startTime);

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startTime = mediaPlayer.getCurrentPosition();
                        startTimeView.setText(String.format("%d : %d",
                                TimeUnit.MILLISECONDS.toMinutes((long) startTime),
                                TimeUnit.MILLISECONDS.toSeconds((long) startTime) -
                                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.
                                                toMinutes((long) startTime)))
                        );
                        seekBar.setProgress((int)startTime);
                        handler.postDelayed(this,100);
                    }
                }, 100);
                pauseButton.setEnabled(true);
                playButton.setEnabled(false);
            }
        });
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaPlayer.pause();
                pauseButton.setEnabled(false);
                playButton.setEnabled(true);
            }
        });
        forwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int temp = (int)startTime;
                if((temp+forwardTime)<=finalTime){
                    startTime = startTime + forwardTime;
                    mediaPlayer.seekTo((int) startTime);
                }
            }
        });
        rewindButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int temp = (int)startTime;

                if((temp-backwardTime)>0){
                    startTime = startTime - backwardTime;
                    mediaPlayer.seekTo((int) startTime);
                }
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mMediaPlayer) {
                mMediaPlayer.release();
            }
        });
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    /**
     * This is a overriden method which is used to identify a particular message if it is a sent message or received message.
     * @param position Used to get hold of a particular message in the view.
     * @return integer value corresponding to conversation type
     *         1 : if message is sent from user
     *         2 : if message is received by user
     */
    @Override
    public int getItemViewType(int position) {
        if (messageList.get(position).isTypeOutbox()) {
            //sent message
            return MESSAGE_TYPE_SENT;
        } else {
            return MESSAGE_TYPE_RECEIVED;
        }
    }

    /**
     * ViewHolder for received message.
     * It uses received_message_item layout.
     */
    class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView profileName;
        TextView messageBody;
        TextView messageTime;
        ImageView imageViewForAttachment;
        LinearLayout attachmentView;
        ProgressBar attachmentProgress;
        TextView attachmentProgressText;
        ImageView videoViewForAttachment;
        ImageView locationViewForAttachment;

        /**
         * AUDIO Attachment
         */
        RelativeLayout audioViewForAttachment;
        Button forwardButton;
        Button pauseButton;
        Button playButton;
        Button rewindButton;
        TextView audioName;
        TextView startTime;
        TextView finalTime;
        SeekBar seekBar;

        public ReceivedMessageHolder(View itemView) {
            super(itemView);
            messageBody = itemView.findViewById(R.id.message_body);
            messageTime = itemView.findViewById(R.id.message_time);
            profileImage = itemView.findViewById(R.id.profile_image);
            profileName = itemView.findViewById(R.id.profile_name);
            imageViewForAttachment = itemView.findViewById(R.id.preview);
            attachmentView = itemView.findViewById(R.id.attachment_view);
            attachmentProgress = itemView.findViewById(R.id.attachment_progress_bar);
            attachmentProgressText = itemView.findViewById(R.id.attachment_progress_bar_text);
            videoViewForAttachment = itemView.findViewById(R.id.attachment_video);
            locationViewForAttachment = itemView.findViewById(R.id.attachment_location_thumbnail);

            audioViewForAttachment = itemView.findViewById(R.id.attachment_audio_layout);
            forwardButton = itemView.findViewById(R.id.attachment_audio_forward_button);
            pauseButton = itemView.findViewById(R.id.attachment_audio_pause_button);
            playButton = itemView.findViewById(R.id.attachment_audio_play_button);
            rewindButton = itemView.findViewById(R.id.attachment_audio_rewind_button);
            audioName = itemView.findViewById(R.id.attachment_audio_name);
            startTime = itemView.findViewById(R.id.attachment_audio_start_time);
            finalTime = itemView.findViewById(R.id.attachment_audio_final_time);
            seekBar = itemView.findViewById(R.id.attachment_audio_seekbar);

            //setting profile
            if(messageList.get(0).getGroupId() == null){
                //contact
                Contact contact = new AppContactService(mContext).getContactById(messageList.get(0).getContactIds());
                if(contact.getImageURL() == null || contact.getImageURL().equalsIgnoreCase(null)) {
                    profileImage.setImageResource(R.drawable.profile);
                }
                else{
                    Glide.with(mContext).load(contact.getImageURL()).
                            thumbnail(0.5f).
                            into(profileImage);
                }
            }else{
                //channel
                Channel channel = ChannelService.getInstance(mContext).getChannelInfo(messageList.get(0).getGroupId());
                if(channel.getImageUrl() == null || channel.getImageUrl().equalsIgnoreCase(null)) {
                    profileImage.setImageResource(R.drawable.group_profile);
                }
                else {
                    Glide.with(mContext).load(channel.getImageUrl()).
                            thumbnail(0.5f).
                            into(profileImage);
                }
            }
        }
    }

    /**
     * ViewHolder for sent message.
     * It uses sent_message_item layout.
     */
    class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView messageBody;
        TextView messageTime;
        ImageView status;
        ImageView imageViewForAttachment;
        LinearLayout attachmentView;
        ProgressBar attachmentProgress;
        TextView attachmentProgressText;
        ImageView videoViewForAttachment;
        ImageView locationViewForAttachment;

        /**
         * AUDIO Attachment
         */
        RelativeLayout audioViewForAttachment;
        Button forwardButton;
        Button pauseButton;
        Button playButton;
        Button rewindButton;
        TextView audioName;
        TextView startTime;
        TextView finalTime;
        SeekBar seekBar;

        public SentMessageHolder(View itemView) {
            super(itemView);
            messageBody = itemView.findViewById(R.id.message_body);
            messageTime = itemView.findViewById(R.id.message_time);
            status = itemView.findViewById(R.id.delivery_status);
            imageViewForAttachment = itemView.findViewById(R.id.preview);
            attachmentView = itemView.findViewById(R.id.sender_attachment_view);
            attachmentProgress = itemView.findViewById(R.id.attachment_progress_bar);
            attachmentProgressText = itemView.findViewById(R.id.attachment_progress_bar_text);
            videoViewForAttachment = itemView.findViewById(R.id.attachment_video);
            locationViewForAttachment = itemView.findViewById(R.id.attachment_location_thumbnail);

            audioViewForAttachment = itemView.findViewById(R.id.attachment_audio_layout);
            forwardButton = itemView.findViewById(R.id.attachment_audio_forward_button);
            pauseButton = itemView.findViewById(R.id.attachment_audio_pause_button);
            playButton = itemView.findViewById(R.id.attachment_audio_play_button);
            rewindButton = itemView.findViewById(R.id.attachment_audio_rewind_button);
            audioName = itemView.findViewById(R.id.attachment_audio_name);
            startTime = itemView.findViewById(R.id.attachment_audio_start_time);
            finalTime = itemView.findViewById(R.id.attachment_audio_final_time);
            seekBar = itemView.findViewById(R.id.attachment_audio_seekbar);
        }
    }
}
