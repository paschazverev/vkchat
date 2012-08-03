package ru.nacu.vkmsg.ui.chats;

import android.database.ContentObserver;
import android.view.View;
import android.widget.TextView;
import ru.android.common.db.DatabaseTools;
import ru.android.common.task.ModernAsyncTask;
import ru.nacu.vkmsg.R;
import ru.nacu.vkmsg.VKMessenger;
import ru.nacu.vkmsg.dao.VKContentProvider;
import ru.nacu.vkmsg.ui.profiles.ProfileFragment;

/**
 * @author quadro
 * @since 7/2/12 3:28 PM
 */
public final class FriendNumberWatcher {
    public static final String TAG = "FriendNumberWatcher";

    private final TextView extra;

    public final ContentObserver observer = new ContentObserver(VKMessenger.getHandler()) {
        @Override
        public void onChange(boolean selfChange) {
            refresh();
        }
    };

    public static final String[] PROJECTION = new String[]{"count(1)"};
    public static final String SELECTION = "friend = " + ProfileFragment.FRECV;

    @SuppressWarnings("unchecked")
    public void refresh() {
        new ModernAsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                return DatabaseTools.fetchINTAndClose(
                        VKContentProvider.q(VKContentProvider.CONTENT_URI_FRIEND, PROJECTION, SELECTION, null, null)
                );
            }

            @Override
            protected void onPostExecute(Integer integer) {
                super.onPostExecute(integer);
                extra.setVisibility(integer == 0 ? View.GONE : View.VISIBLE);
                extra.setText(Integer.toString(integer));
            }
        }.execute();
    }

    public FriendNumberWatcher(View v) {
        this.extra = (TextView) v.findViewById(R.id.extra);
        v.setTag(this);
    }
}
