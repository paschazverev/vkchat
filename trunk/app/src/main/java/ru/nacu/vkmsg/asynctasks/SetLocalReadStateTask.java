package ru.nacu.vkmsg.asynctasks;

import android.content.ContentProviderOperation;
import ru.android.common.logs.Logs;
import ru.android.common.task.ModernAsyncTask;
import ru.nacu.vkmsg.Init;
import ru.nacu.vkmsg.dao.Queries;
import ru.nacu.vkmsg.dao.Tables;
import ru.nacu.vkmsg.dao.VKContentProvider;

import java.util.ArrayList;

/**
 * @author quadro
 * @since 6/28/12 10:59 AM
 */
public final class SetLocalReadStateTask extends ModernAsyncTask<Void, Void, Void> {
    public static final String TAG = "SetLocalReadStateTask";

    private final long dialogId;

    public SetLocalReadStateTask(long dialogId) {
        this.dialogId = dialogId;
    }

    @Override
    protected Void doInBackground(Void... params) {
        Logs.d(TAG, "doInBackground()");

        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>(1);
        operations.add(ContentProviderOperation.newUpdate(VKContentProvider.CONTENT_URI_MESSAGE).
                withSelection(Queries.SELECTION_DIALOG_NOT_MARKED,
                        new String[]{Long.toString(Init.getUserId()), Long.toString(dialogId)}).
                withValue(Tables.Columns.LOCAL_STATUS, 1).build());

        VKContentProvider.b(operations);

        return null;
    }
}
