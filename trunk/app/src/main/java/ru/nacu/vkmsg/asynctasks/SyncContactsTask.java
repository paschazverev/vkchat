package ru.nacu.vkmsg.asynctasks;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.ContactsContract;
import com.perm.kate.api.User;
import ru.android.common.db.DatabaseTools;
import ru.android.common.logs.Logs;
import ru.android.common.task.ModernAsyncTask;
import ru.nacu.commons.asynclist.State;
import ru.nacu.vkmsg.Flags;
import ru.nacu.vkmsg.VKMessenger;
import ru.nacu.vkmsg.dao.Tables;
import ru.nacu.vkmsg.dao.VKContentProvider;

import java.util.*;

import static android.provider.ContactsContract.Contacts._ID;

/**
 * @author quadro
 * @since 7/6/12 7:39 PM
 */
public final class SyncContactsTask extends ModernAsyncTask<Void, Void, Void> {
    public static final String TAG = "SyncContactsTask";
    public static final String[] PROJECTION = {_ID,};

    @Override
    protected Void doInBackground(Void... params) {
        boolean success = false;
        try {
            success = load();
        } finally {
            if (!success) {
                Loading.setContacts(State.NONE);
                VKContentProvider.notifyChange(VKContentProvider.CONTENT_URI_CONTACT);
            }
        }

        return null;
    }

    private boolean load() {
        Loading.setContacts(State.START);
        VKContentProvider.notifyChange(VKContentProvider.CONTENT_URI_CONTACT);
        long start = System.currentTimeMillis();
        final ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        operations.add(ContentProviderOperation.newDelete(VKContentProvider.CONTENT_URI_PROFILE)
                .withSelection("_id < 0", null)
                .build());

        operations.add(ContentProviderOperation.newUpdate(VKContentProvider.CONTENT_URI_PROFILE)
                .withValue(Tables.Columns.CONTACT, 0)
                .build());

        getContacts(VKMessenger.getCtx().getContentResolver(), operations);
        Loading.setContacts(State.NONE);
        Flags.setContactsSynced();
        VKContentProvider.b(operations);
        Logs.d(TAG, "time=" + (System.currentTimeMillis() - start));
        return true;
    }

    public static void getContacts(ContentResolver cr, ArrayList<ContentProviderOperation> operations) {
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, new String[]{ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME}, null, null, null);
        final List<Contact> contacts = new ArrayList<Contact>();
        final Set<String> phones = new HashSet<String>();

        while (cur.moveToNext()) {
            final Contact con = processAccount(cr, cur);
            if (con.phone.length == 0)
                continue;

            if ((phones.size() + con.phone.length) > 1000) {
                loadFromVk(contacts, phones, operations);
                contacts.clear();
                phones.clear();
            }

            phones.addAll(Arrays.asList(con.phone));
            contacts.add(con);
        }

        loadFromVk(contacts, phones, operations);
    }

    public static String toMSISDN(String phone) {
        String simple = phone.replaceAll("[^0-9+]", "");
        if (!simple.startsWith("+") && (simple.startsWith("8") || simple.startsWith("7")) && simple.length() == 11) {
            simple = "+7" + simple.substring(1);
        }
        return simple;
    }

    private static void loadFromVk(List<Contact> list, Set<String> phones, ArrayList<ContentProviderOperation> operations) {
        ArrayList<User> users;
        try {
            users = VKMessenger.getApi().getFriendsByPhones(phones, LoadDialogsTask.DEFAULT_FIELDS);
        } catch (Exception e) {
            return;
        }

        Map<String, User> map = new HashMap<String, User>();
        for (User user : users) {
            map.put(toMSISDN(user.phone), user);
        }

        for (Contact contact : list) {
            User found = null;
            for (String p : contact.phone) {
                final User user = map.get(p);
                if (user != null) {
                    found = user;
                    break;
                }
            }

            if (found != null) {
                operations.add(ContentProviderOperation.newInsert(VKContentProvider.CONTENT_URI_PROFILE)
                        .withValue(Tables.Columns._ID, found.uid)
                        .withValue(Tables.Columns.PHONE_NAME, contact.name)
                        .withValue(Tables.Columns.PHONE, DatabaseTools.idsToString(Arrays.asList(contact.phone)))
                        .withValue(Tables.Columns.FIRST_NAME, found.first_name)
                        .withValue(Tables.Columns.LAST_NAME, found.last_name)
                        .withValue(Tables.Columns.PHOTO, found.photo_medium)
                        .withValue(Tables.Columns.PHOTO_BIG, found.photo_big)
                        .withValue(Tables.Columns.BDATE, LoadDialogsTask.parseDate(found.birthdate))
                        .withValue(Tables.Columns.SEX, LoadDialogsTask.parseSex(found.sex))
                        .withValue(Tables.Columns.CONTACT, 1)
                        .build()
                );
            } else {
                operations.add(ContentProviderOperation.newInsert(VKContentProvider.CONTENT_URI_PROFILE)
                        .withValue(Tables.Columns._ID, -contact.id)
                        .withValue(Tables.Columns.PHONE_NAME, contact.name)
                        .withValue(Tables.Columns.PHONE, DatabaseTools.idsToString(Arrays.asList(contact.phone)))
                        .withValue(Tables.Columns.CONTACT, 1)
                        .build()
                );
            }
        }
    }

    private static Contact processAccount(ContentResolver cr, Cursor cur) {
        long id = cur.getLong(0);
        String displayName = cur.getString(1);
        Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{Long.toString(id)}, null);

        try {
            final String[] phones = new String[pCur.getCount()];
            int i = 0;
            while (pCur.moveToNext()) {
                phones[i] = toMSISDN(pCur.getString(0));
                i++;
            }

            return new Contact(id, displayName, phones);
        } finally {
            pCur.close();
        }
    }

    private static class Contact {
        private final long id;
        private final String name;
        private final String[] phone;

        private Contact(long id, String name, String[] phone) {
            this.id = id;
            this.name = name;
            this.phone = phone;
        }
    }
}
