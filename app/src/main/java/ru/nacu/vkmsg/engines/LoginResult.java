package ru.nacu.vkmsg.engines;

/**
 * @author quadro
 * @since 6/20/12 4:56 PM
 */
public final class LoginResult {
    public final String token;
    public final String userId;
    public final boolean needCaptcha;
    public final String captchaSid;
    public final String captchaUrl;

    public LoginResult(String token, String userId, boolean needCaptcha, String captchaSid, String captchaUrl) {
        this.token = token;
        this.userId = userId;
        this.needCaptcha = needCaptcha;
        this.captchaSid = captchaSid;
        this.captchaUrl = captchaUrl;
    }
}
