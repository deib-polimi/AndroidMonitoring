package com.android.server.runtimemonitor;

import android.content.Context;
import android.content.Intent;
import android.app.PendingIntent;
import android.app.ActivityManager;
import android.content.IntentFilter;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;

import android.content.BroadcastReceiver;
import android.net.ConnectivityManager;
import android.os.ServiceManager;
import android.util.Slog;
import android.net.Network;
import android.app.AlarmManager;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import android.net.TrafficStats;
import org.json.JSONArray;
import org.json.JSONObject;

class NetworkHandler extends BroadcastReceiver implements InfoReadyCallback<StaticInfo>{

    private final static int THREAD_ID=10382801;
    private final static String TAG = "RuntimeMonitor";
    private final static String observableProcesses= "http://10.0.2.2:5000/static/obs-packages.json";
    private final static String dataUpload= "http://10.0.2.2:5000/data";
    private final static String viewUpload= "http://10.0.2.2:5000/view";
    private final static String logUpload= "http://10.0.2.2:5000/logs";
    private final static String INTENT = "com.android.server.runtimemonitor.SYNC";
    private final static int refreshPeriod = 60000;
    private Context context;
    private boolean registered = false;
    private boolean connected = false;
    private Object lock = new Object();
    private ConnectivityManager cm;
    private CallbackHandler callback;
    private AlarmManager am;
    private PendingIntent syncPI;
    private Object mapLock = new Object();
    private Set<String> obspSet; 
    private StaticInfo staticInfo;
    private boolean staticInfoReady=false;
    private DBManager dbManager; 
    
    private boolean downloadOnConnect= true;    



    public NetworkHandler(Context context, DBManager dbManager){
        this.context = context;
        this.dbManager = dbManager;
        obspSet = new HashSet<String>();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        StaticInfo si = new StaticInfo(activityManager);
        si.readInfo(this);
        cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(INTENT);
        context.registerReceiver(this,intentFilter);
        Intent syncIntent = new Intent(INTENT);
        syncIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        syncPI = PendingIntent.getBroadcast(context, 0, syncIntent, 0);
        register();
        scheduleSync();
    }


