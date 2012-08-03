package ru.nacu.vkmsg.ui.progress;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;

/**
 * @author quadro
 * @since 03.04.11 20:32
 */
public abstract class ProgressDialogTask {
    protected volatile boolean success;

    public void processText(TextView tv) {
        tv.setVisibility(View.GONE);
    }

    public abstract void run(Activity ctx);

    public abstract void onPostExecute(Activity ctx);
}
