package ru.nacu.vkmsg.asynctasks;

import android.content.ContentProviderOperation;
import com.perm.kate.api.User;
import ru.android.common.db.DatabaseTools;
import ru.android.common.logs.Logs;
import ru.android.common.task.ModernAsyncTask;
import ru.nacu.commons.asynclist.State;
import ru.nacu.vkmsg.Init;
import ru.nacu.vkmsg.VKMessenger;
import ru.nacu.vkmsg.dao.Tables;
import ru.nacu.vkmsg.dao.VKContentProvider;
import ru.nacu.vkmsg.ui.profiles.ProfileFragment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * @author quadro
 * @since 7/2/12 10:35 AM
 */
public final class LoadFriendsTask extends ModernAsyncTask<Void, Void, Void> {
    public static final String TAG = "LoadFriendsTask";

    @Override
    protected Void doInBackground(Void... params) {
        boolean success = false;
        try {
            success = load0();
        } finally {
            if (!success) {
                Loading.setFriends(State.NONE);
                VKContentProvider.notifyChange(VKContentProvider.CONTENT_URI_FRIEND);
            }
        }

        return null;
    }

    private boolean load0() {
        Loading.setFriends(State.START);
        VKMessenger.getCtx().getContentResolver().notifyChange(VKContentProvider.CONTENT_URI_FRIEND, null);
        long start = System.currentTimeMillis();

        ArrayList<Object[]> inFriends;
        try {
            inFriends = VKMessenger.getApi().getRequestsFriends(true);
        } catch (Exception e) {
            Logs.d(TAG, e.getMessage(), e);
            return false;
        }

        ArrayList<Object[]> outFriends;
        try {
            outFriends = VKMessenger.getApi().getRequestsFriends(false);
        } catch (Exception e) {
            Logs.d(TAG, e.getMessage(), e);
            return false;
        }

        ArrayList<User> suggestions;
        try {
            suggestions = VKMessenger.getApi().getFriendSuggestions();
        } catch (Exception e) {
            Logs.d(TAG, e.getMessage(), e);
            return false;
        }

        final Set<Long> in = new HashSet<Long>();
        for (Object[] f : inFriends) {
            long id = (Long) f[0];
            in.add(id);
        }

        final Set<Long> out = new HashSet<Long>();
        for (Object[] f : outFriends) {
            long id = (Long) f[0];
            out.add(id);
        }

        final Set<Long> s = new HashSet<Long>();
        for (User u : suggestions) {
            s.add(u.uid);
            if (s.size() == 100)
                break;
        }

        final Set<Long> all = new HashSet<Long>();
        all.addAll(in);
        all.addAll(out);
        all.addAll(s);
        ArrayList<User> profiles;
        try {
            if (all.size() > 0) {
                Logs.d(TAG, "all: " + all);
                profiles = VKMessenger.getApi().getProfiles(all, LoadDialogsTask.DEFAULT_FIELDS, null);
            } else {
                profiles = new ArrayList<User>();
            }
        } catch (Exception e) {
            profiles = new ArrayList<User>();
            Logs.d(TAG, e.getMessage(), e);
        }

        ArrayList<User> friends;
        try {
            friends = VKMessenger.getApi().getFriends(null, LoadDialogsTask.DEFAULT_FIELDS, null);
        } catch (Exception e) {
            Logs.d(TAG, e.getMessage(), e);
            return false;
        }

        Logs.d(TAG, "done loading data. time=" + (System.currentTimeMillis() - start));
        final ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        operations.add(ContentProviderOperation.newUpdate(VKContentProvider.CONTENT_URI_PROFILE)
                .withValue(Tables.Columns.FRIEND, 0)
                .withValue(Tables.Columns.POP, 0)
                .build());

        if (profiles != null) {
            for (User user : profiles) {
                final int friend;
                if (in.contains(user.uid)) {
                    friend = ProfileFragment.FRECV;
                } else if (out.contains(user.uid)) {
                    friend = ProfileFragment.FSENT;
                } else {
                    friend = ProfileFragment.FSUGG;
                }

                operations.add(
                        ContentProviderOperation.newInsert(VKContentProvider.CONTENT_URI_PROFILE)
                                .withValue(Tables.Columns._ID, user.uid)
                                .withValue(Tables.Columns.FIRST_NAME, user.first_name)
                                .withValue(Tables.Columns.LAST_NAME, user.last_name)
                                .withValue(Tables.Columns.PHOTO, user.photo_medium)
                                .withValue(Tables.Columns.PHOTO_BIG, user.photo_big)
                                .withValue(Tables.Columns.BDATE, LoadDialogsTask.parseDate(user.birthdate))
                                .withValue(Tables.Columns.SEX, LoadDialogsTask.parseSex(user.sex))
                                .withValue(Tables.Columns.FRIEND, friend)
                                .build()
                );
            }
        }

        Set<Long> friendIds = new HashSet<Long>(friends.size());
        int pop = 5;
        for (User f : friends) {
            operations.add(
                    ContentProviderOperation.newInsert(VKContentProvider.CONTENT_URI_PROFILE)
                            .withValue(Tables.Columns._ID, f.uid)
                            .withValue(Tables.Columns.FIRST_NAME, f.first_name)
                            .withValue(Tables.Columns.LAST_NAME, f.last_name)
                            .withValue(Tables.Columns.PHOTO, f.photo_medium)
                            .withValue(Tables.Columns.PHOTO_BIG, f.photo_big)
                            .withValue(Tables.Columns.BDATE, LoadDialogsTask.parseDate(f.birthdate))
                            .withValue(Tables.Columns.SEX, LoadDialogsTask.parseSex(f.sex))
                            .withValue(Tables.Columns.POP, pop)
                            .withValue(Tables.Columns.ONLINE, f.online != null && f.online ? 1 : 0)
                            .withValue(Tables.Columns.FRIEND, 1)
                            .build()
            );

            if (pop != 0)
                pop--;

            friendIds.add(f.uid);
        }

        operations.add(ContentProviderOperation.newUpdate(VKContentProvider.CONTENT_URI_PROFILE)
                .withSelection("_id in (" + DatabaseTools.idsToString(friendIds) + ")", null)
                .withValue(Tables.Columns.FRIEND, 1)
                .build());

        Loading.setFriends(State.NONE);
        VKContentProvider.b(operations);
        Init.setFriendsUpdateTime(System.currentTimeMillis());

        if (Logs.enabled) {
            Logs.d(TAG, "doInBackground time=" + (System.currentTimeMillis() - start));
        }
        return true;
    }
}
