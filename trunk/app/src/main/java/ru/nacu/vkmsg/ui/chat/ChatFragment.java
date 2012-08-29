package ru.nacu.vkmsg.ui.chat;

import android.app.Activity;
import android.content.*;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.widget.*;
import ru.android.common.UiTools;
import ru.android.common.asyncloader.AsyncListHelper;
import ru.android.common.asyncloader.DefaultCache;
import ru.android.common.db.DatabaseTools;
import ru.android.common.lists.ContentDescriptor;
import ru.android.common.logs.Logs;
import ru.android.common.task.ModernAsyncTask;
import ru.nacu.commons.StaticSoftCache;
import ru.nacu.commons.asynclist.AsyncListFragment;
import ru.nacu.commons.asynclist.State;
import ru.nacu.vkmsg.*;
import ru.nacu.vkmsg.asynctasks.*;
import ru.nacu.vkmsg.dao.DateTools;
import ru.nacu.vkmsg.dao.Queries;
import ru.nacu.vkmsg.dao.Tables;
import ru.nacu.vkmsg.dao.VKContentProvider;
import ru.nacu.vkmsg.ui.*;
import ru.nacu.vkmsg.ui.chat.attachments.LoadPictureTask;
import ru.nacu.vkmsg.ui.chat.menu.AttachMenu;
import ru.nacu.vkmsg.ui.map.MapActivity;
import ru.nacu.vkmsg.ui.progress.ProgressDialog;
import ru.nacu.vkmsg.updates.LongPoll;
import ru.nacu.vkmsg.updates.media.MyMediaController;

import java.io.File;
import java.util.*;

/**
 * @author quadro
 * @since 6/24/12 7:36 PM
 */
public final class ChatFragment extends AsyncListFragment implements View.OnClickListener, TextWatcher, Runnable {
    public static final String TYPING_INTENT = "ru.nacu.vkmsg.Typing";

    public static final String TAG = "ChatFragment";

    private boolean search;

    private AsyncListHelper<String, Bitmap, ImageView> downloader;
    private AsyncListHelper<String, Bitmap, ImageView> attachDownloader;
    private AsyncListHelper<String, Bitmap, ImageView> imageAttachmentDownloader;
    private AsyncListHelper<String, Bitmap, ImageView> videoAttachmentDownloader;
    private AsyncListHelper<Long, FwdMessageAsyncListHandler.LoadResult, View> fwdDownloader;

    private AttachMenu menu;
    private boolean topMenuAdded;
    private boolean menuShown;

    private final long me;

    private static long dialogId;
    private long userId;
    private long chatId;

    private static boolean visible;
    private EditText msgText;
    private TextView footer;

    private int attachThumbnailSize;
    private int attachVideoWidth;
    private int buttonSize;
    private int attachVideoHeight;
    private View attachButton;
    private View btnSend;
    private ImageView geob;
    private View photob;
    private View galleryb;
    private View attachField;
    private LinearLayout attachContainer;
    private TextView messagesAttachB;

    public static boolean isPaused() {
        return !visible;
    }

    public static long getDialogId() {
        return dialogId;
    }

    public ChatFragment() {
        super(false, Constants.LOADER_CHAT);
        me = Init.getUserId();
    }

