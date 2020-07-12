package android.app;

import java.util.List;
import android.app.RuntimeView;


/**
 * System private API for talking with the activity manager service.  This
 * provides calls from the application back to the activity manager.
 *
 * {@hide}
 */
oneway interface IRuntimeMonitor {
    void reportRuntimeView(in RuntimeView hierarchy, in int pid);
    void reportHeapInfo(in long maxMemory, in long totalMemory, in long freeMemory, in int pid);
    void reportLog(in String msg, in String tag, in String type, in int pid);
}