    private void scheduleSync(){
        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + refreshPeriod,syncPI);
    }


    public void signalInfoReady(StaticInfo data){
        this.staticInfo = data;
        this.staticInfoReady=true;
    }

    @Override
    public void onReceive (Context context, Intent intent){
        if(intent.getAction() == INTENT){
            Slog.i(TAG,"Sync Started");
            scheduleSync();
            downloadObservableProcesses();
            if(this.staticInfoReady){
                syncLocalData();
            }
        }
    }

    public boolean isObservable(String pckName){
        return obspSet.contains(pckName);
    }

    public void register(){
        synchronized(lock){
            if(!registered){
                callback = new CallbackHandler();
                cm.registerDefaultNetworkCallback(callback);
                registered = true;
            }
        }
    }

    public void unregister(){
        synchronized(lock){
            if(registered){
                cm.unregisterNetworkCallback(callback);
                registered = false;
            }
        }
    }

    private class CallbackHandler extends ConnectivityManager.NetworkCallback{
        @Override
        public void onAvailable(Network network) {
            synchronized(lock){
                connected=true;
                Slog.i(TAG, "Network available");
                if(downloadOnConnect){
                    downloadObservableProcesses();                      
                    if(staticInfoReady){
                        syncLocalData();
                    }
                }
            }
        }

        @Override
        public void onLost(Network network) {
            synchronized(lock){
                connected=false;
                Slog.i(TAG, "Network  Lost");
            } 
        }


    }


    private void downloadObservableProcesses(){
        downloadOnConnect = false;
        new Thread(new Runnable(){
            
            @Override
            public void run() {
                    Slog.i(TAG,"Observable processes list download started");
                    TrafficStats.setThreadStatsTag(THREAD_ID);
                    HttpURLConnection urlConnection = null;
                    try {
                        URL url = new URL(observableProcesses);
                        urlConnection = (HttpURLConnection) url.openConnection();
                        String result ="";
                        int code = urlConnection.getResponseCode();
            
                        if(code==200){
                            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                            if (in != null) {
                                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
                                String line = "";
            
                                while ((line = bufferedReader.readLine()) != null)
                                    result += line;
                            }
                            in.close();
                        }
                        JSONObject json =  new JSONObject(result);
                        JSONArray jsArray = json.getJSONArray("packages");
                        Set<String> newSet = new HashSet<String>();
                        for(int i = 0; i < jsArray.length(); i++){
                            newSet.add((String)jsArray.get(i));
                        }
                        obspSet = newSet;
                        Slog.i(TAG,"Observable processes list download ended");
                    } catch (Exception e){
                        downloadOnConnect = true;
                        Slog.e(TAG, e.getMessage());
                    }finally {
                        urlConnection.disconnect();
                    }
                
            }
        }).start();
    }

    private void syncLocalData(){
        new Thread(new Runnable(){
            
            @Override
            public void run() {
                try{
                    Slog.i(TAG,"Synchronization started");
                    List<IUploadableData> info = dbManager.getDynamicInfo();
                    JSONArray jInfo = convertToJSON(info);
                    long ts = getMaxTimestamp(info);
                    boolean success = uploadData(null,jInfo,dataUpload);
                    if(success){
                        dbManager.deleteDynamicData(ts);
                    } 

                    List<String> views = dbManager.getScannedActivityNames();
                    for(String activity : views){
                        BufferedReader reader = new BufferedReader(new FileReader("/data/system/runtime_monitor/"+activity+".json"));
                        String file = "";   
                        String line = "";
                        while((line = reader.readLine())!=null){
                            file += line;
                        }
                        reader.close();
                        JSONObject jView = new JSONObject(file);
                        success = uploadData(jView,null,viewUpload);
                        if(success){
                            dbManager.deleteScannedActivity(activity);
                            File f= new File("/data/system/runtime_monitor/"+activity+".json");
                            Files.deleteIfExists(f.toPath());
                        } 
                    }

                    List<IUploadableData> logs = dbManager.getLogs();
                    JSONArray jLog = convertToJSON(logs);
                    ts = getMaxTimestamp(logs);
                    success = uploadData(null,jLog,logUpload);
                    if(success){
                        dbManager.deleteLogs(ts);
                    } 


                    Slog.i(TAG,"Synchronization finished");
                }catch(Exception e){
                    Slog.e(TAG, e.getMessage());
                    Slog.i(TAG,"Synchronization finished with errors");
                }
            }
        }).start();
    }

    private JSONArray convertToJSON(List<IUploadableData> data){
        JSONArray jData = new JSONArray();
        for(IUploadableData d: data){
            jData.put(d.toJSON());
        }
        return jData;
    }

    private long getMaxTimestamp(List<IUploadableData> data){
        long timestamp = 0;
        for(IUploadableData d: data){
            if(d.getTimestamp() > timestamp){
                timestamp = d.getTimestamp();
            }     
        }
        return timestamp;
    }

    private boolean uploadData(JSONObject objectData, JSONArray arrayData, String stringUrl){

        TrafficStats.setThreadStatsTag(THREAD_ID);
        try {

            JSONObject jsonData = staticInfo.toJSON();

            if(objectData != null){
                jsonData.put("data", objectData);
            }else if(arrayData !=null && arrayData.length() > 0){
                jsonData.put("data", arrayData);
            }else{
                return false;
            }
            
            URL url = new URL(stringUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", "application/json");
            con.setDoOutput(true);
            con.setUseCaches(false);

            String jsonString = jsonData.toString();
            byte[] input = jsonString.getBytes("utf-8");
            con.setRequestProperty("Content-Length", Integer.toString(input.length));
            
            try(DataOutputStream wr = new DataOutputStream(con.getOutputStream())){
                wr.write(input);		
                wr.flush();	
            }
    
            int code = con.getResponseCode();
            if(code == 200){
                return true;
            }  
            
        } catch(Exception e){
            Slog.e(TAG,"Data upload failed");
            Slog.e(TAG,e.getMessage());
        }
        return false;
    }

}

