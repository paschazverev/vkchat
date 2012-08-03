package ru.nacu.vkmsg.ui.progress;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.KeyEvent;
import android.widget.TextView;
import ru.android.common.task.ModernAsyncTask;
import ru.nacu.vkmsg.R;
import ru.nacu.vkmsg.VKMessenger;

import java.io.Serializable;

/**
 * @author quadro
 * @since 03.04.11 19:56
 */
public final class ProgressDialog extends Activity {
    private ProgressDialogTask task;

    public static void showProgressDialog(Activity context, ProgressDialogTask task) {
        Intent intent = new Intent(context, ProgressDialog.class);
        if (task instanceof Serializable) {
            intent.putExtra("serializable", true);
            intent.putExtra("task", (Serializable) task);
        } else if (task instanceof Parcelable) {
            intent.putExtra("serializable", false);
            intent.putExtra("task", (Parcelable) task);
        } else {
            throw new RuntimeException("Task should be serializable or parcelable");
        }
        context.startActivity(intent);
    }

    @SuppressWarnings({"unchecked"})
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.progress);
        TextView tv = (TextView) findViewById(R.id.progress_text);

        if (getIntent().getBooleanExtra("serializable", false)) {
            task = (ProgressDialogTask) getIntent().getSerializableExtra("task");
        } else {
            task = (ProgressDialogTask) getIntent().getParcelableExtra("task");
        }

        task.processText(tv);

        VKMessenger.checkMainThread();
        new ModernAsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                task.run(ProgressDialog.this);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                task.onPostExecute(ProgressDialog.this);
                finish();
            }
        }.execute();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean b = super.onKeyDown(keyCode, event);
        return keyCode == KeyEvent.KEYCODE_BACK || b;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.progress);
    }
}