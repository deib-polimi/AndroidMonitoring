package com.android.server.runtimemonitor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import android.os.Build;
import android.app.ActivityManager;
import android.util.Slog;

import android.view.DisplayInfo;

import org.json.JSONObject;

class StaticInfo {

    private final static String TAG = "RuntimeMonitorStaticInfo";

    // Device Info
    public String brand;
    public String device;
    public String model;

    // Build Info
    public String buildNr;
    public String API;

    // CPU Info
    public String cpuModel;
    public int coreNr;

    // RAM Info
    public long RAM;

    private ActivityManager am;
    private StaticInfo thisObj;

    StaticInfo(ActivityManager am){
        this.am=am;
        thisObj=this;
    }

    private void getBasicInfo() {
        brand = Build.BRAND;
        device = Build.DEVICE;
        model = Build.MODEL;
        API = Build.VERSION.SDK;
        buildNr = Build.DISPLAY;
    }

    private void setCPUInfo(String model, String cpuNr) {
        cpuModel = model;
        coreNr = Integer.parseInt(cpuNr) + 1;
    }

    private void setRAM(long RAM) {
        this.RAM = RAM;
    }


    @Override
    public String toString() {
        return "DEVICE INFO\n" + "BRAND: " + brand + "\n" + "DEVICE:" + device + "\n" + "MODEL: " + model + "\n"
                + "BUILD NR: " + buildNr + "\n" + "API: " + API + "\n" + "CPU: " + cpuModel + "\n" + "CORES: "
                + String.valueOf(coreNr) + "\n" + "RAM: ";
    }

    public void readInfo(InfoReadyCallback<StaticInfo> callback){
        new Thread(new Runnable(){
        
            @Override
            public void run() {
                // Read build and device info
                getBasicInfo();
                try{
                    // Read CPU name and core number
                    BufferedReader reader = new BufferedReader(new FileReader("/proc/cpuinfo"));
                    String line = reader.readLine();
                    String model = "";
                    String cpunr = "0";
                    while (line != null) {
                        if (line.startsWith("model name")) {
                            model = line.split("[ ]+", 3)[2];
                        } else if (line.startsWith("processor")) {
                            cpunr = line.split("[ ]+", 3)[1];
                        }
                        line = reader.readLine();
                    }
                    reader.close();
                    
                    setCPUInfo(model, cpunr);

                    // Read RAM info
                    ActivityManager.MemoryInfo mem = new ActivityManager.MemoryInfo();
                    am.getMemoryInfo(mem);
                    setRAM(mem.totalMem / 1024);
                    
                    callback.signalInfoReady(thisObj);
                }catch(Exception e){
                    Slog.e(TAG,e.getMessage());
                }

            }
        }).start();
    }

    public JSONObject toJSON(){
        try{
            JSONObject obj = new JSONObject();
            obj.put("brand",brand);
            obj.put("device",device);
            obj.put("model",model);
            obj.put("build",buildNr);
            obj.put("API",API);
            obj.put("cpuModel",cpuModel);
            obj.put("coreNr",String.valueOf(coreNr));
            obj.put("RAM",String.valueOf(RAM));
    
            return obj;
        }catch(Exception e){
            return null;
        }
    }
}
