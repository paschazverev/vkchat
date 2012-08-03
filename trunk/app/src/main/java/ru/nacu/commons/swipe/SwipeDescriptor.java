package ru.nacu.commons.swipe;

/**
 * @author quadro
 * @since 05.04.11 20:16
 */
public interface SwipeDescriptor {
    SwipeAction getLeftAction();

    SwipeAction getRightAction();

    SwipeAction getUpAction();

    SwipeAction getDownAction();

    int getDownArrowResource();

    void enterReadMode();

    void enterMoveMode();

    boolean isBottom();

    boolean isTop();

    boolean isLeft();

    boolean isRight();

    int getTextColor();
}
