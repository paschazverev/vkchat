package com.perm.kate.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

//Fields are optional. Should be null if not populated
public class Chat implements Serializable {
    private static final long serialVersionUID = 1L;
    public long id;
    public String title;
    public long[] users;

    public static Chat parse(JSONObject o) throws JSONException {
        Chat c = new Chat();
        c.id = o.getLong("chat_id");
        c.title = o.getString("title");
        final JSONArray a = o.getJSONArray("users");
        c.users = new long[a.length()];
        for (int i = 0; i < a.length(); i++) {
            final long id = a.getLong(i);
            if (id > 0)
                c.users[i] = id;
        }
        return c;
    }
}
