package com.android.server.runtimemonitor;

public class DBStrings {

        private DBStrings() {
        }

        public static final String LOG_TABLE_NAME = "log_table";
        public static final String LOG_COLUMN_NAME_ID = "id";
        public static final String LOG_COLUMN_NAME_LOG_ENTRY = "log_entry";
        public static final String LOG_COLUMN_NAME_PACKAGE_NAME = "package_name";
        public static final String LOG_COLUMN_NAME_TIMESTAMP = "timestamp";

        public static final String LOG_CREATION_QUERY = "CREATE TABLE " + LOG_TABLE_NAME + " (" + LOG_COLUMN_NAME_ID
                        + " INTEGER PRIMARY KEY AUTOINCREMENT, " + LOG_COLUMN_NAME_LOG_ENTRY + " TEXT, "
                        + LOG_COLUMN_NAME_PACKAGE_NAME + " TEXT, "
                        + LOG_COLUMN_NAME_TIMESTAMP + " INTEGER);";

        public static final String DYNAMIC_DATA_TABLE_NAME = "dd_table";
        public static final String DYNAMIC_DATA_COLUMN_NAME_ID = "id";
        public static final String DYNAMIC_DATA_COMUMN_NAME_TIMESTAMP = "timestamp";
        public static final String DYNAMIC_DATA_COLUMN_NAME_PACKAGE = "package";
        public static final String DYNAMIC_DATA_COLUMN_NAME_AVAIL_RAM = "availRam";
        public static final String DYNAMIC_DATA_COLUMN_NAME_LOW_MEMORY = "lowMemory";
        public static final String DYNAMIC_DATA_COLUMN_NAME_THRESHOLD = "threshold";
        public static final String DYNAMIC_DATA_COLUMN_NAME_AVAILABLE_HEAP = "availHeap";
        public static final String DYNAMIC_DATA_COLUMN_NAME_USED_HEAP = "usedHeap";
        public static final String DYNAMIC_DATA_COLUMN_NAME_MAX_HEAP = "maxAvailHeap";
        public static final String DYNAMIC_DATA_COLUMN_NAME_CPU_USAGE = "cpuUsage";
        public static final String DYNAMIC_DATA_COLUMN_NAME_AVG_CPU_USAGE = "avgCpuUsage";
        public static final String DYNAMIC_DATA_COLUMN_NAME_RSS = "rss";
        public static final String DYNAMIC_DATA_COLUMN_NAME_VSZ = "vsz";

        public static final String DYNAMIC_DATA_CREATION_QUERY = "CREATE TABLE " + DYNAMIC_DATA_TABLE_NAME + " ("
                        + DYNAMIC_DATA_COLUMN_NAME_ID + " INTEGER PRIMARY KEY, "
                        + DYNAMIC_DATA_COMUMN_NAME_TIMESTAMP + " INTEGER,"
                        + DYNAMIC_DATA_COLUMN_NAME_PACKAGE + " TEXT, " 
                        + DYNAMIC_DATA_COLUMN_NAME_AVAIL_RAM + " BINGINT, " 
                        + DYNAMIC_DATA_COLUMN_NAME_LOW_MEMORY + " BOOLEAN, "
                        + DYNAMIC_DATA_COLUMN_NAME_THRESHOLD + " BIGINT, " 
                        + DYNAMIC_DATA_COLUMN_NAME_AVAILABLE_HEAP + " BIGINT, " 
                        + DYNAMIC_DATA_COLUMN_NAME_USED_HEAP + " BIGINT, "
                        + DYNAMIC_DATA_COLUMN_NAME_MAX_HEAP + " BIGINT,"
                        + DYNAMIC_DATA_COLUMN_NAME_CPU_USAGE + " INTEGER,"
                        + DYNAMIC_DATA_COLUMN_NAME_AVG_CPU_USAGE + " INTEGER,"
                        + DYNAMIC_DATA_COLUMN_NAME_RSS + " INTEGER,"
                        + DYNAMIC_DATA_COLUMN_NAME_VSZ + " INTEGER)";

        public static final String ACTIVITY_TABLE_NAME = "activity_table";
        public static final String ACTIVITY_COLUMN_NAME_PACKAGE = "package";
        public static final String ACTIVITY_COLUMN_NAME_ACTIVITY = "activity";

        public static final String ACTIVITY_CREATION_QUERY = "CREATE TABLE " + ACTIVITY_TABLE_NAME + " ("
                        + ACTIVITY_COLUMN_NAME_PACKAGE + " TEXT, " + ACTIVITY_COLUMN_NAME_ACTIVITY + " TEXT)";

}