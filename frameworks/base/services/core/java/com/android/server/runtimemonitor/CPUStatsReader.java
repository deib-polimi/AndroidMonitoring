package com.android.server.runtimemonitor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;

import android.util.Slog;

class CPUStatsReader {

    private static final String TAG="RuntimeMonitor";

    public CPUStatsReader() {
    }

    public CPUStats readStats(int pid) {
        try {
            CPUStats result = new CPUStats();

            Process process = Runtime.getRuntime().exec("ps -p " + String.valueOf(pid) + " -o %cpu=,vsz=,rss=");
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            // We expect to read two lines only
            br.readLine();
            String line = br.readLine();
            if(line != null){
                line=line.trim();
                String[] vals = line.split("[ ]+");
                result.avgUsage = Double.parseDouble(vals[0]);
                result.vsz= Long.parseLong(vals[1]);
                result.rss=Long.parseLong(vals[2]);
            }
            br.close();

            Process psProc = Runtime.getRuntime().exec("top -b -n 2 -d 1 -p " +String.valueOf(pid));
            br = new BufferedReader(new InputStreamReader(psProc.getInputStream()));
            String temp;
            // We want element 9 of the last line
            while((temp = br.readLine())!=null){
                line = temp.trim();
            }
            result.currUsage = Double.parseDouble(line.split("[ ]+")[8]);

            br.close();

            return result;
        } catch (Exception e) {
            Slog.e(TAG,e.getMessage());
        }
        return null;

    }


    public class CPUStats{
        public int pid;
        public double currUsage;
        public double avgUsage;
        public long rss;
        public long vsz;
    }
}