package com.android.server.runtimemonitor;

import android.app.IProcessObserver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.android.server.am.ActivityManagerService;
import com.android.server.am.ProcessRecord;
import com.android.server.SystemService;
import android.os.ServiceManager;
import android.app.IApplicationThread;
import android.app.ActivityManager;

import android.app.ActivityManager.RunningAppProcessInfo;

import android.util.Slog;
import android.content.Context;
import android.app.IRuntimeMonitor;
import android.app.RuntimeView;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;

import android.hardware.display.IDisplayManager;
import android.view.DisplayInfo;

import org.json.JSONObject;

import com.android.server.runtimemonitor.*;
import com.android.server.runtimemonitor.CPUStatsReader.CPUStats;

public class RuntimeMonitorService extends IRuntimeMonitor.Stub {

    final static String TAG = "RuntimeMonitor";

    private ActivityManagerService am;
    private IDisplayManager dm;
    private Context context;

    private ProcessObserver po;
    private Map<Integer, String> pidProcessName;
    private List<Integer> observablePids;
    private Map<Integer, ProcessRecord> pidProcessRecord;
    private DBManager dbManager;

    private final int readInterval = 30000;

    private Thread monitorThread;
    private ScreenLockReceiver broadReceiver;
    private CPUStatsReader cpuStatsReader;

    private boolean screenOn;
    private Object lock = new Object();
    private NetworkHandler networkHandler;

    public RuntimeMonitorService(Context context) {
        this.context = context;
        this.observablePids = new ArrayList<Integer>();
        this.pidProcessName = new HashMap<Integer, String>();
        this.pidProcessRecord = new HashMap<Integer, ProcessRecord>();
        this.cpuStatsReader = new CPUStatsReader();
        this.dbManager = new DBManager(context);
        this.dbManager.clearDB();
        this.screenOn = true;
        broadReceiver = new ScreenLockReceiver();
        IntentFilter on = new IntentFilter("android.intent.action.SCREEN_ON");
        IntentFilter off = new IntentFilter("android.intent.action.SCREEN_OFF");
        context.registerReceiver(broadReceiver, on);
        context.registerReceiver(broadReceiver, off);
    }

    public void setActivityManager(ActivityManagerService am) {
        this.am = am;
    }

