package ru.nacu.vkmsg.ui.chats;

import android.app.Activity;
import android.content.*;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import ru.android.common.UiTools;
import ru.android.common.asyncloader.AsyncListHelper;
import ru.android.common.db.DatabaseTools;
import ru.android.common.lists.ContentDescriptor;
import ru.android.common.task.ModernAsyncTask;
import ru.nacu.commons.StaticSoftCache;
import ru.nacu.commons.asynclist.AsyncListFragment;
import ru.nacu.commons.asynclist.State;
import ru.nacu.vkmsg.*;
import ru.nacu.vkmsg.asynctasks.LoadDialogsTask;
import ru.nacu.vkmsg.asynctasks.Loading;
import ru.nacu.vkmsg.asynctasks.SearchMessagesTask;
import ru.nacu.vkmsg.dao.DateTools;
import ru.nacu.vkmsg.dao.Queries;
import ru.nacu.vkmsg.dao.Tables;
import ru.nacu.vkmsg.dao.VKContentProvider;
import ru.nacu.vkmsg.ui.ThumbnailAsyncListHandler;
import ru.nacu.vkmsg.ui.contacts.SelectContactActivity;

import java.util.ArrayList;

/**
 * @author quadro
 * @since 7/1/12 4:37 PM
 */
public final class ChatsFragment extends AsyncListFragment implements View.OnClickListener, TextWatcher, Runnable {
    public static final int SELECT_CONTACTS = 5679;

    private AsyncListHelper<String, Bitmap, ImageView> downloader;
    private final long me = Init.getUserId();
    private View btnImg;
    private TextView search;

