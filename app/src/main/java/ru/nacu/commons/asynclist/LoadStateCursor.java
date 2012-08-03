package ru.nacu.commons.asynclist;

import android.database.Cursor;
import android.database.CursorWrapper;

/**
 * Обертка вокруг курсора, чтобы оповещать о том, что данные грузятся
 *
 * @author quadro
 * @since 7/1/12 4:28 PM
 */
public final class LoadStateCursor extends CursorWrapper {
    private final State currentState;
    private final boolean inverse;
    private State pos = State.NONE;

    public LoadStateCursor(Cursor cursor, State currentState, boolean inverse) {
        super(cursor);
        this.inverse = inverse;
        this.currentState = currentState;
    }

    @Override
    public boolean moveToPosition(int position) {
        final State s = currentState;
        if (s == State.START) {
            return false;
        } else if (s == State.END) {
            if (inverse) {
                if (position == 0) {
                    pos = State.END;
                    return true;
                } else {
                    pos = State.NONE;
                    return super.moveToPosition(position - 1);
                }
            } else {
                if (position == super.getCount()) {
                    pos = State.END;
                    return true;
                } else {
                    pos = State.NONE;
                    return super.moveToPosition(position);
                }
            }
        } else {
            pos = State.NONE;
            return super.moveToPosition(position);
        }
    }

    @Override
    public int getCount() {
        final State s = currentState;
        if (s == State.START)
            return 0;
        else if (s == State.END) {
            return super.getCount() + 1;
        }

        return super.getCount();
    }

    @Override
    public long getLong(int columnIndex) {
        switch (pos) {
            case NONE:
                return super.getLong(columnIndex);
            case START:
                return 0;
            case END:
                return 0;
        }

        return super.getLong(columnIndex);
    }
}
