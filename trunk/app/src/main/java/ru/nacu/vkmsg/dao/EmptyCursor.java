package ru.nacu.vkmsg.dao;

import android.database.AbstractCursor;

/**
 * @author quadro
 * @since 7/7/12 10:16 AM
 */
public final class EmptyCursor extends AbstractCursor {
    public static final String[] C = new String[]{Tables.Columns._ID};

    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public String[] getColumnNames() {
        return C;
    }

    @Override
    public String getString(int i) {
        return null;
    }

    @Override
    public short getShort(int i) {
        return 0;
    }

    @Override
    public int getInt(int i) {
        return 0;
    }

    @Override
    public long getLong(int i) {
        return 0;
    }

    @Override
    public float getFloat(int i) {
        return 0;
    }

    @Override
    public double getDouble(int i) {
        return 0;
    }

    @Override
    public boolean isNull(int i) {
        return false;
    }
}
