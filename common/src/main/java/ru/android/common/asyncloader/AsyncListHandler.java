package ru.android.common.asyncloader;

import android.view.View;

/**
 * Обработчик, который нужно реализовать для асинхронной загрузки данных в списке
 *
 * @author quadro
 */
public interface AsyncListHandler<ValueType, KeyType, ViewType extends View> {
    /**
     * Этот метод будет вызван после загрузки данных
     *
     * @param viewType переданный view
     * @param value    загруженные данные
     * @param itemId   идентификатор строки
     */
    void setLoadedValue(ViewType viewType, ValueType value, long itemId);

    void setNoValue(ViewType viewType, long itemId);

    /**
     * Этот метод будет вызван при начале загрузки данных
     *
     * @param viewType view, для которого грузятся данные
     * @param itemId   идентификатор строки
     */
    void setLoading(ViewType viewType, long itemId);

    /**
     * В этом методе должны грузиться все необходимые данные. Вызывается в background потоке
     *
     * @param key идентификатор данных, которые должны быть загружены
     * @return должен вернуть загруженные данные
     */
    ValueType loadInBackground(KeyType key);

    /**
     * должен загрузить данные из кэша. null, если нет данных
     *
     * @param key    идентификатор данных, которые нужно загрузить
     * @param itemId идентификатор элемента списка
     * @return данные или null, если нет данных
     */
    ValueType loadFromCache(KeyType key, long itemId);

    /**
     * сохранить данные на диск
     *
     * @param key    идентификатор
     * @param value  данные
     * @param itemId идентификатор записи
     */
    void saveToDiskCache(KeyType key, ValueType value, long itemId);
}
