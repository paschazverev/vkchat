package ru.nacu.vkmsg.ui;

import android.content.Context;
import android.database.Cursor;
import android.graphics.*;
import android.widget.ImageView;
import ru.android.common.asyncloader.AsyncListHandler;
import ru.android.common.asyncloader.ImageDecoder;
import ru.android.common.db.DatabaseTools;
import ru.android.common.logs.Logs;
import ru.common.StreamTools;
import ru.nacu.commons.StaticSoftCache;
import ru.nacu.vkmsg.Constants;
import ru.nacu.vkmsg.Init;
import ru.nacu.vkmsg.R;
import ru.nacu.vkmsg.VKMessenger;
import ru.nacu.vkmsg.dao.Tables;
import ru.nacu.vkmsg.dao.VKContentProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author quadro
 * @since 6/22/12 5:17 PM
 */
public final class ThumbnailAsyncListHandler implements AsyncListHandler<Bitmap, String, ImageView> {
    private final int thumbnailSize;
    private final int mask;
    private final int noPhoto;
    private final boolean saveToDisk;

    public static Bitmap EMPTY_BITMAP = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);

    public ThumbnailAsyncListHandler(int thumbnailSize, int mask, int noPhoto, boolean saveToDisk) {
        this.thumbnailSize = thumbnailSize;
        this.mask = mask;
        this.noPhoto = noPhoto;
        this.saveToDisk = saveToDisk;
    }

    @Override
    public void setLoadedValue(ImageView imageView, Bitmap value, long l) {
        if (value == null) {
            imageView.setScaleType(ImageView.ScaleType.CENTER);
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
        imageView.setImageResource(noPhoto);
    }

    @Override
    public Bitmap loadInBackground(String url) {
        if (url.startsWith("chat://")) {
            String[] ids = url.substring(7).split(",");
            long[] users = new long[ids.length];
            int i = 0;
            for (String id : ids) {
                try {
                    users[i] = Long.parseLong(id.trim());
                } catch (NumberFormatException e) {
                    users[i] = 0;
                }

                i++;
            }

            return getGroupBitmap(users, thumbnailSize);
        }

        return loadInBackground0(url, thumbnailSize, thumbnailSize, mask);
    }

    public static Bitmap loadInBackground0(String url, int w, int h, int mask) {
        if (!url.startsWith("/")) {
            url = url.replaceAll(" ", "%20");
        } else {
            url = "file://" + url;
        }

        try {
            if (mask != 0) {
                return maskImage(VKMessenger.getCtx(),
                        crop(ImageDecoder.decodeFile(new URL(url), new ImageDecoder.ImageSize(w, h)), w, h), mask);
            } else {
                return crop(ImageDecoder.decodeFile(new URL(url), new ImageDecoder.ImageSize(w, h)), w, h);
            }
        } catch (Throwable e) {
            return EMPTY_BITMAP;
        }
    }

    private static Bitmap crop(Bitmap source, int width, int height) {
        int w = source.getWidth();
        int h = source.getHeight();

        if (w < 50 || h < 50) {
            return EMPTY_BITMAP;
        }

        if (w == width && h == height) {
            return source;
        }

        float propX = (float) w / (float) width;
        float propY = (float) h / (float) height;

        float scale;
        float left = 0;
        float top = 0;
        if (propX > propY) {
            scale = (float) height / (float) h;
            float newW = (float) w * scale;
            left = ((float) width - (float) newW) / 2;
        } else {
            scale = (float) width / (float) w;
            float newH = (float) h * scale;
            top = ((float) height - (float) newH) / 2;
        }

        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        final Bitmap scaled = Bitmap.createBitmap(source, 0, 0, w, h, matrix, true);

        if (left == 0 && top == 0) {
            return scaled;
        }

        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(result);
        c.drawBitmap(scaled, left, top, null);
        return result;
    }

    public static Bitmap load(String url, int thumbnailSize, boolean loadFromCache, int mask) {
        if (url == null)
            return null;

        if (loadFromCache) {
            final SoftReference<Bitmap> ref = StaticSoftCache.sSoftCache.get(url);
            Bitmap bitmap = null;
            if (ref != null)
                bitmap = ref.get();

            if (bitmap != null) {
                return bitmap;
            }

            bitmap = loadFromCache0(url);
            if (bitmap != null) {
                return bitmap;
            }
        }

        return loadInBackground0(url, thumbnailSize, thumbnailSize, mask);
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
        if (!saveToDisk)
            return null;

        return loadFromCache0(url);
    }

    @Override
    public void saveToDiskCache(String url, Bitmap bmp, long id) {
        if (!saveToDisk) return;

        FileOutputStream stream = null;
        File file = null;
        File cache;

        try {
            cache = Constants.thumbs;
            file = new File(Constants.thumbs, ru.common.string.Base64.byteArrayToBase64(url.getBytes()));

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

    public static final String[] IMG_PROJECTION = {Tables.Columns._ID, Tables.Columns.PHOTO};

    public static Bitmap getGroupBitmap(long[] users, int thumbnailSize) {
        Set<Long> s = new HashSet<Long>();
        final long me = Init.getUserId();
        for (long user : users) {
            if (user != me && user != 0) {
                s.add(user);
            }

            if (s.size() >= 4) {
                break;
            }
        }

        List<UserImg> imgs = new ArrayList<UserImg>(s.size());
        final Cursor c = VKContentProvider.q(VKContentProvider.CONTENT_URI_PROFILE, IMG_PROJECTION, "_id in (" + DatabaseTools.idsToString(s) + ")", null, "_id desc");
        try {
            while (c.moveToNext()) {
                imgs.add(new UserImg(c.getLong(0), c.getString(1)));
            }
        } finally {
            c.close();
        }

        if (imgs.size() == 1) {
            return loadInBackground0(imgs.get(0).photo, thumbnailSize, thumbnailSize, R.drawable.thumb_mask);
        } else if (imgs.size() == 2) {
            Bitmap r = Bitmap.createBitmap(thumbnailSize, thumbnailSize, Bitmap.Config.ARGB_4444);
            final Bitmap p1 = loadHalfSizePhoto(imgs.get(0).photo, thumbnailSize);
            final Bitmap p2 = loadHalfSizePhoto(imgs.get(1).photo, thumbnailSize);

            final Canvas can = new Canvas(r);
            can.drawBitmap(p1, 0, 0, null);
            can.drawBitmap(p2, thumbnailSize - p2.getWidth(), 0, null);
            return maskImage(VKMessenger.getCtx(), r, R.drawable.thumb_mask);
        } else if (imgs.size() == 3) {
            Bitmap r = Bitmap.createBitmap(thumbnailSize, thumbnailSize, Bitmap.Config.ARGB_4444);
            final Bitmap p1 = loadHalfSizePhoto(imgs.get(0).photo, thumbnailSize);
            final Bitmap p2 = loadQuarterSizePhoto(imgs.get(1).photo, thumbnailSize);
            final Bitmap p3 = loadQuarterSizePhoto(imgs.get(2).photo, thumbnailSize);

            final Canvas can = new Canvas(r);
            can.drawBitmap(p1, 0, 0, null);
            can.drawBitmap(p2, thumbnailSize - p2.getWidth(), 0, null);
            can.drawBitmap(p3, thumbnailSize - p3.getWidth(), thumbnailSize - p3.getHeight(), null);
            return maskImage(VKMessenger.getCtx(), r, R.drawable.thumb_mask);
        } else if (imgs.size() == 4) {
            Bitmap r = Bitmap.createBitmap(thumbnailSize, thumbnailSize, Bitmap.Config.ARGB_4444);
            final Bitmap p1 = loadQuarterSizePhoto(imgs.get(0).photo, thumbnailSize);
            final Bitmap p2 = loadQuarterSizePhoto(imgs.get(1).photo, thumbnailSize);
            final Bitmap p3 = loadQuarterSizePhoto(imgs.get(2).photo, thumbnailSize);
            final Bitmap p4 = loadQuarterSizePhoto(imgs.get(3).photo, thumbnailSize);

            final Canvas can = new Canvas(r);
            can.drawBitmap(p1, 0, 0, null);
            can.drawBitmap(p2, thumbnailSize - p2.getWidth(), 0, null);
            can.drawBitmap(p3, thumbnailSize - p3.getWidth(), thumbnailSize - p3.getHeight(), null);
            can.drawBitmap(p4, 0, thumbnailSize - p4.getHeight(), null);
            return maskImage(VKMessenger.getCtx(), r, R.drawable.thumb_mask);
        }

        return null;
    }

    private static Bitmap loadHalfSizePhoto(String photo, int thumbnailSize) {
        int width = (int) ((float) thumbnailSize / 2 - (float) thumbnailSize / 30);
        return loadInBackground0(photo, width, thumbnailSize, 0);
    }

    private static Bitmap loadQuarterSizePhoto(String photo, int thumbnailSize) {
        int s = (int) ((float) thumbnailSize / 2 - (float) thumbnailSize / 30);
        return loadInBackground0(photo, s, s, 0);
    }

    private static class UserImg {
        private final long id;
        private final String photo;

        private UserImg(long id, String photo) {
            this.id = id;
            this.photo = photo;
        }
    }

    public static Bitmap maskImage(Context ctx, Bitmap bmp, int maskResourceId) {
        Bitmap mask = BitmapFactory.decodeResource(ctx.getResources(), maskResourceId);
        Bitmap result = Bitmap.createBitmap(mask.getWidth(), mask.getHeight(), Bitmap.Config.ARGB_8888);

        Canvas c = new Canvas(result);

        final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));

        c.drawBitmap(bmp, 0, 0, null);
        c.drawBitmap(mask, 0, 0, mPaint);

        return result;
    }

    public static Size getImageSize(String fileName) {
        final BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(fileName, o);
        return new Size(1, o.outWidth, o.outHeight);
    }
}
