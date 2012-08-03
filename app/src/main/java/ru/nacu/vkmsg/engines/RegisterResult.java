package ru.nacu.vkmsg.engines;

/**
 * @author quadro
 * @since 6/20/12 5:00 PM
 */
public final class RegisterResult {
    public final String sid;
    public final boolean enterCode;

    public RegisterResult(String sid, boolean enterCode) {
        this.sid = sid;
        this.enterCode = enterCode;
    }
}
