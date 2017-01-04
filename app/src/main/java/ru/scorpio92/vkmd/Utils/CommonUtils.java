package ru.scorpio92.vkmd.Utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Created by scorpio92 on 31.10.16.
 */

public class CommonUtils {

    public static String getHumanTimeFromMilliseconds(int milliseconds) {
        String s = "";
        try {
            long hours, min, sec;
            if (milliseconds >= 3600 * 1000) {
                hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
                min = TimeUnit.MILLISECONDS.toMinutes(milliseconds) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliseconds));
                sec = TimeUnit.MILLISECONDS.toSeconds(milliseconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds));
                if(hours < 10)
                    s += "0" + Long.toString(hours) + ":";
                else
                    s += Long.toString(hours) + ":";
                if(min < 10)
                    s += "0" + Long.toString(min) + ":";
                else
                    s += Long.toString(min) + ":";
                if(sec < 10)
                    s += "0" + Long.toString(sec);
                else
                    s += Long.toString(sec);
            } else {
                min = TimeUnit.MILLISECONDS.toMinutes(milliseconds);
                sec = TimeUnit.MILLISECONDS.toSeconds(milliseconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds));
                s += "00:";
                if(min < 10)
                    s += "0" + Long.toString(min) + ":";
                else
                    s += Long.toString(min) + ":";
                if(sec < 10)
                    s += "0" + Long.toString(sec);
                else
                    s += Long.toString(sec);
            }
            return s;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "00:00:00";
    }

    public static String getAppDataDir(Context context) {
        String path = "";
        try {
            PackageManager m = context.getPackageManager();
            String s = context.getPackageName();
            PackageInfo p = m.getPackageInfo(s, 0);
            path = p.applicationInfo.dataDir ;
        } catch (Exception e) {e.printStackTrace();}
        return path;
    }

    public static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }

    /*public static boolean isOnline() {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            int exitValue = ipProcess.waitFor();
            return (exitValue == 0);
        } catch(Exception e) { e.printStackTrace(); }

        return false;
    }*/

    public static boolean isOnline() {
        final int TEST_CONNECTION_TIMEOUT = 1234;
        final boolean[] b = new boolean[1];

        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    Log.w("isOnline", "test connection started with timeout: " + TEST_CONNECTION_TIMEOUT + " ms");
                    HttpURLConnection urlc = (HttpURLConnection) (new URL("http://www.google.com").openConnection());
                    urlc.setRequestProperty("User-Agent", "Test");
                    urlc.setRequestProperty("Connection", "close");
                    urlc.setConnectTimeout(TEST_CONNECTION_TIMEOUT);
                    urlc.connect();
                    b[0] = (urlc.getResponseCode() == 200);
                    Log.w("isOnline", "test connection ended with result: " + b[0]);
                } catch (Exception e) {e.printStackTrace();}//do time consuming operations
            }
        });

        try {
            b[0] = false;
            thread.start();
            Log.w("isOnline", "start test connection wait");
            Thread.sleep(TEST_CONNECTION_TIMEOUT + 1);
            Log.w("isOnline", "end test connection wait");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return b[0];
    }

    public static boolean getBooleanSetting(Context context, String key, boolean defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, defaultValue);
    }

    public static String getStringSetting(Context context, String key, String defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(key, defaultValue);
    }
}
