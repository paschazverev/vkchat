package ru.nacu.commons.swipe;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * @author quadro
 */
public final class SwipeLabel extends LinearLayout {

    public final ImageView arrow;
    public final ImageView image;
    private final TextView label;
    private final VerticalLabelView vLabel;

    public final void setText(int resId) {
        if (label != null) {
            label.setText(resId);
        } else {
            vLabel.setText(resId);
        }
    }

    public final void setTextColor(int c) {
        if (label != null) {
            label.setTextColor(c);
        } else {
            vLabel.setTextColor(c);
        }
    }

    public SwipeLabel(Context context, boolean vertical) {
        super(context);
        if (vertical) {
            setOrientation(LinearLayout.VERTICAL);
            arrow = new ImageView(context);
            final LayoutParams pi = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
            pi.setMargins(10, 10, 10, 10);
            pi.weight = 0;
            addView(arrow, pi);

            vLabel = new VerticalLabelView(context);
            final LayoutParams p2 = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
            p2.weight = 1;
            p2.setMargins(10, 10, 10, 10);
            //vLabel.setGravity(Gravity.CENTER_VERTICAL);
            //vLabel.setTypeface(Typeface.DEFAULT_BOLD);
            addView(vLabel, p2);

            image = new ImageView(context);
            final LayoutParams p1 = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
            p1.setMargins(10, 10, 10, 10);
            p1.weight = 0;
            addView(image, p1);
            label = null;
        } else {
            setOrientation(LinearLayout.HORIZONTAL);
            arrow = new ImageView(context);
            final LayoutParams pi = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT);
            pi.setMargins(10, 10, 10, 10);
            pi.weight = 0;
            addView(arrow, pi);

            label = new TextView(context);
            final LayoutParams p2 = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT);
            p2.weight = 1;
            p2.setMargins(10, 10, 10, 10);
            label.setGravity(Gravity.CENTER_VERTICAL);
            label.setTypeface(Typeface.DEFAULT_BOLD);
            addView(label, p2);

            image = new ImageView(context);
            final LayoutParams p1 = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT);
            p1.setMargins(10, 10, 10, 10);
            p1.weight = 0;
            addView(image, p1);
            vLabel = null;
        }
    }
}
