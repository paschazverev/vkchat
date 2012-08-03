package ru.nacu.vkmsg.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.widget.ImageView;
import ru.android.common.asyncloader.AsyncListHandler;
import ru.android.common.logs.Logs;
import ru.common.StreamTools;
import ru.common.string.Base64;
import ru.nacu.vkmsg.Constants;
import ru.nacu.vkmsg.R;
import ru.nacu.vkmsg.VKMessenger;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;

/**
 * @author quadro
 * @since 6/22/12 5:17 PM
 */
public final class VideoAttachAsyncListHandler implements AsyncListHandler<Bitmap, String, ImageView> {
    private final int width;
    private final int height;

    public static Bitmap EMPTY_BITMAP = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);

    public VideoAttachAsyncListHandler(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void setLoadedValue(ImageView imageView, Bitmap value, long l) {
        if (value == null) {
            imageView.setImageBitmap(EMPTY_BITMAP);
        } else {
            imageView.setImageBitmap(value);
        }
    }

    @Override
    public void setNoValue(ImageView imageView, long l) {
    }

    @Override
    public void setLoading(ImageView imageView, long l) {
        imageView.setImageDrawable(null);
    }

    @Override
    public Bitmap loadInBackground(String url) {
        if (!url.startsWith("/")) {
            url = url.replaceAll(" ", "%20");
        } else {
            url = "file://" + url;
        }

        boolean drawPlay = !url.startsWith("http://maps.google.com/maps/api/staticmap");

        try {
            final Bitmap bitmap = BitmapFactory.decodeStream(new URL(url).openStream());
            final Size s = getSize(bitmap.getWidth(), bitmap.getHeight(), width, height);
            if (s.scale == 1) {
                return drawPlay(bitmap, drawPlay);
            } else {
                Matrix matrix = new Matrix();
                matrix.postScale(s.scale, s.scale);
                return drawPlay(Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true), drawPlay);
            }
        } catch (Exception e) {
            return EMPTY_BITMAP;
        }
    }

    private Bitmap drawPlay(Bitmap b, boolean draw) {
        if (draw) {
            final Bitmap newB = Bitmap.createBitmap(b.getWidth(), b.getHeight(), Bitmap.Config.ARGB_8888);
            final Canvas c = new Canvas(newB);
            Bitmap play = BitmapFactory.decodeResource(VKMessenger.getCtx().getResources(), R.drawable.video_play);
            c.drawBitmap(b, 0, 0, null);
            c.drawBitmap(play, (b.getWidth() - play.getWidth()) / 2, (b.getHeight() - play.getHeight()) / 2, null);
            return newB;
        } else {
            return b;
        }
    }

    /**
     * какой размер будет у картинки
     *
     * @param w      реальные размеры картинки
     * @param h      реальные
     * @param width  что получить
     * @param height что получить
     * @return размер картинки
     */
    public static Size getSize(int w, int h, int width, int height) {
        float propX = (float) w / (float) width;
        float propY = (float) h / (float) height;

        float scale;
        if (propX < propY) {
            scale = (float) height / (float) h;
            return new Size(scale, (int) ((float) w * scale), (int) ((float) h * scale));
        } else {
            scale = (float) width / (float) w;
            return new Size(scale, (int) ((float) w * scale), (int) ((float) h * scale));
        }
    }

    public static Bitmap loadFromCache0(String url) {
        try {
            if (url.startsWith("http://maps.google.com/maps/api/staticmap?center=")) {
                url = url.substring("http://maps.google.com/maps/api/staticmap?center=".length());
            }

            final String path = Base64.byteArrayToBase64(url.getBytes());
            File f = new File(Constants.thumbs, path);
            if (f.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(
                        f.getAbsolutePath()
                );

                if (bitmap.getWidth() == 1 || bitmap.getHeight() == 1) {
                    return EMPTY_BITMAP;
                }

                return bitmap;
            }
        } catch (Exception e) {
            //ignore
        }

        return null;
    }

    @Override
    public Bitmap loadFromCache(String url, long itemId) {
        return loadFromCache0(url);
    }

    @Override
    public void saveToDiskCache(String url, Bitmap bmp, long id) {
        FileOutputStream stream = null;
        File file = null;
        File cache;

        try {
            cache = Constants.thumbs;
            if (url.startsWith("http://maps.google.com/maps/api/staticmap?center=")) {
                url = url.substring("http://maps.google.com/maps/api/staticmap?center=".length());
            }

            final String path = Base64.byteArrayToBase64(url.getBytes());
            file = new File(cache, path);

            if (!file.exists()) {
                if ((cache.exists() && cache.isDirectory()) || cache.mkdirs()) {
                    stream = new FileOutputStream(file);
                    bmp.compress(Bitmap.CompressFormat.PNG, 90, stream);
                } else {
                    Logs.d("ImageDownloader", "Can't save file to disk thumbs. can't create dir");
                }
            }
        } catch (Exception e) {
            //ignore
            Logs.d("ImageDownloader", "can't save image to thumbs", e);

            if (file != null) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }

        } finally {
            StreamTools.close(stream);
        }
    }
}
