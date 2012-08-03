package ru.nacu.vkmsg;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Window;
import android.widget.Toast;
import ru.android.common.logs.Logs;
import ru.nacu.vkmsg.dao.VKContentProvider;
import ru.nacu.vkmsg.ui.login.LoginActivity;

/**
 * @author quadro
 * @since 6/20/12 4:38 PM
 */
public final class MainActivity extends Activity {
    public static final String TAG = "MainActivity";

    public static final int LOGIN = 1;

    public static boolean checked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        long start = System.currentTimeMillis();
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean changed = checked || sp.getBoolean("changed_token", false);

        if (Init.getUserId() == 0 || !changed) {
            final SharedPreferences.Editor e = sp.edit();
            e.putBoolean("changed_token", true);
            e.commit();
            checked = true;

            if (Init.getUserId() != 0) {
                Init.updateToken(0, null);
                Flags.clear();
                VKContentProvider.clearData();
                Toast.makeText(this, R.string.pls_reauth, Toast.LENGTH_LONG).show();
            }

            Logs.d(TAG, "starting LoginActivity in onCreate");
            startActivityForResult(new Intent(this, LoginActivity.class), LOGIN);
        } else {
            checked = true;
            Logs.d(TAG, "starting App Activity in onCreate");
            startActivity(new Intent(this, PhoneActivity.class));
            finish();
        }

        Logs.d(TAG, "onCreate() time=" + (System.currentTimeMillis() - start));

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case LOGIN: {
                if (data != null) {
                    Init.updateToken(data.getLongExtra("user_id", 0), data.getStringExtra("user_token"));
                    Logs.d(TAG, "starting App Activity in onActivityResule");
                    startActivity(new Intent(this, PhoneActivity.class));
                    finish();
                } else {
                    //todo обработка того, что не возможно залогиниться
                    Logs.d(TAG, "can't login. exiting");
                    finish();
                }

                break;
            }
        }
    }
}
