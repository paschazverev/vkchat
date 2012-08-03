package ru.nacu.commons.swipe;

/**
 * Представляет собой действие, которое можно сделать в PostActivity
 *
 * @author quadro
 * @since 18.02.11 13:35
 */
public interface SwipeAction {
    boolean canExecute();

    void execute();

    int getLabelResource();

    int getIconResource();
}
