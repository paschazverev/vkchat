package ru.nacu.commons.swipe;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.RelativeLayout;

/**
 * Предоставляет возможность делать swipe в разные стороны
 *
 * @author quadro
 * @since 05.04.11 19:33
 */
@SuppressWarnings({"UnusedDeclaration"})
public class SwipeView extends RelativeLayout {
    private final RotateAnimation downToUp;
    private final RotateAnimation downToUpImm;
    private final RotateAnimation upToDown;
    private final RotateAnimation upToDownImm;
    private final RotateAnimation leftToRight;
    private final RotateAnimation leftToRightImm;
    private final RotateAnimation rightToLeft;
    private final RotateAnimation rightToLeftImm;

    private final SwipeLabel leftLabel;
    private final SwipeLabel rightLabel;
    private final SwipeLabel topLabel;
    private final SwipeLabel bottomLabel;

    private View content;

    private SwipeDescriptor descriptor;

    private boolean swipeUpActivated = false;
    private boolean swipeDownActivated = false;
    private boolean swipeLeftActivated = false;
    private boolean swipeRightActivated = false;

    private boolean swipeEnabled = true;
    private int swipeSensitivity = 110;

    public void setSwipeEnabled(boolean swipeEnabled) {
        this.swipeEnabled = swipeEnabled;
    }

    public void setSwipeSensitivity(int swipeSensitivity) {
        this.swipeSensitivity = swipeSensitivity;
    }

    /**
     * координаты touch event, когда пользователь начал swipe
     */
    private int swipeStartX;
    private int swipeStartY;

    /**
     * Количество пальцев
     */
    private int down = 0;

    public void setDescriptor(SwipeDescriptor descriptor) {
        this.descriptor = descriptor;

        LayoutParams topParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        LayoutParams bottomParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        LayoutParams leftParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        LayoutParams rightParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        leftParams.addRule(RelativeLayout.CENTER_VERTICAL);
        leftParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        leftLabel.setVisibility(View.INVISIBLE);
        leftLabel.arrow.setImageResource(descriptor.getDownArrowResource());
        leftLabel.setTextColor(descriptor.getTextColor());

        rightParams.addRule(RelativeLayout.CENTER_VERTICAL);
        rightParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        rightLabel.setVisibility(View.INVISIBLE);
        rightLabel.arrow.setImageResource(descriptor.getDownArrowResource());
        rightLabel.setTextColor(descriptor.getTextColor());

        topParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        topParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        topLabel.setVisibility(View.INVISIBLE);
        topLabel.arrow.setImageResource(descriptor.getDownArrowResource());
        topLabel.setTextColor(descriptor.getTextColor());

        bottomParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        bottomParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        bottomLabel.setVisibility(View.INVISIBLE);
        bottomLabel.arrow.setImageResource(descriptor.getDownArrowResource());
        bottomLabel.setTextColor(descriptor.getTextColor());

        super.addView(leftLabel, leftParams);
        super.addView(rightLabel, rightParams);
        super.addView(topLabel, topParams);
        super.addView(bottomLabel, bottomParams);

        setUpActivated(false, true);
        setDownActivated(false, true);
        setLeftActivated(false, true);
        setRightActivated(false, true);
    }

    public SwipeView(Context context) {
        this(context, null);
    }

