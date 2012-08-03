package ru.nacu.vkmsg.ui.chat;

import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.Intent;
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
import java.util.HashSet;

/**
 * @author quadro
 * @since 7/4/12 5:55 PM
 */
public final class CreateChatTask extends ProgressDialogTask implements Serializable {
    public static final String TAG = "CreateChatTask";
    public static final String CREATED_INTENT = "ru.nacu.vkmsg.ChatCreated";

    private final long dialogId;
    private final long[] users;
    private final String title;

    private volatile boolean success;
    private volatile long chatId;

    public CreateChatTask(long dialogId, long[] users, String title) {
        this.dialogId = dialogId;
        this.users = users;
        this.title = title;
    }

    @Override
    public void run(Activity ctx) {
        final HashSet<Long> longs = new HashSet<Long>(users.length);
        for (long user : users) {
            longs.add(user);

        }
        try {
            chatId = VKMessenger.getApi().createChat(longs, title);
        } catch (Exception e) {
            Logs.d(TAG, e.getMessage(), e);
            return;
        }

        final ArrayList<ContentProviderOperation> o = new ArrayList<ContentProviderOperation>(1);
        o.add(ContentProviderOperation.newUpdate(VKContentProvider.CONTENT_URI_DIALOG)
                .withSelection(Queries.SELECTION_ID, new String[]{Long.toString(dialogId)})
                .withValue(Tables.Columns.CHAT_ID, chatId)
                .build());

        VKContentProvider.b(o);
        success = true;
    }

    @Override
    public void onPostExecute(Activity ctx) {
        if (!success) {
            Toast.makeText(ctx, R.string.cant_create_group_chat, Toast.LENGTH_LONG).show();
        } else {
            VKMessenger.getCtx().sendBroadcast(new Intent(CREATED_INTENT).putExtra("dialogId", dialogId).putExtra("chatId", chatId));
        }
    }
}
