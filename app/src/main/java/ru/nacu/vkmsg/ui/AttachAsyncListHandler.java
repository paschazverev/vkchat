package ru.nacu.vkmsg.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.widget.ImageView;
import ru.android.common.asyncloader.AsyncListHandler;
import ru.android.common.asyncloader.ImageDecoder;
import ru.android.common.logs.Logs;
import ru.common.StreamTools;
import ru.nacu.vkmsg.Constants;
import ru.nacu.vkmsg.R;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;

/**
 * @author quadro
 * @since 6/22/12 5:17 PM
 */
public final class AttachAsyncListHandler implements AsyncListHandler<Bitmap, String, ImageView> {
    private final int thumbnailSize;

    public static Bitmap EMPTY_BITMAP = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);

    public AttachAsyncListHandler(int thumbnailSize) {
        this.thumbnailSize = thumbnailSize;
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
        imageView.setImageDrawable(null);
    }

    @Override
    public void setLoading(ImageView imageView, long l) {
        imageView.setImageResource(R.drawable.no_photo);
    }

    @Override
    public Bitmap loadInBackground(String url) {
        if (!url.startsWith("/")) {
            url = url.replaceAll(" ", "%20");
        } else {
            url = "file://" + url;
        }

        try {
            final Bitmap bitmap = ImageDecoder.decodeFile(new URL(url), new ImageDecoder.ImageSize(thumbnailSize, thumbnailSize));
            final Size s = getSize(thumbnailSize, bitmap.getWidth(), bitmap.getHeight());
            Matrix matrix = new Matrix();
            matrix.postScale(s.scale, s.scale);
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (Throwable e) {
            return EMPTY_BITMAP;
        }
    }

    public static Size getSize(int thumbnailSize, int width, int height) {
        final float scale;
        if (width > height) {
            scale = (float) thumbnailSize / (float) width;
        } else {
            scale = (float) thumbnailSize / (float) height;
        }

        return new Size(scale, (int) ((float) width * scale), (int) ((float) height * scale));
    }

    public static Bitmap loadFromCache0(String url) {
        try {
            File f = new File(Constants.thumbs, ru.common.string.Base64.byteArrayToBase64(url.getBytes()));
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
            file = new File(cache, ru.common.string.Base64.byteArrayToBase64(url.getBytes()));

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
