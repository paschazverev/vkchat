package ru.nacu.vkmsg.ui.contacts;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import ru.nacu.vkmsg.R;

/**
 * @author quadro
 * @since 7/4/12 1:05 PM
 */
public final class SelectContactActivity extends SherlockFragmentActivity implements SelectContactsFragment.SelectContactsHost, View.OnClickListener {

    private View back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_contact_activity);
        setResult(RESULT_CANCELED);

        back = findViewById(R.id.back);
        back.setOnClickListener(this);

        final TextView title = (TextView) findViewById(R.id.title);
        title.setText(R.string.select_contact);
        findViewById(R.id.img).setVisibility(View.INVISIBLE);
    }

    @Override
    public void onUserSelect(long userId) {
        setResult(RESULT_OK, new Intent().putExtra("userId", userId));
        finish();
    }

    @Override
    public void onGroupSelect(Intent data) {
        setResult(RESULT_OK, data);
        finish();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onClick(View view) {
        if (view == back) {
            finish();
        }
    }

}
