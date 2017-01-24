package ru.scorpio92.kmd.Operations;

import android.os.AsyncTask;

import com.loopj.android.http.HttpGet;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.params.BasicHttpParams;
import cz.msebera.android.httpclient.params.HttpConnectionParams;
import cz.msebera.android.httpclient.params.HttpParams;
import ru.scorpio92.kmd.Utils.InternetUtils;

/**
 * Created by scorpio92 on 1/5/17.
 */

public class GetTrackCount implements InternetUtils.InternetConnectionCallback {

    public static final int GET_TRACKS_COUNT_STATUS_OK = 0;
    public static final int GET_TRACKS_COUNT_STATUS_FAIL = 1;
    public static final int GET_TRACKS_COUNT_NO_INTERNET = 2;

    private final String versionAPI = "3.0";
    private final int TIMEOUT = 3000;

    private GetTrackCountCallback callback;
    private String userID;
    private String ACCESS_TOKEN;
    private int count;

    public GetTrackCount(GetTrackCountCallback callback, String userID, String ACCESS_TOKEN) {
        this.callback = callback;
        this.userID = userID;
        this.ACCESS_TOKEN = ACCESS_TOKEN;
        count = -1;
        new InternetUtils().checkInternetConnectionAsync(GetTrackCount.this);
    }

    @Override
    public void onCheckComplete(boolean result) {
        if(result)
            new Task().execute();
        else
            callback.onGetTrackCount(-1, GET_TRACKS_COUNT_NO_INTERNET);
    }

    private class Task extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... params) {

            StringBuilder sb = new StringBuilder();

            try {
                HttpGet httpget = new HttpGet("https://api.vk.com/method/audio.getCount?oid=" + userID + "&access_token=" + ACCESS_TOKEN + "&v=" + versionAPI);
                HttpParams httpParams = new BasicHttpParams();
                HttpConnectionParams.setConnectionTimeout(httpParams, TIMEOUT);
                HttpConnectionParams.setSoTimeout(httpParams, TIMEOUT);
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
                return GET_TRACKS_COUNT_STATUS_FAIL;
            }
            count = parseResponse(sb.toString());
            if(count != -1)
                return GET_TRACKS_COUNT_STATUS_OK;
            else
                return GET_TRACKS_COUNT_STATUS_FAIL;
        }

        @Override
        protected void onPostExecute(Integer responseCode) {
            super.onPostExecute(responseCode);
            callback.onGetTrackCount(count, responseCode);
        }
    }

    private Integer parseResponse(String response) {
        try {
            JSONObject dataJsonObj = new JSONObject(response);
            return Integer.valueOf(dataJsonObj.getString("response").trim());
        } catch (Exception e) {e.printStackTrace();}
        return -1;
    }

    public interface GetTrackCountCallback {
        void onGetTrackCount(int count, int responseCode);
    }
}
