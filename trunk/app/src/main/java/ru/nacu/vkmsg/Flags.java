package ru.nacu.vkmsg;

import android.content.Context;
import android.content.SharedPreferences;
import ru.android.common.logs.Logs;
import ru.nacu.vkmsg.dao.Tables;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Флаги для разных диалогов/сообщений - загружены или нет
 *
 * @author quadro
 * @since 6/26/12 6:03 PM
 */
public final class Flags {
    public static final String TAG = "Flags";

    private static final SharedPreferences sp = VKMessenger.getCtx().getSharedPreferences(Tables.FLAG, Context.MODE_PRIVATE);

    private final static Map<Long, Boolean> loadedDialogs = Collections.synchronizedMap(new HashMap<Long, Boolean>());
    private final static Map<Long, Boolean> fullyLoadedDialogs = Collections.synchronizedMap(new HashMap<Long, Boolean>());

    private static volatile boolean searchFullyLoaded = false;
    private static volatile boolean dialogListLoaded = false;
    private static volatile boolean dialogListFullyLoaded = false;
    private static volatile boolean contactsSynced = false;

    @SuppressWarnings("unchecked")
    public static void load() {
        loadedDialogs.clear();
        fullyLoadedDialogs.clear();
        dialogListLoaded = sp.contains("dialogs");
        contactsSynced = sp.contains("contacts");
        dialogListFullyLoaded = sp.contains("dialogs_full");
    }

    @SuppressWarnings("unchecked")
    public static void clear() {
        Logs.d(TAG, "clear()");
        dialogListLoaded = false;
        contactsSynced = false;
        dialogListFullyLoaded = false;
        loadedDialogs.clear();
        fullyLoadedDialogs.clear();
        Init.setFriendsUpdateTime(0);

        final SharedPreferences.Editor edit = sp.edit();
        edit.clear();
        edit.commit();
    }

    public static boolean isDialogLoaded(long id) {
        Boolean cached = loadedDialogs.get(id);
        if (cached != null) {
            return cached;
        } else {
            cached = sp.getBoolean("d_" + id, false);
            loadedDialogs.put(id, cached);
            return cached;
        }
    }

    public static void setDialogListFullyLoaded() {
        Flags.dialogListFullyLoaded = true;
        final SharedPreferences.Editor e = sp.edit();
        e.putBoolean("dialogs_full", true);
        e.commit();

    }

    public static boolean isDialogFullyLoaded(long id) {
        Boolean cached = fullyLoadedDialogs.get(id);
        if (cached != null) {
            return cached;
        } else {
            cached = sp.getBoolean("d_full_" + id, false);
            fullyLoadedDialogs.put(id, cached);
            return cached;
        }
    }

    public static void setDialogFullyLoaded(long id) {
        fullyLoadedDialogs.put(id, true);
        final SharedPreferences.Editor e = sp.edit();
        e.putBoolean("d_full_" + id, true);
        e.commit();
    }

    public static void setDialogLoaded(long id) {
        loadedDialogs.put(id, true);
        final SharedPreferences.Editor e = sp.edit();
        e.putBoolean("d_" + id, true);
        e.commit();
    }

    public static void setContactsSynced() {
        Flags.contactsSynced = true;
        final SharedPreferences.Editor e = sp.edit();
        e.putBoolean("contacts", true);
        e.commit();
    }

    public static boolean isContactsSynced() {
        return contactsSynced;
    }

    public static void setDialogListLoaded() {
        Flags.dialogListLoaded = true;
        final SharedPreferences.Editor e = sp.edit();
        e.putBoolean("dialogs", true);
        e.commit();
    }

    public static boolean isDialogListLoaded() {
        return dialogListLoaded;
    }

    public static boolean isDialogListFullyLoaded() {
        return dialogListFullyLoaded;
    }

    public static boolean isSearchFullyLoaded() {
        return searchFullyLoaded;
    }

    public static void setSearchFullyLoaded(boolean searchFullyLoaded) {
        Flags.searchFullyLoaded = searchFullyLoaded;
    }
}
