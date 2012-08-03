package ru.nacu.vkmsg.dao;

/**
 * @author quadro
 * @since 6/22/12 4:32 PM
 */
public interface Queries {
    String[] _ID_ONLY = new String[]{Tables.Columns._ID};

    String SELECTION_CHAT_ID = "chat_id = ?";
    String SELECTION_NOT_DELETED = "deleted is null and regular = 1";
    String SELECTION_DELETED = "deleted  = 1";
    String SELECTION_USER_ID = "user_id = ?";
    String SELECTION_ID = "_id = ?";
    String SELECTION_CONTACT = "contact = 1";
    String SELECTION_FILTER = "search_string like ?";
    String SELECTION_SEARCH_ONLY = "search is not null";
    String SELECTION_FRIENDS_ONLINE = "online = 1 and friend = 1";
    String SELECTION_FRIENDS_ONLINE_FILTER = "online = 1 and friend = 1 and search_string like ?";
    String SELECTION_FRIENDS = "friend = 1";
    String SELECTION_FRIENDS_FILTER = "friend = 1 and search_string like ?";
    String SELECTION_FRIENDS_SUGGESTIONS = "friend = 2 or friend = 3 or friend = 4";
    String SELECTION_UNSENT_MESSAGES = "_id > 1000000000000";
    String SELECTION_WITH_MESSAGE = "last_message_id is not null and regular = 1";
    String SELECTION_DIALOG = "dialog_id = ?";
    String SELECTION_DIALOG_NOT_MARKED = "writer_id <> ? and dialog_id = ? and local_status <> 1 and server_status <> 1";
    String SELECTION_DIALOG_MARKED = "writer_id <> ? and local_status = 1 and server_status <> 1";

    String[] MESSAGE_PROJECTION = new String[]{
            Tables.Columns._ID, Tables.Columns.PHOTO, Tables.Columns.FIRST_NAME,
            Tables.Columns.LAST_NAME, Tables.Columns.DT,
            Tables.Columns.BODY, Tables.Columns.WRITER_ID, Tables.Columns.DIALOG_ID,
            Tables.Columns.LOCAL_STATUS, Tables.Columns.SERVER_STATUS, Tables.Columns.ATTACHMENT,
            Tables.Columns.SEARCH
    };

    interface MessageColumns {
        int _ID = 0;
        int PHOTO = 1;
        int FIRST_NAME = 2;
        int LAST_NAME = 3;
        int DT = 4;
        int BODY = 5;
        int WRITER_ID = 6;
        int DIALOG_ID = 7;
        int LOCAL_STATUS = 8;
        int SERVER_STATUS = 9;
        int ATTACHMENT = 10;
        int SEARCH = 11;
    }

    String DT_DESC = Tables.Columns.DT + " desc";
    String DT_ASC = Tables.Columns.DT;

    String[] SEND_PROJECTION = new String[]{
            Tables.Columns._ID, Tables.Columns.PHOTO, Tables.Columns.FIRST_NAME,
            Tables.Columns.LAST_NAME, Tables.Columns.TITLE,
            Tables.Columns.BODY, Tables.Columns.USER_ID,
            Tables.Columns.CHAT_ID, Tables.Columns.ATTACHMENT
    };

    interface SendColumns {
        int _ID = 0;
        int PHOTO = 1;
        int FIRST_NAME = 2;
        int LAST_NAME = 3;
        int TITLE = 4;
        int BODY = 5;
        int USER_ID = 6;
        int CHAT_ID = 7;
        int ATTACHMENT = 8;
    }

    String[] DIALOG_PROJECTION = new String[]{
            Tables.Columns._ID, Tables.Columns.PHOTO, Tables.Columns.FIRST_NAME,
            Tables.Columns.LAST_NAME, Tables.Columns.DT, Tables.Columns.TITLE,
            Tables.Columns.BODY, Tables.Columns.WRITER_ID, Tables.Columns.USER_ID,
            Tables.Columns.CHAT_ID, Tables.Columns.LOCAL_STATUS,
            Tables.Columns.MESSAGE_ID, Tables.Columns.ONLINE, Tables.Columns.USER_IDS,
            "writer_first_name", "writer_last_name"
    };

    interface DialogColumns {
        int _ID = 0;
        int PHOTO = 1;
        int FIRST_NAME = 2;
        int LAST_NAME = 3;
        int DT = 4;
        int TITLE = 5;
        int BODY = 6;
        int WRITER_ID = 7;
        int USER_ID = 8;
        int CHAT_ID = 9;
        int LOCAL_STATUS = 10;
        int MESSAGE_ID = 11;
        int ONLINE = 12;
        int USER_IDS = 13;
        int WRITER_FIRST_NAME = 14;
        int WRITER_LAST_NAME = 15;
    }

    String[] ONLY_DIALOG_ID_PROJECTION = new String[]{Tables.Columns.DIALOG_ID};

    String[] CHECK_MESSAGE_COLUMN_PROJECTION = new String[]{Tables.Columns._ID, Tables.Columns.DIALOG_ID, Tables.Columns.WRITER_ID};

    interface CheckMsgColumns {
        int _ID = 0;
        int DIALOG_ID = 1;
        int WRITER_ID = 2;
    }

    String[] MAX_MSG_PROJECTION = new String[]{"max(_id)"};
    String SELECTION_MAX_MSG = "_id <= 1000000000000";
}