    public SwipeView(Context context, AttributeSet attrs) {
        super(context, attrs);

        rightLabel = new SwipeLabel(context, true);
        leftLabel = new SwipeLabel(context, true);
        topLabel = new SwipeLabel(context, false);
        bottomLabel = new SwipeLabel(context, false);

        downToUp = new RotateAnimation(0, 180,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        downToUp.setInterpolator(new LinearInterpolator());
        downToUp.setDuration(250);
        downToUp.setFillAfter(true);

        downToUpImm = new RotateAnimation(0, 180,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        downToUpImm.setDuration(0);
        downToUpImm.setFillAfter(true);

        leftToRight = new RotateAnimation(90, -90,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        leftToRight.setInterpolator(new LinearInterpolator());
        leftToRight.setDuration(250);
        leftToRight.setFillAfter(true);

        leftToRightImm = new RotateAnimation(90, -90,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        leftToRightImm.setDuration(0);
        leftToRightImm.setFillAfter(true);

        rightToLeft = new RotateAnimation(-90, 90,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        rightToLeft.setInterpolator(new LinearInterpolator());
        rightToLeft.setDuration(250);
        rightToLeft.setFillAfter(true);

        rightToLeftImm = new RotateAnimation(-90, 90,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        rightToLeftImm.setDuration(0);
        rightToLeftImm.setFillAfter(true);

        upToDown = new RotateAnimation(180, 0,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        upToDown.setInterpolator(new LinearInterpolator());
        upToDown.setDuration(250);
        upToDown.setFillAfter(true);

        upToDownImm = new RotateAnimation(180, 0,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        upToDownImm.setDuration(0);
        upToDownImm.setFillAfter(true);
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        if (child != leftLabel && child != rightLabel && child != topLabel && child != bottomLabel) {
            content = child;
            super.addView(child, params);
        } else {
            super.addView(child, params);
        }
    }

    private boolean cancelled = false;

    public boolean dispatchTouchEvent(MotionEvent me) {
        if (swipeEnabled) {
            if (me.getAction() == MotionEvent.ACTION_DOWN
                    || me.getAction() == 5
                    || me.getAction() == 261
                    || me.getAction() == 517) {
                down++;
            }

            if (me.getAction() == MotionEvent.ACTION_UP
                    || me.getAction() == 6
                    || me.getAction() == 262
                    || me.getAction() == 518) {
                down--;
            }

            if (me.getAction() == MotionEvent.ACTION_MOVE) {
                if (down < 2) {
                    if (!swipeLeftActivated) {
                        if (descriptor.getLeftAction() != null && descriptor.getLeftAction().canExecute() && isRight() && !swipeRightActivated && swipeUpDistance == 0 && swipeDownDistance == 0) {
                            swipeLeftActivated = true;
                            enterMoveMode();

                            swipeStartX = (int) me.getX();
                        }
                    } else {
                        int swipeDistance = swipeStartX - (int) me.getX();
                        if (swipeDistance < 0) {
                            swipeLeftActivated = false;

                            if (setSwipeLeftDistance(0)) {
                                final MotionEvent ev = MotionEvent.obtain(me);
                                ev.setAction(MotionEvent.ACTION_CANCEL);
                                super.dispatchTouchEvent(ev);
                                cancelled = true;
                                return true;
                            }
                        } else {
                            if (setSwipeLeftDistance(swipeDistance)) {
                                final MotionEvent ev = MotionEvent.obtain(me);
                                ev.setAction(MotionEvent.ACTION_CANCEL);
                                super.dispatchTouchEvent(ev);
                                cancelled = true;
                                return true;
                            }
                        }
                    }

                    if (!swipeRightActivated) {
                        if (descriptor.getRightAction() != null && descriptor.getRightAction().canExecute() && isLeft() && !swipeLeftActivated && swipeUpDistance == 0 && swipeDownDistance == 0) {
                            swipeRightActivated = true;

                            enterMoveMode();

                            swipeStartX = (int) me.getX();
                        }
                    } else {
                        int swipeDistance = (int) me.getX() - swipeStartX;
                        if (swipeDistance < 0) {
                            swipeRightActivated = false;

                            if (setSwipeRightDistance(0)) {
                                final MotionEvent ev = MotionEvent.obtain(me);
                                ev.setAction(MotionEvent.ACTION_CANCEL);
                                super.dispatchTouchEvent(ev);
                                cancelled = true;
                                return true;
                            }
                        } else {
                            if (setSwipeRightDistance(swipeDistance)) {
                                final MotionEvent ev = MotionEvent.obtain(me);
                                ev.setAction(MotionEvent.ACTION_CANCEL);
                                super.dispatchTouchEvent(ev);
                                cancelled = true;
                                return true;
                            }
                        }
                    }

                    if (!swipeUpActivated) {
                        if (descriptor.getUpAction() != null && descriptor.getUpAction().canExecute() && isBottom()
                                && !swipeDownActivated && swipeLeftDistance == 0 && swipeRightDistance == 0) {
                            swipeUpActivated = true;
                            enterMoveMode();

                            swipeStartY = (int) me.getY();
                        }
                    } else {
                        int swipeDistance = swipeStartY - (int) me.getY();
                        if (swipeDistance < 0) {
                            swipeUpActivated = false;
                            if (setSwipeUpDistance(0)) {
                                final MotionEvent ev = MotionEvent.obtain(me);
                                ev.setAction(MotionEvent.ACTION_CANCEL);
                                super.dispatchTouchEvent(ev);
                                cancelled = true;
                                return true;
                            }
                        } else if (isBottom()) {
                            if (setSwipeUpDistance(swipeDistance)) {
                                final MotionEvent ev = MotionEvent.obtain(me);
                                ev.setAction(MotionEvent.ACTION_CANCEL);
                                super.dispatchTouchEvent(ev);
                                cancelled = true;
                                return true;
                            }
                        }
                    }

                    if (!swipeDownActivated) {
                        if (descriptor.getDownAction() != null && descriptor.getDownAction().canExecute() && isTop()
                                && !swipeUpActivated && swipeLeftDistance == 0 && swipeRightDistance == 0) {

                            swipeDownActivated = true;
                            enterMoveMode();

                            swipeStartY = (int) me.getY();
                        }
                    } else {
                        int swipeDistance = (int) me.getY() - swipeStartY;
                        if (swipeDistance < 0) {
                            swipeDownActivated = false;

                            if (setSwipeDownDistance(0)) {
                                final MotionEvent ev = MotionEvent.obtain(me);
                                ev.setAction(MotionEvent.ACTION_CANCEL);
                                super.dispatchTouchEvent(ev);
                                cancelled = true;
                                return true;
                            }
                        } else if (isTop()) {
                            if (setSwipeDownDistance(swipeDistance)) {
                                final MotionEvent ev = MotionEvent.obtain(me);
                                ev.setAction(MotionEvent.ACTION_CANCEL);
                                super.dispatchTouchEvent(ev);
                                cancelled = true;
                                return true;
                            }
                        }
                    }

                }
            } else if (me.getAction() == MotionEvent.ACTION_UP) {
                if (swipeUpActivated) {
                    swipeUpActivated = false;
                    setUpActivated(false, true);

                    if (isBottom()) {
                        int swipeDistance = swipeStartY - (int) me.getY();
                        enterReadMode();

                        setSwipeUpDistance(0);

                        if (swipeDistance > maxSwipeY)
                            descriptor.getUpAction().execute();
                    }
                }

                if (swipeDownActivated) {
                    swipeDownActivated = false;
                    setDownActivated(false, true);

                    if (isTop()) {
                        int swipeDistance = (int) me.getY() - swipeStartY;
                        setSwipeDownDistance(0);

                        enterReadMode();

                        if (swipeDistance > maxSwipeY) {
                            descriptor.getDownAction().execute();
                        }
                    }
                }

                if (swipeLeftActivated) {
                    swipeLeftActivated = false;
                    setLeftActivated(false, true);
                    if (isRight()) {
                        int swipeDistance = swipeStartX - (int) me.getX();
                        setSwipeLeftDistance(0);

                        enterReadMode();

                        if (swipeDistance > maxSwipeX) {
                            descriptor.getLeftAction().execute();
                        }
                    }
                }

                if (swipeRightActivated) {
                    swipeRightActivated = false;
                    setRightActivated(false, true);
                    if (isLeft()) {
                        int swipeDistance = (int) me.getX() - swipeStartX;
                        setSwipeRightDistance(0);

                        enterReadMode();

                        if (swipeDistance > maxSwipeX) {
                            descriptor.getRightAction().execute();
                        }
                    }
                }
            }
        }

        if (cancelled) {
            cancelled = false;

            if (me.getAction() == MotionEvent.ACTION_MOVE) {
                final MotionEvent newEvent = MotionEvent.obtain(me);
                newEvent.setAction(MotionEvent.ACTION_DOWN);
                super.dispatchTouchEvent(newEvent);
            }

            return super.dispatchTouchEvent(me);
        } else {
            return super.dispatchTouchEvent(me);
        }
    }

    private boolean isBottom() {
        return descriptor.isBottom();
    }

    private boolean isTop() {
        return descriptor.isTop();
    }

    private boolean isLeft() {
        return descriptor.isLeft();
    }

    private boolean isRight() {
        return descriptor.isRight();
    }

    private boolean isMoving = false;

    /**
     * Проставить для wv и view определенные размеры (а не fill_parent)
     */
    private void enterMoveMode() {
        if (isMoving)
            return;

        isMoving = true;

        maxSwipeX = Math.min(getWidth() / 3, swipeSensitivity + 200);
        maxSwipeY = Math.min(getHeight() / 3, swipeSensitivity + 200);

        descriptor.enterMoveMode();
    }

    private void enterReadMode() {
        isMoving = false;
        scrollTo(0, 0);

        descriptor.enterReadMode();
    }

    private int maxSwipeX = 300;
    private int maxSwipeY = 300;

    private int swipeDownDistance = 0;
    private double multiplier = 0.55;

    private boolean downActivated = false;
    private boolean upActivated = false;
    private boolean leftActivated = false;
    private boolean rightActivated = false;

    private boolean setSwipeDownDistance(int distance) {
        boolean dispatched = true;
        if (distance < swipeSensitivity && distance > 0) {
            dispatched = false;

            distance = 0;
        }

        if (distance == swipeDownDistance) {
            return distance != 0 && dispatched;
        }

        if (swipeDownDistance == 0) {
            final LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
            lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            lp.topMargin = -topLabel.getHeight() - 10;
            topLabel.setLayoutParams(lp);
            topLabel.image.setImageResource(descriptor.getDownAction().getIconResource());
            topLabel.setText(descriptor.getDownAction().getLabelResource());
            topLabel.setVisibility(View.VISIBLE);
        }

        swipeDownDistance = distance;
        if (distance != 0) {
            swipeUpActivated = false;
            swipeLeftActivated = false;
            swipeRightActivated = false;
        }

        setDownActivated(maxSwipeY < distance, false);
        scrollTo(0, 0 - (int) (distance * multiplier));

        return true;
    }

    public void setDownActivated(boolean downActivated, boolean immediate) {
        if (downActivated != this.downActivated) {
            if (this.downActivated) {
                topLabel.arrow.clearAnimation();
                if (immediate)
                    topLabel.arrow.startAnimation(upToDownImm);
                else
                    topLabel.arrow.startAnimation(upToDown);
            } else {
                topLabel.arrow.clearAnimation();
                topLabel.arrow.startAnimation(downToUp);
            }
        }

        this.downActivated = downActivated;
    }

    private int swipeRightDistance = 0;

    private boolean setSwipeRightDistance(int distance) {
        boolean dispatched = true;
        if (distance < swipeSensitivity && distance > 0) {
            dispatched = false;

            distance = 0;
        }

        if (distance == swipeRightDistance) {
            return distance != 0 && dispatched;
        }

        if (swipeRightDistance == 0) {
            final LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            lp.addRule(RelativeLayout.CENTER_VERTICAL);
            lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            lp.leftMargin = -leftLabel.getWidth() - 10;
            leftLabel.setLayoutParams(lp);
            leftLabel.image.setImageResource(descriptor.getRightAction().getIconResource());
            leftLabel.setText(descriptor.getRightAction().getLabelResource());
            leftLabel.setVisibility(View.VISIBLE);
        }

        swipeRightDistance = distance;
        if (distance != 0) {
            swipeDownActivated = false;
            swipeLeftActivated = false;
            swipeUpActivated = false;
        }

        setRightActivated(maxSwipeX < distance, false);

        scrollTo(-(int) (distance * multiplier), 0);

        return true;
    }

    public void setRightActivated(boolean rightActivated, boolean immediate) {
        if (rightActivated != this.rightActivated) {
            if (this.rightActivated) {
                leftLabel.arrow.clearAnimation();
                if (immediate)
                    leftLabel.arrow.startAnimation(leftToRightImm);
                else
                    leftLabel.arrow.startAnimation(leftToRight);
            } else {
                leftLabel.arrow.clearAnimation();
                leftLabel.arrow.startAnimation(rightToLeft);
            }
        }

        this.rightActivated = rightActivated;
    }

    private int swipeUpDistance = 0;

    private boolean setSwipeUpDistance(int distance) {
        boolean dispatched = true;
        if (distance < swipeSensitivity && distance > 0) {
            dispatched = false;

            distance = 0;
        }

        if (distance == swipeUpDistance) {
            return distance != 0 && dispatched;
        }

        if (swipeUpDistance == 0) {
            final LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
            lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            lp.bottomMargin = -bottomLabel.getHeight() - 10;
            bottomLabel.setLayoutParams(lp);
            bottomLabel.image.setImageResource(descriptor.getUpAction().getIconResource());
            bottomLabel.setText(descriptor.getUpAction().getLabelResource());
            bottomLabel.setVisibility(View.VISIBLE);
        }

        if (distance != 0) {
            swipeDownActivated = false;
            swipeLeftActivated = false;
            swipeRightActivated = false;
        }

        swipeUpDistance = distance;
        setUpActivated(maxSwipeY < distance, false);
        scrollTo(0, (int) (distance * multiplier));
        return true;
    }

    public void setUpActivated(boolean upActivated, boolean immediate) {
        if (upActivated != this.upActivated) {
            if (this.upActivated) {
                bottomLabel.arrow.clearAnimation();
                if (immediate)
                    bottomLabel.arrow.startAnimation(downToUpImm);
                else
                    bottomLabel.arrow.startAnimation(downToUp);
            } else {
                bottomLabel.arrow.clearAnimation();
                bottomLabel.arrow.startAnimation(upToDown);
            }
        }

        this.upActivated = upActivated;
    }

    private int swipeLeftDistance = 0;

    private boolean setSwipeLeftDistance(int distance) {
        boolean dispatched = true;
        if (distance < swipeSensitivity && distance > 0) {
            dispatched = false;

            distance = 0;
        }

        if (distance == swipeLeftDistance) {
            return distance != 0 && dispatched;
        }

        if (swipeLeftDistance == 0) {
            final LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            lp.addRule(RelativeLayout.CENTER_VERTICAL);
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            lp.rightMargin = -rightLabel.getWidth() - 10;
            rightLabel.setLayoutParams(lp);
            rightLabel.image.setImageResource(descriptor.getLeftAction().getIconResource());
            rightLabel.setText(descriptor.getLeftAction().getLabelResource());
            rightLabel.setVisibility(View.VISIBLE);
        }

        swipeLeftDistance = distance;
        if (distance != 0) {
            swipeDownActivated = false;
            swipeUpActivated = false;
            swipeRightActivated = false;
        }

        setLeftActivated(maxSwipeX < distance, false);
        scrollTo((int) (distance * multiplier), 0);

        return true;
    }

    public void setLeftActivated(boolean leftActivated, boolean immediate) {
        if (leftActivated != this.leftActivated) {
            if (this.leftActivated) {
                rightLabel.arrow.clearAnimation();
                if (immediate)
                    rightLabel.arrow.startAnimation(rightToLeftImm);
                else
                    rightLabel.arrow.startAnimation(rightToLeft);
            } else {
                rightLabel.arrow.clearAnimation();
                rightLabel.arrow.startAnimation(leftToRight);
            }
        }

        this.leftActivated = leftActivated;
    }

}
