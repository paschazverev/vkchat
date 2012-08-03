package ru.nacu.vkmsg.engines;

/**
 * @author quadro
 * @since 6/20/12 5:04 PM
 */
public final class ConfirmResult {
    public final boolean success;
    public final String error;
    public final String uid;

    public ConfirmResult(boolean success, String error, String uid) {
        this.success = success;
        this.error = error;
        this.uid = uid;
    }
}
