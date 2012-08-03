package ru.nacu.vkmsg.asynctasks;

import android.content.ContentProviderOperation;
import android.database.Cursor;
import android.util.Log;
import com.perm.kate.api.Photo;
import org.json.JSONObject;
import ru.android.common.db.DatabaseTools;
import ru.android.common.logs.Logs;
import ru.android.common.task.ModernAsyncTask;
import ru.nacu.vkmsg.Init;
import ru.nacu.vkmsg.VKMessenger;
import ru.nacu.vkmsg.dao.Queries;
import ru.nacu.vkmsg.dao.Tables;
import ru.nacu.vkmsg.dao.VKContentProvider;
import ru.nacu.vkmsg.ui.login.CaptchaActivity;
import ru.nacu.vkmsg.ui.settings.UploadProfileImageTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Читает из бд данные, которые нужно отправить и отправляет их (исходящи сообщения, статусы о прочтении)
 *
 * @author quadro
 * @since 6/27/12 8:04 PM
 */
public final class SendInformationTask extends ModernAsyncTask<Void, Void, Void> {
    public static final String TAG = "SendInformationTask";

    public SendInformationTask() {
    }

    private long send(long userId, long chatId, String title, String message, String attachment) {
        try {
            List<String> attachments = null;
            Attachments.Geo geo = null;
            String forwarded = null;
            if (attachment != null && attachment.length() > 0) {
                Attachments a = Attachments.parse(attachment);
                if (a.photos != null) {
                    for (Attachments.Photo photo : a.photos) {
                        final String server = VKMessenger.getApi().photosGetMessagesUploadServer();
                        final String result = UploadProfileImageTask.multipartRequest(server, photo.getBiggestPhoto(), "photo");
                        if (Logs.enabled) {
                            Logs.d(TAG, "upload result: " + result);
                        }

                        final JSONObject js = new JSONObject(result);
                        if (attachments == null) {
                            attachments = new ArrayList<String>();
                        }

                        for (Photo p : VKMessenger.getApi().saveMessagesPhoto(js.getString("server"), js.getString("photo"), js.getString("hash"))) {
                            attachments.add("photo" + p.owner_id + "_" + p.pid);
                        }
                    }
                }

                if (a.messages != null && a.messages.size() > 0) {
                    Set<Long> ids = new HashSet<Long>(a.messages.size());
                    for (Attachments.Forwarded message1 : a.messages) {
                        ids.add(message1.id);
                    }

                    forwarded = DatabaseTools.idsToString(ids);
                }

                if (a.geo != null) {
                    geo = a.geo;
                }
            }

            return Long.parseLong(VKMessenger.getApi().sendMessage(userId, chatId, message, title, attachments,
                    geo != null ? geo.lat + "" : null, geo != null ? geo.lon + "" : null,
                    CaptchaActivity.getCaptchaKey(), CaptchaActivity.getCaptchaSid(), null, forwarded));

        } catch (Exception e) {
            if (Logs.enabled) {
                Logs.d(TAG, e.getMessage(), e);
            } else {
                Log.e(TAG, e.getMessage());
            }

            return 0;
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        synchronized (SendInformationTask.class) {
            return do0();
        }
    }

    private Void do0() {
        final ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>(1);

        if (Logs.enabled) {
            Logs.d(TAG, "doInBackground()");
        }

        final Cursor d = VKMessenger.getCtx().getContentResolver().query(
                VKContentProvider.CONTENT_URI_MESSAGE,
                Queries._ID_ONLY,
                Queries.SELECTION_DELETED,
                null,
                null
        );

        try {
            Set<Long> ids = new HashSet<Long>();
            while (d.moveToNext()) {
                ids.add(d.getLong(0));
            }

            if (ids.size() > 0) {
                boolean deleted = false;
                try {
                    VKMessenger.getApi().deleteMessages(DatabaseTools.idsToString(ids));
                    deleted = true;
                } catch (Exception e) {
                    Logs.d(TAG, e.getMessage(), e);
                    //nothing
                }

                if (deleted) {
                    final ArrayList<ContentProviderOperation> o = new ArrayList<ContentProviderOperation>(1);
                    o.add(ContentProviderOperation.newDelete(VKContentProvider.CONTENT_URI_MESSAGE)
                            .withSelection("_id in (" + DatabaseTools.idsToString(ids) + ")", null)
                            .build());
                    VKContentProvider.b(o);
                }
            }
        } finally {
            d.close();
        }

        final Cursor c = VKMessenger.getCtx().getContentResolver().query(
                VKContentProvider.CONTENT_URI_MESSAGE_AND_DIALOG,
                Queries.SEND_PROJECTION,
                Queries.SELECTION_UNSENT_MESSAGES,
                null,
                Tables.Columns._ID
        );

        try {

            while (c.moveToNext()) {
                final long initialMessageId = c.getLong(Queries.SendColumns._ID);
                final long userId = c.getLong(Queries.SendColumns.USER_ID);
                final long chatId = c.getLong(Queries.SendColumns.CHAT_ID);
                final String title = c.getString(Queries.SendColumns.TITLE);
                final String message = c.getString(Queries.SendColumns.BODY);
                final String attachment = c.getString(Queries.SendColumns.ATTACHMENT);

                synchronized (SendInformationTask.class) {
                    long messageId = send(userId, chatId, title, message, attachment);

                    if (messageId != 0) {
                        operations.clear();
                        operations.add(ContentProviderOperation.newUpdate(VKContentProvider.CONTENT_URI_MESSAGE).
                                withSelection(Queries.SELECTION_ID, new String[]{Long.toString(initialMessageId)}).
                                withValue(Tables.Columns._ID, messageId).build()
                        );

                        VKContentProvider.b(operations);
                    } else {
                        VKMessenger.getHandler().removeCallbacks(resend);
                        VKMessenger.getHandler().postDelayed(resend, 60000);
                    }
                }
            }
        } finally {
            c.close();
        }

        /**
         * id сообщений, для которых нужно отправить статус о прочтении
         */
        final Set<Long> messageIds = new HashSet<Long>();
        final Cursor q = VKMessenger.getCtx().getContentResolver().query(
                VKContentProvider.CONTENT_URI_MESSAGE,
                Queries._ID_ONLY,
                Queries.SELECTION_DIALOG_MARKED,
                new String[]{Long.toString(Init.getUserId())},
                null
        );

        try {
            while (q.moveToNext()) {
                messageIds.add(q.getLong(0));
            }
        } finally {
            q.close();
        }

        try {
            if (messageIds.size() != 0) {
                Logs.d(TAG, "marking read: " + messageIds);
                VKMessenger.getApi().markAsNewOrAsRead(messageIds, true);
            }
        } catch (Exception e) {
            VKMessenger.getHandler().removeCallbacks(resend);
            VKMessenger.getHandler().postDelayed(resend, 60000);
            return null;
        }

        if (messageIds.size() > 0) {
            operations.clear();
            operations.add(ContentProviderOperation.newUpdate(VKContentProvider.CONTENT_URI_MESSAGE).
                    withSelection("_id in (" + DatabaseTools.idsToString(messageIds) + ")", null).
                    withValue(Tables.Columns.SERVER_STATUS, 1).
                    build());
            VKContentProvider.b(operations);
        }

        return null;
    }

    private static Runnable resend = new Runnable() {
        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            new SendInformationTask().execute();
        }
    };
}
