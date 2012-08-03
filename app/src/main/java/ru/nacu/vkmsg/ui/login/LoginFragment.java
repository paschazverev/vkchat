package ru.nacu.vkmsg.ui.login;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragment;
import com.perm.kate.api.Auth;
import com.perm.kate.api.CaptchaError;
import ru.android.common.logs.Logs;
import ru.nacu.vkmsg.R;
import ru.nacu.vkmsg.VKMessenger;
import ru.nacu.vkmsg.ui.progress.ProgressDialog;
import ru.nacu.vkmsg.ui.progress.ProgressDialogTask;

import java.io.IOException;
import java.io.Serializable;

/**
 * @author quadro
 * @since 6/21/12 11:46 AM
 */
public final class LoginFragment extends SherlockFragment implements View.OnClickListener {
    public static final String TAG = "LoginFragment";

    public static final String LOGIN_INTENT = "ru.nacu.vkmsg.Login";

    private View signin;
    private EditText teId;
    private EditText tePass;

    public static final int CAPTCHA_CODE = 1;

    private final BroadcastReceiver loginReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Logs.d(TAG, "loginReceiver: " + intent + "; extras: " + intent.getExtras());
            if (intent.hasExtra("captcha")) {
                startActivityForResult(new Intent(context, CaptchaActivity.class)
                        .putExtra("sid", intent.getStringExtra("sid"))
                        .putExtra("url", intent.getStringExtra("img")), CAPTCHA_CODE);
            } else if (intent.hasExtra("error")) {
                error.setVisibility(View.VISIBLE);
                errorText.setText(R.string.login_error);
            } else if (intent.hasExtra("exception")) {
                error.setVisibility(View.VISIBLE);
                errorText.setText(R.string.unexpected_error);
            } else {
                getHost().onSignup(intent);
            }
        }
    };

    private View error;
    private View signup;
    private TextView errorText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VKMessenger.getCtx().registerReceiver(loginReceiver, new IntentFilter(LOGIN_INTENT));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        VKMessenger.getCtx().unregisterReceiver(loginReceiver);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAPTCHA_CODE && resultCode == Activity.RESULT_OK) {
            String sid = data.getStringExtra("sid");
            String key = data.getStringExtra("key");
            ProgressDialog.showProgressDialog(getActivity(), new AuthTask(teId.getText().toString(), tePass.getText().toString(), sid, key));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.login, null);
        teId = (EditText) view.findViewById(R.id.edit_id);
        tePass = (EditText) view.findViewById(R.id.edit_pass);
        signin = view.findViewById(R.id.btn_submit);
        signin.setOnClickListener(this);
        signup = view.findViewById(R.id.signup);
        signup.setOnClickListener(this);
        error = view.findViewById(R.id.error);
        errorText = (TextView) view.findViewById(R.id.error_text);

        return view;
    }

    @Override
    public void onClick(View view) {
        if (view == signin) {
            ProgressDialog.showProgressDialog(getActivity(), new AuthTask(teId.getText().toString(), tePass.getText().toString(), null, null));
        } else if (view == signup) {
            getHost().onRegister();
        }
    }

    public LoginFragmentHost getHost() {
        return (LoginFragmentHost) getActivity();
    }

    public interface LoginFragmentHost {
        void onSignup(Intent data);

        void onRegister();
    }

    private static class AuthTask extends ProgressDialogTask implements Serializable {
        private final String id;
        private final String password;
        private final String captchaSid;
        private final String captchaKey;

        private AuthTask(String id, String password, String captchaSid, String captchaKey) {
            this.id = id;
            this.password = password;
            this.captchaSid = captchaSid;
            this.captchaKey = captchaKey;
        }

        @Override
        public void run(Activity ctx) {
            try {
                final Object[] token = Auth.getToken(
                        id, password, "notify,status,messages,notifications,friends,photos,audio,video,docs,notes", captchaSid, captchaKey);

                VKMessenger.getCtx().sendBroadcast(
                        new Intent(LOGIN_INTENT).putExtra("user_id", (Long) token[1]).putExtra("user_token", (String) token[0]));
            } catch (CaptchaError e) {
                VKMessenger.getCtx().sendBroadcast(
                        new Intent(LOGIN_INTENT).putExtra("captcha", true).putExtra("sid", e.sid).putExtra("img", e.img));
            } catch (IOException e) {
                if (e.toString().contains("Received authentication challenge")) {
                    VKMessenger.getCtx().sendBroadcast(
                            new Intent(LOGIN_INTENT).putExtra("error", true));
                } else {
                    VKMessenger.getCtx().sendBroadcast(
                            new Intent(LOGIN_INTENT).putExtra("exception", true));
                }
            } catch (Exception e) {
                VKMessenger.getCtx().sendBroadcast(
                        new Intent(LOGIN_INTENT).putExtra("exception", true));
                Logs.d(TAG, e.getMessage(), e);
            }
        }

        @Override
        public void onPostExecute(Activity ctx) {
        }
    }
}
