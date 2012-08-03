package ru.nacu.vkmsg.updates.media;

/**
 * @author quadro
 * @since 7/9/12 9:32 PM
 */
public interface MediaListener {
    void onBufferingProgressListener(int progress);

    void onPlayingProgressListener(int progress);

    void onStop();
}
