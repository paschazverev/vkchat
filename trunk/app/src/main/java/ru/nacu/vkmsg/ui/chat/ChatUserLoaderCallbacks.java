package ru.nacu.vkmsg.ui.chat;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import ru.android.common.UiTools;
import ru.nacu.vkmsg.R;
import ru.nacu.vkmsg.VKMessenger;
import ru.nacu.vkmsg.dao.Queries;
import ru.nacu.vkmsg.dao.Tables;
import ru.nacu.vkmsg.dao.VKContentProvider;
import ru.nacu.vkmsg.ui.ThumbnailAsyncListHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * @author quadro
 * @since 7/6/12 4:31 PM
 */
public final class ChatUserLoaderCallbacks implements LoaderManager.LoaderCallbacks<ChatUserLoaderCallbacks.LoadResult> {
    private final ChatFragment fragment;
    private final int thumbSize;

    public ChatUserLoaderCallbacks(ChatFragment fragment) {
        this.fragment = fragment;
        thumbSize = (int) UiTools.dpToPix(60, VKMessenger.getCtx());
    }

    public static final String[] PROJECTION = {Tables.Columns.FIRST_NAME, Tables.Columns.LAST_NAME,
            Tables.Columns.USER_ID, Tables.Columns.USER_IDS, Tables.Columns.TITLE, Tables.Columns.PHOTO, Tables.Columns.ONLINE};

    public static final int FIRST_NAME = 0;
    public static final int LAST_NAME = 1;
    public static final int USER_ID = 2;
    public static final int USER_IDS = 3;
    public static final int TITLE = 4;
    public static final int PHOTO = 5;
    public static final int ONLINE = 6;

    @Override
    public Loader<LoadResult> onCreateLoader(int id, Bundle args) {
        return new ru.nacu.commons.loader.AsyncTaskLoader<LoadResult>(fragment.getActivity(), VKContentProvider.CONTENT_URI_FRIEND) {
            @Override
            public LoadResult loadInBackground() {
                final long dialogId = ChatFragment.getDialogId();

                final Cursor c = VKContentProvider.q(VKContentProvider.CONTENT_URI_DIALOG, PROJECTION, Queries.SELECTION_ID,
                        new String[]{Long.toString(dialogId)}, null);

                String fTitle;
                Bitmap fImage;
                boolean online;

                Map<Long, String> users;
                try {
                    if (c.moveToNext()) {
                        final String firstName = c.getString(FIRST_NAME);
                        final String lastName = c.getString(LAST_NAME);
                        final String title = c.getString(TITLE);
                        final String photo = c.getString(PHOTO);
                        users = new HashMap<Long, String>();

                        if (fragment.getChatId() != 0) {
                            fTitle = title;
                            online = false;
                            fImage = null;
                            loadUserNames(users, c.getString(USER_IDS));
                        } else {
                            fTitle = firstName + " " + lastName;
                            fImage = ThumbnailAsyncListHandler.load(photo, (int) thumbSize, true, R.drawable.thumb_mask);
                            online = c.getInt(ONLINE) == 1;
                            users.put(c.getLong(USER_ID), firstName);
                        }
                    } else {
                        throw new RuntimeException("Can't find dialog " + dialogId);
                    }
                } finally {
                    c.close();
                }

                return new LoadResult(fImage, fTitle, online, users);
            }
        };
    }

    public static final String[] PROJECTION_NAMES = {Tables.Columns._ID, Tables.Columns.FIRST_NAME};

    private void loadUserNames(Map<Long, String> names, String ids) {
        final Cursor c = VKContentProvider.q(VKContentProvider.CONTENT_URI_PROFILE, PROJECTION_NAMES, "_id in (" + ids + ")", null, null);
        try {
            while (c.moveToNext()) {
                names.put(c.getLong(0), c.getString(1));
            }
        } finally {
            c.close();
        }
    }

    @Override
    public void onLoadFinished(Loader<LoadResult> loadResultLoader, LoadResult data) {
        if (fragment.getAdapter() != null && !fragment.getActivity().isFinishing()) {
            fragment.onInformationLoaded(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<LoadResult> loadResultLoader) {

    }

    public static class LoadResult {
        public final Bitmap image;
        public final String title;
        public final boolean online;
        public final Map<Long, String> users;

        public LoadResult(Bitmap image, String title, boolean online, Map<Long, String> users) {
            this.image = image;
            this.title = title;
            this.online = online;
            this.users = users;
        }
    }
}
