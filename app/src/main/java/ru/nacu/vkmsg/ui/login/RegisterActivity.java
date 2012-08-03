package ru.nacu.vkmsg.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import ru.nacu.vkmsg.R;

/**
 * @author quadro
 * @since 6/21/12 12:01 PM
 */
public final class RegisterActivity extends SherlockFragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.register_activity);
        setResult(RESULT_CANCELED);
    }
}
