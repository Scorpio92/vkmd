package ru.scorpio92.kmd.Operations;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import ru.scorpio92.kmd.Types.MainDB;
import ru.scorpio92.kmd.Types.Track;
import ru.scorpio92.kmd.Types.TrackList;
import ru.scorpio92.kmd.Utils.DBUtils;

/**
 * Created by scorpio92 on 03.11.16.
 */

public class GetTrackListFromResponseOrDB {

    public static final int IS_GET_TRACKLIST_FROM_RESPONSE = 0;
    public static final int IS_GET_TRACKLIST_FROM_ONLINE_SEARCH_RESPONSE = 1;
    public static final int IS_GET_SAVED_TRACKLIST_FROM_DB = 2;
    public static final int IS_GET_TRACKLIST_FOR_DOWNLOAD_FROM_DB = 3;
    private int operation;

    private Context context;
    private GetTrackListFromResponseOrDBCallback callback;
    private String response;
    private TrackList tracks;

    public GetTrackListFromResponseOrDB(int operation, Context context, String response) {
        this.operation = operation;
        this.context = context;
        callback = (GetTrackListFromResponseOrDBCallback) context;
        this.response = response;
        tracks = new TrackList();
        try {
            new Task().execute();
        } catch (Exception e) {
            e.printStackTrace();
            callback.onResponseParseComplete(tracks);
        }
    }

    public GetTrackListFromResponseOrDB(int operation, Context context, GetTrackListFromResponseOrDBCallback callback, String response) {
        this.operation = operation;
        this.context = context;
        this.callback = callback;
        this.response = response;
        tracks = new TrackList();
        try {
            new Task().execute();
        } catch (Exception e) {
            e.printStackTrace();
            callback.onResponseParseComplete(tracks);
        }
    }

    //Парсим ответ
    private class Task extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            TrackList savedTracks = new TrackList();
            JSONObject dataJsonObj;
            JSONArray responseJA = null;
            int startIdx = 0;

            try {
                switch (operation) {
                    case IS_GET_TRACKLIST_FROM_ONLINE_SEARCH_RESPONSE:
                        startIdx = 1;
                    case IS_GET_TRACKLIST_FROM_RESPONSE:

                        Log.w("GetTrackListFromResponseOrDB", "startIdx: " + startIdx);
                        savedTracks = getTracksFromDB();

                        dataJsonObj = new JSONObject(response);

                        //v3.0
                        //{"response":[{"aid":456239053,"owner_id":385867856,"artist":"Scorpions","title":"Moment Of Glory","duration":307,"url":"https:\/\/cs5-2v4.vk-cdn.net\/p11\/ecbe6f86e31292.mp3?extra=PYXvNJyUHiPWsAXwRQXngtZaE1bGEpnYGTD-YaiZ4a0CwVV0RLpPsFHIh3FU31gS5-A0SteYTlxe4vJ5eMd2deALqffCN0O_0vN4QuBwhADA_upgabffl4vzTKSAQHsKD3uPhcDn-PYXEXqk","lyrics_id":"3998451","genre":1},

                        //v5.59
                        //{"response":{"count":40,"items":[{"id":456239053,"owner_id":385867856,"artist":"Scorpions","title":"Moment Of Glory","duration":307,"date":1477827108,"url":"https:\/\/cs5-2v4.vk-cdn.net\/p11\/ecbe6f86e31292.mp3?extra=PYXvNJyUHiPWsAXwRQXngtZaE1bGEpnYGTD-YaiZ4a0CwVV0RLpPsFHIh3FU31gS5-A0SteYTlxe4vJ5eMd2deALqffCN0O_0vN4QuBwhADA_upgabffl4vzTKSAQHsKD3uPhcDn-PYXEXqk","lyrics_id":3998451,"genre_id":1},

                        responseJA = dataJsonObj.getJSONArray("response");

                        for (int i = startIdx; i < responseJA.length(); i++) {
                            try {
                                JSONObject trackJSON = responseJA.getJSONObject(i);
                                String oid = trackJSON.getString("owner_id").trim();
                                String aid = trackJSON.getString("aid").trim();
                                String artist = trackJSON.getString("artist").trim();
                                String title = trackJSON.getString("title").trim();
                                String duration = trackJSON.getString("duration").trim();
                                String url = trackJSON.getString("url").trim();

                                Track savedTrack = savedTracks.containsTrack(Integer.valueOf(aid));
                                if (savedTrack != null)
                                    tracks.addTrack(savedTrack);
                                else
                                    tracks.addTrack(new Track(Integer.parseInt(oid), Integer.parseInt(aid), artist, title, Integer.parseInt(duration), url, null, false));
                            } catch (Exception e) {
                                Log.w("GetTrackListFromResponseOrDB", responseJA.getJSONObject(i).toString());
                                e.printStackTrace();
                            }
                        }
                        break;

                    case IS_GET_SAVED_TRACKLIST_FROM_DB:
                    case IS_GET_TRACKLIST_FOR_DOWNLOAD_FROM_DB:
                        tracks = getTracksFromDB();
                        break;

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            switch (operation) {
                case IS_GET_TRACKLIST_FROM_RESPONSE:
                case IS_GET_TRACKLIST_FROM_ONLINE_SEARCH_RESPONSE:
                    callback.onResponseParseComplete(tracks);
                    break;
                case IS_GET_SAVED_TRACKLIST_FROM_DB:
                case IS_GET_TRACKLIST_FOR_DOWNLOAD_FROM_DB:
                    callback.onGetSavedTracksComplete(tracks);
                    break;
            }
        }
    }

