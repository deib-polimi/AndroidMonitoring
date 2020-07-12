package com.android.server.runtimemonitor;

import android.content.Context;
import android.util.Slog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;

import com.android.server.runtimemonitor.DBHelper;
import com.android.server.runtimemonitor.DBStrings;
import android.database.sqlite.SQLiteDatabase;
import android.content.ContentValues;
import android.database.Cursor;

public class DBManager {

    private DBHelper dbHelper;
    private final static String TAG = "RuntimeMonitor";

    public DBManager(Context context) {
        dbHelper = new DBHelper(context);
    }



    public boolean addLog(String packageName, String logEntry) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DBStrings.LOG_COLUMN_NAME_LOG_ENTRY, logEntry);
        cv.put(DBStrings.LOG_COLUMN_NAME_PACKAGE_NAME, packageName);
        Date now = new Date();
        cv.put(DBStrings.LOG_COLUMN_NAME_TIMESTAMP, now.getTime());
        try {
            db.insert(DBStrings.LOG_TABLE_NAME, null, cv);
            return true;
        } catch (Exception e) {
            Slog.e(TAG, e.getMessage());
        }
        return false;
    }

    public List<IUploadableData> getLogs() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM "+ DBStrings.LOG_TABLE_NAME,null);

        List<IUploadableData> logs = new ArrayList();
        while (cursor.moveToNext()) {
           logs.add(new LogRecord(cursor));
        }
        cursor.close();
        return logs;
    }

    public void deleteLogs(long ts){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("DELETE FROM "+ DBStrings.LOG_TABLE_NAME + " WHERE "+ DBStrings.LOG_COLUMN_NAME_TIMESTAMP+" <= "+String.valueOf(ts));
    }

    public boolean saveDynamicInfo(DynamicInfo info) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DBStrings.DYNAMIC_DATA_COLUMN_NAME_PACKAGE, info.packageName);
        cv.put(DBStrings.DYNAMIC_DATA_COMUMN_NAME_TIMESTAMP, info.ts);
        cv.put(DBStrings.DYNAMIC_DATA_COLUMN_NAME_AVAIL_RAM, info.availableRam);
        cv.put(DBStrings.DYNAMIC_DATA_COLUMN_NAME_LOW_MEMORY, info.lowMemory);
        cv.put(DBStrings.DYNAMIC_DATA_COLUMN_NAME_THRESHOLD, info.threshold);
        cv.put(DBStrings.DYNAMIC_DATA_COLUMN_NAME_AVAILABLE_HEAP, info.availableHeap);
        cv.put(DBStrings.DYNAMIC_DATA_COLUMN_NAME_USED_HEAP, info.usedHeap);
        cv.put(DBStrings.DYNAMIC_DATA_COLUMN_NAME_MAX_HEAP, info.maxAvailableHeap);
        cv.put(DBStrings.DYNAMIC_DATA_COLUMN_NAME_CPU_USAGE, info.usage);
        cv.put(DBStrings.DYNAMIC_DATA_COLUMN_NAME_AVG_CPU_USAGE, info.avgUsage);
        cv.put(DBStrings.DYNAMIC_DATA_COLUMN_NAME_VSZ, info.vsz);
        cv.put(DBStrings.DYNAMIC_DATA_COLUMN_NAME_RSS, info.rss);
        try {
            db.insert(DBStrings.DYNAMIC_DATA_TABLE_NAME, null, cv);
            return true;
        } catch (Exception e) {
            Slog.e(TAG, e.getMessage());
        }
        return false;
    }

    public boolean saveAnalyzedView(String className, String pkg) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DBStrings.ACTIVITY_COLUMN_NAME_PACKAGE, pkg);
        cv.put(DBStrings.ACTIVITY_COLUMN_NAME_ACTIVITY, className);
        try {
            db.insert(DBStrings.ACTIVITY_TABLE_NAME, null, cv);
            return true;
        } catch (Exception e) {
            Slog.e(TAG, e.getMessage());
        }
        return false;
    }

    public boolean activityAlreadyScanned(String className, String pkg) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery(String.format("SELECT * FROM %s WHERE %s=\"%s\" and %s=\"%s\";",
                    DBStrings.ACTIVITY_TABLE_NAME, DBStrings.ACTIVITY_COLUMN_NAME_PACKAGE, pkg,
                    DBStrings.ACTIVITY_COLUMN_NAME_ACTIVITY, className), null);
            int lines = cursor.getCount();
            cursor.close();
            return lines > 0;
        } catch (Exception e) {
            Slog.e(TAG, e.getMessage());
        }

        return false;

    }

    public void clearDB() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.execSQL("DROP TABLE IF EXISTS " + DBStrings.LOG_TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + DBStrings.ACTIVITY_TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + DBStrings.DYNAMIC_DATA_TABLE_NAME);
            db.execSQL(DBStrings.LOG_CREATION_QUERY);
            db.execSQL(DBStrings.ACTIVITY_CREATION_QUERY);
            db.execSQL(DBStrings.DYNAMIC_DATA_CREATION_QUERY);
        } catch (Exception e) {
            Slog.e(TAG, e.getMessage());
        }
    }

    public List<IUploadableData> getDynamicInfo(){
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try{
            Cursor cursor = db.rawQuery("SELECT * FROM " + DBStrings.DYNAMIC_DATA_TABLE_NAME+ " ORDER BY " + DBStrings.DYNAMIC_DATA_COMUMN_NAME_TIMESTAMP + " ASC",null);
            List<IUploadableData> data = new ArrayList<>();
            while (cursor.moveToNext()) {
                DynamicInfo info = new DynamicInfo(cursor);
                data.add(info);
            }
            cursor.close();
            return data;
        }catch(Exception e){
            Slog.e(TAG, e.getMessage());
        }
        return null;
    }

    public List<String> getScannedActivityNames(){
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try{
            Cursor cursor = db.rawQuery("SELECT * FROM " + DBStrings.ACTIVITY_TABLE_NAME,null);
            List<String> scannedActivities = new ArrayList<String>();
            while(cursor.moveToNext()){
                scannedActivities.add(cursor.getString(cursor.getColumnIndexOrThrow(DBStrings.ACTIVITY_COLUMN_NAME_ACTIVITY)));
            }   
            cursor.close();
            return scannedActivities;
        }catch(Exception e){
            Slog.e(TAG,e.getMessage());
        }

        return null;
    }

    public void deleteDynamicData(long timestamp){
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        db.execSQL("DELETE FROM "+ DBStrings.DYNAMIC_DATA_TABLE_NAME+ " WHERE " + DBStrings.DYNAMIC_DATA_COMUMN_NAME_TIMESTAMP + " <= " +String.valueOf(timestamp));
    }

    public void deleteScannedActivity(String activityName){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("DELETE FROM "+ DBStrings.ACTIVITY_TABLE_NAME + " WHERE "+ DBStrings.ACTIVITY_COLUMN_NAME_ACTIVITY + " = '"+ activityName+ "'");
    }
}