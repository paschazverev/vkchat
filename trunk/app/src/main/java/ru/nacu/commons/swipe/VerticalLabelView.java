package ru.nacu.commons.swipe;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextPaint;
import android.view.View;

public class VerticalLabelView extends View {
    private TextPaint mTextPaint;
    private String mText = "";
    private int mAscent;
    private Rect text_bounds = new Rect();

    final static int DEFAULT_TEXT_SIZE = 15;

    public VerticalLabelView(Context context) {
        super(context);
        initLabelView();
    }

    private void initLabelView() {
        mTextPaint = new TextPaint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(DEFAULT_TEXT_SIZE);
        mTextPaint.setColor(0xFF000000);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        setPadding(3, 3, 3, 3);
    }

    public void setText(int text) {
        mText = getContext().getString(text);
        requestLayout();
        invalidate();
    }

    public void setTextSize(int size) {
        mTextPaint.setTextSize(size);
        requestLayout();
        invalidate();
    }

    public void setTextColor(int color) {
        mTextPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        mTextPaint.getTextBounds(mText, 0, mText.length(), text_bounds);
        setMeasuredDimension(
                measureWidth(widthMeasureSpec),
                measureHeight(heightMeasureSpec));
    }

    private int measureWidth(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
        } else {
            // Measure the text
            result = text_bounds.height() + getPaddingLeft() + getPaddingRight();

            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by measureSpec
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    private int measureHeight(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        mAscent = (int) mTextPaint.ascent();
        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
        } else {
            // Measure the text
            result = text_bounds.width() + getPaddingTop() + getPaddingBottom();

            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by measureSpec
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float text_horizontally_centered_origin_x = getPaddingLeft() + text_bounds.width() / 2f;
        float text_horizontally_centered_origin_y = getPaddingTop() - mAscent;

        canvas.translate(text_horizontally_centered_origin_y, text_horizontally_centered_origin_x);
        canvas.rotate(-90);

        int w = text_bounds.height() + getPaddingLeft() + getPaddingRight();
        canvas.drawText(mText, 0, (getWidth() - w) / 2, mTextPaint);
    }
}