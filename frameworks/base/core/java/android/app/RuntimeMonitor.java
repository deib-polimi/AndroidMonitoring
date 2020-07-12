package android.app;

import android.app.IRuntimeMonitor;
import android.os.IBinder;
import android.os.Process;
import android.os.ServiceManager;
import android.app.RuntimeView;
import android.util.Slog;

/** @hide */
public class RuntimeMonitor {
    private final static String TAG = "RuntimeMonitor";
    private static IRuntimeMonitor monitor;

    public static void reportRuntimeView(RuntimeView hierarchy) {
        IRuntimeMonitor monitor = getService();
        try {
            monitor.reportRuntimeView(hierarchy, Process.myPid());
        } catch (Exception e) {
            Slog.e(TAG, e.getMessage());
        }
    }

    public static void reportHeapInfo(long maxMemory, long totalMemory, long freeMemory) {
        IRuntimeMonitor monitor = getService();
        try {
            monitor.reportHeapInfo(maxMemory, totalMemory, freeMemory, Process.myPid());
        } catch (Exception e) {
            Slog.e(TAG, e.getMessage());
        }
    }

    public static void reportLog(String msg, String tag, String priority) {
        IRuntimeMonitor monitor = getService();
        if (monitor != null) {
            try {
                monitor.reportLog(msg, tag, priority, Process.myPid());
            } catch (Exception e) {
                Slog.e(TAG, e.getMessage());
            }
        }
    }

    private static IRuntimeMonitor getService() {
        if (monitor == null) {
            IBinder b = ServiceManager.getService("runtime_monitor");
            if (b == null) {
                Slog.i(TAG, "binder is null");
                return null;
            }
            monitor = IRuntimeMonitor.Stub.asInterface(b);
        }
        return monitor;
    }
}