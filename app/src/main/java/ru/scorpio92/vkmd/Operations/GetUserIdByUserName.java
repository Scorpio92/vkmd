package ru.scorpio92.vkmd.Operations;

import android.os.AsyncTask;
import android.util.Log;

import com.loopj.android.http.HttpGet;

import org.json.JSONArray;
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
import ru.scorpio92.vkmd.Utils.InternetUtils;

/**
 * Created by scorpio92 on 1/5/17.
 */

public class GetUserIdByUserName implements InternetUtils.InternetConnectionCallback {

    public static final int GET_USER_ID_STATUS_OK = 0;
    public static final int GET_USER_ID_STATUS_FAIL = 1;
    public static final int GET_USER_ID_NO_INTERNET = 2;

    private final String versionAPI = "3.0";
    private final int TIMEOUT = 15000;

    private GetUserIdByUserNameCallback callback;
    private String username;
    private String ACCESS_TOKEN;
    private String userID;

    public GetUserIdByUserName (GetUserIdByUserNameCallback callback, String username, String ACCESS_TOKEN) {
        userID = null;
        this.callback = callback;
        this.username = username;
        this.ACCESS_TOKEN = ACCESS_TOKEN;
        new InternetUtils().checkInternetConnectionAsync(GetUserIdByUserName.this);
    }

    @Override
    public void onCheckComplete(boolean result) {
        if(result)
            new Task().execute();
        else
            callback.onGetUserIDByUserName(userID, GET_USER_ID_NO_INTERNET);
    }

    private class Task extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... params) {

            StringBuilder sb = new StringBuilder();

            try {
                HttpGet httpget = new HttpGet("https://api.vk.com/method/users.search?q=" + username + "&count=1&access_token=" + ACCESS_TOKEN + "&v=" + versionAPI);
                HttpParams httpParams = new BasicHttpParams();
                HttpConnectionParams.setConnectionTimeout(httpParams, TIMEOUT);
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
                return GET_USER_ID_STATUS_FAIL;
            }
            userID = parseResponse(sb.toString());
            if(userID != null)
                return GET_USER_ID_STATUS_OK;
            else
                return GET_USER_ID_STATUS_FAIL;
        }

        @Override
        protected void onPostExecute(Integer responseCode) {
            super.onPostExecute(responseCode);
            callback.onGetUserIDByUserName(userID, responseCode);
        }
    }

    private String parseResponse(String response) {
        try {
            JSONObject dataJsonObj = new JSONObject(response);
            JSONArray responseObj = dataJsonObj.getJSONArray("response");
            JSONObject obj = responseObj.getJSONObject(1);
            return obj.getString("uid").trim();
        } catch (Exception e) {e.printStackTrace();}
        return null;
    }

    public interface GetUserIdByUserNameCallback {
        void onGetUserIDByUserName(String id, int responseCode);
    }
}
