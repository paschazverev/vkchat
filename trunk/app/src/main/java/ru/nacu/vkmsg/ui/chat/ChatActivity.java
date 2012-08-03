package ru.nacu.vkmsg.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import ru.android.common.logs.Logs;
import ru.nacu.vkmsg.PhoneActivity;
import ru.nacu.vkmsg.R;
import ru.nacu.vkmsg.ui.profiles.ProfileActivity;
import ru.nacu.vkmsg.updates.LongPoll;

/**
 * @author quadro
 * @since 6/21/12 1:05 PM
 */
public final class ChatActivity extends SherlockFragmentActivity implements ChatFragment.Host, View.OnClickListener {
    public static final int PROFILE_SHOW = 501;

    public static final String TAG = "ChatActivity";
    private ChatFragment list;
    private View back;
    private ImageView img;
    private TextView title;
    private long dialogId;
    private long userId;
    private long chatId;
    private View actionbar;
    private View selection;
    private Button btnCancel;
    private Button btnFwd;
    private Button btnDel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logs.d(TAG, "onCreate()");

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.chat_activity);

        actionbar = findViewById(R.id.actionbar);
        selection = findViewById(R.id.selection_bar);
        list = (ChatFragment) getSupportFragmentManager().findFragmentById(R.id.list);
        dialogId = getIntent().getLongExtra("dialogId", 0);
        userId = getIntent().getLongExtra("userId", 0);
        chatId = getIntent().getLongExtra("chatId", 0);
        list.show(
                getIntent().hasExtra("search"),
                dialogId,
                userId,
                chatId
        );

        btnCancel = (Button) findViewById(R.id.btn_cancel);
        btnFwd = (Button) findViewById(R.id.btn_fwd);
        btnDel = (Button) findViewById(R.id.btn_del);

        btnCancel.setOnClickListener(this);
        btnFwd.setOnClickListener(this);
        btnDel.setOnClickListener(this);

        back = findViewById(R.id.back);
        back.setOnClickListener(this);
        img = (ImageView) findViewById(R.id.img);
        img.setOnClickListener(this);
        title = (TextView) findViewById(R.id.title);
        title.setOnClickListener(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        dialogId = intent.getLongExtra("dialogId", 0);
        userId = intent.getLongExtra("userId", 0);
        chatId = intent.getLongExtra("chatId", 0);

        list.show(
                false,
                dialogId,
                userId,
                chatId
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        LongPoll.start(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LongPoll.setTimeoutForStopServer();
    }

    @Override
    public void onInformationLoaded(ChatUserLoaderCallbacks.LoadResult data) {
        title.setText(data.title);
        title.setCompoundDrawablesWithIntrinsicBounds(0, 0, data.online ? R.drawable.online : 0, 0);
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);
        img.setImageBitmap(data.image);
    }

    @Override
    public void onSelectionChange(int size) {
        actionbar.setVisibility(size > 0 ? View.GONE : View.VISIBLE);
        selection.setVisibility(size == 0 ? View.GONE : View.VISIBLE);

        btnDel.setText(getString(R.string.delete) + " " + size);
        btnFwd.setText(getString(R.string.forward) + " " + size);
    }

    @Override
    public void onClick(View view) {
        if (view == back) {
            if (getCallingActivity() != null) {
                finish();
            } else {
                finish();
                startActivity(new Intent(this, PhoneActivity.class));
            }
        } else if (view == img) {
            if (chatId == 0) {
                startActivityForResult(new Intent(this, ProfileActivity.class).putExtra("profileId", userId), PROFILE_SHOW);
            }
        } else if (view == title) {
            if (chatId != 0) {
                startActivity(
                        new Intent(this, GroupChatManagerActivity.class)
                                .putExtra("dialogId", dialogId)
                                .putExtra("chatId", chatId)
                );
            }
        } else if (view == btnCancel) {
            list.cancelSelection();
        } else if (view == btnDel) {
            list.deleteMessages();
        } else if (view == btnFwd) {
            list.forwardMessages();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return list.dispatchTouchEvent() || super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        final boolean b = list.onKeyDown(keyCode);
        return b || super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PROFILE_SHOW: {
                if (resultCode == RESULT_OK) {
                    onNewIntent(data);
                }

                break;
            }
        }
    }
}
