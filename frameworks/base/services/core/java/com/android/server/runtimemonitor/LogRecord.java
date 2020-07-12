package com.android.server.runtimemonitor;


import android.database.Cursor;
import android.util.Slog;
import org.json.JSONObject;


public class LogRecord implements IUploadableData{

    private final static String TAG = "RuntimeMonitor";
    String pkg;
    String log;
    long ts;


    LogRecord(Cursor cursor){
        try{
            pkg = cursor.getString(cursor.getColumnIndexOrThrow(DBStrings.LOG_COLUMN_NAME_PACKAGE_NAME));
            log = cursor.getString(cursor.getColumnIndexOrThrow(DBStrings.LOG_COLUMN_NAME_LOG_ENTRY));
            ts = cursor.getLong(cursor.getColumnIndexOrThrow(DBStrings.LOG_COLUMN_NAME_TIMESTAMP));
        }catch(Exception e){
            Slog.e(TAG,e.getMessage());
        }
    }

    LogRecord(String pkg, String log, long ts){
        this.pkg = pkg;
        this.log = log;
        this.ts = ts;
    }

    public long getTimestamp(){
        return this.ts;
    }

    public JSONObject toJSON(){
        try{
            JSONObject jo = new JSONObject();
            jo.put("package",pkg);
            jo.put("log",log);
            return jo;
        }catch(Exception e){
            Slog.e(TAG,e.getMessage());
            return null;
        }
    }

}