package ru.scorpio92.vkmd.Operations;

import android.os.AsyncTask;
import android.util.Log;

import com.loopj.android.http.HttpGet;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.params.BasicHttpParams;
import cz.msebera.android.httpclient.params.HttpConnectionParams;
import cz.msebera.android.httpclient.params.HttpParams;
import ru.scorpio92.vkmd.Interfaces.OperationsCallbacks;
import ru.scorpio92.vkmd.Utils.CommonUtils;

/**
 * Created by scorpio92 on 03.11.16.
 */

//https://vk.com/dev/audio.get

public class GetTrackListByOwnerID {

    public static final int GET_MUSIC_LIST_STATUS_OK = 0;
    public static final int GET_MUSIC_LIST_STATUS_FAIL = 1;
    public static final int GET_MUSIC_LIST_NO_INTERNET = 2;

    private final String versionAPI = "3.0";
    private final String maxTracksCount = "2000";

    private OperationsCallbacks callback;
    private String USER_ID, ACCESS_TOKEN;

    private String RESPONSE;

    public GetTrackListByOwnerID(OperationsCallbacks callback, String USER_ID, String ACCESS_TOKEN) {
        this.callback = callback;
        this.USER_ID = USER_ID;
        this.ACCESS_TOKEN = ACCESS_TOKEN;
        RESPONSE = "";
        try {
            if(CommonUtils.isOnline())
                new Task().execute();
            else
                callback.onGetTrackListComplete(GET_MUSIC_LIST_NO_INTERNET, RESPONSE);
        } catch (Exception e) {
            e.printStackTrace();
            callback.onGetTrackListComplete(GET_MUSIC_LIST_STATUS_FAIL, RESPONSE);
        }
    }

    private class Task extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... params) {

            StringBuilder sb = new StringBuilder();

            try {
                HttpGet httpget = new HttpGet("https://api.vk.com/method/audio.get?oid=" + USER_ID + "&need_user=0&count=" + maxTracksCount + "&offset=0&access_token=" + ACCESS_TOKEN + "&v=" + versionAPI);
                HttpParams httpParams = new BasicHttpParams();
                HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
                HttpClient httpclient = new DefaultHttpClient(httpParams);
                HttpResponse response = httpclient.execute(httpget);
                HttpEntity entity = response.getEntity();

                if (entity != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                sb.append("error");
                return GET_MUSIC_LIST_STATUS_FAIL;
            }
            RESPONSE = sb.toString();
            return GET_MUSIC_LIST_STATUS_OK;
        }

        @Override
        protected void onPostExecute(Integer responseCode) {
            super.onPostExecute(responseCode);
            Log.w("RESPONSE", RESPONSE);
            callback.onGetTrackListComplete(responseCode, RESPONSE);
        }
    }
}
