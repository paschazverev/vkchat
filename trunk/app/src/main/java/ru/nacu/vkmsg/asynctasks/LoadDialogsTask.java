package ru.nacu.vkmsg.asynctasks;

import android.content.ContentProviderOperation;
import com.perm.kate.api.Message;
import com.perm.kate.api.User;
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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * @author quadro
 * @since 6/21/12 4:52 PM
 */
public final class LoadDialogsTask extends ModernAsyncTask<Void, Void, Void> {
    public static final String TAG = "LoadDialogsTask";
    public static final String DEFAULT_FIELDS = "first_name,last_name,uid,photo_medium,photo_big,bdate,sex";

    private final long offset;
    private final int count;

    public LoadDialogsTask(long offset, int count) {
        this.offset = offset;
        this.count = count;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        boolean success = false;
        try {
            success = load0();
        } finally {
            Loading.setDialogs(State.NONE);
            if (!success) {
                VKContentProvider.notifyChange(VKContentProvider.CONTENT_URI_DIALOG);
            }
        }

        return null;
    }

    private boolean load0() {
        Loading.setDialogs(offset == 0 ? State.START : State.END);
        VKMessenger.getCtx().getContentResolver().notifyChange(VKContentProvider.CONTENT_URI_DIALOG, null);

        if (!Flags.isDialogListLoaded()) {
            Flags.setDialogListLoaded();
        }

        long start = System.currentTimeMillis();
        final ArrayList<Message> messages;
        try {
            messages = VKMessenger.getApi().getMessagesDialogs(offset, count);
        } catch (Exception e) {
            Logs.d(TAG, e.getMessage(), e);
            return false;
        }

        Logs.d(TAG, "getMessagesDialogs execution time=" + (System.currentTimeMillis() - start));

        final ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        final HashSet<Long> profiles = new HashSet<Long>(messages.size());

        long me = Init.getUserId();

        Set<Long> availableProfiles = DatabaseTools.fetchLongColumnAndClose(
                VKMessenger.getCtx().getContentResolver().query(
                        VKContentProvider.CONTENT_URI_PROFILE,
                        Queries._ID_ONLY,
                        null, null, null
                )
        );

        if (!availableProfiles.contains(Init.getUserId())) {
            profiles.add(Init.getUserId());
        }

        for (Message m : messages) {
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
                        .withValue(Tables.Columns.ATTACHMENT, Attachments.loadAttachmentsInformationWithVideos(m.attachments))
                        .withValue(Tables.Columns.USER_IDS, userIds)
                        .withValue(Tables.Columns.SERVER_STATUS, read)
                        .withValue(Tables.Columns.TITLE, m.title)
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

            operations.add(builder.build());
        }

        loadProfiles(operations, profiles);

        if (messages.size() < count) {
            Flags.setDialogListFullyLoaded();
        }

        Loading.setDialogs(State.NONE);
        VKContentProvider.b(operations);
        Logs.d(TAG, "execution time=" + (System.currentTimeMillis() - start));
        return true;
    }

    public static void loadProfiles(ArrayList<ContentProviderOperation> operations, Set<Long> ids) {
        if (ids == null || ids.size() == 0)
            return;

        try {
            ArrayList<User> users = VKMessenger.getApi().getProfiles(ids, DEFAULT_FIELDS, "nom");
            for (User user : users) {
                operations.add(0,
                        ContentProviderOperation.newInsert(VKContentProvider.CONTENT_URI_PROFILE)
                                .withValue(Tables.Columns._ID, user.uid)
                                .withValue(Tables.Columns.FIRST_NAME, user.first_name)
                                .withValue(Tables.Columns.LAST_NAME, user.last_name)
                                .withValue(Tables.Columns.PHOTO, user.photo_medium)
                                .withValue(Tables.Columns.PHOTO_BIG, user.photo_big)
                                .withValue(Tables.Columns.BDATE, parseDate(user.birthdate))
                                .withValue(Tables.Columns.SEX, parseSex(user.sex))
                                .build()
                );
            }
        } catch (Exception e) {
            Logs.d(TAG, e.getMessage(), e);
        }
    }

    public static final DateFormat fmt = new SimpleDateFormat("dd.MM.yyyy");

    public static long parseDate(String s) {
        try {
            if (s == null) return 0;
            return fmt.parse(s).getTime() / 1000;
        } catch (ParseException e) {
            return 0;
        }
    }

    public static Integer parseSex(Integer sex) {
        if (sex == null) return null;
        switch (sex) {
            case 1:
                return 1;
            case 2:
                return 2;
        }

        return null;
    }
}
