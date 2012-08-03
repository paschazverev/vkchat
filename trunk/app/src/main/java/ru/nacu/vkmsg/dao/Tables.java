package ru.nacu.vkmsg.dao;

/**
 * @author quadro
 * @since 6/21/12 6:02 PM
 */
public interface Tables {
    String FLAG = "flag";
    String DIALOG = "dialog";
    String MESSAGE_DIALOG_V = "message_dialog_v";
    String DIALOG_V = "dialog_v";
    String PROFILE = "profile";
    String MESSAGE = "message";
    String MESSAGE_V = "message_v";

    interface Columns {
        String _ID = "_id";
        String TITLE = "title";
        String BODY = "body";
        String CHAT_ID = "chat_id";
        String USER_ID = "user_id";
        String WRITER_ID = "writer_id";
        String USER_IDS = "user_ids";
        String DIALOG_ID = "dialog_id";
        String MESSAGE_ID = "message_id";
        String DT = "dt";
        String FIRST_NAME = "first_name";
        String LAST_NAME = "last_name";
        String BDATE = "bdate";
        String SEX = "sex";
        String PHOTO = "photo";
        String PHOTO_BIG = "photo_big";
        String SEARCH = "search";
        String DELETED = "deleted";
        String REGULAR = "regular";
        String FRIEND = "friend";
        String POP = "pop";
        String PHONE = "phone";
        String ONLINE = "online";
        String LOCAL_STATUS = "local_status";
        String SERVER_STATUS = "server_status";
        String CONTACT = "contact";
        String PHONE_NAME = "phone_name";
        String SEARCH_STRING = "search_string";
        String ATTACHMENT = "attachment";
    }
}
