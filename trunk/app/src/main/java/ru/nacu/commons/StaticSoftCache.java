package ru.nacu.commons;

import android.graphics.Bitmap;
import ru.android.common.asyncloader.AsyncCache;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author quadro
 * @since 7/3/12 5:59 PM
 */
public final class StaticSoftCache implements AsyncCache<String, Bitmap> {
    private final int hardCacheCapacity;

    public StaticSoftCache(int hardCacheCapacity) {
        this.hardCacheCapacity = hardCacheCapacity;
        sHardCache = new LinkedHashMap<String, Bitmap>(hardCacheCapacity / 2, 0.75f, true) {
            private static final long serialVersionUID = 1335259441684181986L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Bitmap> eldest) {
                if (size() > StaticSoftCache.this.hardCacheCapacity) {
                    // Entries push-out of hard reference cache are transferred to soft reference cache
                    sSoftCache.put(eldest.getKey(), new SoftReference<Bitmap>(eldest.getValue()));
                    return true;
                } else
                    return false;
            }
        };
    }

    private final HashMap<String, Bitmap> sHardCache;

    public static final ConcurrentHashMap<String, SoftReference<Bitmap>> sSoftCache =
            new ConcurrentHashMap<String, SoftReference<Bitmap>>();

    @Override
    public void put(String k, Bitmap v) {
        synchronized (sHardCache) {
            sHardCache.put(k, v);
            sSoftCache.put(k, new SoftReference<Bitmap>(v));
        }
    }

    @Override
    public Bitmap get(String key) {
        // First try the hard reference cache
        synchronized (sHardCache) {
            final Bitmap value = sHardCache.get(key);
            if (value != null) {
                // Move element to first position, so that it is removed last
                sHardCache.remove(key);
                sHardCache.put(key, value);
                return value;
            }
        }
        // Then try the soft reference cache
        SoftReference<Bitmap> reference = sSoftCache.get(key);
        if (reference != null) {
            final Bitmap value = reference.get();
            if (value != null) {
                return value;
            } else {
                sSoftCache.remove(key);
            }
        }
        return null;
    }
}
