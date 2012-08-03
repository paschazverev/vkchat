package ru.nacu.vkmsg.ui.login;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import ru.android.common.task.ModernAsyncTask;
import ru.nacu.vkmsg.R;
import ru.nacu.vkmsg.asynctasks.SendInformationTask;

import java.net.URL;

/**
 * @author quadro
 * @since 6/29/12 5:30 PM
 */
public final class CaptchaActivity extends Activity implements View.OnClickListener {

    private EditText edit;

    private static boolean requested = false;
    private static String captchaKey;
    private static String captchaSid;

    public static boolean isRequested() {
        return requested;
    }

    public static void setRequested(boolean requested) {
        CaptchaActivity.requested = requested;
    }

    public static String getCaptchaKey() {
        return captchaKey;
    }

    public static String getCaptchaSid() {
        return captchaSid;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onCreate(Bundle savedInstanceState) {
        requested = true;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.captcha);
        edit = (EditText) findViewById(R.id.value);

        final View prg = findViewById(R.id.progress);
        final ImageView img = (ImageView) findViewById(R.id.img);

        img.setVisibility(View.GONE);

        findViewById(R.id.submit).setOnClickListener(this);

        new ModernAsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... params) {
                try {
                    return BitmapFactory.decodeStream(new URL(getIntent().getStringExtra("url")).openStream());
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                super.onPostExecute(bitmap);
                if (bitmap != null) {
                    prg.setVisibility(View.GONE);
                    img.setVisibility(View.VISIBLE);
                    img.setImageBitmap(bitmap);
                }
            }
        }.execute();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onClick(View view) {
        captchaKey = edit.getText().toString();
        captchaSid = getIntent().getStringExtra("sid");
        setResult(RESULT_OK, new Intent().putExtra("key", captchaKey).putExtra("sid", captchaSid));
        finish();

        requested = false;
        new SendInformationTask().execute();
    }
}