    public ChatsFragment() {
        super(true, Constants.LOADER_CHATS);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        q = null;
        float thumb = UiTools.dpToPix(60, VKMessenger.getCtx());
        downloader = new AsyncListHelper<String, Bitmap, ImageView>(
                new ThumbnailAsyncListHandler((int) thumb, R.drawable.thumb_mask, R.drawable.no_photo, true), new StaticSoftCache(30), R.id.download_task);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case SELECT_CONTACTS: {
                if (resultCode == Activity.RESULT_OK) {
                    if (data.hasExtra("userId")) {
                        final long id = data.getLongExtra("userId", 0);
                        new ModernAsyncTask<Void, Void, Long>() {
                            @Override
                            protected Long doInBackground(Void... params) {
                                Long dialogId = DatabaseTools.fetchLONGAndClose(VKContentProvider.q(
                                        VKContentProvider.CONTENT_URI_DIALOG, Queries._ID_ONLY, Queries.SELECTION_USER_ID,
                                        new String[]{Long.toString(id)}, null));

                                if (dialogId == null) {
                                    final ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>(1);
                                    operations.add(ContentProviderOperation.newInsert(VKContentProvider.CONTENT_URI_DIALOG)
                                            .withValue(Tables.Columns.USER_ID, id)
                                            .withValue(Tables.Columns.TITLE, "...")
                                            .build());

                                    final ContentProviderResult[] r = VKContentProvider.b(operations);
                                    return ContentUris.parseId(r[0].uri);
                                }

                                return dialogId;
                            }

                            @Override
                            protected void onPostExecute(Long aLong) {
                                super.onPostExecute(aLong);
                                getHost().onDialogSelect(aLong, id, 0);
                            }
                        }.execute();
                    } else if (data.hasExtra("dialogId")) {
                        getHost().onDialogSelect(data.getLongExtra("dialogId", 0), 0, data.getLongExtra("chatId", 0));
                    }
                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.chats, null);
        final View back = v.findViewById(R.id.back);
        back.setVisibility(View.INVISIBLE);
        btnImg = v.findViewById(R.id.img);
        btnImg.setOnClickListener(this);

//        VKMessenger.getHandler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                Debug.startMethodTracing("/sdcard/vkmsg", 20000000);
//                VKMessenger.getHandler().postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        Debug.stopMethodTracing();
//                    }
//                }, 10000);
//            }
//        }, 7000);
        return v;
    }

    @Override
    public void setListAdapter(ListAdapter adapter) {
        if (getListAdapter() == null) {
            final View v = View.inflate(getActivity(), R.layout.search_header, null);
            search = (TextView) v.findViewById(R.id.search);
            search.addTextChangedListener(this);
            getListView().addHeaderView(v);
        }

        super.setListAdapter(adapter);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    @Override
    public void afterTextChanged(Editable editable) {
        VKMessenger.getHandler().removeCallbacks(this);
        VKMessenger.getHandler().postDelayed(this, 500);
    }

    private static String q = null;

    public static String getQ() {
        return q;
    }

    public static boolean eq(Object o1, Object o2) {
        return o1 == null && o2 == null || o1 != null && o1.equals(o2);
    }

    @SuppressWarnings("unchecked")
    public void setQ(final String q) {
        if (!eq(q, this.q)) {
            this.q = q;

            if (q != null) {
                Loading.setSearchDialogs(State.START);
                new SearchMessagesTask(q).execute();
                getLoaderManager().restartLoader(Constants.LOADER_CHATS, null, this);
            } else {
                getLoaderManager().restartLoader(Constants.LOADER_CHATS, null, this);
            }
        }
    }

    @Override
    public void run() {
        String trim = search.getText().toString().trim();
        if (trim.length() != 0) {
            if (!trim.startsWith("%")) {
                trim = "%" + trim;
            }
            if (!trim.endsWith("%")) {
                trim = trim + "%";
            }

            setQ(trim);
        } else {
            setQ(null);
        }
    }


    @Override
    protected ContentDescriptor getDescriptor() {
        if (q == null) {
            return new ContentDescriptor(
                    VKContentProvider.CONTENT_URI_DIALOG,
                    VKContentProvider.CONTENT_URI_DIALOG,
                    Queries.DIALOG_PROJECTION,
                    Queries.SELECTION_WITH_MESSAGE,
                    null, Queries.DT_DESC
            );
        } else {
            return new ContentDescriptor(
                    VKContentProvider.CONTENT_URI_SEARCH_DIALOG,
                    VKContentProvider.CONTENT_URI_SEARCH_DIALOG,
                    Queries.DIALOG_PROJECTION,
                    null,
                    null, Queries.DT_DESC
            );
        }
    }

    @Override
    protected View newView(Context context, Cursor cursor, ViewGroup parent) {
        final long id = cursor.getLong(0);
        if (id <= 0) {
            return View.inflate(VKMessenger.getCtx(), R.layout.loading_row, null);
        } else {
            final View v = View.inflate(VKMessenger.getCtx(), R.layout.chat_row, null);
            v.setTag(new Holder(
                    (ImageView) v.findViewById(R.id.img),
                    (TextView) v.findViewById(R.id.user_name),
                    (TextView) v.findViewById(R.id.msg_time),
                    (TextView) v.findViewById(R.id.msg_body),
                    v.findViewById(R.id.img_out),
                    v.findViewById(R.id.img_status)));
            return v;
        }
    }

    @Override
    protected void bindView(View view, Context context, Cursor c) {
        final long id = c.getLong(0);
        if (id <= 0) {
            return;
        }

        Holder h = (Holder) view.getTag();

        final long dt = c.getLong(Queries.DialogColumns.DT);
        final long chatId = c.getLong(Queries.DialogColumns.CHAT_ID);
        final String firstName = c.getString(Queries.DialogColumns.FIRST_NAME);
        final String lastName = c.getString(Queries.DialogColumns.LAST_NAME);
        final String title = c.getString(Queries.DialogColumns.TITLE);
        String body = c.getString(Queries.DialogColumns.BODY);
        final long writerId = c.getLong(Queries.DialogColumns.WRITER_ID);
        final int localStatus = c.getInt(Queries.DialogColumns.LOCAL_STATUS);
        final long messageId = c.getLong(Queries.DialogColumns.MESSAGE_ID);

        if (writerId == me) {
            h.imgOut.setVisibility(View.VISIBLE);
            h.imgStatus.setVisibility(View.VISIBLE);

            if (messageId > 1000000000000l) {
                h.imgStatus.setImageResource(R.drawable.clock);
            } else if (localStatus == 1) {
                h.imgStatus.setImageResource(R.drawable.read);
            } else {
                h.imgStatus.setImageResource(R.drawable.unread);
            }

            view.setBackgroundResource(R.drawable.normal_msg_bg);
        } else {
            h.imgOut.setVisibility(View.INVISIBLE);
            h.imgStatus.setVisibility(View.INVISIBLE);
            view.setBackgroundResource(localStatus == 1 ? R.drawable.normal_msg_bg : R.drawable.unread_msg_bg);
        }

        if (chatId != 0) {
            if (writerId != me) {
                final String wFirstName = c.getString(Queries.DialogColumns.WRITER_FIRST_NAME);
                final String wLastName = c.getString(Queries.DialogColumns.WRITER_LAST_NAME);
                body = wFirstName + " " + wLastName + "\n" + body;
            }

            h.user.setText(" " + title);
            h.body.setText(body);
            h.user.setCompoundDrawablesWithIntrinsicBounds(R.drawable.multichat, 0, 0, 0);
        } else {
            h.user.setText(firstName + " " + lastName);
            int online = c.getInt(Queries.DialogColumns.ONLINE);
            h.user.setCompoundDrawablesWithIntrinsicBounds(0, 0, online == 1 ? R.drawable.online_list : 0, 0);
            if (title != null && !title.contains("...")) {
                h.body.setText(title);
            } else {
                if (body == null)
                    h.body.setText("");
                else
                    h.body.setText(body);
            }
        }

        h.time.setText(DateTools.formatDialogDate(context, dt * 1000, R.string.yesterday));

        final String photo = c.getString(Queries.DialogColumns.PHOTO);
        if (chatId == 0) {
            if (photo != null && photo.length() != 0)
                downloader.download(photo, id, h.img);
            else {
                h.img.setImageResource(R.drawable.no_photo);
                downloader.cancelDownload(id, null, h.img);
            }
        } else {
            String userIds = c.getString(Queries.DialogColumns.USER_IDS);
            downloader.download("chat://" + userIds, id, h.img);
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int i) {
        if (downloader != null) {
            downloader.setScrolling(i != AbsListView.OnScrollListener.SCROLL_STATE_IDLE);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void tryLoadStart() {
        if (q == null && getActivity() != null && !getActivity().isFinishing() && !Flags.isDialogListLoaded()) {
            new LoadDialogsTask(0, 15).execute();
        }
    }

    @Override
    protected boolean isLoadingStart() {
        return q == null ? Loading.getDialogs() == State.START : Loading.getSearchDialogs() == State.START;
    }

    @SuppressWarnings("unchecked")
    protected void tryLoadEnd() {
        if (q == null && getActivity() != null && !getActivity().isFinishing() &&
                Loading.getDialogs() == State.NONE && !Flags.isDialogListFullyLoaded()) {
            new LoadDialogsTask(getAdapter().getCount(), 15).execute();
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int pos, long id) {
        super.onListItemClick(l, v, pos, id);
        if (id == 0) {
            return;
        }

        pos--;

        Cursor c = (Cursor) getAdapter().getItem(pos);
        if (c != null) {
            final long dialogId = c.getLong(Queries.DialogColumns._ID);
            final long userId = c.getLong(Queries.DialogColumns.USER_ID);
            final long chatId = c.getLong(Queries.DialogColumns.CHAT_ID);
            getHost().onDialogSelect(dialogId, userId, chatId);
        }
    }

    public ChatsFragmentHost getHost() {
        return (ChatsFragmentHost) getActivity();
    }

    @Override
    public void onClick(View view) {
        if (view == btnImg) {
            startActivityForResult(new Intent(getActivity(), SelectContactActivity.class).putExtra("showGroup", true), SELECT_CONTACTS);
        }
    }

    public interface ChatsFragmentHost {
        void onDialogSelect(long dialogId, long userId, long chatId);
    }

    private static class Holder {
        public final ImageView img;
        public final TextView user;
        public final TextView time;
        public final TextView body;
        public final View imgOut;
        public final ImageView imgStatus;

        private Holder(ImageView img, TextView user, TextView time, TextView body, View imgOut, View imgStatus) {
            this.img = img;
            this.user = user;
            this.time = time;
            this.body = body;
            this.imgOut = imgOut;
            this.imgStatus = (ImageView) imgStatus;
        }
    }
}
