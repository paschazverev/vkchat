package ru.nacu.vkmsg;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import ru.nacu.commons.tabs.TabFragmentActivity;
import ru.nacu.vkmsg.dao.VKContentProvider;
import ru.nacu.vkmsg.ui.chat.ChatActivity;
import ru.nacu.vkmsg.ui.chats.ChatsFragment;
import ru.nacu.vkmsg.ui.chats.FriendNumberWatcher;
import ru.nacu.vkmsg.ui.chats.MessageNumberWatcher;
import ru.nacu.vkmsg.ui.contacts.ContactsFragment;
import ru.nacu.vkmsg.ui.profiles.ProfileActivity;
import ru.nacu.vkmsg.ui.search.SearchFragment;
import ru.nacu.vkmsg.ui.settings.SettingsFragment;
import ru.nacu.vkmsg.updates.LongPoll;

import java.util.Arrays;
import java.util.List;

/**
 * @author quadro
 * @since 7/2/12 12:30 PM
 */
public final class PhoneActivity extends TabFragmentActivity implements
        ChatsFragment.ChatsFragmentHost, ContactsFragment.Host, SearchFragment.Host {

    public static final int CHATS_ACTIVITY = 500;
    public static final int PROFILE_SHOW = 501;

    private MessageNumberWatcher messageNumberWatcher;
    private FriendNumberWatcher friendNumberWatcher;

    public PhoneActivity() {
        super(R.layout.phone_activity, R.id.realtabcontent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected List<TabInfo> getTabs(Bundle args) {
        final View i1 = View.inflate(this, R.layout.tab_indicator, null);
        final View i2 = View.inflate(this, R.layout.tab_indicator, null);
        final View i3 = View.inflate(this, R.layout.tab_indicator, null);
        final View i4 = View.inflate(this, R.layout.tab_indicator, null);

        messageNumberWatcher = new MessageNumberWatcher(i1);
        friendNumberWatcher = new FriendNumberWatcher(i3);

        updateItem(i1, R.string.messages, R.drawable.msg);
        updateItem(i2, R.string.contacts, R.drawable.cont);
        updateItem(i3, R.string.search, R.drawable.search);
        updateItem(i4, R.string.settings, R.drawable.stg);

        return Arrays.asList(
                new TabInfo(i1, "messages", ChatsFragment.class, args),
                new TabInfo(i2, "contacts", ContactsFragment.class, args),
                new TabInfo(i3, "search", SearchFragment.class, args),
                new TabInfo(i4, "settings", SettingsFragment.class, args)
        );
    }

    private void updateItem(View v, int string, int pic) {
        final TextView tv = (TextView) v.findViewById(R.id.label);
        tv.setText(string);
        final ImageView iv = (ImageView) v.findViewById(R.id.icon);
        iv.setImageResource(pic);
    }

    @Override
    protected void onStart() {
        super.onStart();
        VKMessenger.getCtx().getContentResolver().registerContentObserver(VKContentProvider.CONTENT_URI_DIALOG, true,
                messageNumberWatcher.observer);

        messageNumberWatcher.refresh();

        VKMessenger.getCtx().getContentResolver().registerContentObserver(VKContentProvider.CONTENT_URI_FRIEND, true,
                friendNumberWatcher.observer);

        friendNumberWatcher.refresh();
    }

    @Override
    protected void onStop() {
        super.onStop();
        VKMessenger.getCtx().getContentResolver().unregisterContentObserver(messageNumberWatcher.observer);
        VKMessenger.getCtx().getContentResolver().unregisterContentObserver(friendNumberWatcher.observer);
    }

    @SuppressWarnings("unchecked")
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
    public void onDialogSelect(long dialogId, long userId, long chatId) {
        final Intent i = new Intent(this, ChatActivity.class)
                .putExtra("dialogId", dialogId)
                .putExtra("userId", userId)
                .putExtra("chatId", chatId);

        if (ChatsFragment.getQ() != null) {
            i.putExtra("search", true);
        }

        startActivityForResult(
                i,
                CHATS_ACTIVITY
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PROFILE_SHOW: {
                if (resultCode == RESULT_OK) {
                    onDialogSelect(
                            data.getLongExtra("dialogId", 0),
                            data.getLongExtra("userId", 0),
                            data.getLongExtra("chatId", 0)
                    );
                }

                break;
            }
        }
    }

    @Override
    public void onProfileShow(long id) {
        startActivityForResult(new Intent(this, ProfileActivity.class).putExtra("profileId", id), PROFILE_SHOW);
    }
}
