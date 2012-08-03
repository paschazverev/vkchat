package ru.android.common;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;

/**
 * An assortment of UI helpers.
 */
public class UiTools {

    public static final boolean IS_HONEYCOMB = Build.VERSION.SDK_INT >= 11;
    public static final boolean IS_ICS = Build.VERSION.SDK_INT >= 14;

    public static float getMinDimension(Context ctx) {
        Display display = ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();

        if (width > height) {
            return (float) height / ctx.getResources().getDisplayMetrics().density;
        } else {
            return (float) width / ctx.getResources().getDisplayMetrics().density;
        }
    }

    @SuppressWarnings({"SuspiciousNameCombination"})
    public static boolean isTablet(Context ctx) {
        float dpWidth = getMinDimension(ctx);
        return dpWidth >= 550;
    }

    public static boolean isPortrait(Context ctx) {
        Display display = ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();

        return width < height;
    }

    public static float dpToPix(int dp, Context ctx) {
        Resources r = ctx.getResources();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
    }

}
