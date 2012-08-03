package ru.nacu.vkmsg.ui.chat;

import android.app.Activity;
import android.widget.Toast;
import ru.nacu.vkmsg.R;
import ru.nacu.vkmsg.VKMessenger;
import ru.nacu.vkmsg.ui.progress.ProgressDialogTask;

import java.io.Serializable;

/**
 * @author quadro
 * @since 7/4/12 1:18 PM
 */
public final class ChangeTitleTask extends ProgressDialogTask implements Serializable {
    private final long chatId;
    private final String title;

    private volatile boolean success = false;

    public ChangeTitleTask(long chatId, String title) {
        this.chatId = chatId;
        this.title = title;
    }

    @Override
    public void run(Activity ctx) {
        try {
            VKMessenger.getApi().editChat(chatId, title);
            success = true;
        } catch (Exception e) {
            //ignore
        }
    }

    @Override
    public void onPostExecute(Activity ctx) {
        if (!success) {
            Toast.makeText(ctx, R.string.error_editing_chat, Toast.LENGTH_LONG).show();
        }
    }
}
