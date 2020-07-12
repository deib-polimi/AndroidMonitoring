package com.android.server.runtimemonitor;

import org.json.JSONObject;

public interface IUploadableData {

    long getTimestamp();

    JSONObject toJSON();

}