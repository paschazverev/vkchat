package ru.android.common;

import android.content.Context;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author enaku_adm
 * @since 25.10.2010 17:34:25
 */
public class DateTools {
    private static DateFormat timeFormat;
    private static DateFormat shortDateFormat;
    private static DateFormat fullDateFormat;

    public static synchronized DateFormat getTimeFormat(Context ctx) {
        if (timeFormat == null) {
            timeFormat = android.text.format.DateFormat.getTimeFormat(ctx);
        }

        return timeFormat;
    }

    public static synchronized DateFormat getShortDateFormat(Context ctx) {
        if (shortDateFormat == null) {
            shortDateFormat = android.text.format.DateFormat.getDateFormat(ctx);
        }

        return shortDateFormat;
    }

    public static synchronized DateFormat getFullDateFormat(Context ctx) {
        if (fullDateFormat == null) {
            fullDateFormat = android.text.format.DateFormat.getLongDateFormat(ctx);
        }

        return fullDateFormat;
    }

    /**
     * Форматирует дату наилучшим образом. Если дата указывает на время сегодня, то пишется только время.
     * Если вчера и ранее, то только дата.
     *
     * @param time время в миллисекундах от 1970 г.
     * @param ctx  контекст
     * @return отформатированную дату
     */
    public static String formatDate(Date time, Context ctx) {
        if (isToday(time))
            return getTimeFormat(ctx).format(time);
        else
            return getShortDateFormat(ctx).format(time);
    }

    public static String fullFormatDate(Date time, Context ctx, int todayResourceId) {
        if (isToday(time))
            return ctx.getString(todayResourceId) + ", " + getTimeFormat(ctx).format(time);
        else
            return getFullDateFormat(ctx).format(time) + ", " + getTimeFormat(ctx).format(time);
    }

    public static boolean isToday(Date time) {
        Calendar today = Calendar.getInstance();

        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        return (time.getTime() - today.getTimeInMillis()) > 0;
    }
}
