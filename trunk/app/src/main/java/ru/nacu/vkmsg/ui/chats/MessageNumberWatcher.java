package ru.nacu.vkmsg.ui.chats;

import android.database.ContentObserver;
import android.view.View;
import android.widget.TextView;
import ru.android.common.db.DatabaseTools;
import ru.android.common.task.ModernAsyncTask;
import ru.nacu.vkmsg.Init;
import ru.nacu.vkmsg.R;
import ru.nacu.vkmsg.VKMessenger;
import ru.nacu.vkmsg.dao.VKContentProvider;

/**
 * @author quadro
 * @since 7/2/12 3:28 PM
 */
public final class MessageNumberWatcher {
    public static final String TAG = "MessageNumberWatcher";

    private final TextView extra;

    public final ContentObserver observer = new ContentObserver(VKMessenger.getHandler()) {
        @Override
        public void onChange(boolean selfChange) {
            refresh();
        }
    };

    public static final String[] PROJECTION = new String[]{"count(1)"};
    public static final String SELECTION = "writer_id <> ? and local_status <> 1 and server_status <> 1";

    @SuppressWarnings("unchecked")
    public void refresh() {
        new ModernAsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                return DatabaseTools.fetchINTAndClose(
                        VKContentProvider.q(VKContentProvider.CONTENT_URI_MESSAGE, PROJECTION, SELECTION,
                                new String[]{Long.toString(Init.getUserId())}, null)
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

    public MessageNumberWatcher(View v) {
        this.extra = (TextView) v.findViewById(R.id.extra);
        v.setTag(this);
    }
}