    private final BroadcastReceiver typingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(isAdded() && getActivity() != null && !getActivity().isFinishing()) {
                long userId = intent.getLongExtra("userId", 0);
                long chatId = intent.getLongExtra("chatId", 0);

                if (ChatFragment.this.chatId != 0 && ChatFragment.this.chatId != chatId) {
                    return;
                }

                final String v = users.get(userId);
                if (v != null) {
                    footer.setText(v + " " + getString(R.string.typing));
                    VKMessenger.getHandler().postDelayed(ChatFragment.this, 5000);
                }
            }
        }
    };

    @Override
    public void run() {
        footer.setText("");
    }

    private Map<Long, String> users = new HashMap<Long, String>();

    public long getUserId() {
        return userId;
    }

    public long getChatId() {
        return chatId;
    }

    public Size getSize(int width, int height) {
        final float scale;
        if (width > height) {
            scale = (float) attachThumbnailSize / (float) width;
        } else {
            scale = (float) attachThumbnailSize / (float) height;
        }

        return new Size(scale, (int) ((float) width * scale), (int) ((float) height * scale));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        menu = new AttachMenu(getActivity(), this);
        attachThumbnailSize = (int) UiTools.dpToPix(180, getActivity());
        buttonSize = (int) UiTools.dpToPix(87, getActivity());
        attachVideoWidth = (int) UiTools.dpToPix(180, getActivity());
        attachVideoHeight = (int) UiTools.dpToPix(135, getActivity());

        float thumb = UiTools.dpToPix(60, VKMessenger.getCtx());
        downloader = new AsyncListHelper<String, Bitmap, ImageView>(
                new ThumbnailAsyncListHandler((int) thumb, R.drawable.thumb_mask, R.drawable.no_photo, true), new StaticSoftCache(30), R.id.download_task);

        float thumbAttach = UiTools.dpToPix(87, VKMessenger.getCtx());
        attachDownloader = new AsyncListHelper<String, Bitmap, ImageView>(
                new ThumbnailAsyncListHandler((int) thumbAttach, R.drawable.thumb_mask_big, R.drawable.no_photo_attach, false),
                new DefaultCache<String, Bitmap>(5), R.id.download_task);

        imageAttachmentDownloader = new AsyncListHelper<String, Bitmap, ImageView>(
                new AttachAsyncListHandler(attachVideoWidth), new StaticSoftCache(10), R.id.download_task);

        videoAttachmentDownloader = new AsyncListHelper<String, Bitmap, ImageView>(
                new VideoAttachAsyncListHandler(attachVideoWidth, attachVideoHeight), new StaticSoftCache(10), R.id.download_task);

        fwdDownloader = new AsyncListHelper<Long, FwdMessageAsyncListHandler.LoadResult, View>(
                new FwdMessageAsyncListHandler(), new FwdMessageCache(10), R.id.download_task
        );

        VKMessenger.getCtx().registerReceiver(typingReceiver, new IntentFilter(TYPING_INTENT));

    }

    public void showMenu() {
        if (!topMenuAdded) {
            WindowManager manager
                    = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);

            WindowManager.LayoutParams params
                    = new WindowManager.LayoutParams(
                    ViewGroup.LayoutParams.FILL_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

            params.gravity = Gravity.BOTTOM;
            params.alpha = 1f;
            params.windowAnimations = R.style.ToolBarAnimation;
            manager.addView(menu, params);
            topMenuAdded = true;
        }

        menuShown = true;
    }

    public void hideMenu() {
        if (topMenuAdded) {
            WindowManager manager
                    = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
            manager.removeView(menu);
            topMenuAdded = false;
        }

        menuShown = false;
    }

    private void switchMenu() {
        if (menuShown) {
            hideMenu();
        } else {
            showMenu();
        }
    }

    public boolean dispatchTouchEvent() {
        if (menuShown) {
            hideMenu();
            return true;
        }

        return false;
    }

    public boolean onKeyDown(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (photos == null || photos.size() == 0) {
                switchMenu();
            }

            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (menuShown) {
                hideMenu();
                return true;
            }
        }

        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        VKMessenger.getCtx().unregisterReceiver(typingReceiver);
    }


    private static String imageFile;
    private static List<String> photos;
    private static Attachments.Geo geo;
    private static Set<Long> forwarded;

    private List<ImageView> photoViews;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.chat, null);
        msgText = (EditText) v.findViewById(R.id.msg_text);
        btnSend = v.findViewById(R.id.msg_send);
        btnSend.setOnClickListener(this);
        msgText.addTextChangedListener(this);
        footer = new TextView(getActivity());
        attachButton = v.findViewById(R.id.attach_sel);
        attachButton.setOnClickListener(this);
        messagesAttachB = (TextView) v.findViewById(R.id.attach_messagesb);
        messagesAttachB.setOnClickListener(this);
        geob = (ImageView) v.findViewById(R.id.attach_locationb);
        photob = v.findViewById(R.id.attach_photob);
        galleryb = v.findViewById(R.id.attach_galleryb);
        geob.setOnClickListener(this);
        photob.setOnClickListener(this);
        galleryb.setOnClickListener(this);
        attachField = v.findViewById(R.id.attach_field);
        attachContainer = (LinearLayout) v.findViewById(R.id.attach_container);
        attachField.setVisibility(View.GONE);

        if (photos != null) {
            final ArrayList<String> copy = new ArrayList<String>(photos);
            photos.clear();
            for (String s : copy) {
                addPhoto0(s);
            }
        }

        checkForwarded();
        checkGeo();

        return v;
    }

    public static final int CAMERA_PIC_REQUEST = 5000;
    public static final int GALLERY_PIC_REQUEST = 5001;
    public static final int MAP_REQUEST = 5002;

    private void removePhoto0(int pos) {
        photoViews.get(pos).setVisibility(View.GONE);
        final ImageView removed = photoViews.remove(pos);
        photoViews.add(removed);
        photos.remove(pos);

        checkAttachField();
    }

    private void checkForwarded() {
        if (forwarded != null)
            messagesAttachB.setText(getString(R.string.messages) + "\n" + forwarded.size());

        messagesAttachB.setVisibility(forwarded != null && forwarded.size() > 0 ? View.VISIBLE : View.GONE);
        checkAttachField();
    }

    private void checkAttachField() {
        if ((forwarded != null && forwarded.size() > 0) || (photos != null && photos.size() > 0) || geo != null) {
            attachField.setVisibility(View.VISIBLE);
            attachButton.setVisibility(View.GONE);
        } else {
            attachField.setVisibility(View.GONE);
            attachButton.setVisibility(View.VISIBLE);
        }
    }

    private void addGeo(double lon, double lat) {
        geo = new Attachments.Geo(lat, lon, null);
        checkGeo();
    }

    private void checkGeo() {
        checkAttachField();
        if (geo != null) {
            attachDownloader.download(geo.getMapImage(buttonSize, buttonSize), 0, geob);
        } else {
            geob.setImageResource(R.drawable.attach_geob);
        }
    }

    private void addPhoto0(String file) {
        if (photos == null) {
            photos = new ArrayList<String>();
        }

        if (photoViews == null) {
            photoViews = new ArrayList<ImageView>();
        }

        photos.add(file);
        int idx = photos.size();

        final ImageView iv;
        if (photoViews.size() <= idx) {
            iv = new ImageView(getActivity());
            final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            final float dp3 = UiTools.dpToPix(3, getActivity());
            lp.leftMargin = (int) dp3;
            lp.rightMargin = (int) dp3;
            lp.topMargin = (int) dp3;
            lp.bottomMargin = (int) dp3;
            iv.setOnClickListener(this);
            attachContainer.addView(iv, 0, lp);
            photoViews.add(iv);
        } else {
            iv = photoViews.get(idx);
        }

        iv.setVisibility(View.VISIBLE);
        attachDownloader.download(file, photoViews.size() - 1, iv);
        checkAttachField();
    }

    public void addPhoto() {
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        File photo;
        try {
            // place where to store camera taken picture
            photo = this.createTemporaryFile("picture", ".jpg");
            photo.delete();
        } catch (Exception e) {
            Log.v(TAG, "Can't create file to take picture!");
            Toast.makeText(getActivity(), "Please check SD card! Image shot is impossible!", 10000);
            return;
        }

        imageFile = photo.getAbsolutePath();
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photo));
        startActivityForResult(intent, CAMERA_PIC_REQUEST);
    }

    private File createTemporaryFile(String part, String ext) throws Exception {
        File tempDir = Environment.getExternalStorageDirectory();
        tempDir = new File(tempDir.getAbsolutePath() + "/.temp/");
        if (!tempDir.exists()) {
            tempDir.mkdir();
        }

        return File.createTempFile(part, ext, tempDir);
    }

    public void addGalleryPhoto() {
        Intent i = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, GALLERY_PIC_REQUEST);
    }

    public void switchLocation() {
        if (geo == null)
            startActivityForResult(new Intent(getActivity(), MapActivity.class), MAP_REQUEST);
        else {
            geo = null;
            checkGeo();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case MAP_REQUEST: {
                if (resultCode == Activity.RESULT_OK) {
                    addGeo(data.getDoubleExtra("lon", 0), data.getDoubleExtra("lat", 0));
                }

                break;
            }

            case CAMERA_PIC_REQUEST: {
                if (resultCode == Activity.RESULT_OK)
                    addPhoto0(imageFile);

                break;
            }

            case GALLERY_PIC_REQUEST: {
                if (resultCode == Activity.RESULT_OK) {
                    Uri selectedImage = data.getData();
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};

                    Cursor cursor = getActivity().getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                    cursor.moveToFirst();

                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String filePath = cursor.getString(columnIndex);
                    cursor.close();

                    if (filePath != null) {
                        addPhoto0(filePath);
                    } else {
                        Toast.makeText(getActivity(), R.string.cant_send_this_photo, 5000).show();
                    }
                }
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        final View footer = View.inflate(getActivity(), R.layout.footer, null);
        this.footer = (TextView) footer.findViewById(R.id.footer_text);
        getListView().addFooterView(footer);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int i) {
        if (downloader != null) {
            downloader.setScrolling(i != AbsListView.OnScrollListener.SCROLL_STATE_IDLE);
        }
        if (imageAttachmentDownloader != null) {
            imageAttachmentDownloader.setScrolling(i != AbsListView.OnScrollListener.SCROLL_STATE_IDLE);
        }
        if (videoAttachmentDownloader != null) {
            videoAttachmentDownloader.setScrolling(i != AbsListView.OnScrollListener.SCROLL_STATE_IDLE);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onClick(View view) {
        if (view == attachButton) {
            switchMenu();
        } else if (view == btnSend) {
            if ((photos != null && photos.size() > 0) || geo != null || (forwarded != null && forwarded.size() > 0)) {
                new ModernAsyncTask<Void, Void, Void>() {
                    private volatile String attachments;

                    @Override
                    protected Void doInBackground(Void... params) {
                        final Attachments a = new Attachments(new ArrayList<Attachments.Photo>(), null, null, new ArrayList<Attachments.Forwarded>(), null, geo);
                        if (photos != null) {
                            for (String photo : photos) {
                                Size s = ThumbnailAsyncListHandler.getImageSize(photo);
                                a.photos.add(new Attachments.Photo(null, photo, photo, photo, photo, photo, s.width, s.height));
                            }
                        }

                        if (forwarded != null && forwarded.size() > 0) {
                            final Cursor c = VKContentProvider.q(VKContentProvider.CONTENT_URI_MESSAGE, new String[]{Tables.Columns._ID, Tables.Columns.WRITER_ID, Tables.Columns.DT, Tables.Columns.BODY, Tables.Columns.ATTACHMENT}, "_id in (" + DatabaseTools.idsToString(forwarded) + ")", null, null);
                            try {
                                while (c.moveToNext()) {
                                    a.messages.add(new Attachments.Forwarded(
                                            c.getLong(0),
                                            c.getLong(1),
                                            c.getLong(2),
                                            c.getString(3),
                                            Attachments.parse(c.getString(4))
                                    ));
                                }
                            } finally {
                                c.close();
                            }
                        }

                        attachments = a.serialize().toString();
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        sendMessage(attachments);

                        if (photos != null)
                            photos.clear();

                        if (photoViews != null) {
                            for (ImageView photoView : photoViews) {
                                photoView.setVisibility(View.GONE);
                            }
                        }

                        if (forwarded != null) {
                            forwarded.clear();
                        }

                        geo = null;

                        checkForwarded();
                        checkGeo();
                    }
                }.execute();

            } else {
                sendMessage(null);
            }

        } else if (view == galleryb) {
            addGalleryPhoto();
        } else if (view == photob) {
            addPhoto();
        } else if (view == geob) {
            switchLocation();
        } else if (messagesAttachB == view) {
            forwarded.clear();
            checkForwarded();
        } else {
            if (photoViews != null) {
                int pos = 0;
                for (ImageView photoView : new ArrayList<ImageView>(photoViews)) {
                    if (view == photoView) {
                        removePhoto0(pos);
                        break;
                    }

                    pos++;
                }
            }
        }
    }

    private void sendMessage(String attaches) {
        if (attaches == null && msgText.getText().length() == 0) {
            return;
        }

        final ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>(1);
        operations.add(
                ContentProviderOperation.newInsert(VKContentProvider.CONTENT_URI_MESSAGE).
                        withValue(Tables.Columns.WRITER_ID, Init.getUserId()).
                        withValue(Tables.Columns.BODY, msgText.getText().toString()).
                        withValue(Tables.Columns.DT, System.currentTimeMillis() / 1000).
                        withValue(Tables.Columns.DIALOG_ID, dialogId).
                        withValue(Tables.Columns.ATTACHMENT, attaches).
                        withValue(Tables.Columns.LOCAL_STATUS, 0).
                        withValue(Tables.Columns.SERVER_STATUS, 0).build());

        VKContentProvider.b(operations);
        msgText.setText("");
    }

    private boolean initCompleted = false;
    private final ChatUserLoaderCallbacks infoCallbacks = new ChatUserLoaderCallbacks(this);

    @SuppressWarnings("unchecked")
    public void show(boolean search, final long dialogId, long userId, final long chatId) {
        this.search = search;
        ChatFragment.dialogId = dialogId;
        this.userId = userId;
        this.chatId = chatId;

        final LoaderManager m = getLoaderManager();
        if (initCompleted) {
            m.restartLoader(Constants.LOADER_CHAT, null, this);
            m.restartLoader(Constants.LOADER_CHAT_TITLE, null, infoCallbacks);
        } else {
            m.initLoader(Constants.LOADER_CHAT, null, this);
            m.initLoader(Constants.LOADER_CHAT_TITLE, null, infoCallbacks);
            initCompleted = true;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        new SetLocalReadStateTask(dialogId).execute();

        int oldCount = getAdapter().getCount();
        int scrollY = getFirstItemPos();
        final int first = getListView().getFirstVisiblePosition();

        super.onLoadFinished(loader, data);

        int newCount = getAdapter().getCount();
        getListView().setSelectionFromTop(first + newCount - oldCount, scrollY);
        Logs.d(TAG, "onLoadFinished() " + oldCount + "; " + newCount + "; " + scrollY);
    }

    @Override
    protected ContentDescriptor getDescriptor() {
        final Uri uri = ContentUris.withAppendedId(VKContentProvider.CONTENT_URI_MESSAGE, dialogId);
        return new ContentDescriptor(uri, uri, Queries.MESSAGE_PROJECTION, Queries.SELECTION_NOT_DELETED, null, Queries.DT_ASC);
    }

    @Override
    protected InternalCursorAdapter createAdapter() {
        return new InternalCursorAdapter() {
            @Override
            public int getViewTypeCount() {
                return 5;
            }

            @Override
            public int getItemViewType(int position) {
                final Cursor c = getItem(position);
                final long id = c.getLong(Queries.MessageColumns._ID);
                if (id == 0)
                    return 0;

                final long writerId = c.getLong(Queries.MessageColumns.WRITER_ID);
                final String a = c.getString(Queries.MessageColumns.ATTACHMENT);
                if (a == null) {
                    return me == writerId ? 2 : 1;
                } else {
                    return me == writerId ? 3 : 4;
                }
            }
        };
    }

    @Override
    protected View newView(Context context, Cursor c, ViewGroup parent) {
        final long id = c.getLong(0);
        if (id == 0) {
            return View.inflate(VKMessenger.getCtx(), R.layout.loading_row, null);
        } else {
            final long writerId = c.getLong(Queries.MessageColumns.WRITER_ID);
            final String a = c.getString(Queries.MessageColumns.ATTACHMENT);

            final View view;
            if (a == null) {
                if (writerId == me) {
                    view = View.inflate(context, R.layout.message_row_out, null);
                } else {
                    view = View.inflate(context, R.layout.message_row_in, null);
                }
            } else {
                if (writerId == me) {
                    view = View.inflate(context, R.layout.messagea_row_out, null);
                } else {
                    view = View.inflate(context, R.layout.messagea_row_in, null);
                }
            }

            Holder h = new Holder(
                    (TextView) view.findViewById(R.id.msg), (TextView) view.findViewById(R.id.time),
                    (TextView) view.findViewById(R.id.dt), (ImageView) view.findViewById(R.id.img_stat),
                    (ImageView) view.findViewById(R.id.img), (ImageView) view.findViewById(R.id.geo_attach),
                    view.findViewById(R.id.inner_container));

            view.setTag(h);
            return view;
        }
    }

    @Override
    protected void bindView(View view, Context context, Cursor c) {
        final long id = c.getLong(Queries.MessageColumns._ID);
        if (id <= 0) {
            return;
        }

        boolean s = c.getInt(Queries.MessageColumns.SEARCH) == 1;
        view.setBackgroundResource(search && s ? R.color.light_bg_search : R.color.light_bg);

        Holder h = (Holder) view.getTag();
        final long writerId = c.getLong(Queries.MessageColumns.WRITER_ID);
        final int localStatus = c.getInt(Queries.MessageColumns.LOCAL_STATUS);
        final long currentTime = c.getLong(Queries.MessageColumns.DT) * 1000;
        final String body = c.getString(Queries.MessageColumns.BODY);
        if (body == null || body.length() == 0)
            h.msg.setText("");
        else
            h.msg.setText(body);

        h.time.setText(DateTools.formatTime(context, currentTime));

        if (me == writerId) {
            if (id > 1000000000000l) {
                h.status.setImageResource(R.drawable.clock);
            } else if (localStatus == 1) {
                h.status.setImageResource(R.drawable.read);
            } else {
                h.status.setImageResource(R.drawable.unread);
            }
        } else {
            if (chatId == 0) {
                h.thumb.setVisibility(View.GONE);
            } else {
                h.thumb.setVisibility(View.VISIBLE);
                downloader.download(c.getString(Queries.MessageColumns.PHOTO), id, h.thumb);
            }
        }

        String a = c.getString(Queries.MessageColumns.ATTACHMENT);
        if (a != null) {
            final Attachments attachments = Attachments.parse(a);

            showMessages(id, context, h.innerContainer, attachments.messages);
            showDocuments(context, h.innerContainer, attachments.documents);
            showAudios(context, h.innerContainer, attachments.audios);
            showVideos(id, context, h.innerContainer, attachments.videos);
            showPhotos(id, context, h.innerContainer, attachments.photos);

            if (attachments.geo != null) {
                h.geoAttach.setVisibility(View.VISIBLE);
                videoAttachmentDownloader.download(attachments.geo.getMapImage(attachVideoWidth, attachVideoHeight), id, h.geoAttach);
                final Attachments.Geo geo = attachments.geo;
                h.geoAttach.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:" + geo.lat + "," + geo.lon + "?z=15")));
                    }
                });
            } else {
                h.geoAttach.setVisibility(View.GONE);
            }

            h.msg.setVisibility(body == null || body.trim().length() == 0 ? View.GONE : View.VISIBLE);
        } else {
            h.msg.setVisibility(View.VISIBLE);
        }

        if (h.innerContainer != null) {
            h.innerContainer.setSelected(selected.contains(id));
        } else {
            h.msg.setSelected(selected.contains(id));
        }

        boolean firstInGroup = !c.moveToPrevious();
        if (!firstInGroup) {
            long prevTime = c.getLong(Queries.MessageColumns.DT) * 1000;

            Calendar current = Calendar.getInstance();
            current.setTimeInMillis(currentTime);
            Calendar prev = Calendar.getInstance();
            prev.setTimeInMillis(prevTime);

            firstInGroup = current.get(Calendar.DAY_OF_YEAR) != prev.get(Calendar.DAY_OF_YEAR)
                    || current.get(Calendar.YEAR) != prev.get(Calendar.YEAR);
        }

        if (firstInGroup) {
            h.date.setVisibility(View.VISIBLE);
            h.date.setText(DateTools.formatDate(VKMessenger.getCtx(), currentTime));
        } else {
            h.date.setVisibility(View.GONE);
        }
    }

    public void showPhotos(long id, Context ctx, LinearLayout v, List<Attachments.Photo> photos) {
        AttachmentHolder holder = (AttachmentHolder) v.getTag();
        for (ImageView photo : holder.photos) {
            photo.setVisibility(View.GONE);
        }

        if (photos != null && photos.size() > 0) {
            int pos = 0;
            for (Attachments.Photo p : photos) {
                final ImageView view;
                if (holder.photos.size() > pos) {
                    view = holder.photos.get(pos);
                    view.setVisibility(View.VISIBLE);
                } else {
                    view = new ImageView(ctx);
                    view.setScaleType(ImageView.ScaleType.CENTER);
                    int idx = v.indexOfChild(holder.photosTitle);
                    final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.topMargin = 7;
                    v.addView(view, idx + 1 + holder.photos.size(), lp);
                    holder.photos.add(view);
                }

                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
                final Size s = getSize(p.width, p.height);
                if (s.width != 0 && s.height != 0) {
                    params.width = s.width;
                    params.height = s.height;
                } else {
                    params.width = attachThumbnailSize;
                    params.height = attachThumbnailSize;
                }

                view.setLayoutParams(params);
                imageAttachmentDownloader.download(p.getThumbnailPhoto(), combineId(id, pos), view);
                final String biggest = p.getBiggestPhoto();
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ProgressDialog.showProgressDialog(getActivity(), new LoadPictureTask(biggest));
                    }
                });

                pos++;
            }
            holder.photosTitle.setVisibility(View.VISIBLE);
        } else {
            for (View photo : holder.photos) {
                photo.setVisibility(View.GONE);
            }
            holder.photosTitle.setVisibility(View.GONE);
        }
    }

    public void showVideos(long id, Context ctx, LinearLayout v, List<Attachments.Video> videos) {
        AttachmentHolder holder = (AttachmentHolder) v.getTag();
        for (ImageView video : holder.videos) {
            video.setVisibility(View.GONE);
        }

        if (videos != null && videos.size() > 0) {
            int pos = 0;
            for (Attachments.Video vid : videos) {
                final ImageView view;
                if (holder.videos.size() > pos) {
                    view = holder.videos.get(pos);
                    view.setVisibility(View.VISIBLE);
                } else {
                    view = new ImageView(ctx);
                    view.setScaleType(ImageView.ScaleType.CENTER);
                    int idx = v.indexOfChild(holder.videosTitle);
                    final LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(attachVideoWidth, attachVideoHeight);
                    p.topMargin = 7;
                    v.addView(view, idx + 1 + holder.videos.size(), p);
                    holder.videos.add(view);
                }

                final String url = vid.player;
                videoAttachmentDownloader.download(vid.image, combineId(id, pos), view);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    }
                });

                pos++;
            }
            holder.videosTitle.setVisibility(View.VISIBLE);
        } else {
            holder.videosTitle.setVisibility(View.GONE);
        }
    }

    public void showDocuments(Context ctx, LinearLayout v, List<Attachments.Document> documents) {
        AttachmentHolder holder = (AttachmentHolder) v.getTag();
        for (Button document : holder.documents) {
            document.setVisibility(View.GONE);
        }
        if (documents != null && documents.size() > 0) {
            int pos = 0;
            for (Attachments.Document a : documents) {
                final Button view;
                if (holder.documents.size() > pos) {
                    view = holder.documents.get(pos);
                    view.setVisibility(View.VISIBLE);
                } else {
                    view = new Button(ctx);
                    view.setSingleLine();
                    view.setEllipsize(TextUtils.TruncateAt.END);
                    view.setTextColor(Color.WHITE);
                    view.setBackgroundResource(R.drawable.doc);
                    view.setPadding(10, 3, 10, 3);
                    int idx = v.indexOfChild(holder.documentsTitle);
                    final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.topMargin = 7;
                    v.addView(view, idx + 1 + holder.documents.size(), lp);
                    holder.documents.add(view);
                }

                view.setText(a.title);
                final String url = a.url;
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(browserIntent);
                    }
                });
                pos++;
            }
            holder.documentsTitle.setVisibility(View.VISIBLE);
        } else {
            holder.documentsTitle.setVisibility(View.GONE);
        }
    }

    public void showAudios(Context ctx, LinearLayout v, List<Attachments.Audio> audios) {
        AttachmentHolder holder = (AttachmentHolder) v.getTag();
        for (MyMediaController audio : holder.audios) {
            audio.setVisibility(View.GONE);
        }
        if (audios != null && audios.size() > 0) {
            int pos = 0;
            for (Attachments.Audio a : audios) {
                final MyMediaController view;
                if (holder.audios.size() > pos) {
                    view = holder.audios.get(pos);
                    view.setVisibility(View.VISIBLE);
                } else {
                    view = new MyMediaController(ctx);
                    int idx = v.indexOfChild(holder.audiosTitle);
                    v.addView(view, idx + 1 + holder.audios.size());
                    holder.audios.add(view);
                }

                view.setTrack(a.url, a.artist, a.track);
                pos++;
            }
            holder.audiosTitle.setVisibility(View.VISIBLE);
        } else {
            holder.audiosTitle.setVisibility(View.GONE);
        }
    }

    public void showMessages(long id, Context ctx, LinearLayout v, List<Attachments.Forwarded> messages) {
        AttachmentHolder holder = (AttachmentHolder) v.getTag();
        for (View message : holder.messages) {
            message.setVisibility(View.GONE);
        }

        if (messages != null && messages.size() > 0) {
            int pos = 0;
            for (Attachments.Forwarded m : messages) {
                final MessageHolder h;
                final View view;
                if (holder.messages.size() > pos) {
                    view = holder.messages.get(pos);
                    view.setVisibility(View.VISIBLE);
                    h = (MessageHolder) view.getTag();
                } else {
                    view = View.inflate(ctx, R.layout.attach_message, null);
                    h = new MessageHolder(
                            (ImageView) view.findViewById(R.id.img_fwd),
                            (TextView) view.findViewById(R.id.name_fwd),
                            (TextView) view.findViewById(R.id.time_fwd),
                            (TextView) view.findViewById(R.id.body_fwd)
                    );

                    view.setTag(h);
                    int idx = v.indexOfChild(holder.messagesTitle);
                    v.addView(view, idx + 1 + holder.messages.size());
                    holder.messages.add(view);
                }

                h.time.setText(DateTools.formatDialogDate(ctx, m.date * 1000, R.string.yesterday));
                if (m.body != null && m.body.length() > 0) {
                    h.body.setText(m.body);
                    h.body.setVisibility(View.VISIBLE);
                } else {
                    h.body.setVisibility(View.GONE);
                }

                fwdDownloader.download(m.userId, combineId(id, pos), view);
                pos++;
            }
            holder.messagesTitle.setVisibility(View.VISIBLE);
        } else {
            holder.messagesTitle.setVisibility(View.GONE);
        }
    }

    private static long combineId(long id, int pos) {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + pos;
        return result;
    }

    @Override
    public void onResume() {
        super.onResume();
        visible = true;

        final ListView list = getListView();
        list.setDividerHeight(0);
    }

    @Override
    public void onPause() {
        super.onPause();
        visible = false;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void tryLoadStart() {
        if (Loading.getDialog(dialogId) == State.NONE && !Flags.isDialogLoaded(dialogId)) {
            new LoadMessagesTask(20, 0, userId, chatId, dialogId).execute();
        }
    }

    @Override
    protected boolean isLoadingStart() {
        return Loading.getDialog(dialogId) == State.START;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void tryLoadEnd() {
        if (Loading.getDialog(dialogId) == State.NONE && !Flags.isDialogFullyLoaded(dialogId)) {
            new LoadMessagesTask(20, getAdapter().getCount(), userId, chatId, dialogId).execute();
        }
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    private long lastSentTime = 0;

    @Override
    public void afterTextChanged(Editable editable) {
        if (System.currentTimeMillis() - lastSentTime > 5000) {
            lastSentTime = System.currentTimeMillis();
            LongPoll.setActivity(userId, chatId);
        }
    }

    public void onInformationLoaded(ChatUserLoaderCallbacks.LoadResult data) {
        getHost().onInformationLoaded(data);
        users = data.users;
    }

    public Host getHost() {
        return (Host) getActivity();
    }

    private Set<Long> selected = new HashSet<Long>();

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        if (id <= 0) return;

        final Holder h = (Holder) v.getTag();
        boolean sel;
        if (selected.contains(id)) {
            selected.remove(id);
            sel = false;
        } else {
            selected.add(id);
            sel = true;
        }

        if (h.innerContainer != null) {
            h.innerContainer.setSelected(sel);
        } else {
            h.msg.setSelected(sel);
        }

        getHost().onSelectionChange(selected.size());
    }

    private static class AttachmentHolder {
        public final List<View> messages = new ArrayList<View>();
        public final List<MyMediaController> audios = new ArrayList<MyMediaController>();
        public final List<ImageView> videos = new ArrayList<ImageView>();
        public final List<ImageView> photos = new ArrayList<ImageView>();
        public final List<Button> documents = new ArrayList<Button>();
        public final View messagesTitle;
        public final View audiosTitle;
        public final View videosTitle;
        public final View photosTitle;
        public final View documentsTitle;

        private AttachmentHolder(View messagesTitle, View audiosTitle, View videosTitle, View photosTitle, View documentsTitle) {
            this.messagesTitle = messagesTitle;
            this.audiosTitle = audiosTitle;
            this.videosTitle = videosTitle;
            this.photosTitle = photosTitle;
            this.documentsTitle = documentsTitle;
        }
    }

    private static class Holder {
        public final TextView msg;
        public final TextView time;
        public final TextView date;
        public final ImageView status;
        public final ImageView thumb;
        public final ImageView geoAttach;
        public final LinearLayout innerContainer;

        private Holder(TextView msg, TextView time, TextView date, ImageView status, ImageView thumb, ImageView geoAttach, View innerContainer) {
            this.msg = msg;
            this.time = time;
            this.date = date;
            this.status = status;
            this.thumb = thumb;
            this.geoAttach = geoAttach;
            this.innerContainer = (LinearLayout) innerContainer;

            if (innerContainer != null) {
                innerContainer.setTag(
                        new AttachmentHolder(innerContainer.findViewById(R.id.attach_message_title),
                                innerContainer.findViewById(R.id.attach_audio_title),
                                innerContainer.findViewById(R.id.attach_video_title),
                                innerContainer.findViewById(R.id.attach_photo_title),
                                innerContainer.findViewById(R.id.attach_doc_title)));
            }
        }
    }

    public static class MessageHolder {
        public final ImageView img;
        public final TextView name;
        public final TextView time;
        public final TextView body;

        private MessageHolder(ImageView img, TextView name, TextView time, TextView body) {
            this.img = img;
            this.name = name;
            this.time = time;
            this.body = body;
        }
    }

    public void cancelSelection() {
        selected.clear();
        getAdapter().notifyDataSetChanged();
        getHost().onSelectionChange(0);
    }

    @SuppressWarnings("unchecked")
    public void deleteMessages() {
        new ModernAsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                final ArrayList<ContentProviderOperation> o = new ArrayList<ContentProviderOperation>(1);
                o.add(ContentProviderOperation.newUpdate(VKContentProvider.CONTENT_URI_MESSAGE)
                        .withSelection("_id in (" + DatabaseTools.idsToString(selected) + ")", null)
                        .withValue(Tables.Columns.DELETED, 1)
                        .build());

                VKContentProvider.b(o);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                selected.clear();
                getHost().onSelectionChange(0);
                new SendInformationTask().execute();
            }
        }.execute();
    }

    public void forwardMessages() {
        if (forwarded == null) {
            forwarded = new HashSet<Long>(selected);
        } else {
            forwarded.addAll(selected);
        }

        selected.clear();
        getHost().onSelectionChange(0);

        checkForwarded();
    }

    public static interface Host {
        void onInformationLoaded(ChatUserLoaderCallbacks.LoadResult data);

        void onSelectionChange(int count);
    }
}
