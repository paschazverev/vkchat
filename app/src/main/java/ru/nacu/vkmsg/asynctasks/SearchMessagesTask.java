package ru.nacu.vkmsg.asynctasks;

import android.content.ContentProviderOperation;
import android.database.Cursor;
import com.perm.kate.api.Message;
import ru.android.common.db.DatabaseTools;
import ru.android.common.logs.Logs;
import ru.android.common.task.ModernAsyncTask;
import ru.nacu.commons.asynclist.State;
import ru.nacu.vkmsg.Init;
import ru.nacu.vkmsg.VKMessenger;
import ru.nacu.vkmsg.dao.Queries;
import ru.nacu.vkmsg.dao.Tables;
import ru.nacu.vkmsg.dao.VKContentProvider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author quadro
 * @since 7/7/12 6:11 PM
 */
public final class SearchMessagesTask extends ModernAsyncTask<Void, Void, Void> {
    public static final String TAG = "SearchMessagesTask";

    private final String q;

    public SearchMessagesTask(String q) {
        this.q = q;
    }

    public static final String[] CHECK_PROJECTION = {Tables.Columns.USER_ID, Tables.Columns.CHAT_ID};

    @Override
    protected Void doInBackground(Void... params) {
        synchronized (SearchMessagesTask.class) {
            doInBackground0();
        }

        return null;
    }

    private void doInBackground0() {
        boolean success = false;
        try {
            success = load();
        } finally {
            if (!success) {
                Loading.setSearchDialogs(State.NONE);
                VKContentProvider.notifyChange(VKContentProvider.CONTENT_URI_DIALOG);
            }
        }
    }

