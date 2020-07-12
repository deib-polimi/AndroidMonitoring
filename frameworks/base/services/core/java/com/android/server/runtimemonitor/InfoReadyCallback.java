package com.android.server.runtimemonitor;

interface InfoReadyCallback<T>{
    void signalInfoReady(T data);
}