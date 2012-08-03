package ru.nacu.vkmsg.updates;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import ru.android.common.logs.Logs;
import ru.android.common.task.ModernAsyncTask;

/**
 * @author quadro
 * @since 7/1/12 11:40 PM
 */
public final class TimerReceiver extends BroadcastReceiver {
    public static final String TAG = "TimerReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Logs.enabled) {
            Logs.d(TAG, "onReceive()");
        }

        if (!LongPoll.isRunning()) {
            ModernAsyncTask.THREAD_POOL_EXECUTOR.execute(PushReceiver.checkUpdates);
        }

    }
}
