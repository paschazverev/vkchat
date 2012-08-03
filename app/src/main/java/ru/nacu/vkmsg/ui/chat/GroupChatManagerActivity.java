package ru.nacu.vkmsg.ui.chat;

import android.content.ContentUris;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import ru.android.common.logs.Logs;
import ru.nacu.vkmsg.Constants;
import ru.nacu.vkmsg.R;
import ru.nacu.vkmsg.dao.Queries;
import ru.nacu.vkmsg.dao.Tables;
import ru.nacu.vkmsg.dao.VKContentProvider;
import ru.nacu.vkmsg.updates.LongPoll;

/**
 * @author quadro
 * @since 7/4/12 11:56 AM
 */
public final class GroupChatManagerActivity extends SherlockFragmentActivity implements View.OnClickListener,
        LoaderManager.LoaderCallbacks<GroupChatManagerActivity.LoadResult> {

    public static final String TAG = "GroupChatManagerActivity";

    private GroupChatManagerFragment list;
    private long chatId;
    private long dialogId;
    private TextView title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.group_members_activity);
        list = (GroupChatManagerFragment) getSupportFragmentManager().findFragmentById(R.id.list);
        chatId = getIntent().getLongExtra("chatId", 0);
        dialogId = getIntent().getLongExtra("dialogId", 0);

        findViewById(R.id.img).setVisibility(View.INVISIBLE);
        findViewById(R.id.back).setOnClickListener(this);
        title = (TextView) findViewById(R.id.title);

        getSupportLoaderManager().initLoader(Constants.LOADER_GROUP_MEMBERS_TITLE, null, this);
    }

    @Override
    public void onClick(View view) {
        finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        LongPoll.start(false);
    }

    @Override
    public void onPause() {
        super.onPause();
        LongPoll.setTimeoutForStopServer();
    }

    public static final String[] PROJECTION = new String[]{Tables.Columns.USER_IDS, Tables.Columns.TITLE};

    @Override
    public Loader<LoadResult> onCreateLoader(int id, Bundle args) {
        return new ru.nacu.commons.loader.AsyncTaskLoader<LoadResult>(this, ContentUris.withAppendedId(VKContentProvider.CONTENT_URI_DIALOG, dialogId)) {
            @Override
            public LoadResult loadInBackground() {
                Logs.d(TAG, "loadInBackground()");
                String title = null;
                long[] idList;
                final Cursor c = VKContentProvider.q(VKContentProvider.CONTENT_URI_DIALOG, PROJECTION, Queries.SELECTION_ID,
                        new String[]{Long.toString(dialogId)}, null);

                String ids = null;
                try {
                    if (c.moveToNext()) {
                        ids = c.getString(0);
                        title = c.getString(1);
                    }
                } finally {
                    c.close();
                }

                if (ids == null)
                    return null;

                final String[] split = ids.split(",");
                idList = new long[split.length];
                int i = 0;
                for (String s : split) {
                    idList[i] = Long.parseLong(s.trim());
                    i++;
                }

                return new LoadResult(title, idList);
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<LoadResult> loadResultLoader, LoadResult data) {
        list.setData(dialogId, chatId, data != null ? data.users : new long[0], data != null ? data.title : "");
        title.setText(getString(R.string.chat_members) + ": " + data.users.length);
    }

    @Override
    public void onLoaderReset(Loader<LoadResult> loadResultLoader) {
    }

    public static class LoadResult {
        public final String title;
        public final long[] users;

        public LoadResult(String title, long[] users) {
            this.title = title;
            this.users = users;
        }
    }
}
