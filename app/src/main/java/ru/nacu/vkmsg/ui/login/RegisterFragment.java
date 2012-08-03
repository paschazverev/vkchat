package ru.nacu.vkmsg.ui.login;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragment;
import com.perm.kate.api.Auth;
import com.perm.kate.api.KException;
import org.json.JSONException;
import ru.nacu.vkmsg.R;
import ru.nacu.vkmsg.VKMessenger;
import ru.nacu.vkmsg.ui.progress.ProgressDialog;
import ru.nacu.vkmsg.ui.progress.ProgressDialogTask;

import java.io.IOException;
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author quadro
 * @since 6/30/12 10:22 AM
 */
public final class RegisterFragment extends SherlockFragment {
    public static final String SIGNUP_INTENT = "ru.nacu.vkmsg.Signup";
    public static final String CONFIRM_INTENT = "ru.nacu.vkmsg.Confirm";

    private static final Pattern phonePattern = Pattern.compile("^[+]?[0-9]{10,13}$");
    private static final Pattern namePattern = Pattern.compile("^[\\s\\w]{3,}$");

    private ImageView imgId;
    private ImageView imgFirst;
    private ImageView imgLast;
    private EditText editId;
    private EditText editFirst;
    private EditText editLast;

    String sid = null;

    private final BroadcastReceiver signupReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (CONFIRM_INTENT.equals(intent.getAction())) {
                if (intent.hasExtra("error")) {
                    KException k = (KException) intent.getSerializableExtra("error");
                    switch (k.error_code) {
                        case 1110:
                        case 1004:
                        case 100: {
                            error.setVisibility(View.VISIBLE);
                            errorText.setText(R.string.register_params_error);
                            break;
                        }

                        default:
                            error.setVisibility(View.VISIBLE);
                            errorText.setText(R.string.unexpected_error);
                    }
                } else if (intent.hasExtra("exception")) {
                    error.setVisibility(View.VISIBLE);
                    errorText.setText(R.string.unexpected_error);
                } else {
                    //hooray
                    getActivity().finish();
                }

                return;
            }

