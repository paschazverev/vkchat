package ru.nacu.vkmsg.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import ru.android.common.logs.Logs;
import ru.nacu.vkmsg.R;

/**
 * @author quadro
 * @since 6/21/12 12:01 PM
 */
public final class LoginActivity extends SherlockFragmentActivity implements LoginFragment.LoginFragmentHost {
    public static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.login_activity);
        setResult(RESULT_CANCELED);
    }

    @Override
    public void onSignup(Intent data) {
        if (data != null) {
            setResult(RESULT_OK, data);
        }

        finish();
        Logs.d(TAG, "finish()");
    }

    @Override
    public void onRegister() {
        startActivity(new Intent(this, RegisterActivity.class));
    }
}
