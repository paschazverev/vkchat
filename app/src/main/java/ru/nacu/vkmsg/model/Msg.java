package ru.nacu.vkmsg.model;

/**
 * @author quadro
 * @since 6/28/12 2:41 PM
 */
public final class Msg {
    public final long id;
    public final long dialogId;
    public final String body;

    public Msg(long id, long dialogId, String body) {
        this.id = id;
        this.dialogId = dialogId;
        this.body = body;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Msg msg = (Msg) o;

        if (id != msg.id) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    @Override
    public String toString() {
        return "Msg#" + id;
    }
}