    private boolean load() {
        long start = System.currentTimeMillis();
        Set<Msg> found = new HashSet<Msg>();
        List<Message> all = new ArrayList<Message>();

        try {
            ArrayList<Message> messages;
            int offset = 0;
            do {
                messages = VKMessenger.getApi().searchMessages(q, offset, 100);
                for (Message message : messages) {
                    found.add(new Msg(message.uid, message.chat_id));
                }

                all.addAll(messages);

                offset += 100;
            } while (messages.size() == 100);

        } catch (Exception e) {
            Logs.d(TAG, e.getMessage(), e);
            return false;
        }

        final Set<Msg> copy = new HashSet<Msg>(found);
        final Set<Msg> toSet = new HashSet<Msg>();
        final String selection = "user_id || ',' || chat_id in (" + DatabaseTools.idsToString(found) + ")";
        final Cursor c = VKContentProvider.q(VKContentProvider.CONTENT_URI_DIALOG, CHECK_PROJECTION, selection, null, null);
        try {
            while (c.moveToNext()) {
                final Msg msg = new Msg(c.getLong(0), c.getLong(1));
                copy.remove(msg);
                toSet.add(msg);
            }
        } finally {
            c.close();
        }

        final ArrayList<ContentProviderOperation> o = new ArrayList<ContentProviderOperation>();
        o.add(ContentProviderOperation.newUpdate(VKContentProvider.CONTENT_URI_DIALOG)
                .withValue(Tables.Columns.SEARCH, null)
                .withSelection(Queries.SELECTION_SEARCH_ONLY, null)
                .build());

        o.add(ContentProviderOperation.newUpdate(VKContentProvider.CONTENT_URI_MESSAGE)
                .withValue(Tables.Columns.SEARCH, null)
                .withSelection(Queries.SELECTION_SEARCH_ONLY, null)
                .build());

        final HashSet<Long> profiles = new HashSet<Long>();
        long me = Init.getUserId();
        Set<Long> availableProfiles = DatabaseTools.fetchLongColumnAndClose(
                VKMessenger.getCtx().getContentResolver().query(
                        VKContentProvider.CONTENT_URI_PROFILE,
                        Queries._ID_ONLY,
                        null, null, null
                )
        );

        for (Msg msg : copy) {
            ArrayList<Message> h = null;
            try {
                h = VKMessenger.getApi().getMessagesHistory(msg.userId, msg.chatId, me, 0l, 1);
            } catch (Exception e) {
                Logs.d(TAG, e.getMessage(), e);
                //ignore
            }

            if (h.size() > 0) {
                Message m = h.get(0);

                if (!availableProfiles.contains(m.uid))
                    profiles.add(m.uid);

                final long writerId;
                String userIds;
                if (m.chat_id != null) {
                    writerId = m.uid;
                    try {
                        final Set<Long> chatUsers = VKMessenger.getApi().getChatUsers(m.chat_id);
                        for (Long chatUser : chatUsers) {
                            if (!availableProfiles.contains(chatUser)) {
                                profiles.add(chatUser);
                            }
                        }
                        userIds = DatabaseTools.idsToString(chatUsers);
                    } catch (Exception e) {
                        Logs.d(TAG, e.getMessage(), e);
                        userIds = null;
                    }
                } else {
                    writerId = m.is_out ? me : m.uid;
                    userIds = null;
                }

                final int read = Integer.valueOf(m.read_state);

                final ContentProviderOperation.Builder builder;
                try {
                    builder = ContentProviderOperation.
                            newInsert(VKContentProvider.CONTENT_URI_MESSAGE)
                            .withValue(Tables.Columns._ID, m.mid)
                            .withValue(Tables.Columns.CHAT_ID, m.chat_id == null ? 0 : m.chat_id)
                            .withValue(Tables.Columns.USER_ID, m.chat_id != null ? 0 : m.uid)
                            .withValue(Tables.Columns.USER_IDS, userIds)
                            .withValue(Tables.Columns.SERVER_STATUS, read)
                            .withValue(Tables.Columns.ATTACHMENT, Attachments.loadAttachmentsInformationWithVideos(m.attachments))
                            .withValue(Tables.Columns.TITLE, m.title)
                            .withValue(Tables.Columns.SEARCH, 1)
                            .withValue(Tables.Columns.BODY, m.body)
                            .withValue(Tables.Columns.WRITER_ID, writerId)
                            .withValue(Tables.Columns.DT, Long.parseLong(m.date));
                } catch (Exception e) {
                    Logs.d(TAG, e.getMessage(), e);
                    return false;
                }

                if (read == 1 || m.is_out) {
                    builder.withValue(Tables.Columns.LOCAL_STATUS, read);
                }

                o.add(builder.build());
            }
        }

        o.add(ContentProviderOperation.newUpdate(VKContentProvider.CONTENT_URI_MESSAGE)
                .withValue(Tables.Columns.SEARCH, null)
                .withSelection(Queries.SELECTION_SEARCH_ONLY, null)
                .build());

        for (Message m : all) {
            long chatId;
            long userId;
            long writerId;
            if (m.chat_id != null) {
                chatId = m.chat_id;
                userId = 0;
                writerId = m.uid;
            } else {
                chatId = 0;
                userId = m.uid;
                writerId = m.is_out ? me : userId;
            }

            final ContentProviderOperation.Builder builder;
            builder = ContentProviderOperation.newInsert(VKContentProvider.CONTENT_URI_MESSAGE)
                    .withValue(Tables.Columns._ID, m.mid)
                    .withValue(Tables.Columns.WRITER_ID, writerId)
                    .withValue(Tables.Columns.USER_ID, userId)
                    .withValue(Tables.Columns.CHAT_ID, chatId)
                    .withValue(Tables.Columns.BODY, m.body)
                    .withValue(Tables.Columns.TITLE, m.title)
                    .withValue(Tables.Columns.DT, Long.parseLong(m.date))
                    .withValue(Tables.Columns.SEARCH, 1)
                    .withValue(Tables.Columns.SERVER_STATUS, m.read_state);

            if (m.is_out || "1".equals(m.read_state)) {
                builder.withValue(Tables.Columns.LOCAL_STATUS, m.read_state);
            }

            o.add(builder.build());
        }

        LoadDialogsTask.loadProfiles(o, profiles);

        final String selectionToSet = "user_id || ',' || chat_id in (" + DatabaseTools.idsToString(toSet) + ")";
        o.add(ContentProviderOperation.newUpdate(VKContentProvider.CONTENT_URI_DIALOG)
                .withValue(Tables.Columns.SEARCH, 1)
                .withSelection(selectionToSet, null)
                .build());

        Loading.setSearchDialogs(State.NONE);
        VKContentProvider.b(o);
        VKContentProvider.notifyChange(VKContentProvider.CONTENT_URI_SEARCH_DIALOG);
        Logs.d(TAG, "time=" + (System.currentTimeMillis() - start));
        return true;
    }

    private static class Msg {
        private final long userId;
        private final long chatId;

        private Msg(Long userId, Long chatId) {
            this.chatId = chatId == null ? 0 : chatId;
            this.userId = this.chatId == 0 ? (userId == null ? 0 : userId) : 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Msg msg = (Msg) o;

            if (chatId != msg.chatId) return false;
            if (userId != msg.userId) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = (int) (userId ^ (userId >>> 32));
            result = 31 * result + (int) (chatId ^ (chatId >>> 32));
            return result;
        }

        @Override
        public String toString() {
            return "'" + userId + "," + chatId + "'";
        }
    }
}
