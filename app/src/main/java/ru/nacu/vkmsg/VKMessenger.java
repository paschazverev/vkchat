package ru.nacu.vkmsg;

import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import com.perm.kate.api.Api;
import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import ru.android.common.logs.Logs;
import ru.nacu.vkmsg.asynctasks.SendInformationTask;
import ru.nacu.vkmsg.ui.chat.ChatActivity;

/**
 * @author quadro
 * @since 6/21/12 12:43 PM
 */
@ReportsCrashes(formKey = "dHdRdnplV0o0S0dkcTkwcFhMdWlMeWc6MQ")
public final class VKMessenger extends Application {
    private static volatile Context ctx;
    private static volatile SharedPreferences sp;
    static volatile Api api;
    private static volatile Handler handler;

    private static volatile Thread main;

    public static void checkMainThread() {
        if (Thread.currentThread() != main) {
            throw new RuntimeException("Is not in main thread");
        }
    }

    public static Handler getHandler() {
        return handler;
    }

    public static Api getApi() {
        return api;
    }

    public static Context getCtx() {
        return ctx;
    }

    public static SharedPreferences getSp() {
        return sp;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate() {
        ACRA.init(this);
        super.onCreate();
        main = Thread.currentThread();
        Logs.TAG = "VKMessenger";
        Logs.enabled = true;
        ctx = getApplicationContext();
        sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        handler = new Handler();
        if (Init.getUserId() != 0) {
            api = new Api(Init.getUserToken(), Constants.API_ID);
        }

        Flags.load();
        Init.init(true);
        new SendInformationTask().execute();
    }

    public static final int OPEN_CHATS = 500000000;
    public static final int OPEN_CHAT = 600000000;

    public static PendingIntent openChat(long dialogId, long userId, long chatId) {
        final Intent intent = new Intent(getCtx(), ChatActivity.class);
        intent.putExtra("dialogId", dialogId);
        intent.putExtra("chatId", chatId);
        intent.putExtra("userId", userId);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(getCtx(), (int) (OPEN_CHAT + dialogId), intent, 0);
    }

    public static PendingIntent openChats() {
        final Intent intent = new Intent(getCtx(), PhoneActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(getCtx(), OPEN_CHATS, intent, 0);
    }

}
