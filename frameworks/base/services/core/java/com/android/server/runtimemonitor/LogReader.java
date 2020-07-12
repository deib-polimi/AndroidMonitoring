package com.android.server.runtimemonitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.android.server.runtimemonitor.DBManager;

import android.util.Slog;

public class LogReader {

    private final int readPeriod = 15000;
    private Map<Integer, String> activePidLastRead;
    private Map<Integer, String> pidProcessNameMap;
    private List<Integer> activePids;
    private List<Integer> pidsToBeRemoved;
    private Thread readingThread;
    private final String logLevel = "W";
    private final String TAG = "RuntimeMonitor";
    private DBManager dbManager;

    LogReader(DBManager dbManager) {
        this.dbManager = dbManager;
        startReading();
        activePids = new ArrayList<>();
        pidsToBeRemoved = new ArrayList<>();
        activePidLastRead = new HashMap<Integer, String>();
        pidProcessNameMap = new HashMap<Integer, String>();
    }

    public synchronized void listenToProcess(int pid, String pname) {
        if (activePids.indexOf(pid) < 0) {
            activePids.add(pid);
            pidProcessNameMap.put(pid, pname);
        }
    }

    public synchronized void removeProcess(int pid) {
        int index = activePids.indexOf(pid);
        if (index >= 0) {
            pidsToBeRemoved.add(pid);
        }
    }

    private void removeProcessesAfterRead() {
        for (int pid : pidsToBeRemoved) {
            int index = activePids.indexOf(pid);
            if (index >= 0) {
                activePids.remove(index);
                activePidLastRead.remove(pid);
                pidProcessNameMap.remove(pid);
            }
        }
    }

    private void startReading() {
        try {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    readingThread = Thread.currentThread();

                    while (readingThread == Thread.currentThread()) {
                        for (int pid : activePids) {
                            try {
                                List<String> logs = readLogs(pid);
                                saveLogs(logs, pidProcessNameMap.get(pid));
                                removeProcessesAfterRead();
                            } catch (Exception e) {
                                Slog.e(TAG, e.getMessage());
                            }
                        }
                        try {
                            Thread.sleep(readPeriod);
                        } catch (Exception e) {
                            Slog.e(TAG, e.getMessage());
                        }
                    }
                }
            }).start();
        } catch (Exception e) {
            Slog.e(TAG, e.getMessage());
        }
    }

    private synchronized List<String> readLogs(int pid) throws IOException {
        String logcatString = "logcat *:" + logLevel + " -d --pid=" + String.valueOf(pid);
        if (activePidLastRead.get(pid) != null) {
            logcatString += " -T " + activePidLastRead.get(pid);
        }
        Process process = Runtime.getRuntime().exec(logcatString);

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        List<String> logs = new ArrayList<String>();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            // Add line to logs
            logs.add(line.trim());
            // Save last read so that next time we can start reading from this.
            String[] lsplt = line.split("[ ]+");
            activePidLastRead.put(pid, "'" + lsplt[0] + " " + (lsplt[1]) + "'");
        }
        return logs;
    }

    private void saveLogs(List<String> logs, String packageName) throws IOException {
        for (String log : logs) {
            dbManager.addLog(packageName, log);
        }
    }

}