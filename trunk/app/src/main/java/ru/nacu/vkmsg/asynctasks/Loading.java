package ru.nacu.vkmsg.asynctasks;

import ru.nacu.commons.asynclist.State;

import java.util.HashMap;
import java.util.Map;

/**
 * Статусы операций загрузки
 *
 * @author quadro
 * @since 7/1/12 4:26 PM
 */
public final class Loading {
    /**
     * Статус загрузки диалогов
     */
    private static State dialogs = State.NONE;

    private static State friends = State.NONE;

    private static State search = State.NONE;

    private static State searchDialogs = State.NONE;

    private static State contacts = State.NONE;

    /**
     * Статус загрузки сообщений для диалога
     */
    private static Map<Long, State> messages = new HashMap<Long, State>();

    public synchronized static State getContacts() {
        return contacts;
    }

    public synchronized static void setContacts(State contacts) {
        Loading.contacts = contacts;
    }

    public synchronized static State getSearch() {
        return search;
    }

    public synchronized static void setSearch(State s) {
        Loading.search = s;
    }

    public synchronized static State getSearchDialogs() {
        return searchDialogs;
    }

    public synchronized static void setSearchDialogs(State searchDialogs) {
        Loading.searchDialogs = searchDialogs;
    }

    public synchronized static State getDialogs() {
        return dialogs;
    }

    public synchronized static void setDialogs(State s) {
        Loading.dialogs = s;
    }

    public synchronized static State getFriends() {
        return friends;
    }

    public synchronized static void setFriends(State s) {
        Loading.friends = s;
    }

    public synchronized static State getDialog(long id) {
        final State state = messages.get(id);
        return state == null ? State.NONE : state;
    }

    public synchronized static void setDialog(long id, State s) {
        messages.put(id, s);
    }
}
