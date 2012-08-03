package ru.nacu.vkmsg.ui.chat;

/**
 * @author quadro
 * @since 7/10/12 2:24 AM
 */
public class Temp {
    public final long id;
    public final int pos;

    public Temp(long id, int pos) {
        this.id = id;
        this.pos = pos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Temp temp = (Temp) o;

        if (id != temp.id) return false;
        if (pos != temp.pos) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + pos;
        return result;
    }
}
