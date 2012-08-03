package ru.nacu.vkmsg.dao;

import android.content.Context;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author quadro
 * @since 6/25/12 11:15 AM
 */
public final class DateTools {
    private static DateFormat timeFormat;
    private static DateFormat onlyDayMonth = new SimpleDateFormat("dd.MM");
    private static DateFormat dayMonthYear = new SimpleDateFormat("dd.MM.yy");
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

    public static String formatTime(Context ctx, long dt) {
        return getTimeFormat(ctx).format(new Date(dt));
    }

    public static String formatDate(Context ctx, long dt) {
        return getFullDateFormat(ctx).format(new Date(dt));
    }

    public static String formatDialogDate(Context ctx, long dt, int yesterdayString) {
        DateDifference diff = isTodayYesterdayThisYear(dt);

        switch (diff) {
            case TODAY:
                return getTimeFormat(ctx).format(new Date(dt));
            case YESTERDAY:
                return ctx.getString(yesterdayString);
            case THIS_YEAR:
                return onlyDayMonth.format(new Date(dt));
            case OTHER:
                return dayMonthYear.format(new Date(dt));
        }

        return null;
    }

    private static long today;
    private static long yesterday;
    private static long thisYear;
    private static long lastUpdate;

    public static void init() {
        lastUpdate = System.currentTimeMillis();

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar yesterday = Calendar.getInstance();
        yesterday.setTimeInMillis(today.getTimeInMillis());
        yesterday.add(Calendar.DAY_OF_MONTH, -1);
        lastUpdate = System.currentTimeMillis();

        Calendar thisYear = Calendar.getInstance();
        thisYear.set(Calendar.DAY_OF_YEAR, 0);

        DateTools.today = today.getTimeInMillis();
        DateTools.yesterday = yesterday.getTimeInMillis();
        DateTools.thisYear = thisYear.getTimeInMillis();
    }

    public static DateDifference isTodayYesterdayThisYear(long dt) {
        if (System.currentTimeMillis() - lastUpdate > 60000) {
            init();
        }

        if ((dt - today) > 0) return DateDifference.TODAY;
        if ((dt - yesterday) > 0) return DateDifference.YESTERDAY;
        if ((dt - thisYear) > 0) return DateDifference.THIS_YEAR;
        return DateDifference.OTHER;
    }

    public enum DateDifference {
        TODAY,
        YESTERDAY,
        THIS_YEAR,
        OTHER
    }
}
