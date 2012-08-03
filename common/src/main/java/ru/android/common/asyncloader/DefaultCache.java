package ru.android.common.asyncloader;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author quadro
 * @since 7/3/12 5:59 PM
 */
public class DefaultCache<KeyType, ValueType> implements AsyncCache<KeyType, ValueType> {
    private final int hardCacheCapacity;

    public DefaultCache(int hardCacheCapacity) {
        this.hardCacheCapacity = hardCacheCapacity;
        sHardCache = new LinkedHashMap<KeyType, ValueType>(hardCacheCapacity / 2, 0.75f, true) {
            private static final long serialVersionUID = 1335259441684181986L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<KeyType, ValueType> eldest) {
                if (size() > DefaultCache.this.hardCacheCapacity) {
                    // Entries push-out of hard reference cache are transferred to soft reference cache
                    sSoftCache.put(eldest.getKey(), new SoftReference<ValueType>(eldest.getValue()));
                    return true;
                } else
                    return false;
            }
        };
    }

    private final HashMap<KeyType, ValueType> sHardCache;

    private final ConcurrentHashMap<KeyType, SoftReference<ValueType>> sSoftCache =
            new ConcurrentHashMap<KeyType, SoftReference<ValueType>>();

    @Override
    public void put(KeyType k, ValueType v) {
        synchronized (sHardCache) {
            sHardCache.put(k, v);
        }
    }

    @Override
    public ValueType get(KeyType key) {
        // First try the hard reference cache
        synchronized (sHardCache) {
            final ValueType value = sHardCache.get(key);
            if (value != null) {
                // Move element to first position, so that it is removed last
                sHardCache.remove(key);
                sHardCache.put(key, value);
                return value;
            }
        }
        // Then try the soft reference cache
        SoftReference<ValueType> reference = sSoftCache.get(key);
        if (reference != null) {
            final ValueType value = reference.get();
            if (value != null) {
                return value;
            } else {
                sSoftCache.remove(key);
            }
        }
        return null;
    }
}
