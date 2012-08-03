package ru.nacu.vkmsg.asynctasks;

import android.content.ContentProviderOperation;
import android.content.ContentUris;
import com.perm.kate.api.Message;
import ru.android.common.db.DatabaseTools;
import ru.android.common.logs.Logs;
import ru.android.common.task.ModernAsyncTask;
import ru.nacu.commons.asynclist.State;
import ru.nacu.vkmsg.Flags;
import ru.nacu.vkmsg.Init;
import ru.nacu.vkmsg.VKMessenger;
import ru.nacu.vkmsg.dao.Queries;
import ru.nacu.vkmsg.dao.Tables;
import ru.nacu.vkmsg.dao.VKContentProvider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * @author quadro
 * @since 6/24/12 6:52 PM
 */
public final class LoadMessagesTask extends ModernAsyncTask<Void, Void, Void> {
    public static final String TAG = "LoadMessagesTask";

    private final int count;
    private final long offset;
    private final long userId;
    private final long chatId;
    private final long dialogId;

    public LoadMessagesTask(int count, long offset, long userId, long chatId, long dialogId) {
        this.count = count;
        this.offset = offset;
        this.userId = userId;
        this.chatId = chatId;
        this.dialogId = dialogId;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        boolean success = false;
        try {
            success = load();
        } finally {
            if (!success) {
                Loading.setDialog(dialogId, State.NONE);
                VKContentProvider.notifyChange(ContentUris.withAppendedId(VKContentProvider.CONTENT_URI_DIALOG, dialogId));
            }
        }

        return null;
    }

    private boolean load() {
        Logs.d(TAG, "doInBackground()");
        Loading.setDialog(dialogId, offset == 0 ? State.START : State.END);
        VKMessenger.getCtx().getContentResolver().notifyChange(
                ContentUris.withAppendedId(VKContentProvider.CONTENT_URI_MESSAGE, dialogId), null);

        if (!Flags.isDialogLoaded(dialogId)) {
            Flags.setDialogLoaded(dialogId);
        }

        long me = Init.getUserId();
        ArrayList<Message> history;
        try {
            history = VKMessenger.getApi().getMessagesHistory(userId, chatId, me, offset, count);
        } catch (Exception e) {
            Logs.d(TAG, e.getMessage(), e);
            return false;
        }

        final ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        Set<Long> availableProfiles = DatabaseTools.fetchLongColumnAndClose(
                VKMessenger.getCtx().getContentResolver().query(
                        VKContentProvider.CONTENT_URI_PROFILE,
                        Queries._ID_ONLY,
                        null, null, null
                )
        );

        Set<Long> profiles = new HashSet<Long>();
        for (Message m : history) {
            if (!availableProfiles.contains(m.uid)) {
                profiles.add(m.uid);
            }

            long writerId = m.is_out ? me : m.uid;
            final int read = Integer.valueOf(m.read_state);

            final ContentProviderOperation.Builder builder;
            try {
                builder = ContentProviderOperation.newInsert(VKContentProvider.CONTENT_URI_MESSAGE)
                        .withValue(Tables.Columns._ID, m.mid)
                        .withValue(Tables.Columns.WRITER_ID, writerId)
                        .withValue(Tables.Columns.BODY, m.body)
                        .withValue(Tables.Columns.DT, Long.parseLong(m.date))
                        .withValue(Tables.Columns.DIALOG_ID, dialogId)
                        .withValue(Tables.Columns.ATTACHMENT, Attachments.loadAttachmentsInformationWithVideos(m.attachments))
                        .withValue(Tables.Columns.SERVER_STATUS, read);
            } catch (Exception e) {
                Logs.d(TAG, e.getMessage(), e);
                return false;
            }

            if (m.is_out || read == 1) {
                builder.withValue(Tables.Columns.LOCAL_STATUS, read);
            }

            operations.add(
                    builder
                            .build()
            );
        }

        LoadDialogsTask.loadProfiles(operations, profiles);
        Loading.setDialog(dialogId, State.NONE);
        if (operations.size() > 0) {
            VKContentProvider.b(operations);
        } else {
            VKMessenger.getCtx().getContentResolver().notifyChange(ContentUris.withAppendedId(
                    VKContentProvider.CONTENT_URI_MESSAGE, dialogId), null);
        }

        if (history.size() < count) {
            Flags.setDialogFullyLoaded(dialogId);
        }

        return true;
    }
}
