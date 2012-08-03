package ru.nacu.vkmsg.updates;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import com.google.android.c2dm.C2DMBaseReceiver;
import com.google.android.c2dm.C2DMessaging;
import ru.android.common.logs.Logs;
import ru.android.common.task.ModernAsyncTask;
import ru.nacu.vkmsg.Constants;
import ru.nacu.vkmsg.VKMessenger;

/**
 * @author quadro
 * @since 6/26/12 3:05 PM
 */
public final class PushReceiver extends C2DMBaseReceiver {
    public static final int RETRY_CODE = 976336;
    public static final int RETRY_CODE_2 = 976337;

    public static final String TAG = "PushReceiver";

    public PushReceiver() {
        super(Constants.C2DN_PUBLISHER_EMAIL);
    }

    public static final Runnable checkUpdates = new CheckUpdates();

    @Override
    protected void onMessage(Context context, Intent intent) {
        Logs.d(TAG, "onMessage(); keys: " + intent.getExtras().keySet());
        if ("vkmsg".equals(intent.getStringExtra("collapse_key"))) {
            if (!LongPoll.isRunning()) {
                Logs.d(TAG, "loading changes after PUSH notification");
                ModernAsyncTask.THREAD_POOL_EXECUTOR.execute(checkUpdates);
            }
        }
    }

    @Override
    public void onError(Context context, String errorId) {
        Logs.d(TAG, "onError() " + errorId);
        AlarmManager am = (AlarmManager) VKMessenger.getCtx().getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, System.currentTimeMillis() + 10 * 60000,
                PendingIntent.getBroadcast(context, RETRY_CODE, new Intent(RetryPushReceiver.RETRY_FULL), 0));
    }

    @Override
    public void onRegistration(Context context, final String registrationId) {
        Logs.d(TAG, "onRegistration() " + registrationId);
        registerDevice(registrationId);
    }

    @SuppressWarnings("unchecked")
    public static void registerDevice(final String registrationId) {
        new ModernAsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    final String s = VKMessenger.getApi().registerDevice(registrationId,
                            Build.DEVICE + " " + Build.MODEL, Build.VERSION.SDK, 0);

                    Logs.d(TAG, "registerDevice result: " + s);
                } catch (Exception e) {
                    Logs.d(TAG, e.getMessage(), e);
                    AlarmManager am = (AlarmManager) VKMessenger.getCtx().getSystemService(Context.ALARM_SERVICE);
                    am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, System.currentTimeMillis() + 10 * 60000,
                            PendingIntent.getBroadcast(VKMessenger.getCtx(), RETRY_CODE_2,
                                    new Intent(RetryPushReceiver.RETRY_REGISTER_DEVICE), 0));
                }
                return null;
            }
        }.execute();
    }

    @SuppressWarnings("unchecked")
    public static void unregisterDevice() {
        new ModernAsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    VKMessenger.getApi().unregisterDevice(C2DMessaging.getRegistrationId(VKMessenger.getCtx()));
                } catch (Exception e) {
                    //ignore
                    Logs.d(TAG, e.getMessage(), e);
                }

                return null;
            }
        }.execute();
    }

    private static class CheckUpdates implements Runnable, Comparable<ModernAsyncTask.HasPriority>, ModernAsyncTask.HasPriority {
        @Override
        public int compareTo(ModernAsyncTask.HasPriority other) {
            return getTaskPriority() > other.getTaskPriority() ? 1 : getTaskPriority() == other.getTaskPriority() ? 0 : -1;
        }

        @Override
        public int getTaskPriority() {
            return 0;
        }

        @Override
        public void run() {
            LongPoll.runServer(true);
        }
    }
}
