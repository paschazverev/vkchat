package ru.nacu.vkmsg.ui.profiles;

import android.app.Activity;
import android.content.ContentProviderOperation;
import android.widget.Toast;
import ru.android.common.logs.Logs;
import ru.nacu.vkmsg.R;
import ru.nacu.vkmsg.VKMessenger;
import ru.nacu.vkmsg.dao.Queries;
import ru.nacu.vkmsg.dao.Tables;
import ru.nacu.vkmsg.dao.VKContentProvider;
import ru.nacu.vkmsg.ui.progress.ProgressDialogTask;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * @author quadro
 * @since 7/5/12 9:48 PM
 */
public final class RemoveFriendTask extends ProgressDialogTask implements Serializable {
    public static final String TAG = "RemoveFriendTask";

    private final long profileId;

    public RemoveFriendTask(long profileId) {
        this.profileId = profileId;
    }

    @Override
    public void run(Activity ctx) {
        try {
            VKMessenger.getApi().deleteFriend(profileId);
        } catch (Exception e) {
            Logs.d(TAG, e.getMessage(), e);
            return;
        }

        final ArrayList<ContentProviderOperation> o = new ArrayList<ContentProviderOperation>();
        o.add(ContentProviderOperation.newUpdate(VKContentProvider.CONTENT_URI_PROFILE)
                .withValue(Tables.Columns.FRIEND, 0)
                .withSelection(Queries.SELECTION_ID, new String[]{Long.toString(profileId)})
                .build());
        VKContentProvider.b(o);
        success = true;
    }

    @Override
    public void onPostExecute(Activity ctx) {
        if (!success) {
            Toast.makeText(ctx, R.string.unexpected_error, Toast.LENGTH_LONG).show();
        }
    }
}
