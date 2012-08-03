package ru.nacu.vkmsg.updates.media;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import ru.android.common.logs.Logs;
import ru.nacu.vkmsg.VKMessenger;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * @author quadro
 * @since 7/9/12 6:33 PM
 */
public final class MediaService extends Service {
    //todo делать проигрывание в сервисе
    public static final String START_INTENT = "ru.nacu.vkmsg.StartPlayback";
    public static final String STOP_INTENT = "ru.nacu.vkmsg.StopPlayback";

    private static volatile MediaPlayer player;
    private static String current;

    private static final MediaPlayer.OnBufferingUpdateListener onBufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
            final MediaListener l = listener != null ? listener.get() : null;
            if (l != null) {
                l.onBufferingProgressListener(i);
            }
        }
    };
    private static final MediaPlayer.OnCompletionListener completionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            stop();
        }
    };
    private static final MediaPlayer.OnErrorListener errorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
            Logs.d("MediaService", "onError: " + i + "; " + i1);
            stop();
            return false;
        }
    };

    public static void startPlayback(String file, MediaListener l) {
        if (player != null) {
            stop();
        }

        setListener(l);
        player = new MediaPlayer();
        player.setOnBufferingUpdateListener(onBufferingUpdateListener);
        player.setOnCompletionListener(completionListener);
        player.setOnErrorListener(errorListener);

        player.reset();
        try {
            player.setDataSource(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final Thread thread = new Thread() {
            @Override
            public void run() {
                while (player != null) {
                    final MediaListener l = listener != null ? listener.get() : null;
                    if (l != null) {
                        try {
                            l.onPlayingProgressListener(player.getCurrentPosition());
                        } catch (Exception e) {
                            //ignore
                        }
                    }

                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };

        player.prepareAsync();
        player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                player.start();
                VKMessenger.getHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        thread.start();
                    }
                }, 2000);

            }
        });

        current = file;
    }

    private static WeakReference<MediaListener> listener;

    public static void setListener(MediaListener l) {
        listener = new WeakReference<MediaListener>(l);
    }

    public static void stop() {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }

        final MediaListener l = listener.get();
        if (l != null) {
            l.onStop();
        }

        current = null;
    }

    public static void seekTo(int pos) {
        if (player != null)
            player.seekTo(pos);
    }

    public static int getDuration() {
        if (player != null)
            return player.getDuration();
        return 0;
    }

    public static int getCurrentPosition() {
        if (player != null)
            return player.getCurrentPosition();

        return 0;
    }

    public static String getCurrent() {
        return current;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
