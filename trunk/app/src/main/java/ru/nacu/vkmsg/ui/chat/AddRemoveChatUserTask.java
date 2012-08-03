package ru.nacu.vkmsg.ui.chat;

import android.app.Activity;
import android.content.ContentProviderOperation;
import android.widget.Toast;
import ru.android.common.db.DatabaseTools;
import ru.android.common.logs.Logs;
import ru.nacu.vkmsg.R;
import ru.nacu.vkmsg.VKMessenger;
import ru.nacu.vkmsg.dao.Queries;
import ru.nacu.vkmsg.dao.Tables;
import ru.nacu.vkmsg.dao.VKContentProvider;
import ru.nacu.vkmsg.ui.progress.ProgressDialogTask;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Set;

/**
 * @author quadro
 * @since 7/4/12 3:58 PM
 */
public final class AddRemoveChatUserTask extends ProgressDialogTask implements Serializable {
    public static final String TAG = "AddRemoveChatUserTask";

    public final long dialogId;
    public final long chatId;
    public final long userId;

    public final boolean add;
    private volatile boolean success;

    public AddRemoveChatUserTask(long dialogId, long chatId, long userId, boolean add) {
        this.dialogId = dialogId;
        this.chatId = chatId;
        this.userId = userId;
        this.add = add;
    }

    public static final String[] PROJECTION = new String[]{Tables.Columns.USER_IDS};

    @Override
    public void run(Activity ctx) {
        try {
            final String users = DatabaseTools.fetchStringAndClose(VKContentProvider.q(VKContentProvider.CONTENT_URI_DIALOG, PROJECTION, Queries.SELECTION_ID, new String[]{Long.toString(dialogId)}, null));
            final Set<Long> longs = DatabaseTools.parseToSet(users);

            if (add) {
                if (chatId != 0)
                    VKMessenger.getApi().addChatUser(chatId, userId);

                longs.add(userId);
            } else {
                if (chatId != 0)
                    VKMessenger.getApi().removeChatUser(chatId, userId);

                longs.remove(userId);
            }

            final ArrayList<ContentProviderOperation> o = new ArrayList<ContentProviderOperation>();
            o.add(ContentProviderOperation.newUpdate(VKContentProvider.CONTENT_URI_DIALOG)
                    .withValue(Tables.Columns.USER_IDS, DatabaseTools.idsToString(longs))
                    .withSelection(Queries.SELECTION_ID, new String[]{Long.toString(dialogId)})
                    .build());
            VKContentProvider.b(o);

            success = true;
        } catch (Exception e) {
            Logs.d(TAG, e.getMessage(), e);
            //ignore
        }
    }

    @Override
    public void onPostExecute(Activity ctx) {
        if (!success) {
            Toast.makeText(ctx, R.string.error_editing_chat, Toast.LENGTH_LONG).show();
        }
    }
}
