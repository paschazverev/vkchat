package ru.nacu.commons.preferences;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import ru.android.common.UiTools;

/**
 * @author quadro
 * @since 3/25/12 6:34 PM
 */
public final class ChildListPreference extends ListPreference {

    public ChildListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ChildListPreference(Context context) {
        super(context);
    }

    @Override
    public View getView(View convertView, ViewGroup parent) {
        View v = super.getView(convertView, parent);
        if (v != null) {
            Boolean b = (Boolean) v.getTag();
            if (b == null || !b) {
                LinearLayout l = new LinearLayout(getContext());
                l.addView(v);
                final float v1 = UiTools.dpToPix(20, getContext());
                v.setPadding((int) (v.getPaddingLeft() + v1), v.getPaddingTop(), v.getPaddingRight(), v.getPaddingBottom());
                l.setTag(true);
                v = l;
            }
        }
        return v;
    }
}
