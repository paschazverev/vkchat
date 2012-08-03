package ru.nacu.vkmsg.notification;

import android.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.database.Cursor;
import ru.android.common.logs.Logs;
import ru.android.common.task.ModernAsyncTask;
import ru.nacu.vkmsg.VKMessenger;
import ru.nacu.vkmsg.dao.Queries;
import ru.nacu.vkmsg.dao.VKContentProvider;
import ru.nacu.vkmsg.model.Msg;
import ru.nacu.vkmsg.ui.chat.ChatFragment;

import java.util.HashSet;
import java.util.Set;

/**
 * @author quadro
 * @since 6/28/12 2:40 PM
 */
public class Notifications {
    public static final String TAG = "Notifications";

    public static final int NOTIFICATION_INCOMING_MESSAGE = 1000;

    public static final Set<Msg> incoming = new HashSet<Msg>();

    private static class UpdateNotificationTask extends ModernAsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            synchronized (Notifications.class) {
                Logs.d(TAG, "updateNotification " + incoming);
                final NotificationManager nm = (NotificationManager) VKMessenger.getCtx().getSystemService(Context.NOTIFICATION_SERVICE);

                if (incoming.size() > 0) {
                    final Set<Long> dialogs = new HashSet<Long>();
                    for (Msg msg : incoming) {
                        dialogs.add(msg.dialogId);
                    }

                    if (dialogs.size() == 1) {
                        final String message = incoming.size() == 1 ? incoming.iterator().next().body :
                                (VKMessenger.getCtx().getString(ru.nacu.vkmsg.R.string.you_have_new_messages) + " " + incoming.size());

                        final Long dialogId = dialogs.iterator().next();
                        final Cursor c = VKMessenger.getCtx().getContentResolver().query(
                                VKContentProvider.CONTENT_URI_DIALOG,
                                Queries.DIALOG_PROJECTION,
                                Queries.SELECTION_ID,
                                new String[]{Long.toString(dialogId)},
                                null
                        );

                        String s;
                        long userId = 0;
                        long chatId = 0;

                        try {
                            if (c.moveToNext()) {
                                chatId = c.getLong(Queries.DialogColumns.CHAT_ID);
                                userId = c.getLong(Queries.DialogColumns.USER_ID);
                                if (chatId != 0) {
                                    s = c.getString(Queries.DialogColumns.TITLE);
                                } else {
                                    s = c.getString(Queries.DialogColumns.FIRST_NAME) + " "
                                            + c.getString(Queries.DialogColumns.LAST_NAME);
                                }
                            } else {
                                s = VKMessenger.getCtx().getString(ru.nacu.vkmsg.R.string.app_name);
                            }
                        } finally {
                            c.close();
                        }

                        if (userId != 0 || chatId != 0) {
                            final Notification n = new Notification();
                            n.defaults |= Notification.DEFAULT_SOUND;
                            n.icon = R.drawable.stat_notify_chat;
                            n.setLatestEventInfo(VKMessenger.getCtx(), s, message, VKMessenger.openChat(dialogId, userId, chatId));
                            nm.notify(NOTIFICATION_INCOMING_MESSAGE, n);
                        } else {
                            final Notification n = new Notification();
                            n.defaults |= Notification.DEFAULT_SOUND;
                            n.icon = R.drawable.stat_notify_chat;
                            n.setLatestEventInfo(VKMessenger.getCtx(), s, message, VKMessenger.openChats());
                            nm.notify(NOTIFICATION_INCOMING_MESSAGE, n);
                        }
                    } else {
                        final Notification n = new Notification();
                        n.defaults |= Notification.DEFAULT_SOUND;
                        n.icon = R.drawable.stat_notify_chat;
                        n.setLatestEventInfo(VKMessenger.getCtx(),
                                VKMessenger.getCtx().getString(ru.nacu.vkmsg.R.string.app_name),
                                VKMessenger.getCtx().getString(ru.nacu.vkmsg.R.string.you_have_new_messages) + " " + incoming.size(),
                                VKMessenger.openChats());

                        nm.notify(NOTIFICATION_INCOMING_MESSAGE, n);
                    }
                } else {
                    nm.cancel(NOTIFICATION_INCOMING_MESSAGE);
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    public static boolean add(Msg msg) {
        if (ChatFragment.isPaused() || ChatFragment.getDialogId() != msg.dialogId) {
            if (Logs.enabled)
                Logs.d(TAG, "add() " + msg.id);
            incoming.add(msg);
            return true;
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    public static boolean remove(long id) {
        if (Logs.enabled) {
            Logs.d(TAG, "remove(" + id + "); incoming: " + incoming);
        }
        if (incoming.remove(new Msg(id, 0, null))) {
            if (Logs.enabled)
                Logs.d(TAG, "remove() " + id);

            return true;
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    public static void update() {
        new UpdateNotificationTask().execute();
    }
}
