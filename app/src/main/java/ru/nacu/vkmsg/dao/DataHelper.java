package ru.nacu.vkmsg.dao;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import ru.common.StreamTools;

import java.io.InputStream;

/**
 * @author quadro
 * @since 6/21/12 4:57 PM
 */
public final class DataHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "data";
    public static final int DATABASE_VERSION = 19;

    private void executeScript(SQLiteDatabase db, String name, boolean stopOnError) {
        InputStream create = getClass().getClassLoader().getResourceAsStream(name + ".sql");
        try {
            String s = StreamTools.readToString(create);
            String[] execute = s.split(";");

            for (String line : execute) {
                if (line.trim().length() != 0) {
                    try {
                        db.execSQL(line);
                    } catch (Exception e) {
                        if (stopOnError) { //else ignore
                            throw e;
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            StreamTools.close(create);
        }
    }

    public DataHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        executeScript(sqLiteDatabase, "create", true);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVer, int newVer) {
        for (int i = oldVer + 1; i <= newVer; i++) {
            executeScript(db, "update" + i, true);

            if (i == 12) {
                db.beginTransaction();
                final Cursor c = db.rawQuery("select _id, first_name, last_name, phone_name from profile", null);
                try {
                    while (c.moveToNext()) {
                        long id = c.getLong(0);
                        String first = c.getString(1);
                        String last = c.getString(2);
                        String phoneName = c.getString(3);

                        String searchString = "";
                        if (first != null) {
                            searchString += first.toLowerCase();
                        }
                        if (last != null) {
                            searchString += last.toLowerCase();
                        }
                        if (phoneName != null) {
                            searchString += phoneName.toLowerCase();
                        }

                        db.execSQL("update profile set search_string = ? where _id = ?", new Object[]{searchString, id});
                    }

                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                    c.close();
                }
            }
        }
    }
}
