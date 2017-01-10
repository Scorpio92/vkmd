package ru.scorpio92.kmd.Operations;

import android.os.AsyncTask;
import android.util.Log;

import com.loopj.android.http.HttpGet;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URLEncoder;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.params.BasicHttpParams;
import cz.msebera.android.httpclient.params.HttpConnectionParams;
import cz.msebera.android.httpclient.params.HttpParams;
import ru.scorpio92.kmd.Utils.InternetUtils;

/**
 * Created by scorpio92 on 09.01.17.
 */

public class SearchTracks implements InternetUtils.InternetConnectionCallback {

    public static final int SEARCH_TRACKS_STATUS_OK = 0;
    public static final int SEARCH_TRACKS_STATUS_FAIL = 1;
    public static final int SEARCH_TRACKS_NO_INTERNET = 2;

    private final int CONNECTION_TIMEOUT = 15000;
    private final String versionAPI = "3.0";
    public static final int DEFAULT_TRACKS_COUNT = 500;

    private SearchTracksCallback callback;
    private String searchString;
    private String performer_only;
    private String ACCESS_TOKEN;
    private String RESPONSE;


    public SearchTracks(SearchTracksCallback callback, String searchString, boolean search_by_artist, String ACCESS_TOKEN) {
        this.callback = callback;
        this.searchString = searchString;
        this.performer_only = Integer.toString((search_by_artist) ? 1 : 0);
        this.ACCESS_TOKEN = ACCESS_TOKEN;
        RESPONSE = "";
        new InternetUtils().checkInternetConnectionAsync(SearchTracks.this);
    }

    @Override
    public void onCheckComplete(boolean result) {
        if(result)
            new Task().execute();
        else
            callback.onSearchTracksComplete(SEARCH_TRACKS_NO_INTERNET, RESPONSE);
    }

    private class Task extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... params) {

            StringBuilder sb = new StringBuilder();

            try {
                HttpGet httpget = new HttpGet("https://api.vk.com/method/audio.search?q=" + URLEncoder.encode(searchString, "UTF-8") + "&performer_only=" + performer_only + "&count=" + DEFAULT_TRACKS_COUNT + "&v=" + versionAPI + "&access_token=" + ACCESS_TOKEN);
                HttpParams httpParams = new BasicHttpParams();
                HttpConnectionParams.setConnectionTimeout(httpParams, CONNECTION_TIMEOUT);
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
                return SEARCH_TRACKS_STATUS_FAIL;
            }
            RESPONSE = sb.toString();
            return SEARCH_TRACKS_STATUS_OK;
        }

        @Override
        protected void onPostExecute(Integer responseCode) {
            super.onPostExecute(responseCode);
            Log.w("RESPONSE", RESPONSE);
            callback.onSearchTracksComplete(responseCode, RESPONSE);
        }
    }

    public interface SearchTracksCallback {
        void onSearchTracksComplete(int code, String response);
    }
}
