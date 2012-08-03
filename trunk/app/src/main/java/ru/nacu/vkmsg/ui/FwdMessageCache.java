package ru.nacu.vkmsg.ui;

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
public final class FwdMessageCache implements AsyncCache<Long, FwdMessageAsyncListHandler.LoadResult> {
    private final int hardCacheCapacity;

    public FwdMessageCache(int hardCacheCapacity) {
        this.hardCacheCapacity = hardCacheCapacity;
        sHardCache = new LinkedHashMap<Long, FwdMessageAsyncListHandler.LoadResult>(hardCacheCapacity / 2, 0.75f, true) {
            private static final long serialVersionUID = 1335259441684181986L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, FwdMessageAsyncListHandler.LoadResult> eldest) {
                if (size() > FwdMessageCache.this.hardCacheCapacity) {
                    // Entries push-out of hard reference cache are transferred to soft reference cache
                    sSoftCache.put(eldest.getKey(), new SoftReference<FwdMessageAsyncListHandler.LoadResult>(eldest.getValue()));
                    return true;
                } else
                    return false;
            }
        };
    }

    private final HashMap<Long, FwdMessageAsyncListHandler.LoadResult> sHardCache;

    public static final ConcurrentHashMap<Long, SoftReference<FwdMessageAsyncListHandler.LoadResult>> sSoftCache =
            new ConcurrentHashMap<Long, SoftReference<FwdMessageAsyncListHandler.LoadResult>>();

    @Override
    public void put(Long k, FwdMessageAsyncListHandler.LoadResult v) {
        synchronized (sHardCache) {
            sHardCache.put(k, v);
            sSoftCache.put(k, new SoftReference<FwdMessageAsyncListHandler.LoadResult>(v));
        }
    }

    @Override
    public FwdMessageAsyncListHandler.LoadResult get(Long key) {
        // First try the hard reference cache
        synchronized (sHardCache) {
            final FwdMessageAsyncListHandler.LoadResult value = sHardCache.get(key);
            if (value != null) {
                // Move element to first position, so that it is removed last
                sHardCache.remove(key);
                sHardCache.put(key, value);
                return value;
            }
        }
        // Then try the soft reference cache
        SoftReference<FwdMessageAsyncListHandler.LoadResult> reference = sSoftCache.get(key);
        if (reference != null) {
            final FwdMessageAsyncListHandler.LoadResult value = reference.get();
            if (value != null) {
                return value;
            } else {
                sSoftCache.remove(key);
            }
        }
        return null;
    }
}