    private class ScreenLockReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == "android.intent.action.SCREEN_ON") {
                synchronized (lock) {
                    screenOn = true;
                    if (monitorThread == null) {
                        startMonitorThread();
                    }
                }
            } else if (intent.getAction() == "android.intent.action.SCREEN_OFF") {
                synchronized (lock) {
                    screenOn = false;
                }
            }
        }

    }

    /**
     * Process Observer class
     */
    private class ProcessObserver extends IProcessObserver.Stub {

        @Override
        public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
            if (foregroundActivities) {
            Slog.i(TAG,"Foreground process changed");
                try {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if(pidProcessName.get(pid) == null){
                                    String processName = "";
                                    List<RunningAppProcessInfo> processes = am.getRunningAppProcesses();
                                    for (RunningAppProcessInfo process : processes) {
                                        if (process.pid == pid) {
                                            processName = process.processName;
                                            break;
                                        }
                                    }

                                    pidProcessName.put(pid, processName);

                                    if(networkHandler.isObservable(processName)){
                                        observablePids.add(pid);
                                        if (monitorThread == null && screenOn) {
                                            startMonitorThread();
                                        }
                                    }
                                }else if(observablePids.contains(pid) && monitorThread == null && screenOn){
                                        startMonitorThread();
                                }
                                
                            } catch (Exception e) {
                                Slog.e(TAG, e.getMessage());
                            }

                        }
                    }).start();
                } catch (Exception e) {
                    Slog.e(TAG, e.getMessage());
                }
            }
        }

        @Override
        public void onProcessDied(int pid, int uid) {
            pidProcessName.remove(pid);
            int index = observablePids.indexOf(pid);
            if(index >= 0){
                observablePids.remove(index);
            }            
        }

    }

    private void startMonitorThread() {
        synchronized (lock) {
            if (screenOn) {
                monitorThread = getMonitorThread();
                monitorThread.start();
            }
        }
    }

    private Thread getMonitorThread() {
        return new Thread(new Runnable() {

            @Override
            public void run() {
                boolean reading = true;
                while (reading) {
                    synchronized (lock) {
                        if (!screenOn) {
                            break;
                        }
                    }
                    if (observablePids.size() == 0) {
                        monitorThread = null;
                        reading = false;
                    } else {
                        for (int pid: observablePids) {
                            ProcessRecord pr = am.getProcessRecord(pidProcessName.get(pid));
                            try {
                                if (pr.hasForegroundActivities() || pr.hasForegroundServices()) {
                                    pidProcessRecord.put(pr.pid, pr);
                                    pr.getApplicationThread().requestHeapInfo();
                                }
                            } catch (Exception e) {
                                Slog.e(TAG, e.getMessage());
                            }
                        }
                        try {
                            Thread.sleep(readInterval);
                        } catch (Exception e) {

                        }
                    }
                }
                synchronized (lock) {
                    monitorThread = null;
                }
            }
        });
    }

    @Override
    public void reportRuntimeView(RuntimeView hierarchy, int pid) {
        try {
            // Process process = Runtime.getRuntime().exec("mkdir -p
            // /data/system/runtime_monitor");
            ProcessRecord pr = pidProcessRecord.get(pid);
            DisplayInfo di = pr.getDisplayInfo();
            if (di == null) {
                Slog.i(TAG, "DI is null");
            }
            JSONObject obj = new JSONObject();
            obj.put("children", hierarchy.getJSON());

            int pxWidth = di.logicalWidth;
            int pxHeigth = di.logicalHeight;
            double dpi = di.logicalDensityDpi;
            double density = dpi / 160.0;
            double dpWidth = (int) (pxWidth / density);
            double dpHeigth = (int) (pxHeigth / density);

            obj.put("pxWidth", pxWidth);
            obj.put("pxHeigth", pxHeigth);
            obj.put("dpi", dpi);
            obj.put("dpWidth", dpWidth);
            obj.put("dpHeight", dpHeigth);

            String activityName;
            if (pr != null && (activityName = pr.getVisibleActivityName()) != null) {
                obj.put("activity", activityName);
                File file = new File("/data/system/runtime_monitor/" + activityName + ".json");
                file.getParentFile().mkdirs();
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                writer.write(obj.toString());

                writer.close();
            }
        } catch (Exception e) {
            Slog.e(TAG, e.getMessage());
        }
    }

    @Override
    public void reportHeapInfo(long maxMemory, long totalMemory, long freeMemory, int pid) {
        new Thread(new Runnable() {            

            @Override
            public void run() {
                try {
                    DynamicInfo info = new DynamicInfo(pid, pidProcessName.get(pid));

                    /** HEAP information */
                    long usedHeap = totalMemory - freeMemory;
                    info.setHeap(maxMemory - usedHeap, usedHeap, maxMemory);

                    /** RAM information */
                    ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                    am.getMemoryInfo(mi);
                    info.setRam(mi.availMem/1024, mi.lowMemory, mi.threshold/1024);
                    Date now = new Date();
                    info.setTimestamp(now.getTime());
                    
                    /** CPU information */
                    CPUStats cpuStats = cpuStatsReader.readStats(pid);
                    info.setCPUInfo(cpuStats);

                    dbManager.saveDynamicInfo(info);

                    ProcessRecord pr = pidProcessRecord.get(pid);

                    String activityName;
                    if (pr != null && (activityName = pr.getVisibleActivityName()) != null) {
                        if (!dbManager.activityAlreadyScanned(activityName, pidProcessName.get(pid))) {
                            pr.getApplicationThread().requestRuntimeView(pr.getVisibleActivityToken());
                            dbManager.saveAnalyzedView(activityName,pidProcessName.get(pid));
                        }
                    }
                } catch (Exception e) {
                    Slog.e(TAG, e.getMessage());
                }
            }
        }).start();
        ;
    }

    @Override
    public void reportLog(String msg, String tag, String type, int pid) {
        if(observablePids.indexOf(pid) >= 0){
            String processName;
                processName = pidProcessName.get(pid);

            Slog.d(TAG, "Received log from pid "+ String.valueOf(pid) + " "+ processName);
            if (processName != null) {
                String log = String.format("%s %s \n %s", tag, type, msg);
                dbManager.addLog(processName, log);
            }
        }
    }

    private void start() {
        Slog.d(TAG, "Service started");        
        try {
            networkHandler = new NetworkHandler(context,dbManager);
            Slog.d(TAG, "Network handler started");
            po = new ProcessObserver();
            Slog.d(TAG, "Start observing processes");
            am.registerProcessObserver(po);
            dm = IDisplayManager.Stub.asInterface(ServiceManager.getService(Context.DISPLAY_SERVICE));
        } catch (Exception e) {
            Slog.e(TAG, e.getMessage());
        }
    }

    public static final class LifeCycle extends SystemService {
        private final RuntimeMonitorService mService;

        @Override
        public void onStart() {
        }

        public LifeCycle(Context context) {
            super(context);
            mService = new RuntimeMonitorService(context);
        }

        @Override
        public void onBootPhase(int phase) {
            if (phase == PHASE_BOOT_COMPLETED) {
                mService.start();
            }
        }

        public RuntimeMonitorService getService() {
            return mService;
        }
    }

}