            if (intent.hasExtra("error")) {
                KException k = (KException) intent.getSerializableExtra("error");
                switch (k.error_code) {
                    case 100: {
                        error.setVisibility(View.VISIBLE);
                        errorText.setText(R.string.register_params_error);
                        break;
                    }

                    case 1003: {
                        error.setVisibility(View.VISIBLE);
                        errorText.setText(R.string.unexpected_error);
                        break;
                    }

                    case 1004: {
                        imgId.setVisibility(View.VISIBLE);
                        imgId.setImageResource(R.drawable.error);

                        error.setVisibility(View.VISIBLE);
                        errorText.setText(R.string.phone_used);

                        break;
                    }

                    case 1112: {
                        signup();
                        break;
                    }
                }
            } else if (intent.hasExtra("exception")) {
                error.setVisibility(View.VISIBLE);
                errorText.setText(R.string.unexpected_error);
            } else {
                sid = intent.getStringExtra("sid");
                code.setVisibility(View.VISIBLE);
                id.setVisibility(View.GONE);
                first.setVisibility(View.GONE);
                last.setVisibility(View.GONE);
                error.setVisibility(View.INVISIBLE);
                btnSubmit.setText(R.string.confirm);
                btnSubmit.setEnabled(false);
                btnResend.setVisibility(View.VISIBLE);
                pass.setVisibility(View.VISIBLE);
                passRpt.setVisibility(View.VISIBLE);
            }
        }
    };
    private View error;
    private TextView errorText;
    private View code;
    private EditText editCode;
    private Button btnSubmit;
    private View id;
    private View first;
    private View last;
    private Button btnResend;
    private View pass;
    private EditText editPass;
    private ImageView imgPass;
    private View passRpt;
    private EditText editPassRpt;
    private ImageView imgPassRpt;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VKMessenger.getCtx().registerReceiver(signupReceiver, new IntentFilter(SIGNUP_INTENT));
        VKMessenger.getCtx().registerReceiver(signupReceiver, new IntentFilter(CONFIRM_INTENT));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        VKMessenger.getCtx().unregisterReceiver(signupReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.register, container);

        editId = (EditText) v.findViewById(R.id.edit_id);
        editFirst = (EditText) v.findViewById(R.id.edit_first_name);
        editLast = (EditText) v.findViewById(R.id.edit_last_name);

        editId.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable == null || editable.length() == 0) {
                    return;
                }

                final Matcher m = phonePattern.matcher(editable);
                imgId.setImageResource(m.matches() ? R.drawable.ok : R.drawable.error);
            }
        });

        editFirst.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable != null && editable.length() != 0) {
                    final Matcher m = namePattern.matcher(editable);
                    imgFirst.setImageResource(m.matches() ? R.drawable.ok : R.drawable.error);
                }
            }
        });

        editLast.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable != null && editable.length() != 0) {
                    final Matcher m = namePattern.matcher(editable);
                    imgLast.setImageResource(m.matches() ? R.drawable.ok : R.drawable.error);
                }
            }
        });

        btnSubmit = (Button) v.findViewById(R.id.btn_submit);
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sid == null)
                    signup();
                else
                    confirm();
            }
        });

        btnResend = (Button) v.findViewById(R.id.btn_resend);
        btnResend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signup();
            }
        });

        imgId = (ImageView) v.findViewById(R.id.img_id);
        imgFirst = (ImageView) v.findViewById(R.id.img_first_name);
        imgLast = (ImageView) v.findViewById(R.id.img_last_name);

        imgId.setImageDrawable(null);
        imgFirst.setImageDrawable(null);
        imgLast.setImageDrawable(null);

        error = v.findViewById(R.id.error);
        errorText = (TextView) v.findViewById(R.id.error_text);

        code = v.findViewById(R.id.code);
        editCode = (EditText) v.findViewById(R.id.edit_code);

        id = v.findViewById(R.id.id);
        first = v.findViewById(R.id.first);
        last = v.findViewById(R.id.last);

        pass = v.findViewById(R.id.pass);
        editPass = (EditText) v.findViewById(R.id.edit_pass);
        imgPass = (ImageView) v.findViewById(R.id.img_pass);

        passRpt = v.findViewById(R.id.pass_rpt);
        editPassRpt = (EditText) v.findViewById(R.id.edit_pass_rpt);
        imgPassRpt = (ImageView) v.findViewById(R.id.img_pass_rpt);

        editPassRpt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable != null && editable.length() != 0) {
                    btnSubmit.setEnabled(editable.toString().equals(editPass.getText().toString()));
                    imgPassRpt.setImageResource(editable.toString().equals(editPass.getText().toString()) ? R.drawable.ok : R.drawable.error);
                }
            }
        });

        return v;
    }

    private void confirm() {
        ProgressDialog.showProgressDialog(getActivity(),
                new ConfirmTask(editId.getText().toString(), editCode.getText().toString(), editPass.getText().toString())
        );
    }

    private void signup() {
        ProgressDialog.showProgressDialog(getActivity(),
                new SignupTask(editId.getText().toString(), editFirst.getText().toString(), editLast.getText().toString(), sid));
    }

    private static class SignupTask extends ProgressDialogTask implements Serializable {
        private final String id;
        private final String first;
        private final String last;
        private final String sid;

        private SignupTask(String id, String first, String last, String sid) {
            this.id = id;
            this.first = first;
            this.last = last;
            this.sid = sid;
        }

        @Override
        public void run(Activity ctx) {
            try {
                final String sid = Auth.signup(id, first, last, this.sid);
                VKMessenger.getCtx().sendBroadcast(new Intent(SIGNUP_INTENT).putExtra("sid", sid));
            } catch (JSONException e) {
                VKMessenger.getCtx().sendBroadcast(new Intent(SIGNUP_INTENT).putExtra("exception", true));
            } catch (IOException e) {
                VKMessenger.getCtx().sendBroadcast(new Intent(SIGNUP_INTENT).putExtra("exception", true));
            } catch (KException e) {
                VKMessenger.getCtx().sendBroadcast(new Intent(SIGNUP_INTENT).putExtra("error", e));
            }
        }

        @Override
        public void onPostExecute(Activity ctx) {
        }
    }

    private static class ConfirmTask extends ProgressDialogTask implements Serializable {
        private final String phone;
        private final String code;
        private final String pass;

        private ConfirmTask(String phone, String code, String pass) {
            this.phone = phone;
            this.code = code;
            this.pass = pass;
        }

        @Override
        public void run(Activity ctx) {
            try {
                Auth.confirm(phone, code, pass);
            } catch (JSONException e) {
                VKMessenger.getCtx().sendBroadcast(new Intent(CONFIRM_INTENT).putExtra("exception", true));
            } catch (IOException e) {
                VKMessenger.getCtx().sendBroadcast(new Intent(CONFIRM_INTENT).putExtra("exception", true));
            } catch (KException e) {
                VKMessenger.getCtx().sendBroadcast(new Intent(CONFIRM_INTENT).putExtra("error", e));
            }
        }

        @Override
        public void onPostExecute(Activity ctx) {
        }
    }
}
