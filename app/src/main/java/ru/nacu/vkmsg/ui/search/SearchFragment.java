package ru.nacu.vkmsg.ui.search;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import ru.android.common.UiTools;
import ru.android.common.asyncloader.AsyncListHelper;
import ru.android.common.lists.ContentDescriptor;
import ru.android.common.logs.Logs;
import ru.android.common.task.ModernAsyncTask;
import ru.nacu.commons.StaticSoftCache;
import ru.nacu.commons.asynclist.AsyncListFragment;
import ru.nacu.commons.asynclist.State;
import ru.nacu.commons.swipe.SwipeAction;
import ru.nacu.commons.swipe.SwipeDescriptor;
import ru.nacu.commons.swipe.SwipeView;
import ru.nacu.vkmsg.Constants;
import ru.nacu.vkmsg.Flags;
import ru.nacu.vkmsg.R;
import ru.nacu.vkmsg.VKMessenger;
import ru.nacu.vkmsg.asynctasks.LoadFriendsTask;
import ru.nacu.vkmsg.asynctasks.Loading;
import ru.nacu.vkmsg.asynctasks.SearchUsersTask;
import ru.nacu.vkmsg.dao.Queries;
import ru.nacu.vkmsg.dao.Tables;
import ru.nacu.vkmsg.dao.VKContentProvider;
import ru.nacu.vkmsg.ui.ThumbnailAsyncListHandler;
import ru.nacu.vkmsg.ui.contacts.ContactsFragment;
import ru.nacu.vkmsg.ui.profiles.ProfileFragment;

import java.util.ArrayList;

import static ru.nacu.vkmsg.ui.contacts.ContactsFragment.*;

/**
 * @author quadro
 * @since 7/2/12 2:03 PM
 */
public final class SearchFragment extends AsyncListFragment implements TextWatcher, Runnable, SwipeDescriptor, SwipeAction {

    private AsyncListHelper<String, Bitmap, ImageView> downloader;

    private EditText etSearch;

