package com.android.server.runtimemonitor;

import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.content.Context;
import com.android.server.runtimemonitor.DBStrings;

public class DBHelper extends SQLiteOpenHelper {

    public final static String DATABASE_NAME = "runtime_monitor_db";
    public final static int DATABASE_VERSION = 1;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DBStrings.LOG_CREATION_QUERY);
        db.execSQL(DBStrings.ACTIVITY_CREATION_QUERY);
        db.execSQL(DBStrings.DYNAMIC_DATA_CREATION_QUERY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != newVersion) {
            clear(db);
        }
    }

    public void clearDB(SQLiteDatabase db) {
        clear(db);
    }

    private void clear(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + DBStrings.LOG_CREATION_QUERY);
        db.execSQL("DROP TABLE IF EXISTS " + DBStrings.ACTIVITY_CREATION_QUERY);
        db.execSQL("DROP TABLE IF EXISTS " + DBStrings.DYNAMIC_DATA_CREATION_QUERY);
        onCreate(db);
    }
}