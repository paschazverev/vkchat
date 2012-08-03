package ru.nacu.vkmsg.updates;

import android.content.ContentProviderOperation;
import android.content.Intent;
import com.perm.kate.api.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import ru.android.common.db.DatabaseTools;
import ru.android.common.logs.Logs;
import ru.android.common.task.ModernAsyncTask;
import ru.common.StreamTools;
import ru.nacu.vkmsg.Constants;
import ru.nacu.vkmsg.Flags;
import ru.nacu.vkmsg.Init;
import ru.nacu.vkmsg.VKMessenger;
import ru.nacu.vkmsg.asynctasks.Attachments;
import ru.nacu.vkmsg.asynctasks.LoadDialogsTask;
import ru.nacu.vkmsg.asynctasks.LoadFriendsOnlineTask;
import ru.nacu.vkmsg.asynctasks.SendInformationTask;
import ru.nacu.vkmsg.dao.Queries;
import ru.nacu.vkmsg.dao.Tables;
import ru.nacu.vkmsg.dao.VKContentProvider;
import ru.nacu.vkmsg.ui.chat.ChatFragment;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author quadro
 * @since 6/25/12 1:22 PM
 */
public final class LongPoll {
    public static final boolean DEBUG = false;
    public static final String TAG = "LongPoll";

    private static volatile boolean stopping = true;
    private static volatile boolean stopped = true;
    private static volatile boolean restarting = false;

    private static final Runnable stopServer = new Runnable() {
        @Override
        public void run() {
            if (restarting) {
                Logs.d(TAG, "stopped restarting server");
                VKMessenger.getCtx().stopService(new Intent(VKMessenger.getCtx(), LongPollService.class));
                VKMessenger.getHandler().removeCallbacks(restart);
                stopped = true;
            } else {
                stopping = true;
            }
        }
    };

    public static boolean isRunning() {
        return !stopped;
    }

    private static final Runnable restart = new Runnable() {
        @Override
        public void run() {
            start(true);
        }
    };

