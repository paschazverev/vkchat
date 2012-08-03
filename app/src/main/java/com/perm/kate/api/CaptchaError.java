package com.perm.kate.api;

/**
 * @author quadro
 * @since 6/29/12 5:09 PM
 */
public class CaptchaError extends Exception {
    public final String sid;
    public final String img;

    public CaptchaError(String sid, String img) {
        this.sid = sid;
        this.img = img;
    }
}
