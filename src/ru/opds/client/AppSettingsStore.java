package ru.opds.client;

import android.content.Context;
import android.content.SharedPreferences;

public class AppSettingsStore {
    private static final String PREFS = "app_settings";
    private static final String KEY_DOWNLOAD_DIR = "download_dir";
    private static final String DEFAULT_DIR = "/sdcard/Books";

    private final SharedPreferences prefs;

    public AppSettingsStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public String getDownloadDir() {
        return prefs.getString(KEY_DOWNLOAD_DIR, DEFAULT_DIR);
    }

    public void setDownloadDir(String path) {
        prefs.edit().putString(KEY_DOWNLOAD_DIR, path).commit();
    }
}