package ru.nacu.vkmsg.updates;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import ru.nacu.vkmsg.ErrorActivity;
import ru.nacu.vkmsg.R;

/**
 * @author quadro
 * @since 6/27/12 10:26 AM
 */
public final class LongPollService extends Service {
    public static final String ERROR_INTENT = "ru.nacu.vkmsg.PollError";
    public static final String OK_INTENT = "ru.nacu.vkmsg.PollSuccess";

    public static final int POLL_NOTIFICATION_ID = 1;
    public static final int POLL_NOTIFICATION_ID_2 = 2;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ERROR_INTENT.equals(intent.getAction())) {
                final Notification n = new Notification();
                n.icon = android.R.drawable.stat_notify_error;
                n.setLatestEventInfo(context, getString(R.string.service_error), getString(R.string.network_error),
                        PendingIntent.getActivity(context, POLL_NOTIFICATION_ID_2, new Intent(context, ErrorActivity.class), 0));
                startForeground(POLL_NOTIFICATION_ID, n);
            } else if (OK_INTENT.equals(intent.getAction())) {
                stopForeground(true);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        registerReceiver(statusReceiver, new IntentFilter(ERROR_INTENT));
        registerReceiver(statusReceiver, new IntentFilter(OK_INTENT));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(statusReceiver);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        final Notification n = new Notification();
        n.icon = R.drawable.stat_notify_service;
        n.setLatestEventInfo(this, getString(R.string.service_running), getString(R.string.network_error), null);
    }
}
