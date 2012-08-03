package ru.nacu.vkmsg.updates;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.google.android.c2dm.C2DMessaging;
import ru.android.common.task.ModernAsyncTask;
import ru.nacu.vkmsg.Init;

/**
 * @author quadro
 * @since 7/6/12 1:27 PM
 */
public final class RetryPushReceiver extends BroadcastReceiver {
    public static final String RETRY_FULL = "ru.nacu.vkmsg.RetryPushFull";
    public static final String RETRY_REGISTER_DEVICE = "ru.nacu.vkmsg.RetryRegisterPush";

    @Override
    public void onReceive(Context context, Intent intent) {
        ModernAsyncTask.THREAD_POOL_EXECUTOR.execute(PushReceiver.checkUpdates);

        if (RETRY_FULL.equals(intent.getAction())) {
            Init.startPush();
        } else {
            PushReceiver.registerDevice(C2DMessaging.getRegistrationId(context));
        }
    }
}
