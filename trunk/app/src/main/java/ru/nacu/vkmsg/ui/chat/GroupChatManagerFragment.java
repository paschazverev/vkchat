package ru.nacu.vkmsg.ui.chat;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import ru.android.common.UiTools;
import ru.android.common.asyncloader.AsyncListHelper;
import ru.android.common.db.DatabaseTools;
import ru.android.common.lists.ContentDescriptor;
import ru.nacu.commons.StaticSoftCache;
import ru.nacu.commons.asynclist.AsyncListFragment;
import ru.nacu.vkmsg.Constants;
import ru.nacu.vkmsg.R;
import ru.nacu.vkmsg.VKMessenger;
import ru.nacu.vkmsg.dao.Tables;
import ru.nacu.vkmsg.dao.VKContentProvider;
import ru.nacu.vkmsg.ui.ThumbnailAsyncListHandler;
import ru.nacu.vkmsg.ui.contacts.SelectContactActivity;
import ru.nacu.vkmsg.ui.progress.ProgressDialog;

/**
 * @author quadro
 * @since 7/2/12 5:46 PM
 */
public final class GroupChatManagerFragment extends AsyncListFragment implements View.OnClickListener {
    public static final String TAG = "GroupChatManagerFragment";

    public static final String[] PROJECTION = new String[]{
            Tables.Columns._ID, Tables.Columns.FIRST_NAME, Tables.Columns.LAST_NAME, Tables.Columns.ONLINE, Tables.Columns.PHOTO};

    private AsyncListHelper<String, Bitmap, ImageView> downloader;

    public static final int _ID = 0;
    public static final int FIRST_NAME = 1;
    public static final int LAST_NAME = 2;
    public static final int ONLINE = 3;
    public static final int PHOTO = 4;

    private static final String ORDER_BY = Tables.Columns.FIRST_NAME + ", " + Tables.Columns.LAST_NAME;

    private long dialogId;
    private long chatId;
    private long[] users;
    private boolean started = false;
    private View addUser;
    private Button editTitle;
    private EditText etTitle;

    public GroupChatManagerFragment() {
        super(false, Constants.LOADER_GROUP_MEMBERS);
    }

    public void setData(long dialogId, long chatId, long[] users, String title) {
        this.dialogId = dialogId;
        this.users = users;
        this.chatId = chatId;

        if (started) {
            getLoaderManager().restartLoader(Constants.LOADER_GROUP_MEMBERS, null, this);
        } else {
            started = true;
            getLoaderManager().initLoader(Constants.LOADER_GROUP_MEMBERS, null, this);
        }

        etTitle.setText(title);

        if (chatId == 0) {
            editTitle.setText(R.string.create_group_chat);
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final FragmentActivity a = getActivity();
            if (a != null && !a.isFinishing()) {
                a.setResult(Activity.RESULT_OK, intent);
                a.finish();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        float thumb = UiTools.dpToPix(60, VKMessenger.getCtx());
        downloader = new AsyncListHelper<String, Bitmap, ImageView>(
                new ThumbnailAsyncListHandler((int) thumb, R.drawable.thumb_mask, R.drawable.no_photo, true), new StaticSoftCache(30), R.id.download_task);
        VKMessenger.getCtx().registerReceiver(receiver, new IntentFilter(CreateChatTask.CREATED_INTENT));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        VKMessenger.getCtx().unregisterReceiver(receiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.group_members, null);

        addUser = v.findViewById(R.id.btn_add);
        editTitle = (Button) v.findViewById(R.id.btn_edit_title);
        etTitle = (EditText) v.findViewById(R.id.edit_title);

        addUser.setOnClickListener(this);
        editTitle.setOnClickListener(this);

        return v;
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int i) {
        if (downloader != null) {
            downloader.setScrolling(i != AbsListView.OnScrollListener.SCROLL_STATE_IDLE);
        }
    }

    @Override
    protected View newView(Context context, Cursor c, ViewGroup parent) {
        final long id = c.getLong(_ID);
        if (id <= 0) {
            return View.inflate(context, R.layout.loading_row, null);
        } else {
            View v = View.inflate(context, R.layout.group_contact_row, null);
            v.setTag(new Holder(
                    (ImageView) v.findViewById(R.id.ava),
                    v.findViewById(R.id.online),
                    (TextView) v.findViewById(R.id.name),
                    v.findViewById(R.id.del)
            ));

            return v;
        }
    }

    @Override
    protected void bindView(View view, Context context, Cursor c) {
        final long id = c.getLong(_ID);
        if (id <= 0)
            return;

        final Holder h = (Holder) view.getTag();
        final String photo = c.getString(PHOTO);
        if (photo != null) {
            downloader.download(photo, id, h.ava);
        } else {
            downloader.cancelDownload(id, null, h.ava);
            h.ava.setImageResource(R.drawable.multichat);
        }

        final String firstName = c.getString(FIRST_NAME);
        final String lastName = c.getString(LAST_NAME);
        final String fullName = firstName + " " + lastName;
        h.name.setText(fullName);

        h.del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteUser(id);
            }
        });

        int online = c.getInt(ONLINE);
        h.online.setVisibility(online == 1 ? View.VISIBLE : View.INVISIBLE);
    }

    private void deleteUser(long id) {
        ProgressDialog.showProgressDialog(getActivity(), new AddRemoveChatUserTask(dialogId, chatId, id, false));
    }

    @Override
    protected ContentDescriptor getDescriptor() {
        return new ContentDescriptor(
                VKContentProvider.CONTENT_URI_PROFILE, VKContentProvider.CONTENT_URI_PROFILE,
                PROJECTION, "_id in (" + DatabaseTools.idsToString(users) + ")", null, ORDER_BY);
    }

    @Override
    protected void tryLoadStart() {
    }

    @Override
    protected boolean isLoadingStart() {
        return false;
    }

    @Override
    protected void tryLoadEnd() {
    }

    public static final int SELECT_CONTACT = 1;

    @Override
    public void onClick(View view) {
        if (view == editTitle) {
            if (chatId != 0) {
                ProgressDialog.showProgressDialog(getActivity(), new ChangeTitleTask(chatId, etTitle.getText().toString()));
            } else {
                ProgressDialog.showProgressDialog(getActivity(), new CreateChatTask(dialogId, users, etTitle.getText().toString()));
            }
        } else if (view == addUser) {
            startActivityForResult(new Intent(getActivity(), SelectContactActivity.class), SELECT_CONTACT);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_CONTACT && resultCode == Activity.RESULT_OK) {
            ProgressDialog.showProgressDialog(getActivity(), new AddRemoveChatUserTask(dialogId, chatId, data.getLongExtra("userId", 0), true));
        }
    }

    private static class Holder {
        private final ImageView ava;
        private final View online;
        private final TextView name;
        private final View del;

        private Holder(ImageView ava, View online, TextView name, View del) {
            this.ava = ava;
            this.online = online;
            this.name = name;
            this.del = del;
        }
    }
}
