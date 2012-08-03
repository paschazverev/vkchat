package ru.nacu.vkmsg.ui;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.view.View;
import ru.android.common.UiTools;
import ru.android.common.asyncloader.AsyncListHandler;
import ru.nacu.vkmsg.R;
import ru.nacu.vkmsg.VKMessenger;
import ru.nacu.vkmsg.dao.Queries;
import ru.nacu.vkmsg.dao.Tables;
import ru.nacu.vkmsg.dao.VKContentProvider;
import ru.nacu.vkmsg.ui.chat.ChatFragment;

/**
 * @author quadro
 * @since 7/10/12 12:39 AM
 */
public final class FwdMessageAsyncListHandler implements AsyncListHandler<FwdMessageAsyncListHandler.LoadResult, Long, View> {
    private final int thumbnailSize;

    public FwdMessageAsyncListHandler() {
        thumbnailSize = (int) UiTools.dpToPix(60, VKMessenger.getCtx());
    }

    @Override
    public void setLoadedValue(View view, LoadResult value, long itemId) {
        if (value != null) {
            ChatFragment.MessageHolder h = (ChatFragment.MessageHolder) view.getTag();
            h.name.setText(value.name);
            h.img.setImageBitmap(value.thumb);
        }
    }

    @Override
    public void setNoValue(View view, long itemId) {

    }

    @Override
    public void setLoading(View view, long itemId) {
        ChatFragment.MessageHolder h = (ChatFragment.MessageHolder) view.getTag();
        h.name.setText("...");
        h.img.setImageResource(R.drawable.no_photo);
    }

    public static final String[] PROJECTION = {Tables.Columns.FIRST_NAME, Tables.Columns.LAST_NAME, Tables.Columns.PHOTO};

    @Override
    public LoadResult loadInBackground(Long key) {
        final Cursor c = VKContentProvider.q(VKContentProvider.CONTENT_URI_PROFILE, PROJECTION, Queries.SELECTION_ID, new String[]{Long.toString(key)}, null);

        try {
            if (c.moveToNext()) {
                return new LoadResult(c.getString(0) + " " + c.getString(1),
                        ThumbnailAsyncListHandler.load(c.getString(2), thumbnailSize, true, R.drawable.thumb_mask));
            }

            return null;
        } finally {
            c.close();
        }
    }

    @Override
    public LoadResult loadFromCache(Long key, long itemId) {
        return null;
    }

    @Override
    public void saveToDiskCache(Long key, LoadResult value, long itemId) {
    }

    public static class LoadResult {
        public final String name;
        public final Bitmap thumb;

        public LoadResult(String name, Bitmap thumb) {
            this.name = name;
            this.thumb = thumb;
        }
    }
}