    public SearchFragment() {
        super(false, Constants.LOADER_SEARCH);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        float thumb = UiTools.dpToPix(60, VKMessenger.getCtx());
        downloader = new AsyncListHelper<String, Bitmap, ImageView>(
                new ThumbnailAsyncListHandler((int) thumb, R.drawable.thumb_mask, R.drawable.no_photo, true), new StaticSoftCache(30), R.id.download_task);

        getLoaderManager().initLoader(Constants.LOADER_SEARCH, null, this);
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int i) {
        if (downloader != null) {
            downloader.setScrolling(i != AbsListView.OnScrollListener.SCROLL_STATE_IDLE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.search, null);
        final SwipeView swipe = (SwipeView) v.findViewById(R.id.swipe);
        swipe.setDescriptor(this);
        etSearch = (EditText) v.findViewById(R.id.search);
        etSearch.addTextChangedListener(this);
        return v;
    }

    @Override
    protected ContentDescriptor getDescriptor() {
        if (q != null)
            return new ContentDescriptor(VKContentProvider.CONTENT_URI_SEARCH, VKContentProvider.CONTENT_URI_SEARCH,
                    ContactsFragment.PROJECTION, null, null, Tables.Columns.SEARCH);
        else
            return new ContentDescriptor(VKContentProvider.CONTENT_URI_FRIEND, VKContentProvider.CONTENT_URI_FRIEND,
                    ContactsFragment.PROJECTION, Queries.SELECTION_FRIENDS_SUGGESTIONS, null, "friend, _id");
    }

    @Override
    protected View newView(Context context, Cursor c, ViewGroup parent) {
        final long id = c.getLong(_ID);
        if (id <= 0) {
            return View.inflate(context, R.layout.loading_row, null);
        } else {
            View v = View.inflate(context, R.layout.contact_search_row, null);
            v.setTag(new Holder(
                    (ImageView) v.findViewById(R.id.ava),
                    (TextView) v.findViewById(R.id.name),
                    (TextView) v.findViewById(R.id.extra),
                    (TextView) v.findViewById(R.id.group_label))
            );

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

        long bdate = c.getLong(BDATE);
        int sex = c.getInt(SEX);

        if (bdate != 0 || sex != 0) {
            h.extra.setVisibility(View.VISIBLE);
            String html = "";
            if (sex != 0) {
                html += "<b>Пол: </b>" + (sex == 1 ? "жен. " : "муж. ");
            }
            if (bdate != 0) {
                html += "<b>Возраст: </b>" + ((System.currentTimeMillis() / 1000 - bdate) / (86400l * 365l));
            }
            h.extra.setText(Html.fromHtml(html));
        } else {
            h.extra.setVisibility(View.GONE);
        }

        int friend = c.getInt(FRIEND);
        boolean firstInGroup = !c.moveToPrevious();
        if (!firstInGroup) {
            int prevFriend = c.getInt(FRIEND);
            firstInGroup = prevFriend != friend;
        }

        if (firstInGroup && q == null) {
            final int friendString;
            switch (friend) {
                case ProfileFragment.FSUGG: {
                    friendString = R.string.friend_suggestions;
                    break;
                }
                case ProfileFragment.FSENT: {
                    friendString = R.string.friend_sent_req;
                    break;
                }
                default: {
                    friendString = R.string.friend_recv_req;
                    break;
                }
            }

            h.groupLabel.setText(friendString);
            h.groupLabel.setVisibility(View.VISIBLE);
        } else {
            h.groupLabel.setVisibility(View.GONE);
        }
    }

    @Override
    public SwipeAction getLeftAction() {
        return null;
    }

    @Override
    public SwipeAction getRightAction() {
        return null;
    }

    @Override
    public SwipeAction getUpAction() {
        return null;
    }

    @Override
    public SwipeAction getDownAction() {
        return this;
    }

    @Override
    public int getDownArrowResource() {
        return R.drawable.ic_pulltorefresh_arrow;
    }

    @Override
    public void enterReadMode() {
    }

    @Override
    public void enterMoveMode() {
    }

    @Override
    public boolean isBottom() {
        return false;
    }

    @Override
    public boolean isTop() {
        return getAdapter().getCount() == 0 || (getListView().getFirstVisiblePosition() == 0 && getFirstItemPos() == 0);
    }

    @Override
    public boolean isLeft() {
        return false;
    }

    @Override
    public boolean isRight() {
        return false;
    }

    @Override
    public int getTextColor() {
        return Color.BLACK;
    }

    @Override
    public boolean canExecute() {
        return q == null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void execute() {
        new LoadFriendsTask().execute();
    }

    @Override
    public int getLabelResource() {
        return R.string.refresh;
    }

    @Override
    public int getIconResource() {
        return R.drawable.refresh;
    }

    private static class Holder {
        private final ImageView ava;
        private final TextView name;
        private final TextView extra;
        private final TextView groupLabel;

        private Holder(ImageView ava, TextView name, TextView extra, TextView groupLabel) {
            this.ava = ava;
            this.name = name;
            this.extra = extra;
            this.groupLabel = groupLabel;
        }
    }

    private boolean loadedStart = true;

    @SuppressWarnings("unchecked")
    @Override
    protected void tryLoadStart() {
        if (q != null && Loading.getSearch() == State.NONE && !loadedStart) {
            new SearchUsersTask(q, 20, 0).execute();
            loadedStart = true;
        }
    }

    @Override
    protected boolean isLoadingStart() {
        return q != null ? Loading.getSearch() != State.NONE : Loading.getFriends() != State.NONE;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void tryLoadEnd() {
        final int count = getAdapter().getCount();

        if (getActivity() != null && !getActivity().isFinishing() && count != 0 &&
                Loading.getSearch() == State.NONE && !Flags.isSearchFullyLoaded()) {
            new SearchUsersTask(q, 20, count).execute();
        }
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
        VKMessenger.getHandler().postDelayed(this, 1000);
    }

    private String q;

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }

        //отсюда пойдет изменение результатов
        final String currentQ = etSearch.getText().toString();
        if (!currentQ.trim().equals(q)) {
            Logs.d(TAG, "running new search " + currentQ);

            if (currentQ.trim().length() == 0) {
                q = null;
                getLoaderManager().restartLoader(Constants.LOADER_SEARCH, null, this);
                return;
            }

            q = currentQ.trim();

            new ModernAsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    final ArrayList<ContentProviderOperation> o = new ArrayList<ContentProviderOperation>(1);
                    o.add(ContentProviderOperation.newUpdate(VKContentProvider.CONTENT_URI_PROFILE)
                            .withSelection(Queries.SELECTION_SEARCH_ONLY, null)
                            .withValue(Tables.Columns.SEARCH, null)
                            .build());
                    VKContentProvider.b(o);
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    loadedStart = false;
                    getLoaderManager().restartLoader(Constants.LOADER_SEARCH, null, SearchFragment.this);
                }
            }.execute();
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        getHost().onProfileShow(id);
    }

    public Host getHost() {
        return (Host) getActivity();
    }

    public static interface Host {
        void onProfileShow(long id);
    }
}
