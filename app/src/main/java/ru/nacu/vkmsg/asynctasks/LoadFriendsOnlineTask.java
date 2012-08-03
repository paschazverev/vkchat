package ru.nacu.vkmsg.asynctasks;

import android.content.ContentProviderOperation;
import ru.android.common.db.DatabaseTools;
import ru.android.common.logs.Logs;
import ru.android.common.task.ModernAsyncTask;
import ru.nacu.vkmsg.Init;
import ru.nacu.vkmsg.VKMessenger;
import ru.nacu.vkmsg.dao.Tables;
import ru.nacu.vkmsg.dao.VKContentProvider;

import java.util.ArrayList;

/**
 * @author quadro
 * @since 7/2/12 10:35 AM
 */
public final class LoadFriendsOnlineTask extends ModernAsyncTask<Void, Void, Void> {
    public static final String TAG = "LoadFriendsOnlineTask";

    @Override
    protected Void doInBackground(Void... params) {
        long start = System.currentTimeMillis();
        ArrayList<Long> friends;
        try {
            friends = VKMessenger.getApi().getOnlineFriends(null);
        } catch (Exception e) {
            Logs.d(TAG, e.getMessage(), e);
            return null;
        }

        final ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

        operations.add(ContentProviderOperation.newUpdate(VKContentProvider.CONTENT_URI_PROFILE)
                .withValue(Tables.Columns.ONLINE, 0)
                .build());

        operations.add(ContentProviderOperation.newUpdate(VKContentProvider.CONTENT_URI_PROFILE)
                .withSelection("_id in (" + DatabaseTools.idsToString(friends) + ")", null)
                .withValue(Tables.Columns.ONLINE, 1)
                .build());

        VKContentProvider.b(operations);

        if (Logs.enabled) {
            Logs.d(TAG, "doInBackground time=" + (System.currentTimeMillis() - start));
        }
        return null;
    }
}
