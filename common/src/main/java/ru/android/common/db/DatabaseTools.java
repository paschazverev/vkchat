package ru.android.common.db;

import android.database.Cursor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author quadro
 * @since 6/22/12 4:34 PM
 */
public class DatabaseTools {

    public static String idsToString(Collection<?> ids) {
        if (ids == null || ids.size() == 0) return null;

        StringBuilder b = new StringBuilder();
        for (Object id : ids) {
            b.append(id).append(",");
        }

        b.deleteCharAt(b.length() - 1);
        return b.toString();
    }

    public static String idsToString(long[] ids) {
        if (ids == null || ids.length == 0) return null;

        StringBuilder b = new StringBuilder();
        for (Long id : ids) {
            b.append(id).append(",");
        }

        b.deleteCharAt(b.length() - 1);
        return b.toString();
    }

    public static HashSet<String> fetchStringColumnAndClose(Cursor c) {
        try {
            HashSet<String> result = new HashSet<String>(c.getCount());
            while (c.moveToNext()) {
                result.add(c.getString(0));
            }

            return result;
        } finally {
            c.close();
        }
    }

    public static Set<Long> fetchLongColumnAndClose(Cursor c) {
        try {
            Set<Long> result = new HashSet<Long>(c.getCount());
            while (c.moveToNext()) {
                result.add(c.getLong(0));
            }

            return result;
        } finally {
            c.close();
        }
    }

    public static String fetchStringAndClose(Cursor c) {
        try {
            if (c.moveToNext()) {
                return c.getString(0);
            }

            throw new RuntimeException("Impossible???");
        } finally {
            c.close();
        }
    }

    public static int fetchIntAndClose(Cursor c) throws NoDataException {
        try {
            if (c.moveToNext()) {
                if (!c.isNull(0))
                    return c.getInt(0);
            }

            throw new NoDataException();
        } finally {
            c.close();
        }
    }

    public static int fetchIntAndClose(Cursor c, int def) {
        try {
            return fetchIntAndClose(c);
        } catch (NoDataException e) {
            return def;
        }
    }

    public static Integer fetchINTAndClose(Cursor c) {
        try {
            if (c.moveToNext()) {
                return c.getInt(0);
            }

            return null;
        } finally {
            c.close();
        }
    }

    public static long fetchLongAndClose(Cursor c) throws NoDataException {
        try {
            if (c.moveToNext()) {
                if (!c.isNull(0))
                    return c.getLong(0);
            }

            throw new NoDataException();
        } finally {
            c.close();
        }
    }

    public static long fetchLongAndClose(Cursor c, long def) {
        try {
            return fetchLongAndClose(c);
        } catch (NoDataException e) {
            return def;
        }
    }

    public static Long fetchLONGAndClose(Cursor c) {
        try {
            if (c.moveToNext()) {
                return c.getLong(0);
            }

            return null;
        } finally {
            c.close();
        }
    }

    public static <T> T first(Collection<T> col) {
        if (col.size() > 0) {
            return col.iterator().next();
        }

        return null;
    }

    public static long[] parseToArray(String s) {
        final String[] split = s.split(",");
        long[] r = new long[split.length];
        int i = 0;
        for (String s1 : split) {
            r[i] = Long.parseLong(s1.trim());
            i++;
        }

        return r;
    }

    public static Set<Long> parseToSet(String s) {
        final String[] split = s.split(",");
        Set<Long> r = new HashSet<Long>(split.length);
        int i = 0;
        for (String s1 : split) {
            r.add(Long.parseLong(s1.trim()));
            i++;
        }

        return r;
    }

}
