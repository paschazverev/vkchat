package ru.nacu.vkmsg;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import com.google.android.c2dm.C2DMessaging;
import com.perm.kate.api.Api;
import ru.android.common.logs.Logs;
import ru.nacu.vkmsg.asynctasks.LoadFriendsTask;
import ru.nacu.vkmsg.updates.PushReceiver;

/**
 * @author quadro
 * @since 6/21/12 12:41 PM
 */
public final class Init {
    public static final long ONE_DAY = 3600 * 24 * 1000;
    public static final int TIMER_INTENT_CODE = 239087;
    public static final String TIMER_INTENT = "ru.nacu.vkmsg.Timer";

    public static final String TAG = "Init";

    private static long userId = -1;
    private static String userToken;
    private static long friendsUpdateTime = -1;
    private static volatile long lastTimestamp = -1;

    private static boolean connectAlways = false;
    private static boolean usePush = false;
    private static boolean autoStart;
    private static int pollTime;

    private static boolean timerRegistered;

    public static synchronized long getUserId() {
        if (userId == -1) {
            userId = VKMessenger.getSp().getLong("user_id", 0);
        }

        return userId;
    }

    public static synchronized void setUserId(long userId) {
        Init.userId = userId;

        final SharedPreferences.Editor e = VKMessenger.getSp().edit();
        e.putLong("user_id", userId);
        e.commit();
    }

    public static synchronized long getFriendsUpdateTime() {
        if (friendsUpdateTime == -1) {
            friendsUpdateTime = VKMessenger.getSp().getLong("friends_update", 0);
        }

        return friendsUpdateTime;
    }

    public static synchronized void setFriendsUpdateTime(long friendsUpdateTime) {
        Init.friendsUpdateTime = friendsUpdateTime;

        final SharedPreferences.Editor e = VKMessenger.getSp().edit();
        e.putLong("friends_update", friendsUpdateTime);
        e.commit();
    }

    public static synchronized String getUserToken() {
        if (userToken == null) {
            userToken = VKMessenger.getSp().getString("user_token", "");
        }

        return userToken;
    }

    public static synchronized void setUserToken(String token) {
        userToken = token;

        final SharedPreferences.Editor e = VKMessenger.getSp().edit();
        e.putString("user_token", token);
        e.commit();
    }

    public static long getLastTimestamp() {
        if (lastTimestamp == -1) {
            lastTimestamp = VKMessenger.getSp().getLong("last_timestamp", 0);
        }

        return lastTimestamp;
    }

    public static void setLastTimestamp(long lastTimestamp) {
        Init.lastTimestamp = lastTimestamp;
        final SharedPreferences.Editor e = VKMessenger.getSp().edit();
        e.putLong("last_timestamp", lastTimestamp);
        e.commit();
    }

    public static boolean isConnectAlways() {
        return connectAlways;
    }

    public static boolean isUsePush() {
        return usePush;
    }

    /**
     * Читает настройки и инициализирует нужные сервисы
     *
     * @param appCreate true, если вызван метод при создании (false, если при перечитывании настроек)
     */
    public static void init(boolean appCreate) {
        final SharedPreferences sp = VKMessenger.getSp();
        final boolean connectAlways = sp.getBoolean("always_connect", true);
        final boolean usePush = "PUSH".equals(sp.getString("connection_type", "PUSH"));
        final boolean autoStart = sp.getBoolean("autostart", false);
        final int pollTime = Integer.parseInt(sp.getString("poll_time", "5"));

        boolean newPush = connectAlways && usePush && Build.VERSION.SDK_INT > 7;
        boolean newPoll = connectAlways && !usePush || (Build.VERSION.SDK_INT <= 7 && connectAlways);
        boolean oldPush = Init.connectAlways && Init.usePush;
        boolean oldPoll = Init.connectAlways && !Init.usePush;

        if (oldPoll && !newPoll || (pollTime != Init.pollTime && oldPoll && newPoll)) {
            if (Logs.enabled) {
                Logs.d(TAG, "disabling poll");
            }

            stopPoll();
            timerRegistered = false;
        }

        if (oldPush && !newPush) {
            if (Logs.enabled) {
                Logs.d(TAG, "disabling push");
            }

            stopPush();
        }

        if (newPush && !oldPush) {
            if (Logs.enabled) {
                Logs.d(TAG, "starting push");
            }

            if (getUserId() != 0) {
                startPush();
            }
        }

        if (newPoll && !oldPoll || (pollTime != Init.pollTime && newPoll && oldPoll)) {
            if (Logs.enabled) {
                Logs.d(TAG, "starting poll; appCreate=" + appCreate + "; autoStart=" + autoStart);
            }

            if (!appCreate || autoStart && getUserId() != 0) {
                Init.pollTime = pollTime;
                startPoll();
            }
        }

        Init.connectAlways = connectAlways;
        Init.usePush = usePush;
        Init.autoStart = autoStart;
        Init.pollTime = pollTime;

        if (appCreate) {
            tryUpdateFriends();
        }
    }

    public static void stopPoll() {
        AlarmManager am = (AlarmManager) VKMessenger.getCtx().getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = PendingIntent.getBroadcast(VKMessenger.getCtx(), TIMER_INTENT_CODE, new Intent(TIMER_INTENT), 0);

        am.cancel(pi);
        timerRegistered = false;
    }

    private static void stopPush() {
        PushReceiver.unregisterDevice();
        C2DMessaging.unregister(VKMessenger.getCtx());
    }

    public static void startPush() {
        C2DMessaging.register(VKMessenger.getCtx(), Constants.C2DN_PUBLISHER_EMAIL);
    }

    private static void startPoll() {
        AlarmManager am = (AlarmManager) VKMessenger.getCtx().getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = PendingIntent.getBroadcast(VKMessenger.getCtx(), TIMER_INTENT_CODE, new Intent(TIMER_INTENT), 0);

        long firstTime = SystemClock.elapsedRealtime();
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime, pollTime * 60 * 1000, pi);
        timerRegistered = true;
    }

    public static void checkPollStart() {
        if (!timerRegistered && connectAlways && !usePush) {
            startPoll();
        }
    }

    public static void checkPushStart() {
        if (connectAlways && usePush) {
            startPush();
        }
    }

    @SuppressWarnings("unchecked")
    public static void tryUpdateFriends() {
        if (getUserId() != 0 && (System.currentTimeMillis() - getFriendsUpdateTime()) > ONE_DAY) {
            new LoadFriendsTask().execute();
        }
    }

    public static void updateToken(long userId, String token) {
        setUserId(userId);
        setUserToken(token);
        setFriendsUpdateTime(0);

        if (userId != 0) {
            VKMessenger.api = new Api(getUserToken(), Constants.API_ID);

            if (usePush && connectAlways) {
                startPush();
            } else if (connectAlways && !usePush) {
                startPoll();
            }

            tryUpdateFriends();
        } else {
            if (usePush && connectAlways) {
                stopPush();
            } else if (!usePush && connectAlways) {
                stopPoll();
            }
        }
    }
}
