package ru.nacu.vkmsg.dao;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import ru.android.common.db.DatabaseTools;
import ru.android.common.db.SelectionBuilder;
import ru.android.common.logs.Logs;
import ru.android.common.task.ModernAsyncTask;
import ru.nacu.commons.asynclist.LoadStateCursor;
import ru.nacu.vkmsg.Constants;
import ru.nacu.vkmsg.Flags;
import ru.nacu.vkmsg.Init;
import ru.nacu.vkmsg.VKMessenger;
import ru.nacu.vkmsg.asynctasks.Loading;
import ru.nacu.vkmsg.asynctasks.SendInformationTask;
import ru.nacu.vkmsg.model.Msg;
import ru.nacu.vkmsg.notification.Notifications;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Основной ContentProvider приложения. все действия должны выполняться из batch (кроме query)
 * (чтобы проходили в транзакциях и выполнялись дополнительные проверки)
 *
 * @author quadro
 * @since 6/21/12 4:59 PM
 */
public final class VKContentProvider extends ContentProvider {
    public static final boolean DEBUG = false;

    public static final String TAG = "VKContentProvider";

    /**
     * список диалогов, которые были изменены в данной транзакции
     */
    private Set<Long> dialogsChanged = new HashSet<Long>();

    /**
     * Список диалогов, у которых были изменены свойства (название, состав)
     */
    private Set<Long> chatsChanged = new HashSet<Long>();
    private boolean friendsChanged;
    private boolean searchChanged;
    private boolean contactsChanged;

    /**
     * Были ли изменены notifications в транзакции
     */
    private boolean notificationsChanged;

    public static final String CONTENT_AUTHORITY = Constants.PREFIX + "db";
    public static final Uri CONTENT_URI_BASE = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final Uri CONTENT_URI_CONTACT =
            CONTENT_URI_BASE.buildUpon().appendEncodedPath("contact").build();
    public static final Uri CONTENT_URI_SEARCH =
            CONTENT_URI_BASE.buildUpon().appendEncodedPath("search").build();
    public static final Uri CONTENT_URI_DIALOG =
            CONTENT_URI_BASE.buildUpon().appendEncodedPath("dialog").build();
    public static final Uri CONTENT_URI_SEARCH_DIALOG =
            CONTENT_URI_BASE.buildUpon().appendEncodedPath("search_dialog").build();
    public static final Uri CONTENT_URI_FRIEND =
            CONTENT_URI_BASE.buildUpon().appendEncodedPath("friend").build();
    public static final Uri CONTENT_URI_PROFILE =
            CONTENT_URI_BASE.buildUpon().appendEncodedPath("profile").build();
    public static final Uri CONTENT_URI_MESSAGE =
            CONTENT_URI_BASE.buildUpon().appendEncodedPath("message").build();
    public static final Uri CONTENT_URI_MESSAGE_AND_DIALOG =
            CONTENT_URI_BASE.buildUpon().appendEncodedPath("message_dialog").build();

    private static final int PROFILE = 10;
    private static final int FRIEND = 11;
    private static final int SEARCH = 12;
    private static final int CONTACT = 13;
    private static final int DIALOG = 50;
    private static final int SEARCH_DIALOG = 51;
    private static final int MESSAGE = 100;
    private static final int MESSAGE_BY_DIALOG = 101;
    private static final int MESSAGE_AND_DIALOG = 102;

    public static final String DIALOG_CONTENT_TYPE =
            "vnd.android.cursor.dir/vnd.vkmsg.dialog";
    public static final String PROFILE_CONTENT_TYPE =
            "vnd.android.cursor.dir/vnd.vkmsg.profile";
    public static final String MESSAGE_CONTENT_TYPE =
            "vnd.android.cursor.dir/vnd.vkmsg.message";

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = CONTENT_AUTHORITY;

        matcher.addURI(authority, "contact", CONTACT);
        matcher.addURI(authority, "dialog", DIALOG);
        matcher.addURI(authority, "search_dialog", SEARCH_DIALOG);
        matcher.addURI(authority, "profile", PROFILE);
        matcher.addURI(authority, "friend", FRIEND);
        matcher.addURI(authority, "search", SEARCH);
        matcher.addURI(authority, "message", MESSAGE);
        matcher.addURI(authority, "message/*", MESSAGE_BY_DIALOG);
        matcher.addURI(authority, "message_dialog", MESSAGE_AND_DIALOG);

