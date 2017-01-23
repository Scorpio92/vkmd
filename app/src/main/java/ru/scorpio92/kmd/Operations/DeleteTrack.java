package ru.scorpio92.kmd.Operations;

import android.os.AsyncTask;
import android.util.Log;

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
import ru.scorpio92.kmd.Types.Track;
import ru.scorpio92.kmd.Utils.InternetUtils;

/**
 * Created by scorpio92 on 1/18/17.
 */

public class DeleteTrack implements InternetUtils.InternetConnectionCallback {

    public static final int DELETE_TRACK_STATUS_OK = 0;
    public static final int DELETE_TRACK_STATUS_FAIL = 1;
    public static final int DELETE_TRACK_NO_INTERNET = 2;

    private final int CONNECTION_TIMEOUT = 3000;
    private final String versionAPI = "3.0";
    private final int OK_RESPONSE = 1;

    private DeleteTrackCallback callback;
    private String token;
    private Track track;


    public DeleteTrack(DeleteTrackCallback callback, Track track, String token) {
        this.callback = callback;
        this.track = track;
        this.token = token;
        new InternetUtils().checkInternetConnectionAsync(DeleteTrack.this);
    }

    @Override
    public void onCheckComplete(boolean result) {
        if(result)
            new Task().execute();
        else
            callback.OnDeleteTrack(DELETE_TRACK_NO_INTERNET, track);
    }

    private class Task extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... params) {

            StringBuilder sb = new StringBuilder();

            try {
                HttpGet httpget = new HttpGet("https://api.vk.com/method/audio.delete?audio_id=" + track.ID + "&owner_id=" + track.OWNER_ID + "&access_token=" + token + "&v=" + versionAPI);
                HttpParams httpParams = new BasicHttpParams();
                HttpConnectionParams.setConnectionTimeout(httpParams, CONNECTION_TIMEOUT);
                HttpConnectionParams.setSoTimeout(httpParams, CONNECTION_TIMEOUT);
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
                return DELETE_TRACK_STATUS_FAIL;
            }
            Log.w("DeleteTrack", "RESPONSE: " + sb.toString());
            int response = parseResponse(sb.toString());

            if (response == OK_RESPONSE)
                return DELETE_TRACK_STATUS_OK;

            return DELETE_TRACK_STATUS_FAIL;
        }

        @Override
        protected void onPostExecute(Integer responseCode) {
            super.onPostExecute(responseCode);
            callback.OnDeleteTrack(responseCode, track);
        }
    }

    private Integer parseResponse(String response) {
        try {
            JSONObject dataJsonObj = new JSONObject(response);
            return Integer.valueOf(dataJsonObj.getString("response").trim());
        } catch (Exception e) {e.printStackTrace();}
        return -1;
    }

    public interface DeleteTrackCallback {
        void OnDeleteTrack(int code, Track track);
    }
}
