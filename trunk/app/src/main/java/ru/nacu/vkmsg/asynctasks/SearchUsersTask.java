package ru.nacu.vkmsg.asynctasks;

import android.content.ContentProviderOperation;
import com.perm.kate.api.User;
import ru.android.common.logs.Logs;
import ru.android.common.task.ModernAsyncTask;
import ru.nacu.commons.asynclist.State;
import ru.nacu.vkmsg.Flags;
import ru.nacu.vkmsg.VKMessenger;
import ru.nacu.vkmsg.dao.Tables;
import ru.nacu.vkmsg.dao.VKContentProvider;

import java.util.ArrayList;

/**
 * @author quadro
 * @since 7/5/12 10:38 AM
 */
public final class SearchUsersTask extends ModernAsyncTask<Void, Void, Void> {
    public static final String TAG = "SearchUsersTask";

    private final String q;
    private final int count;
    private final long offset;

    public SearchUsersTask(String q, int count, long offset) {
        this.q = q;
        this.count = count;
        this.offset = offset;

        if (offset == 0)
            Loading.setSearch(State.START);
        else
            Loading.setSearch(State.END);

    }

    @Override
    protected Void doInBackground(Void... params) {
        synchronized (SearchUsersTask.class) {
            return doInBackground0();
        }
    }

    private Void doInBackground0() {
        Logs.d(TAG, "doInBackground() " + q + "; " + offset);

        VKContentProvider.notifyChange(VKContentProvider.CONTENT_URI_SEARCH);

        try {
            ArrayList<User> users;
            try {
                users = VKMessenger.getApi().searchUser(q, LoadDialogsTask.DEFAULT_FIELDS, (long) count, offset);
            } catch (Exception e) {
                //todo показывать пользователю ошибку как-то
                return null;
            }

            final ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
            int pos = 1;
            for (User user : users) {
                operations.add(
                        ContentProviderOperation.newInsert(VKContentProvider.CONTENT_URI_PROFILE)
                                .withValue(Tables.Columns._ID, user.uid)
                                .withValue(Tables.Columns.BDATE, LoadDialogsTask.parseDate(user.birthdate))
                                .withValue(Tables.Columns.SEX, LoadDialogsTask.parseSex(user.sex))
                                .withValue(Tables.Columns.FIRST_NAME, user.first_name)
                                .withValue(Tables.Columns.LAST_NAME, user.last_name)
                                .withValue(Tables.Columns.PHOTO, user.photo_medium)
                                .withValue(Tables.Columns.PHOTO_BIG, user.photo_big)
                                .withValue(Tables.Columns.SEARCH, pos + offset)
                                .build()
                );

                pos++;
            }

            VKContentProvider.b(operations);
            Flags.setSearchFullyLoaded(users.size() < count);
        } finally {
            Loading.setSearch(State.NONE);
        }

        return null;
    }
}
