package ru.nacu.vkmsg;

import android.os.Environment;

import java.io.File;

/**
 * @author quadro
 * @since 6/21/12 11:45 AM
 */
public final class Constants {
    public static final int LOADER_CHATS = 3000;
    public static final int LOADER_CHAT = 30001;
    public static final int LOADER_CONTACTS = 30005;
    public static final int LOADER_CHAT_TITLE = 30006;
    public static final int LOADER_SELECT_CONTACTS = 30007;
    public static final int LOADER_GROUP_MEMBERS_TITLE = 30009;
    public static final int LOADER_GROUP_MEMBERS = 30010;
    public static final int LOADER_SEARCH = 30011;
    public static final int LOADER_PROFILE = 30012;
    public static final int LOADER_SETTINGS = 30013;

    public static final String C2DN_PUBLISHER_EMAIL = "evgeny.nacu@gmail.com";

    public static final long MAX_USER_ID = 2000000000;

    public static final String API_ID = "3005073";
    public static final String API_SECRET = "urWZIf1zcp7XRKl2Ijm6";
    public static final String PREFIX = "ru.nacu.vkmsg.prefix.";

    public static final File sdHome = new File(Environment.getExternalStorageDirectory(), "Android/data/ru.nacu.vkmsg");
    public static final File downloads = new File(Environment.getExternalStorageDirectory(), "Download");
    public static final File thumbs = new File(sdHome, "thumbs");
    public static final File attachments = new File(sdHome, "attachments");
}
