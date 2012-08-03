package ru.nacu.vkmsg.ui.contacts;

/**
* @author quadro
* @since 7/5/12 10:58 PM
*/
public interface DialogReadyListener {
    void onReady(long dialogId, long userId, long chatId);
}
