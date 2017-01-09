package ru.scorpio92.kmd.Utils;

import android.os.AsyncTask;
import android.util.Log;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by scorpio92 on 1/5/17.
 */

public class InternetUtils {

    private final String LOG_TAG = "InternetUtils";
    private final String TEST_HOST = "https://www.vk.com";
    private final int DEFAULT_CONNECTION_TIMEOUT = 9999; //ms

    private InternetConnectionCallback callback;


    public interface InternetConnectionCallback {
        void onCheckComplete(boolean result);
    }


    public boolean checkInternetConnectionAsync(InternetConnectionCallback callback) {
        if(callback != null) {
            this.callback = callback;
            new CheckInternetConnectionAsyncTask().execute(DEFAULT_CONNECTION_TIMEOUT);
            return true;
        }
        return false;
    }

    public boolean checkInternetConnectionAsync(InternetConnectionCallback callback, final int connection_timeout) {
        if(callback != null) {
            this.callback = callback;
            new CheckInternetConnectionAsyncTask().execute(connection_timeout);
            return true;
        }
        return false;
    }


    private boolean baseTestConnectionMethod(int connection_timeout) {
        try {
            if(connection_timeout > 0) {
                Log.w(LOG_TAG, "test connection started with timeout: " + connection_timeout + " ms");
                HttpURLConnection urlc = (HttpURLConnection) (new URL(TEST_HOST).openConnection());
                urlc.setRequestProperty("User-Agent", "Test");
                urlc.setRequestProperty("Connection", "close");
                urlc.setConnectTimeout(connection_timeout);
                urlc.connect();
                boolean b = (urlc.getResponseCode() == 200);
                Log.w(LOG_TAG, "test connection ended with result: " + b);
                return b;

            }
        } catch (Exception e) {e.printStackTrace();}
        return false;
    }

    public boolean checkInternetConnection(final int connection_timeout) {
        final boolean[] b = new boolean[1];

        Thread thread = new Thread(new Runnable() {
            public void run() {
                b[0] = baseTestConnectionMethod(connection_timeout);
            }
        });

        try {
            b[0] = false;
            thread.start();
            Log.w(LOG_TAG, "start test connection wait");
            Thread.sleep(connection_timeout + 1);
            Log.w(LOG_TAG, "end test connection wait");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return b[0];
    }

    private class CheckInternetConnectionAsyncTask extends AsyncTask<Integer, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Integer... params) {
            return baseTestConnectionMethod(params[0]);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            callback.onCheckComplete(result);
        }
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
}
