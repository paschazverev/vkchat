package ru.nacu.commons.asynclist;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import com.actionbarsherlock.app.SherlockListFragment;
import ru.android.common.lists.ContentDescriptor;
import ru.nacu.vkmsg.R;

/**
 * @author quadro
 * @since 7/1/12 6:03 PM
 */
public abstract class AsyncListFragment extends SherlockListFragment
        implements LoaderManager.LoaderCallbacks<Cursor>, AbsListView.OnScrollListener {

    public static final String TAG = "AsyncListFragment";

    private InternalCursorAdapter adapter;
    private View loading;
    private View empty;

    protected AsyncListFragment(boolean initOnCreate, int loader) {
        this.initOnCreate = initOnCreate;
        this.loader = loader;
    }

    protected abstract ContentDescriptor getDescriptor();

    public InternalCursorAdapter getAdapter() {
        return adapter;
    }

    /**
     * нужно ли делать инициализацию loader'а при старте
     */
    private final boolean initOnCreate;

    private final int loader;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = createAdapter();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loading = view.findViewById(R.id.loading);
        empty = view.findViewById(R.id.empty);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(adapter);

        if (initOnCreate)
            getLoaderManager().initLoader(loader, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        getListView().setOnScrollListener(this);
    }

    protected InternalCursorAdapter createAdapter() {
        return new InternalCursorAdapter();
    }

    protected abstract View newView(Context context, Cursor cursor, ViewGroup parent);

    protected abstract void bindView(View view, Context context, Cursor cursor);

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final ContentDescriptor d = getDescriptor();
        return new CursorLoader(getActivity(), d.uri, d.projection, d.selection, d.selectionArgs, d.orderBy) {
            @Override
            protected void onForceLoad() {
                super.onForceLoad();
                if (loading != null && empty != null) {
                    if (getAdapter().getCount() == 0) {
                        loading.setVisibility(View.VISIBLE);
                        empty.setVisibility(View.GONE);
                    } else {
                        loading.setVisibility(View.GONE);
                        empty.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public Cursor loadInBackground() {
                tryLoadStart();
                return super.loadInBackground();
            }
        };
    }

    protected abstract boolean isLoadingStart();

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (loading != null && empty != null) {
            if (data.getCount() == 0) {
                loading.setVisibility(isLoadingStart() ? View.VISIBLE : View.GONE);
                empty.setVisibility(isLoadingStart() ? View.GONE : View.VISIBLE);
            } else {
                loading.setVisibility(View.GONE);
                empty.setVisibility(View.GONE);
            }
        }

        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader loader) {
        adapter.swapCursor(null);
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int i) {
    }

    @Override
    public void onScroll(AbsListView listView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        final int count = adapter.getCount();
        if (listView.isStackFromBottom()) {
            if (firstVisibleItem < 5 && count > 0) {
                tryLoadEnd();
            }
        } else {
            if (visibleItemCount > 0 && (firstVisibleItem + visibleItemCount) >= (count - 5) && (count > 0)) {
                tryLoadEnd();
            }
        }
    }

    protected abstract void tryLoadStart();

    protected abstract void tryLoadEnd();

    protected int getFirstItemPos() {
        final View lv = getListView().getChildAt(0);
        return lv != null ? lv.getTop() : 0;
    }

    protected class InternalCursorAdapter extends CursorAdapter {
        public InternalCursorAdapter() {
            super(getActivity(), null, 0);
        }

        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public Cursor getItem(int position) {
            return (Cursor) super.getItem(position);
        }

        @Override
        public int getItemViewType(int position) {
            final Cursor c = getItem(position);
            return c.getLong(0) == 0 ? 0 : 1;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return AsyncListFragment.this.newView(context, cursor, parent);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            AsyncListFragment.this.bindView(view, context, cursor);
        }
    }
}
