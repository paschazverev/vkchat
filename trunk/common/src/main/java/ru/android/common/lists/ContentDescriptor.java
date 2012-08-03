package ru.android.common.lists;

import android.net.Uri;

/**
 * @author quadro
 * @since 20.05.11 11:24
 */
public final class ContentDescriptor {
    public final Uri uri;
    public final Uri observeUri;
    public final String[] projection;
    public final String selection;
    public final String[] selectionArgs;
    public final String orderBy;

    public ContentDescriptor(Uri uri, Uri observeUri, String[] projection, String selection, String[] selectionArgs, String orderBy) {
        this.uri = uri;
        this.observeUri = observeUri;
        this.projection = projection;
        this.selection = selection;
        this.selectionArgs = selectionArgs;
        this.orderBy = orderBy;
    }
}
