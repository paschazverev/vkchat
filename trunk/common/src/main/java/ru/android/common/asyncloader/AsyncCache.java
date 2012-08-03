package ru.android.common.asyncloader;

/**
 * @author quadro
 * @since 7/3/12 5:55 PM
 */
public interface AsyncCache<Key, Value> {
    void put(Key k, Value v);

    Value get(Key k);
}