    @SuppressWarnings("unchecked")
    private static void setOnline() {
        new ModernAsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    VKMessenger.getApi().setOnline();
                } catch (Exception e) {
                    Logs.d(TAG, "setOnline", e);
                }
                return null;
            }
        }.execute();
    }

    @SuppressWarnings("unchecked")
    public static void setActivity(final long uid, final long chatId) {
        new ModernAsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    VKMessenger.getApi().setActivity(uid, chatId);
                } catch (Exception e) {
                    Logs.d(TAG, "setOnline", e);
                }
                return null;
            }
        }.execute();
    }

    /**
     * Стартует сервер, если он еще не стартован
     */
    @SuppressWarnings("unchecked")
    public static void start(final boolean restart) {
        Init.checkPollStart();
        VKMessenger.checkMainThread();

        stopping = false;
        if (stopped || restart) {
            Init.checkPushStart();
            setOnline();
            new LoadFriendsOnlineTask().execute();

            new Thread() {
                @Override
                public void run() {
                    restarting = false;
                    if (!restart) {
                        Logs.d(TAG, "started");
                        VKMessenger.getCtx().startService(new Intent(VKMessenger.getCtx(), LongPollService.class));
                        stopped = false;
                    } else {
                        Logs.d(TAG, "restarted");
                    }

                    boolean success = false;
                    try {
                        success = runServer(false);
                    } finally {
                        if (success) {
                            Logs.d(TAG, "stopped");
                            VKMessenger.getCtx().stopService(new Intent(VKMessenger.getCtx(), LongPollService.class));
                            stopped = true;
                        } else {
                            restarting = true;
                            Logs.d(TAG, "stopped due to an error");
                            VKMessenger.getCtx().sendBroadcast(new Intent(LongPollService.ERROR_INTENT));
                            VKMessenger.getHandler().postDelayed(LongPoll.restart, 10000);
                        }
                    }
                }
            }.start();
        }

        if (!restart)
            VKMessenger.getHandler().removeCallbacks(stopServer);
    }

    public static void setTimeoutForStopServer() {
        VKMessenger.getHandler().postDelayed(stopServer, 5000);
    }

    private static void processHistory(long me, JSONObject history) {
        try {
            processHistory0(me, history);
        } catch (Exception e) {
            Logs.d(TAG, e.getMessage(), e);
        }
    }

    private static void processHistory0(long me, JSONObject history) throws JSONException, IOException, KException {
        final ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

        final JSONArray h = history.getJSONArray("history");
        if (DEBUG) {
            Logs.d(TAG, "history: " + history);
        }
        if (h != null) {
            for (int i = 0; i < h.length(); i++) {
                JSONArray update = h.getJSONArray(i);
                int type = update.getInt(0);
                switch (type) {
                    case 51: {
                        processChatChange(update.getLong(1));
                        break;
                    }

                    case 3: {
                        processFlagRemoval(operations, update);
                        break;
                    }
                    case 2: {
                        processFlagAdd(operations, update);
                        break;
                    }

                    case 8: {
                        processFriend(operations, -update.getLong(1), true);
                        break;
                    }

                    case 9: {
                        processFriend(operations, -update.getLong(1), false);
                        break;
                    }
                }
            }
        }

        Set<Long> profiles = new HashSet<Long>();
        Set<Long> availableProfiles = null;
        Set<Long> profilesToDownload = new HashSet<Long>();
        JSONArray messages = history.getJSONArray("messages");
        if (messages != null) {
            int size = messages.getInt(0);
            for (int i = 0; i < size; i++) {
                JSONObject m = messages.getJSONObject(i + 1);
                final long chatId;
                final long userId;
                final long writerId;
                final long messageId = m.getLong("mid");
                final boolean out = 1 == m.getInt("out");
                String userIds = null;

                if (m.has("chat_id")) {
                    chatId = m.getLong("chat_id");
                    userId = 0;
                    writerId = m.getLong("uid");

                    if (availableProfiles == null) {
                        availableProfiles = DatabaseTools.fetchLongColumnAndClose(
                                VKMessenger.getCtx().getContentResolver().query(
                                        VKContentProvider.CONTENT_URI_PROFILE,
                                        Queries._ID_ONLY,
                                        null, null, null
                                )
                        );
                    }

                    final Set<Long> chatUsers = VKMessenger.getApi().getChatUsers(chatId);
                    for (Long chatUser : chatUsers) {
                        if (!availableProfiles.contains(chatUser)) {
                            profiles.add(chatUser);
                        }
                    }
                    userIds = DatabaseTools.idsToString(chatUsers);
                } else {
                    chatId = 0;
                    userId = m.getLong("uid");
                    writerId = out ? me : userId;
                }

                final int read = m.getInt("read_state");

                JSONArray attachments = m.optJSONArray("attachments");
                JSONObject geo_json = m.optJSONObject("geo");
                JSONArray fwd = m.optJSONArray("fwd_messages");
                final ArrayList<Attachment> a = Attachment.parseAttachments(attachments, 0, 0, geo_json, fwd);

                final ContentProviderOperation.Builder builder;
                try {
                    builder = ContentProviderOperation.newInsert(VKContentProvider.CONTENT_URI_MESSAGE)
                            .withValue(Tables.Columns._ID, messageId)
                            .withValue(Tables.Columns.WRITER_ID, writerId)
                            .withValue(Tables.Columns.USER_ID, userId)
                            .withValue(Tables.Columns.USER_IDS, userIds)
                            .withValue(Tables.Columns.CHAT_ID, chatId)
                            .withValue(Tables.Columns.BODY, Api.unescape(m.getString("body")))
                            .withValue(Tables.Columns.TITLE, m.getString("title"))
                            .withValue(Tables.Columns.ATTACHMENT, Attachments.loadAttachmentsInformationWithVideos(a))
                            .withValue(Tables.Columns.DT, m.getLong("date"))
                            .withValue(Tables.Columns.SERVER_STATUS, read);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                if (!VKContentProvider.checkWriter(writerId)) {
                    profilesToDownload.add(writerId);
                }

                if (out || read == 1) {
                    builder.withValue(Tables.Columns.LOCAL_STATUS, read);
                }

                operations.add(
                        builder
                                .build()
                );
            }
        }

        LoadDialogsTask.loadProfiles(operations, profilesToDownload);

        if (operations.size() > 0) {
            VKContentProvider.b(operations);
        }
    }

    /**
     * Проверить обновления и отключиться
     *
     * @param checkUpdatesOnly true, когда нужно только лишь проверить обновления
     * @return true, если сервис завершился успешно. false - если нужно перезапустить сервис
     */
    public static boolean runServer(boolean checkUpdatesOnly) {
        long me = Init.getUserId();

        final long lastTimestamp = Init.getLastTimestamp();

        String key;
        String server;
        long ts;
        try {
            final Object[] result = VKMessenger.getApi().getLongPollServer();
            key = (String) result[0];
            server = (String) result[1];
            ts = (Long) result[2];
            Init.setLastTimestamp(ts);
        } catch (Exception e) {
            Logs.d(TAG, e.getMessage(), e);
            return false;
        }

        Logs.d(TAG, "successfully connected to long poll server");
        VKMessenger.getCtx().sendBroadcast(new Intent(LongPollService.OK_INTENT));

        //загрузка информации о том, что было раньше
        if (lastTimestamp != 0) {
            JSONObject history = null;
            Long maxMsgId = DatabaseTools.fetchLONGAndClose(VKMessenger.getCtx().getContentResolver().query(
                    VKContentProvider.CONTENT_URI_MESSAGE,
                    Queries.MAX_MSG_PROJECTION,
                    Queries.SELECTION_MAX_MSG,
                    null,
                    null
            ));
            if (maxMsgId != null && maxMsgId == 0) {
                maxMsgId = null;
            }
            try {
                history = VKMessenger.getApi().getLongPollHistory(lastTimestamp, maxMsgId);
            } catch (KException e) {
                if (e.error_code == 10) {
                    if (Logs.enabled) {
                        Logs.d(TAG, "getLongPollHistory returned code 10. clearing all");
                    }
                    Flags.clear();
                    VKContentProvider.clearDataSync();
                }
            } catch (Exception e) {
                Logs.d(TAG, e.getMessage(), e);
                return false;
            }

            if (history != null) {
                processHistory(me, history);
            }
        }

        while (!stopping && !checkUpdatesOnly) {
            try {
                final URL url = new URL("http://" + server + "?act=a_check&key=" + key + "&ts=" + ts + "&wait=25&mode=2");
                final URLConnection c = url.openConnection();
                c.setConnectTimeout(10000);
                c.setReadTimeout(40000);
                c.setUseCaches(false);
                c.setDoOutput(false);
                c.setDoInput(true);
                final String s = StreamTools.readToString(c.getInputStream());
                if (DEBUG) {
                    Logs.d(TAG, "response: " + s);
                }
                final JSONObject response = new JSONObject(s);
                processPollUpdates(me, response.getJSONArray("updates"));
                ts = response.getLong("ts");
                Init.setLastTimestamp(ts);
            } catch (Exception e) {
                Logs.d(TAG, "error in poll server", e);
                return false;
            }
        }

        return true;
    }

    private static void processPollUpdates(long me, JSONArray updates) {
        final ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        final HashSet<Long> profiles = new HashSet<Long>();
        Set<Long> availableProfiles = new HashSet<Long>();

        for (int i = 0; i < updates.length(); i++) {
            try {
                final JSONArray update = updates.getJSONArray(i);

                int type = update.getInt(0);
                switch (type) {
                    case 51: {
                        processChatChange(update.getLong(1));
                        break;
                    }

                    case 61: {
                        VKMessenger.getCtx().sendBroadcast(
                                new Intent(ChatFragment.TYPING_INTENT).putExtra("userId", update.getLong(1)));
                        break;
                    }

                    case 62: {
                        VKMessenger.getCtx().sendBroadcast(
                                new Intent(ChatFragment.TYPING_INTENT)
                                        .putExtra("userId", update.getLong(1))
                                        .putExtra("chatId", update.getLong(2))
                        );
                        break;
                    }

                    case 4: {
                        processPollMessages(availableProfiles, me, operations, profiles, update);
                        break;
                    }

                    case 3: {
                        processFlagRemoval(operations, update);
                        break;
                    }

                    case 2: {
                        processFlagAdd(operations, update);
                        break;
                    }

                    case 8: {
                        processFriend(operations, -update.getLong(1), true);
                        break;
                    }

                    case 9: {
                        processFriend(operations, -update.getLong(1), false);
                        break;
                    }

                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        LoadDialogsTask.loadProfiles(operations, profiles);

        if (operations.size() > 0) {
            synchronized (SendInformationTask.class) {
                try {
                    VKContentProvider.b(operations);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static void processFlagAdd(ArrayList<ContentProviderOperation> operations, JSONArray update) throws JSONException {
        long mId = update.getLong(1);
        int flags = update.getInt(2);

        if ((flags & 128) == 128) {
            operations.add(
                    ContentProviderOperation.newDelete(VKContentProvider.CONTENT_URI_MESSAGE)
                            .withSelection(Queries.SELECTION_ID, new String[]{Long.toString(mId)})
                            .build()
            );
        }
    }

    @SuppressWarnings("unchecked")
    private static void processChatChange(final long id) {
        new ModernAsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Chat chat;
                try {
                    chat = VKMessenger.getApi().getChat(id);
                } catch (Exception e) {
                    Logs.d(TAG, e.getMessage(), e);
                    return null;
                }

                ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

                operations.add(
                        ContentProviderOperation.newUpdate(VKContentProvider.CONTENT_URI_DIALOG)
                                .withValue(Tables.Columns.TITLE, chat.title)
                                .withValue(Tables.Columns.USER_IDS, DatabaseTools.idsToString(chat.users))
                                .withSelection(Queries.SELECTION_CHAT_ID, new String[]{Long.toString(id)})
                                .build()
                );

                VKContentProvider.b(operations);

                return null;
            }
        }.execute();
    }

    private static void processFlagRemoval(ArrayList<ContentProviderOperation> operations, JSONArray update) throws JSONException {
        long mId = update.getLong(1);
        int flags = update.getInt(2);

        if ((flags & 1) == 1) {
            operations.add(
                    ContentProviderOperation.newUpdate(VKContentProvider.CONTENT_URI_MESSAGE)
                            .withValue(Tables.Columns.LOCAL_STATUS, 1)
                            .withValue(Tables.Columns.SERVER_STATUS, 1)
                            .withSelection(Queries.SELECTION_ID, new String[]{Long.toString(mId)})
                            .build()
            );
        }
    }

    private static void processFriend(List<ContentProviderOperation> operations, long id, boolean online) {
        operations.add(ContentProviderOperation.newUpdate(VKContentProvider.CONTENT_URI_PROFILE)
                .withValue(Tables.Columns.ONLINE, online ? 1 : 0)
                .withSelection(Queries.SELECTION_ID, new String[]{Long.toString(id)})
                .build());
    }

    private static void processPollMessages(
            Set<Long> availableProfiles, long me, List<ContentProviderOperation> operations, Set<Long> profiles, JSONArray update)
            throws JSONException, IOException, KException {

        long messageId = update.getLong(1);
        int flags = update.getInt(2);
        final long fromId = update.getLong(3);
        long dt = update.getLong(4);
        String title = update.getString(5);
        String body = update.getString(6);
        JSONObject extra = update.getJSONObject(7);

        long writerId;
        long userId;
        long chatId;
        final boolean out = (flags & 2) == 2;
        final boolean read = (flags & 1) != 1;
        String userIds = null;

        if (fromId > Constants.MAX_USER_ID) {
            userId = 0;
            chatId = fromId - Constants.MAX_USER_ID;
            writerId = extra.getLong("from");

            if (availableProfiles.size() == 0) {
                availableProfiles.addAll(DatabaseTools.fetchLongColumnAndClose(
                        VKMessenger.getCtx().getContentResolver().query(
                                VKContentProvider.CONTENT_URI_PROFILE,
                                Queries._ID_ONLY,
                                null, null, null
                        )
                ));
            }

            final Set<Long> chatUsers = VKMessenger.getApi().getChatUsers(chatId);
            for (Long chatUser : chatUsers) {
                if (!availableProfiles.contains(chatUser)) {
                    profiles.add(chatUser);
                }
            }
            userIds = DatabaseTools.idsToString(chatUsers);
        } else {
            userId = fromId;
            chatId = 0;
            writerId = out ? me : fromId;
        }

        List<Attachment> attachments = null;

        if (update.length() > 7) {
            try {
                final ArrayList<Message> geoInfo = VKMessenger.getApi().getMessagesById(messageId, null);
                attachments = geoInfo.get(0).attachments;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        final ContentProviderOperation.Builder builder;
        try {
            builder = ContentProviderOperation.newInsert(VKContentProvider.CONTENT_URI_MESSAGE)
                    .withValue(Tables.Columns._ID, messageId)
                    .withValue(Tables.Columns.WRITER_ID, writerId)
                    .withValue(Tables.Columns.USER_ID, userId)
                    .withValue(Tables.Columns.CHAT_ID, chatId)
                    .withValue(Tables.Columns.USER_IDS, userIds)
                    .withValue(Tables.Columns.BODY, Api.unescape(body))
                    .withValue(Tables.Columns.TITLE, title)
                    .withValue(Tables.Columns.ATTACHMENT, Attachments.loadAttachmentsInformationWithVideos(attachments))
                    .withValue(Tables.Columns.DT, dt)
                    .withValue(Tables.Columns.SERVER_STATUS, read ? 1 : 0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (!VKContentProvider.checkWriter(writerId)) {
            profiles.add(writerId);
        }

        if (out || read) {
            builder.withValue(Tables.Columns.LOCAL_STATUS, read ? 1 : 0);
        }

        operations.add(
                builder
                        .build()
        );
    }
}
