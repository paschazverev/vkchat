package ru.nacu.vkmsg.ui.profiles;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.View;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import ru.nacu.vkmsg.R;

/**
 * @author quadro
 * @since 7/5/12 5:42 PM
 */
public final class ProfileActivity extends SherlockFragmentActivity implements ProfileFragment.Host, View.OnClickListener {

    private TextView title;
    private ProfileFragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_activity);
        fragment = (ProfileFragment) getSupportFragmentManager().findFragmentById(R.id.profile);
        fragment.setProfileId(getIntent().getLongExtra("profileId", 0));
        findViewById(R.id.img).setVisibility(View.INVISIBLE);
        findViewById(R.id.back).setOnClickListener(this);
        title = (TextView) findViewById(R.id.title);
        setResult(RESULT_CANCELED);
    }

    @Override
    public void onProfileLoad(ProfileFragment.LoadResult result) {
        title.setText(result.name);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        fragment.onCreateContextMenu0(menu, v, menuInfo);
    }

    @Override
    public void writeMessage(long dialogId, long userId, long chatId) {
        setResult(RESULT_OK, new Intent()
                .putExtra("dialogId", dialogId).putExtra("userId", userId).putExtra("chatId", chatId));

        finish();
    }

    @Override
    public void onClick(View view) {
        finish();
    }
}
