package ru.android.common.asyncloader;

import android.os.Handler;
import android.os.Message;
import android.view.View;
import ru.android.common.logs.Logs;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Хелпер, который позволяет загружать любые данные для списков асинхронно.
 * Поддерживает кэширование (3 разных уровня),
 * остановку загрузки данных, если элемент списка уже не виден
 */
public class AsyncListHelper<KeyType, ValueType, ViewType extends View> {
    public static final String TAG = "AsyncListHelper";
    public static final boolean DEBUG = false;

    private final AsyncListHandler<ValueType, KeyType, ViewType> listHandler;
    private final Map<ViewType, ToDownload> toDownloadMap = new HashMap<ViewType, ToDownload>();
    private final Map<ViewType, ToSetValue> toSetMap = new HashMap<ViewType, ToSetValue>();

    private final AsyncCache<KeyType, ValueType> cache;

    private final int downloadTaskId;

    public static final int LOADED = 10001;
    private final Handler handler = new Handler() {
        @SuppressWarnings({"unchecked"})
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (msg.what == LOADED) {
                ValueDownloaderTask task = (ValueDownloaderTask) msg.obj;
                if (task.value != null) {
                    cache.put(task.key, task.value);
                }

                if (task.viewReference != null) {
                    ViewType view = task.viewReference.get();
                    if (view == null)
                        return;

                    ValueDownloaderTask downloaderTask = getValueDownloaderTask(view);

                    if (task == downloaderTask) {
                        if (!scrolling) {
                            listHandler.setLoadedValue(view, task.value, task.itemId);
                        } else {
                            toSetMap.put(view, new ToSetValue(task.value, task.itemId));
                        }
                    }
                }
            }
        }
    };

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "AsyncListHelper #" + mCount.getAndIncrement());
        }
    };

    private static final int KEEP_ALIVE = 1;
    private final static ThreadPoolExecutor service = new ThreadPoolExecutor(2, 255, KEEP_ALIVE,
            TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), sThreadFactory);

    public AsyncListHelper(AsyncListHandler<ValueType, KeyType, ViewType> listHandler, AsyncCache<KeyType, ValueType> cache, int downloadTaskId) {
        this.listHandler = listHandler;
        this.cache = cache;
        this.downloadTaskId = downloadTaskId;
    }

    /**
     * Начать загрузку нужных данных
     *
     * @param key          идентификатор того, что нужно загрузить
     * @param itemId       идентификатор строки
     * @param listItemView view, который показывает элемент списка (нужен, чтобы знать когда отменить загрузку)
     */
    public void download(KeyType key, long itemId, ViewType listItemView) {
        boolean cancelled = cancelDownload(itemId, key, listItemView);

        if (key == null) {
            listHandler.setNoValue(listItemView, itemId);
            return;
        }

        ValueType value = getValueFromCache(key);

        if (value == null) {
            listHandler.setLoading(listItemView, itemId);
            if (cancelled) {
                if (!scrolling) {
                    forceDownload(listItemView, key, itemId);
                } else {
                    toDownloadMap.put(listItemView, new ToDownload(key, itemId));
                }
            }
        } else {
            listHandler.setLoadedValue(listItemView, value, itemId);
        }
    }

    /**
     * Начать загрузку данных асинхронно
     *
     * @param listItemView view, который будет показывать данные
     * @param key          ключ, который говорит что загружать
     * @param itemId       идентификатор строки, для которой нужны данные
     */
    @SuppressWarnings({"unchecked"})
    private void forceDownload(ViewType listItemView, KeyType key, long itemId) {
        if (key == null) {
            listHandler.setLoadedValue(listItemView, null, itemId);
            return;
        }

        final ValueDownloaderTask task = new ValueDownloaderTask(key, listItemView, itemId);
        listItemView.setTag(downloadTaskId, task);
        //noinspection PointlessBooleanExpression,ConstantConditions
        if (Logs.enabled && DEBUG) {
            Logs.d(TAG, "forceDownload setTag view=" + listItemView + "; value=" + task);
        }
        service.execute(task);
    }

    /**
     * Остановить загрузку, если будет загружаться новый элемент. Не останавливать, если тот же
     *
     * @param itemId       идентификатор узла
     * @param key          ключ, который говорит что грузить будем
     * @param listItemView объект, который показыват для какого элемента списка идет загрузка
     * @return true, если загрузка остановлена
     */
    public boolean cancelDownload(long itemId, KeyType key, ViewType listItemView) {
        toDownloadMap.remove(listItemView);
        toSetMap.remove(listItemView);

        ValueDownloaderTask downloaderTask = getValueDownloaderTask(listItemView);

        //noinspection PointlessBooleanExpression,ConstantConditions
        if (Logs.enabled && DEBUG) {
            Logs.d(TAG, "cancelDownload id=" + itemId + "; view=" + listItemView + "; task=" + downloaderTask);
        }

        if (downloaderTask != null) {
            KeyType oldKey = downloaderTask.key;
            if ((oldKey == null) || (!oldKey.equals(key))) {
                downloaderTask.setCancelled(true);
                listItemView.setTag(downloadTaskId, null);
                service.remove(downloaderTask);
            } else {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings({"unchecked"})
    private ValueDownloaderTask getValueDownloaderTask(ViewType listItemView) {
        if (listItemView != null) {
            return (ValueDownloaderTask) listItemView.getTag(downloadTaskId);
        }

        return null;
    }

    private DownloadedValue downloadValue(KeyType key, long itemId) {
        final ValueType fromCache = listHandler.loadFromCache(key, itemId);
        if (fromCache != null) {
            return new DownloadedValue(fromCache, true);
        }

        return new DownloadedValue(listHandler.loadInBackground(key), false);
    }

    /**
     * The actual AsTask that will asynchronously download the image.
     */
    class ValueDownloaderTask implements Runnable {
        private final WeakReference<ViewType> viewReference;
        private final long itemId;
        private final KeyType key;

        private volatile ValueType value;
        private volatile boolean cancelled;

        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }

        public ValueDownloaderTask(KeyType key, ViewType listItemView, long itemId) {
            this.itemId = itemId;
            this.viewReference = new WeakReference<ViewType>(listItemView);
            this.key = key;
        }

        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_LOWEST);
            final DownloadedValue result = downloadValue(key, itemId);

            value = result.value;
            if (cancelled)
                return;

            final Message msg = handler.obtainMessage(LOADED);
            msg.obj = this;
            msg.sendToTarget();

            if (!result.fromCache)
                listHandler.saveToDiskCache(key, value, itemId);
        }

        @Override
        public String toString() {
            return "DownloaderTask key=" + key + "; itemId=" + itemId;
        }
    }

    private boolean scrolling;

    public void setScrolling(boolean scrolling) {
        this.scrolling = scrolling;
        if (!scrolling) {
            for (ViewType view : toDownloadMap.keySet()) {
                ToDownload toDownload = toDownloadMap.get(view);
                forceDownload(view, toDownload.key, toDownload.itemId);
            }

            for (ViewType view : toSetMap.keySet()) {
                final ToSetValue val = toSetMap.get(view);
                listHandler.setLoadedValue(view, val.value, val.itemId);
            }

            toSetMap.clear();
            toDownloadMap.clear();
        }
    }

    /**
     * @param key ключ, по которому нужно взять данные из кэша
     * @return кэшированный объект
     */
    private ValueType getValueFromCache(KeyType key) {
        return cache.get(key);
    }

    public class ToDownload {
        private final KeyType key;
        private final long itemId;

        public ToDownload(KeyType key, long itemId) {
            this.key = key;
            this.itemId = itemId;
        }
    }

    public class DownloadedValue {
        private final ValueType value;
        private final boolean fromCache;

        public DownloadedValue(ValueType value, boolean fromCache) {
            this.value = value;
            this.fromCache = fromCache;
        }
    }

    public class ToSetValue {
        private final ValueType value;
        private final long itemId;

        public ToSetValue(ValueType value, long itemId) {
            this.value = value;
            this.itemId = itemId;
        }
    }
}
