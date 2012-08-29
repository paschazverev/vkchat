package ru.nacu.vkmsg.ui.contacts;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
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
import ru.nacu.vkmsg.asynctasks.SyncContactsTask;
import ru.nacu.vkmsg.dao.Queries;
import ru.nacu.vkmsg.dao.Tables;
import ru.nacu.vkmsg.dao.VKContentProvider;
import ru.nacu.vkmsg.ui.ThumbnailAsyncListHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author quadro
 * @since 7/2/12 5:46 PM
 */
public final class ContactsFragment extends AsyncListFragment implements TabHost.OnTabChangeListener,
        DialogReadyListener, SwipeDescriptor, SwipeAction, View.OnClickListener, TextWatcher, Runnable {

    public static final String[] PROJECTION = new String[]{
            Tables.Columns._ID, Tables.Columns.FIRST_NAME, Tables.Columns.LAST_NAME, Tables.Columns.ONLINE, Tables.Columns.PHOTO,
            Tables.Columns.BDATE, Tables.Columns.SEX, Tables.Columns.FRIEND, Tables.Columns.PHONE_NAME, Tables.Columns.POP
    };

    private AsyncListHelper<String, Bitmap, ImageView> downloader;

    public static final int _ID = 0;
    public static final int FIRST_NAME = 1;
    public static final int LAST_NAME = 2;
    public static final int ONLINE = 3;
    public static final int PHOTO = 4;
    public static final int BDATE = 5;
    public static final int SEX = 6;
    public static final int FRIEND = 7;
    public static final int PHONE_NAME = 8;
    public static final int POP = 9;

    private static final String ORDER_BY = Tables.Columns.POP + " desc, " + Tables.Columns.FIRST_NAME + ", " + Tables.Columns.LAST_NAME;

    private TabHost tabs;
    private View btnSync;
    private View textEmpty;
    private View syncEmpty;
    private EditText search;
    private View searchContainer;

    public ContactsFragment() {
        super(true, Constants.LOADER_CONTACTS);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        float thumb = UiTools.dpToPix(60, VKMessenger.getCtx());
        downloader = new AsyncListHelper<String, Bitmap, ImageView>(
                new ThumbnailAsyncListHandler((int) thumb, R.drawable.thumb_mask, R.drawable.no_photo, true), new StaticSoftCache(30), R.id.download_task);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (tabs != null)
            outState.putString("tab", tabs.getCurrentTabTag());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.contacts, null);

        btnSync = v.findViewById(R.id.btn_sync);
        btnSync.setOnClickListener(this);

        textEmpty = v.findViewById(R.id.text_empty);
        syncEmpty = v.findViewById(R.id.sync_empty);

        final SwipeView swipe = (SwipeView) v.findViewById(R.id.swipe);
        swipe.setDescriptor(this);

        final View i1 = View.inflate(getActivity(), R.layout.tab_indicator_contact, null);
        final View i2 = View.inflate(getActivity(), R.layout.tab_indicator_contact, null);
        final View i3 = View.inflate(getActivity(), R.layout.tab_indicator_contact, null);
        setTitle(i1, R.string.friends);
        setTitle(i2, R.string.online);
        setTitle(i3, R.string.contacts);

        tabs = (TabHost) v.findViewById(android.R.id.tabhost);
        tabs.setup();
        tabs.setOnTabChangedListener(this);

        addTab(this, tabs, tabs.newTabSpec("friends").setIndicator(i1));
        addTab(this, tabs, tabs.newTabSpec("online").setIndicator(i2));
        addTab(this, tabs, tabs.newTabSpec("contacts").setIndicator(i3));

        if (savedInstanceState != null && savedInstanceState.containsKey("tab")) {
            tabs.setCurrentTabByTag(savedInstanceState.getString("tab"));
        }

        searchContainer = v.findViewById(R.id.search_container);
        search = (EditText) v.findViewById(R.id.search);
        search.addTextChangedListener(this);
        return v;
    }

    private void setTitle(View v, int title) {
        final TextView tv = (TextView) v.findViewById(R.id.label);
        tv.setText(title);
    }

    private TabState state = TabState.friends;
    private TabState nextState = TabState.friends;

    @Override
    public void onTabChanged(String s) {
        nextState = TabState.valueOf(s);
        getLoaderManager().restartLoader(Constants.LOADER_CONTACTS, null, this);
        try {
            getListView().setSelection(0);
        } catch (Exception e) {
            //ignore
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        state = nextState;
        super.onLoadFinished(loader, data);

        if (state == TabState.contacts) {
            syncEmpty.setVisibility(View.VISIBLE);
            textEmpty.setVisibility(View.GONE);
            searchContainer.setVisibility(data.getCount() == 0 ? View.GONE : View.VISIBLE);
        } else {
            syncEmpty.setVisibility(View.GONE);
            textEmpty.setVisibility(View.VISIBLE);
            searchContainer.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onReady(long dialogId, long userId, long chatId) {
        final Host host = getHost();
        if (host != null)
            host.onDialogSelect(dialogId, userId, chatId);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onClick(View view) {
        if (view == btnSync) {
            new SyncContactsTask().execute();
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
        VKMessenger.getHandler().postDelayed(this, 500);
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

    class TabFactory implements TabHost.TabContentFactory {

        private final Context mContext;

        /**
         * @param context context
         */
        public TabFactory(Context context) {
            mContext = context;
        }

        /**
         * (non-Javadoc)
         *
         * @see android.widget.TabHost.TabContentFactory#createTabContent(java.lang.String)
         */
        public View createTabContent(String tag) {
            View v = new View(mContext);
            v.setMinimumWidth(0);
            v.setMinimumHeight(0);
            return v;
        }
    }

    private static void addTab(ContactsFragment fr, TabHost tabHost, TabHost.TabSpec tabSpec) {
        // Attach a Tab view factory to the spec
        tabSpec.setContent(fr.new TabFactory(VKMessenger.getCtx()));
        tabHost.addTab(tabSpec);
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
        if (id == 0) {
            return View.inflate(context, R.layout.loading_row, null);
        } else {
            View v = View.inflate(context, R.layout.contact_search_row, null);
            v.setTag(new Holder(
                    (ImageView) v.findViewById(R.id.ava),
                    v.findViewById(R.id.online),
                    (TextView) v.findViewById(R.id.name),
                    (TextView) v.findViewById(R.id.group_label),
                    (TextView) v.findViewById(R.id.extra)));

            return v;
        }
    }

    @Override
    protected void bindView(View view, Context context, Cursor c) {
        final long id = c.getLong(_ID);
        if (id == 0)
            return;

        final Holder h = (Holder) view.getTag();
        final String photo = c.getString(PHOTO);
        if (photo != null) {
            downloader.download(photo, id, h.ava);
        } else {
            downloader.cancelDownload(id, null, h.ava);
            h.ava.setImageResource(R.drawable.no_photo);
        }

        final String firstName = c.getString(FIRST_NAME);
        final String lastName = c.getString(LAST_NAME);
        final String fullName;

        if (state == TabState.contacts) {
            fullName = c.getString(PHONE_NAME);
        } else {
            fullName = firstName + " " + lastName;
        }

        h.name.setText(fullName);
        int online = c.getInt(ONLINE);

        if (state == TabState.contacts) {
            if (firstName != null && lastName != null) {
                h.extra.setText(firstName + " " + lastName);
                h.extra.setVisibility(View.VISIBLE);
            } else {
                h.extra.setVisibility(View.GONE);
            }

            h.online.setVisibility(View.GONE);
        } else {
            h.extra.setVisibility(View.GONE);
            h.online.setVisibility(online == 1 ? View.VISIBLE : View.INVISIBLE);
        }

        int pop = c.getInt(POP);

        boolean firstInGroup = !c.moveToPrevious();
        if (!firstInGroup) {
            final String pFullName;
            if (state == TabState.contacts) {
                pFullName = c.getString(PHONE_NAME);
            } else {
                final String pFirstName = c.getString(FIRST_NAME);
                final String pLastName = c.getString(LAST_NAME);
                pFullName = pFirstName + " " + pLastName;
            }

            int prevPop = c.getInt(POP);
            firstInGroup = pFullName.charAt(0) != fullName.charAt(0) || prevPop != pop;
        }

        if (firstInGroup && pop == 0) {
            h.groupLabel.setVisibility(View.VISIBLE);
            h.groupLabel.setText(fullName.charAt(0) + "");
        } else {
            h.groupLabel.setVisibility(View.GONE);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, final long id) {
        super.onListItemClick(l, v, position, id);
        if (state == TabState.contacts) {
            getHost().onProfileShow(id);
        } else {
            writeMessage(id, this);
        }
    }

    @SuppressWarnings("unchecked")
    public static void writeMessage(final long id, final DialogReadyListener listener) {
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
                listener.onReady(aLong, id, 0);
            }
        }.execute();
    }

    private String q = null;

    public static boolean eq(Object o1, Object o2) {
        return o1 == null && o2 == null || o1 != null && o1.equals(o2);
    }

    public void setQ(String q) {
        if (!eq(q, this.q)) {
            this.q = q;
            getLoaderManager().restartLoader(Constants.LOADER_CONTACTS, null, this);
        }
    }

    @Override
    protected ContentDescriptor getDescriptor() {
        switch (nextState) {
            case friends:
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
            case online:
                if (q == null) {
                    return new ContentDescriptor(
                            VKContentProvider.CONTENT_URI_FRIEND, VKContentProvider.CONTENT_URI_FRIEND,
                            PROJECTION, Queries.SELECTION_FRIENDS_ONLINE, null, ORDER_BY
                    );
                } else {
                    return new ContentDescriptor(
                            VKContentProvider.CONTENT_URI_FRIEND, VKContentProvider.CONTENT_URI_FRIEND,
                            PROJECTION, Queries.SELECTION_FRIENDS_ONLINE_FILTER, new String[]{q}, ORDER_BY
                    );
                }
            case contacts:
                if (q == null) {
                    return new ContentDescriptor(
                            VKContentProvider.CONTENT_URI_CONTACT, VKContentProvider.CONTENT_URI_CONTACT,
                            PROJECTION, null, null, Tables.Columns.PHONE_NAME
                    );
                } else {
                    return new ContentDescriptor(
                            VKContentProvider.CONTENT_URI_CONTACT, VKContentProvider.CONTENT_URI_CONTACT,
                            PROJECTION, Queries.SELECTION_FILTER, new String[]{q}, Tables.Columns.PHONE_NAME
                    );
                }
        }

        throw new RuntimeException("Unknown state: " + state);
    }

    @Override
    protected void tryLoadStart() {
    }

    @Override
    protected boolean isLoadingStart() {
        return state == TabState.contacts ? Loading.getContacts() != State.NONE : Loading.getFriends() != State.NONE;
    }

    @Override
    protected void tryLoadEnd() {
    }

    private static class Holder {
        private final ImageView ava;
        private final View online;
        private final TextView name;
        private final TextView groupLabel;
        private final TextView extra;

        private Holder(ImageView ava, View online, TextView name, TextView groupLabel, TextView extra) {
            this.ava = ava;
            this.online = online;
            this.name = name;
            this.groupLabel = groupLabel;
            this.extra = extra;
        }
    }

    public static enum TabState {
        friends,
        online,
        contacts
    }

    public Host getHost() {
        return (Host) getActivity();
    }

    public interface Host {
        void onDialogSelect(long dialogId, long userId, long chatId);

        void onProfileShow(long id);
    }

    @Override
    protected InternalCursorAdapter createAdapter() {
        return new MyAdapter();
    }

    private String[] sections;
    private Map<Integer, Integer> positionToSection;
    private Map<Integer, Integer> sectionToPosition;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (Logs.enabled) {
            Logs.d(TAG, "onCreateLoader()");
        }

        final TabState s = nextState;

        final ContentDescriptor d = getDescriptor();
        return new CursorLoader(getActivity(), d.uri, d.projection, d.selection, d.selectionArgs, d.orderBy) {
            @Override
            public Cursor loadInBackground() {
                if (Logs.enabled) {
                    Logs.d(TAG, "loadInBackground()");
                }

                tryLoadStart();
                final Cursor c = super.loadInBackground();
                List<String> sections = new ArrayList<String>(50);
                Map<Integer, Integer> positionToSection = new HashMap<Integer, Integer>();
                Map<Integer, Integer> sectionToPosition = new HashMap<Integer, Integer>();
                String prev = null;
                int pos = 0;
                while (c.moveToNext()) {
                    final String firstLetter;
                    final int pop = c.getInt(POP);
                    if (pop != 0) {
                        firstLetter = "pop";
                    } else {
                        if (s == TabState.contacts) {
                            firstLetter = Character.toString(c.getString(PHONE_NAME).charAt(0));
                        } else {
                            final String firstName = c.getString(FIRST_NAME);
                            firstLetter = Character.toString(firstName.charAt(0));
                        }
                    }

                    if (!firstLetter.equals(prev)) {
                        sections.add(firstLetter);
                        sectionToPosition.put(sections.size() - 1, pos);
                    }

                    positionToSection.put(pos, sections.size() - 1);
                    pos++;
                }

                ContactsFragment.this.sections = sections.toArray(new String[sections.size()]);
                ContactsFragment.this.positionToSection = positionToSection;
                ContactsFragment.this.sectionToPosition = sectionToPosition;

                return c;
            }
        };
    }

    protected class MyAdapter extends InternalCursorAdapter implements SectionIndexer {
        @Override
        public Object[] getSections() {
            return sections;
        }

        @Override
        public int getPositionForSection(int section) {
            if (sectionToPosition == null) return 0;
            final Integer integer = sectionToPosition.get(section);
            return integer != null ? integer : 0;
        }

        @Override
        public int getSectionForPosition(int pos) {
            if (positionToSection == null) return 0;
            final Integer integer = positionToSection.get(pos);
            return integer != null ? integer : 0;
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
        if (state == TabState.contacts) {
            return Loading.getContacts() == State.NONE && Flags.isContactsSynced();
        } else {
            return Loading.getFriends() == State.NONE;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void execute() {
        if (state == TabState.contacts) {
            new SyncContactsTask().execute();
        } else {
            new LoadFriendsTask().execute();
        }
    }

    @Override
    public int getLabelResource() {
        return state == TabState.contacts ? R.string.sync_contacts : R.string.refresh;
    }

    @Override
    public int getIconResource() {
        return R.drawable.refresh;
    }
}
