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
import ru.scorpio92.kmd.Constants;
import ru.scorpio92.kmd.Interfaces.OperationsCallbacks;
import ru.scorpio92.kmd.Utils.InternetUtils;

/**
 * Created by scorpio92 on 03.11.16.
 */

public class GetToken implements InternetUtils.InternetConnectionCallback {

    public static final int GET_TOKEN_STATUS_OK = 0;
    public static final int GET_TOKEN_STATUS_FAIL = 1;
    public static final int GET_TOKEN_NO_INTERNET = 2;

    private final int TIMEOUT = 10000;

    private OperationsCallbacks callback;

    private String GENERATE_TOKEN_URL = "";

    private String RESPONSE;


    public GetToken(OperationsCallbacks callback, String user, String password) {
        this.callback = callback;
        GENERATE_TOKEN_URL = Constants.GENERATE_TOKEN_URL.concat("&username=" + user).concat("&password=" + password);
        RESPONSE = "";
        new InternetUtils().checkInternetConnectionAsync(GetToken.this);
    }

    @Override
    public void onCheckComplete(boolean result) {
        if(result)
            new Task().execute();
        else
            callback.onGetTokenComplete(GET_TOKEN_NO_INTERNET, null, null);
    }

    private class Task extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... params) {

            StringBuilder sb = new StringBuilder();

            try {
                HttpGet httpget = new HttpGet(GENERATE_TOKEN_URL);
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
                return GET_TOKEN_STATUS_FAIL;
            }
            RESPONSE = sb.toString();
            return 0;
        }

        @Override
        protected void onPostExecute(Integer responseCode) {
            super.onPostExecute(responseCode);

            if(responseCode == GET_TOKEN_STATUS_FAIL) {
                callback.onGetTokenComplete(GET_TOKEN_STATUS_FAIL, null, null);
                return;
            }

            Log.w("RESPONSE", RESPONSE);

            try {
                if(RESPONSE.length() > 0) {
                    JSONObject jobj = new JSONObject(RESPONSE);
                    String token = getTokenFromRespone(jobj);
                    String userId = getUserIdFromRespone(jobj);
                    if(token.length() > 0 && userId.length() > 0)
                        callback.onGetTokenComplete(GET_TOKEN_STATUS_OK, token, userId);
                    else
                        callback.onGetTokenComplete(GET_TOKEN_STATUS_FAIL, null, null);
                } else {
                    callback.onGetTokenComplete(GET_TOKEN_STATUS_FAIL, null, null);
                }
            } catch (Exception e) {
                e.printStackTrace();
                callback.onGetTokenComplete(GET_TOKEN_STATUS_FAIL, null, null);
            }
        }
    }

    private String getTokenFromRespone(JSONObject jobj) {
        String token = "";
        try {
            token = jobj.getString("access_token").trim();
        } catch (Exception e) {e.printStackTrace();}
        return token;
    }

    private String getUserIdFromRespone(JSONObject jobj) {
        String user_id = "";
        try {
            user_id = jobj.getString("user_id").trim();
        } catch (Exception e) {e.printStackTrace();}
        return user_id;
    }
}