    private TrackList getTracksFromDB() {

        TrackList tracks = new TrackList();
        ArrayList<String> result, als;

        try {
            String selectAllTracks = null;

            switch (operation) {
                case IS_GET_SAVED_TRACKLIST_FROM_DB:
                    selectAllTracks = "SELECT * FROM " + MainDB.MEDIA_TABLE + " WHERE " + MainDB.MEDIA_TABLE_WAS_DOWNLOADED_COLUMN + " = " + "'1'";
                    break;
                case IS_GET_TRACKLIST_FOR_DOWNLOAD_FROM_DB:
                    selectAllTracks = "SELECT * FROM " + MainDB.MEDIA_TABLE + " WHERE " + MainDB.MEDIA_TABLE_NEED_DOWNLOAD_COLUMN + " = " + "'1'";
                    break;
                default:
                    selectAllTracks = "SELECT * FROM " + MainDB.MEDIA_TABLE + " WHERE " + MainDB.MEDIA_TABLE_WAS_DOWNLOADED_COLUMN + " = " + "'1'";
                    break;
            }

            als = new ArrayList<>();
            als.add(MainDB.MEDIA_TABLE_OWNER_ID_COLUMN);
            als.add(MainDB.MEDIA_TABLE_TRACK_ID_COLUMN);
            als.add(MainDB.MEDIA_TABLE_ARTISTS_COLUMN);
            als.add(MainDB.MEDIA_TABLE_TRACK_NAME_COLUMN);
            als.add(MainDB.MEDIA_TABLE_DURATION_COLUMN);
            als.add(MainDB.MEDIA_TABLE_URL_COLUMN);
            als.add(MainDB.MEDIA_TABLE_DOWNLOAD_PATH_COLUMN);
            result = DBUtils.select_from_db(context, selectAllTracks, als, true);
        } catch (Exception e) {
            e.printStackTrace();
            return tracks;
        }

        if(!result.isEmpty()) {
            for (int i = 0; i < result.size(); i = i + als.size()) {
                tracks.addTrack(new Track(Integer.valueOf(result.get(i)), Integer.valueOf(result.get(i + 1)), result.get(i + 2), result.get(i + 3), Integer.valueOf(result.get(i + 4)), result.get(i + 5), result.get(i + 6), true));
            }
        } else {
            Log.w("checkMediaLibrary", "no saved tracks in library");
        }

        return tracks;
    }

    public interface GetTrackListFromResponseOrDBCallback {
        void onResponseParseComplete(TrackList tracks);
        void onGetSavedTracksComplete(TrackList tracks);
    }
}
