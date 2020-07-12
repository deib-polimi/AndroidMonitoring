package com.android.server.runtimemonitor;

import java.sql.Timestamp;    
import java.util.Date;

import com.android.server.runtimemonitor.CPUStatsReader.CPUStats;

import org.json.JSONArray;
import org.json.JSONObject;


import android.database.Cursor;

class DynamicInfo implements IUploadableData{

    int pid;
    long ts;
    String packageName;
    // RAM info
    long availableRam;
    boolean lowMemory;
    long threshold;
    // HEAP info
    long availableHeap;
    long usedHeap;
    long maxAvailableHeap;
    // CPU Info
    double usage;
    double avgUsage;
    long rss;
    long vsz;


    public void setRam(long availableRam, boolean lowMemory, long threshold) {
        this.availableRam = availableRam;
        this.lowMemory = lowMemory;
        this.threshold = threshold;
    }

    public void setHeap(long availableHeap, long usedHeap, long maxAvailableHeap) {
        this.availableHeap = availableHeap;
        this.usedHeap = usedHeap;
        this.maxAvailableHeap = maxAvailableHeap;
    }

    public void setTimestamp(long ts){
        this.ts=ts;
    }

    public void setCPUInfo(CPUStats stats){
        this.usage = stats.currUsage;
        this.avgUsage = stats.avgUsage;
        this.rss = stats.rss;
        this.vsz = stats.vsz;
    }

    public DynamicInfo(int pid, String packageName) {
        this.pid = pid;
        this.packageName = packageName;
    }

    public DynamicInfo(Cursor cursor){
        this.ts = cursor.getLong(cursor.getColumnIndexOrThrow(DBStrings.DYNAMIC_DATA_COMUMN_NAME_TIMESTAMP));
        this.packageName = cursor.getString(cursor.getColumnIndexOrThrow(DBStrings.DYNAMIC_DATA_COLUMN_NAME_PACKAGE));
        this.availableRam = cursor.getLong(cursor.getColumnIndexOrThrow(DBStrings.DYNAMIC_DATA_COLUMN_NAME_AVAIL_RAM));
        this.lowMemory = cursor.getInt(cursor.getColumnIndexOrThrow(DBStrings.DYNAMIC_DATA_COLUMN_NAME_LOW_MEMORY)) > 0;
        this.threshold = cursor.getLong(cursor.getColumnIndexOrThrow(DBStrings.DYNAMIC_DATA_COLUMN_NAME_THRESHOLD));
        this.availableHeap = cursor.getLong(cursor.getColumnIndexOrThrow(DBStrings.DYNAMIC_DATA_COLUMN_NAME_AVAILABLE_HEAP));
        this.usedHeap = cursor.getLong(cursor.getColumnIndexOrThrow(DBStrings.DYNAMIC_DATA_COLUMN_NAME_USED_HEAP));
        this.maxAvailableHeap = cursor.getLong(cursor.getColumnIndexOrThrow(DBStrings.DYNAMIC_DATA_COLUMN_NAME_MAX_HEAP));
        this.usage = cursor.getDouble(cursor.getColumnIndexOrThrow(DBStrings.DYNAMIC_DATA_COLUMN_NAME_CPU_USAGE));
        this.avgUsage = cursor.getDouble(cursor.getColumnIndexOrThrow(DBStrings.DYNAMIC_DATA_COLUMN_NAME_AVG_CPU_USAGE));
        this.rss = cursor.getLong(cursor.getColumnIndexOrThrow(DBStrings.DYNAMIC_DATA_COLUMN_NAME_RSS));
        this.vsz = cursor.getLong(cursor.getColumnIndexOrThrow(DBStrings.DYNAMIC_DATA_COLUMN_NAME_VSZ));
    }

    public DynamicInfo(int pid, String packageName, long availableRam, long threshold, boolean lowMemory,
            long availableHeap, long usedHeap, long maxAvailableHeap,long ts,double cpuUsage, double avgUsage, long rss, long vsz) {

        this.pid = pid;
        this.packageName = packageName;
        this.availableRam = availableRam;
        this.threshold = threshold;
        this.lowMemory = lowMemory;
        this.availableHeap = availableHeap;
        this.usedHeap = usedHeap;
        this.maxAvailableHeap = maxAvailableHeap;
        this.ts=ts;
        this.usage = cpuUsage;
        this.avgUsage = avgUsage;
        this.rss= rss;
        this.vsz = vsz;
    }

    public JSONObject toJSON(){
        JSONObject obj = new JSONObject();
        try{
            obj.put("package",packageName);
            obj.put("timestamp",String.valueOf(ts));
            obj.put("availRam",String.valueOf(availableRam));
            obj.put("isLowMem",String.valueOf(lowMemory));
            obj.put("threshold",String.valueOf(threshold));
            obj.put("availHeap",String.valueOf(availableHeap));
            obj.put("usedHeap",String.valueOf(usedHeap));
            obj.put("maxAvailHeap",String.valueOf(maxAvailableHeap));
            obj.put("cpuUsage", String.valueOf(usage));
            obj.put("avgCpuUsage", String.valueOf(avgUsage));
            obj.put("RSS", String.valueOf(rss));
            obj.put("VSZ", String.valueOf(vsz));
            return obj;

        }catch(Exception e){
            return null;
        }
    }

    public long getTimestamp(){
        return ts;
    }
}