        return matcher;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case SEARCH_DIALOG:
            case DIALOG:
                return DIALOG_CONTENT_TYPE;
            case MESSAGE:
            case MESSAGE_BY_DIALOG:
                return MESSAGE_CONTENT_TYPE;
            case PROFILE:
            case FRIEND:
            case SEARCH:
            case CONTACT:
                return PROFILE_CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    private SQLiteDatabase db;

    @Override
    public boolean onCreate() {
        db = new DataHelper(getContext()).getWritableDatabase();
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (Logs.enabled && DEBUG) {
            Logs.d(TAG, "query() " + uri);
        }

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case CONTACT: {
                if (!Flags.isContactsSynced()) {
                    final Cursor c = db.query(Tables.PROFILE, projection, "1 = 0", null, null, null, null);
                    c.setNotificationUri(getContext().getContentResolver(), CONTENT_URI_CONTACT);
                    return new LoadStateCursor(c, Loading.getContacts(), false);
                }

                final SelectionBuilder s = new SelectionBuilder();
                s.table(Tables.PROFILE).where(Queries.SELECTION_CONTACT).where(selection, selectionArgs);
                final Cursor c = s.query(db, projection, sortOrder);
                c.setNotificationUri(getContext().getContentResolver(), CONTENT_URI_CONTACT);
                return new LoadStateCursor(c, Loading.getContacts(), false);
            }
            case FRIEND: {
                final SelectionBuilder s = new SelectionBuilder();
                s.table(Tables.PROFILE).where(selection, selectionArgs);
                final Cursor c = s.query(db, projection, sortOrder);
                c.setNotificationUri(getContext().getContentResolver(), CONTENT_URI_FRIEND);
                return new LoadStateCursor(c, Loading.getFriends(), false);
            }
            case SEARCH: {
                final SelectionBuilder s = new SelectionBuilder();
                s.table(Tables.PROFILE).where(Queries.SELECTION_SEARCH_ONLY).where(selection, selectionArgs);
                final Cursor c = s.query(db, projection, sortOrder);
                c.setNotificationUri(getContext().getContentResolver(), CONTENT_URI_SEARCH);
                return new LoadStateCursor(c, Loading.getSearch(), false);
            }
            case SEARCH_DIALOG: {
                final SelectionBuilder s = new SelectionBuilder();
                s.table(Tables.DIALOG_V).where(Queries.SELECTION_SEARCH_ONLY).where(selection, selectionArgs);
                final Cursor c = s.query(db, projection, sortOrder);
                c.setNotificationUri(getContext().getContentResolver(), CONTENT_URI_SEARCH_DIALOG);
                return new LoadStateCursor(c, Loading.getSearchDialogs(), false);
            }
            case DIALOG: {
                final SelectionBuilder s = new SelectionBuilder();
                s.table(Tables.DIALOG_V).where(selection, selectionArgs);
                final Cursor c = s.query(db, projection, sortOrder);
                c.setNotificationUri(getContext().getContentResolver(), CONTENT_URI_DIALOG);
                return new LoadStateCursor(c, Loading.getDialogs(), false);
            }
            case MESSAGE_AND_DIALOG: {
                final SelectionBuilder s = new SelectionBuilder();
                s.table(Tables.MESSAGE_DIALOG_V).
                        where(selection, selectionArgs);
                return s.query(db, projection, sortOrder);
            }
            case MESSAGE: {
                final SelectionBuilder s = new SelectionBuilder();
                s.table(Tables.MESSAGE_V).
                        where(selection, selectionArgs);
                return s.query(db, projection, sortOrder);
            }
            case MESSAGE_BY_DIALOG: {
                final SelectionBuilder s = new SelectionBuilder();
                final long dialogId = ContentUris.parseId(uri);
                s.table(Tables.MESSAGE_V).
                        where(Queries.SELECTION_DIALOG, Long.toString(dialogId)).
                        where(selection, selectionArgs);

                final Cursor c = s.query(db, projection, sortOrder);
                c.setNotificationUri(getContext().getContentResolver(), uri);
                return new LoadStateCursor(c, Loading.getDialog(dialogId), true);
            }
            case PROFILE: {
                final SelectionBuilder s = new SelectionBuilder();
                s.table(Tables.PROFILE).where(selection, selectionArgs);
                return s.query(db, projection, sortOrder);
            }
        }

        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues v) {
        if (Logs.enabled && DEBUG)
            Logs.d(TAG, "insert " + uri + ": " + v);

        final int match = sUriMatcher.match(uri);
        if (!db.inTransaction()) {
            throw new RuntimeException("Operation must be processed through batch");
        }

        switch (match) {
            case DIALOG: {
                return ContentUris.withAppendedId(CONTENT_URI_DIALOG, db.insert(Tables.DIALOG, null, v));
            }
            case PROFILE: {
                if (v.containsKey(Tables.Columns.CONTACT)) {
                    contactsChanged = true;
                }

                if (v.containsKey(Tables.Columns.SEARCH)) {
                    searchChanged = true;
                }

                if (v.containsKey(Tables.Columns.FRIEND)) {
                    friendsChanged = true;
                }

                final Long profileId = v.getAsLong(Tables.Columns._ID);

                final String first = v.getAsString(Tables.Columns.FIRST_NAME);
                final String last = v.getAsString(Tables.Columns.LAST_NAME);
                final String phoneName = v.getAsString(Tables.Columns.PHONE_NAME);
                String searchString = "";
                if (first != null) {
                    searchString += first.toLowerCase();
                }
                if (last != null) {
                    searchString += last.toLowerCase();
                }
                if (phoneName != null) {
                    searchString += phoneName.toLowerCase();
                }

                v.put(Tables.Columns.SEARCH_STRING, searchString);

                try {
                    final long id = db.insert(Tables.PROFILE, null, v);
                    if (id <= 0) {
                        throw new SQLiteConstraintException();

                    }
                } catch (SQLiteConstraintException e) {
                    db.update(Tables.PROFILE, v, Queries.SELECTION_ID, new String[]{Long.toString(profileId)});
                }

                friendsChanged = true;
                return ContentUris.withAppendedId(CONTENT_URI_PROFILE, profileId);
            }
            case MESSAGE: {
                if (v.containsKey(Tables.Columns.DIALOG_ID)) {
                    return insertOrUpdateMessage(v);
                } else {
                    Long dialogId;
                    final Long chatId = v.getAsLong(Tables.Columns.CHAT_ID);
                    final Long userId = v.getAsLong(Tables.Columns.USER_ID);

                    if (chatId == null || chatId == 0) {
                        dialogId = DatabaseTools.fetchLONGAndClose(db.query(
                                Tables.DIALOG, Queries._ID_ONLY, Queries.SELECTION_USER_ID, new String[]{Long.toString(userId)}, null, null, null));
                    } else {
                        dialogId = DatabaseTools.fetchLONGAndClose(db.query(
                                Tables.DIALOG, Queries._ID_ONLY, Queries.SELECTION_CHAT_ID, new String[]{Long.toString(chatId)}, null, null, null));
                    }

                    ContentValues dialogValues = new ContentValues();
                    dialogValues.put(Tables.Columns.USER_ID, userId);
                    dialogValues.put(Tables.Columns.CHAT_ID, chatId);
                    if (v.containsKey(Tables.Columns.USER_IDS)) {
                        dialogValues.put(Tables.Columns.USER_IDS, v.getAsString(Tables.Columns.USER_IDS));
                    }
                    if (v.containsKey(Tables.Columns.SEARCH)) {
                        dialogValues.put(Tables.Columns.SEARCH, v.getAsInteger(Tables.Columns.SEARCH));
                    } else {
                        dialogValues.put(Tables.Columns.REGULAR, 1);
                    }

                    dialogValues.put(Tables.Columns.TITLE, v.getAsString(Tables.Columns.TITLE));

                    if (dialogId == null) {
                        dialogId = db.insert(Tables.DIALOG, null, dialogValues);

                        if (!v.containsKey(Tables.Columns.SEARCH))
                            Flags.setDialogLoaded(dialogId);

                    } else {
                        db.update(Tables.DIALOG, dialogValues, Queries.SELECTION_ID, new String[]{Long.toString(dialogId)});
                    }

                    ContentValues mv = new ContentValues(v);
                    mv.remove(Tables.Columns.CHAT_ID);
                    mv.remove(Tables.Columns.USER_ID);
                    mv.remove(Tables.Columns.USER_IDS);
                    mv.put(Tables.Columns.DIALOG_ID, dialogId);
                    return insertOrUpdateMessage(mv);
                }
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    /**
     * Вставляет или обновляет сообщение. должны быть все поля
     */
    private Uri insertOrUpdateMessage(ContentValues v) {
        final long me = Init.getUserId();
        if (Logs.enabled && DEBUG)
            Logs.d(TAG, "insertOrUpdateMessage(): " + v + "; me=" + me);

        if (v.containsKey(Tables.Columns.TITLE)) {
            v.remove(Tables.Columns.TITLE);
        }

        if (!v.containsKey(Tables.Columns.SEARCH)) {
            v.put(Tables.Columns.REGULAR, 1);
        }

        SendInformationTask sendTask = null;
        if (!v.containsKey(Tables.Columns._ID)) {
            final Long maxUnsentId = DatabaseTools.fetchLONGAndClose(db.rawQuery("select max(_id) from message where _id > 1000000000000", null));
            final long messageId;
            if (maxUnsentId != null && maxUnsentId != 0) {
                messageId = maxUnsentId + 1;
            } else {
                messageId = 1000000000001l;
            }

            v.put(Tables.Columns._ID, messageId);
            sendTask = new SendInformationTask();
        }

        final Long dialogId = v.getAsLong(Tables.Columns.DIALOG_ID);
        dialogsChanged.add(dialogId);

        try {
            final long newMessageId = db.insert(Tables.MESSAGE, null, v);
            if (Logs.enabled && DEBUG)
                Logs.d(TAG, "insertOrUpdateMessage() insert result: " + newMessageId);

            if (newMessageId == -1)
                throw new SQLiteConstraintException();

            if (v.containsKey(Tables.Columns.SERVER_STATUS) && v.getAsInteger(Tables.Columns.SERVER_STATUS) == 0 && v.getAsLong(Tables.Columns.WRITER_ID) != me) {
                notificationsChanged = Notifications.add(new Msg(newMessageId, dialogId, v.getAsString(Tables.Columns.BODY))) || notificationsChanged;
            } else if (v.containsKey(Tables.Columns.LOCAL_STATUS) && v.getAsInteger(Tables.Columns.LOCAL_STATUS) == 1 && v.getAsLong(Tables.Columns.WRITER_ID) != me) {
                notificationsChanged = Notifications.remove(newMessageId) || notificationsChanged;
            }

            if (sendTask != null) {
                sendTask.execute();
            }

            return ContentUris.withAppendedId(CONTENT_URI_MESSAGE, newMessageId);
        } catch (SQLiteConstraintException e) {
            if (Logs.enabled && DEBUG)
                Logs.d(TAG, "insertOrUpdateMessage() updating existing");

            final Long mId = v.getAsLong(Tables.Columns._ID);
            db.update(Tables.MESSAGE, v, Queries.SELECTION_ID, new String[]{Long.toString(mId)});

            if (v.containsKey(Tables.Columns.SERVER_STATUS) && v.getAsInteger(Tables.Columns.SERVER_STATUS) == 0 && v.getAsLong(Tables.Columns.WRITER_ID) != me) {
                notificationsChanged = Notifications.add(new Msg(mId, dialogId, v.getAsString(Tables.Columns.BODY))) || notificationsChanged;
            } else if (v.containsKey(Tables.Columns.LOCAL_STATUS) && v.getAsInteger(Tables.Columns.LOCAL_STATUS) == 1 && v.getAsLong(Tables.Columns.WRITER_ID) != me) {
                notificationsChanged = Notifications.remove(mId) || notificationsChanged;
            }

            return ContentUris.withAppendedId(CONTENT_URI_MESSAGE, mId);
        }
    }

    @Override
    public int delete(Uri uri, String where, String[] params) {
        if (Logs.enabled && DEBUG) {
            Logs.d(TAG, "delete(): " + uri + "; where: " + where + "; params: " + Arrays.toString(params));
        }

        if (!db.inTransaction()) {
            throw new RuntimeException("operation must be executed only in batch");
        }

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PROFILE: {
                return db.delete(Tables.PROFILE, where, params);
            }
            case MESSAGE: {
                final Set<Long> dialogs = DatabaseTools.fetchLongColumnAndClose(db.query(Tables.MESSAGE, Queries.ONLY_DIALOG_ID_PROJECTION, where, params, null, null, null));
                dialogsChanged.addAll(dialogs);
                return db.delete(Tables.MESSAGE, where, params);
            }
            case DIALOG: {
                return db.delete(Tables.DIALOG, where, params);
            }
        }

        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    @Override
    public int update(Uri uri, ContentValues v, String where, String[] params) {
        if (Logs.enabled && DEBUG)
            Logs.d(TAG, "update " + uri + ": " + v + "; " + where + "; params: " + Arrays.toString(params));

        final int match = sUriMatcher.match(uri);
        if (!db.inTransaction()) {
            throw new RuntimeException("Operation must be processed through batch");
        }

        switch (match) {
            case DIALOG: {
                final Set<Long> dialogs = DatabaseTools.fetchLongColumnAndClose(db.query(Tables.DIALOG, Queries._ID_ONLY, where, params, null, null, null));
                chatsChanged.addAll(dialogs);
                return db.update(Tables.DIALOG, v, where, params);
            }

            case PROFILE: {
                if (v.containsKey(Tables.Columns.CONTACT)) {
                    contactsChanged = true;
                }

                if (v.containsKey(Tables.Columns.SEARCH)) {
                    searchChanged = true;
                }

                final Set<Long> userIds = DatabaseTools.fetchLongColumnAndClose(
                        db.query(Tables.PROFILE, Queries._ID_ONLY, where, params, null, null, null));
                final Set<Long> dialogs = DatabaseTools.fetchLongColumnAndClose(db.query(Tables.DIALOG, Queries._ID_ONLY, "user_id in (" +
                        DatabaseTools.idsToString(userIds) + ")", null, null, null, null));

                dialogsChanged.addAll(dialogs);
                friendsChanged = true;
                return db.update(Tables.PROFILE, v, where, params);
            }
            case MESSAGE: {
                /**
                 * ID диалогов, которые нужно будет перезагрузить после обновления (оповестить)
                 */
                Set<Long> dialogIds = new HashSet<Long>();
                /**
                 * id изменяемых сообщений
                 */
                Set<Long> ids = new HashSet<Long>();
                /**
                 * ID сообщений, которые прочли (у которых local_status поменялся на 1)
                 */
                Set<Long> readIds = new HashSet<Long>();
                final Cursor c = db.query(Tables.MESSAGE, Queries.CHECK_MESSAGE_COLUMN_PROJECTION, where, params, null, null, null);
                try {
                    long me = Init.getUserId();
                    while (c.moveToNext()) {
                        final long id = c.getLong(Queries.CheckMsgColumns._ID);
                        if (me != c.getLong(Queries.CheckMsgColumns.WRITER_ID)) {
                            readIds.add(id);
                        }

                        ids.add(id);
                        dialogIds.add(c.getLong(Queries.CheckMsgColumns.DIALOG_ID));
                    }
                } finally {
                    c.close();
                }

                //оповестить интерфейс об изменении данных в конкретных диалогах
                dialogsChanged.addAll(dialogIds);

                if (v.containsKey(Tables.Columns.LOCAL_STATUS) && v.getAsInteger(Tables.Columns.LOCAL_STATUS) == 1
                        && ids.size() > 0) {
                    if (Logs.enabled && DEBUG)
                        Logs.d(TAG, "setting read local status to 1");
                    new SendInformationTask().execute();

                    if (Logs.enabled && DEBUG)
                        Logs.d(TAG, "removing " + readIds + " from incoming Notifications");
                    for (Long id : readIds) {
                        notificationsChanged = Notifications.remove(id) || notificationsChanged;
                    }
                }

                return db.update(Tables.MESSAGE, v, where, params);
            }
        }

        throw new UnsupportedOperationException();
    }

    @Override
    /**
     * переопределил метод, чтобы быстрее работало - используются транзакции
     */
    public synchronized ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
        dialogsChanged.clear();
        chatsChanged.clear();
        friendsChanged = false;
        searchChanged = false;
        notificationsChanged = false;
        contactsChanged = false;
        long start = System.currentTimeMillis();
        db.beginTransaction();
        try {
            final ContentProviderResult[] results = super.applyBatch(operations);

            if (dialogsChanged.size() > 0) {
                if (Logs.enabled && DEBUG) {
                    Logs.d(TAG, "updating last message for dialogs: " + dialogsChanged);
                }

                db.execSQL("update dialog set last_message_id = (select max(_id) from message where dialog_id = dialog._id and deleted is null) where _id in (" + DatabaseTools.idsToString(dialogsChanged) + ")");
                db.execSQL("delete from dialog where (select count(1) from message where dialog_id = dialog._id) = 0 and _id in (" + DatabaseTools.idsToString(dialogsChanged) + ")");
            }

            db.setTransactionSuccessful();
            return results;
        } finally {
            db.endTransaction();

            if (Logs.enabled && DEBUG)
                Logs.d(TAG, "applyBatch() dialogsChanged=" + dialogsChanged + "; execution time=" + (System.currentTimeMillis() - start));

            for (Long dialogId : dialogsChanged) {
                getContext().getContentResolver().notifyChange(ContentUris.withAppendedId(CONTENT_URI_MESSAGE, dialogId), null);
            }

            for (Long dialogId : chatsChanged) {
                getContext().getContentResolver().notifyChange(ContentUris.withAppendedId(CONTENT_URI_DIALOG, dialogId), null);
            }

            if (chatsChanged.size() > 0 || dialogsChanged.size() > 0)
                getContext().getContentResolver().notifyChange(CONTENT_URI_DIALOG, null);

            if (friendsChanged) {
                getContext().getContentResolver().notifyChange(CONTENT_URI_FRIEND, null);
            }

            if (contactsChanged) {
                getContext().getContentResolver().notifyChange(CONTENT_URI_CONTACT, null);
            }

            if (searchChanged) {
                getContext().getContentResolver().notifyChange(CONTENT_URI_SEARCH, null);
            }

            if (notificationsChanged) {
                Notifications.update();
            }
        }
    }

    /**
     * Очистить данные синхронно
     */
    public static void clearDataSync() {
        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        operations.add(ContentProviderOperation.newDelete(CONTENT_URI_DIALOG).build());
        operations.add(ContentProviderOperation.newDelete(CONTENT_URI_MESSAGE).build());
        operations.add(ContentProviderOperation.newDelete(CONTENT_URI_PROFILE).build());
        b(operations);
    }

    @SuppressWarnings("unchecked")
    public static void clearData() {
        if (Logs.enabled && DEBUG) {
            Logs.d(TAG, "clearData()");
        }

        new ModernAsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                clearDataSync();
                return null;
            }
        }.execute();
    }

    public static ContentProviderResult[] b(ArrayList<ContentProviderOperation> operations) {
        try {
            return VKMessenger.getCtx().getContentResolver().applyBatch(CONTENT_AUTHORITY, operations);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Cursor q(android.net.Uri uri, java.lang.String[] projection, java.lang.String selection, java.lang.String[] selectionArgs, java.lang.String sortOrder) {
        return VKMessenger.getCtx().getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
    }

    public static void notifyChange(Uri uri) {
        VKMessenger.getCtx().getContentResolver().notifyChange(uri, null);
    }

    public static boolean checkWriter(long id) {
        return DatabaseTools.fetchLONGAndClose(
                q(CONTENT_URI_PROFILE, Queries._ID_ONLY, Queries.SELECTION_ID, new String[]{Long.toString(id)}, null))
                != null;
    }
}
