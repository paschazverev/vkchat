package ru.nacu.vkmsg.engines;

/**
 * @author quadro
 * @since 6/20/12 4:52 PM
 */
public interface MessagingEngine {
    /**
     * android string id для строки, которая появляется при логине/регистрации
     * @return string id
     */
    int getIdentifierStringId();

    RegisterResult register(String identifier, String firstName, String lastName, String password, String sid);

    ConfirmResult confirm(String identifier, String code);

    LoginResult login(String identifier, String password);


}
