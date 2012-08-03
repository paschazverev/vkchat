package ru.nacu.vkmsg.ui.contacts;

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
import ru.android.common.lists.ContentDescriptor;
import ru.android.common.task.ModernAsyncTask;
import ru.nacu.commons.StaticSoftCache;
import ru.nacu.commons.asynclist.AsyncListFragment;
import ru.nacu.commons.asynclist.State;
import ru.nacu.vkmsg.Constants;
import ru.nacu.vkmsg.Init;
import ru.nacu.vkmsg.R;
import ru.nacu.vkmsg.VKMessenger;
import ru.nacu.vkmsg.asynctasks.Loading;
import ru.nacu.vkmsg.dao.Queries;
import ru.nacu.vkmsg.dao.Tables;
import ru.nacu.vkmsg.dao.VKContentProvider;
import ru.nacu.vkmsg.ui.ThumbnailAsyncListHandler;
import ru.nacu.vkmsg.ui.chat.GroupChatManagerActivity;

import java.util.ArrayList;

/**
 * @author quadro
 * @since 7/2/12 5:46 PM
 */
public final class SelectContactsFragment extends AsyncListFragment implements
        View.OnClickListener, TextWatcher, Runnable {

    public static final int CREATE_GROUP = 1;

    public static final String[] PROJECTION = new String[]{
            Tables.Columns._ID, Tables.Columns.FIRST_NAME, Tables.Columns.LAST_NAME, Tables.Columns.ONLINE,
            Tables.Columns.PHOTO, Tables.Columns.POP};


    private AsyncListHelper<String, Bitmap, ImageView> downloader;

    public static final int _ID = 0;
    public static final int FIRST_NAME = 1;
    public static final int LAST_NAME = 2;
    public static final int ONLINE = 3;
    public static final int PHOTO = 4;
    public static final int POP = 5;

    private static final String ORDER_BY = Tables.Columns.POP + " desc, " + Tables.Columns.FIRST_NAME + ", " + Tables.Columns.LAST_NAME;
    private View btnGroup;
    private EditText search;

    public SelectContactsFragment() {
        super(true, Constants.LOADER_SELECT_CONTACTS);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        float thumb = UiTools.dpToPix(60, VKMessenger.getCtx());
        downloader = new AsyncListHelper<String, Bitmap, ImageView>(
                new ThumbnailAsyncListHandler((int) thumb, R.drawable.thumb_mask, R.drawable.no_photo, true), new StaticSoftCache(30), R.id.download_task);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (getActivity().getIntent().hasExtra("showGroup")) {
            final View header = View.inflate(getActivity(), R.layout.create_group_header, null);
            getListView().addHeaderView(header);
            btnGroup = header.findViewById(R.id.btn_create_group);
            btnGroup.setOnClickListener(this);
        }

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.select_contacts, null);

        search = (EditText) v.findViewById(R.id.search);
        search.addTextChangedListener(this);

        return v;
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

    private String q = null;

    public static boolean eq(Object o1, Object o2) {
        return o1 == null && o2 == null || o1 != null && o1.equals(o2);
    }

    public void setQ(String q) {
        if (!eq(q, this.q)) {
            this.q = q;
            getLoaderManager().restartLoader(Constants.LOADER_SELECT_CONTACTS, null, this);
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
            View v = View.inflate(context, R.layout.contact_row, null);
            v.setTag(new Holder(
                    (ImageView) v.findViewById(R.id.ava),
                    v.findViewById(R.id.online),
                    (TextView) v.findViewById(R.id.name),
                    (TextView) v.findViewById(R.id.group_label)));

            return v;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onListItemClick(ListView l, View v, int position, final long id) {
        super.onListItemClick(l, v, position, id);
        getHost().onUserSelect(id);
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

        int online = c.getInt(ONLINE);
        h.online.setVisibility(online == 1 ? View.VISIBLE : View.INVISIBLE);
        int pop = c.getInt(POP);

        boolean firstInGroup = !c.moveToPrevious();
        if (!firstInGroup) {
            final String pFirstName = c.getString(FIRST_NAME);
            final String pLastName = c.getString(LAST_NAME);
            final String pFullName = pFirstName + " " + pLastName;
            final int prevPop = c.getInt(POP);
            firstInGroup = pFullName.charAt(0) != fullName.charAt(0) || pop != prevPop;
        }

        if (firstInGroup && pop == 0) {
            h.groupLabel.setVisibility(View.VISIBLE);
            h.groupLabel.setText(fullName.charAt(0) + "");
        } else {
            h.groupLabel.setVisibility(View.GONE);
        }
    }

    @Override
    protected ContentDescriptor getDescriptor() {
        if (q == null) {
            return new ContentDescriptor(
                    VKContentProvider.CONTENT_URI_FRIEND, VKContentProvider.CONTENT_URI_FRIEND,
                    PROJECTION, Queries.SELECTION_FRIENDS, null, ORDER_BY
            );
        } else {
            return new ContentDescriptor(
                    VKContentProvider.CONTENT_URI_FRIEND, VKContentProvider.CONTENT_URI_FRIEND,
                    PROJECTION, Queries.SELECTION_FRIENDS_FILTER, new String[]{q}, ORDER_BY
            );
        }
    }

    @Override
    protected void tryLoadStart() {
    }

    protected boolean isLoadingStart() {
        return Loading.getFriends() != State.NONE;
    }

    @Override
    protected void tryLoadEnd() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onClick(View view) {
        if (view == btnGroup) {
            new ModernAsyncTask<Void, Void, Long>() {
                @Override
                protected Long doInBackground(Void... params) {
                    final ArrayList<ContentProviderOperation> o = new ArrayList<ContentProviderOperation>();
                    o.add(ContentProviderOperation.newInsert(VKContentProvider.CONTENT_URI_DIALOG)
                            .withValue(Tables.Columns.TITLE, getString(R.string.new_group_chat))
                            .withValue(Tables.Columns.USER_IDS, Long.toString(Init.getUserId()))
                            .build());

                    final ContentProviderResult[] res = VKContentProvider.b(o);
                    return ContentUris.parseId(res[0].uri);
                }

                @Override
                protected void onPostExecute(Long newId) {
                    super.onPostExecute(newId);
                    startActivityForResult(
                            new Intent(getActivity(), GroupChatManagerActivity.class)
                                    .putExtra("dialogId", newId)
                                    .putExtra("chatId", 0),
                            CREATE_GROUP
                    );

                }
            }.execute();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CREATE_GROUP) {
            if (resultCode == Activity.RESULT_OK) {
                getHost().onGroupSelect(data);
            }
        }
    }

    private static class Holder {
        private final ImageView ava;
        private final View online;
        private final TextView name;
        private final TextView groupLabel;

        private Holder(ImageView ava, View online, TextView name, TextView groupLabel) {
            this.ava = ava;
            this.online = online;
            this.name = name;
            this.groupLabel = groupLabel;
        }
    }

    public SelectContactsHost getHost() {
        return (SelectContactsHost) getActivity();
    }

    public interface SelectContactsHost {
        void onUserSelect(long userId);

        void onGroupSelect(Intent data);
    }
}
