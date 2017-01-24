package ru.scorpio92.kmd.Operations;

import android.content.Context;
import android.util.Log;

import ru.scorpio92.kmd.Types.TrackList;

/**
 * Created by scorpio92 on 1/24/17.
 */

public class GetTracks implements GetTrackCount.GetTrackCountCallback, GetTrackListByOwnerID.GetTrackListByOwnerIDCallback, GetTrackListFromResponseOrDB.GetTrackListFromResponseOrDBCallback {

    private final String LOG_TAG = "GetTracks";
    public static final int GET_TRACKS_STATUS_OK = 0;
    public static final int GET_TRACKS_STATUS_FAIL = 1;
    public static final int GET_TRACKS_NO_INTERNET = 2;

    private Context context;
    private GetTrackListCallback callback;
    private String userID;
    private String token;
    private boolean isLPAuth;

    private int tracksCount, currentOffset;
    private TrackList generalTrackList;

    public GetTracks(Context context, String userID, String token) {
        this.context = context;
        callback = (GetTrackListCallback) context;
        this.userID = userID;
        this.token = token;
        isLPAuth = false;

        //1. get tracks count
        new GetTrackCount(this, userID, token);
    }

    public GetTracks(Context context, String userID, String token, boolean isLPAuth) {
        this.context = context;
        callback = (GetTrackListCallback) context;
        this.userID = userID;
        this.token = token;
        this.isLPAuth = isLPAuth;

        //1. get tracks count
        new GetTrackCount(this, userID, token);

    }

    @Override
    public void onGetTrackCount(int count, int responseCode) {
        switch (responseCode) {
            case GetTrackCount.GET_TRACKS_COUNT_STATUS_OK:
                Log.w(LOG_TAG, "count tracks: " + count);
                tracksCount = count;
                currentOffset = 0;
                generalTrackList = new TrackList();
                Log.w(LOG_TAG, "run GetTrackListByOwnerID, currentOffset: " + currentOffset);
                new GetTrackListByOwnerID(this, userID, token, currentOffset);
                break;
            case GetTrackCount.GET_TRACKS_COUNT_STATUS_FAIL:
                Log.w(LOG_TAG, "get tracks count: fail");
                callback.onGetTrackListComplete(GET_TRACKS_STATUS_FAIL, null);
                break;
            case GetTrackCount.GET_TRACKS_COUNT_NO_INTERNET:
                Log.w(LOG_TAG, "get tracks count: problems with internet");
                callback.onGetTrackListComplete(GET_TRACKS_NO_INTERNET, null);
                break;
        }
    }

    @Override
    public void onGetTrackListComplete(int status, String response) {
        switch (status) {
            case GetTrackListByOwnerID.GET_MUSIC_LIST_STATUS_OK:
                Log.w(LOG_TAG, "get tracks bo owner id: OK");
                new GetTrackListFromResponseOrDB(GetTrackListFromResponseOrDB.IS_GET_TRACKLIST_FROM_RESPONSE, context, this, response);
                break;
            case GetTrackListByOwnerID.GET_MUSIC_LIST_STATUS_FAIL:
                Log.w(LOG_TAG, "get tracks bo owner id: FAIL");
                callback.onGetTrackListComplete(GET_TRACKS_STATUS_FAIL, null);
                break;
            case GetTrackListByOwnerID.GET_MUSIC_LIST_NO_INTERNET:
                Log.w(LOG_TAG, "get tracks bo owner id: problems with internet");
                callback.onGetTrackListComplete(GET_TRACKS_NO_INTERNET, null);
                break;
        }
    }

    @Override
    public void onResponseParseComplete(TrackList tracks) {
        generalTrackList.getAllTracks().addAll(tracks.getAllTracks());
        if(tracks.getAllTracks().isEmpty() && currentOffset == 0) {
            callback.onGetTrackListComplete(GET_TRACKS_STATUS_FAIL, null);
        } else {
            currentOffset = currentOffset + GetTrackListByOwnerID.DEFAULT_TRACKS_COUNT;
            if(currentOffset < tracksCount) {
                Log.w(LOG_TAG, "GetTrackListByOwnerID, currentOffset: " + currentOffset);
                new GetTrackListByOwnerID(this, userID, token, currentOffset);
            }  else {
                if(isLPAuth) {
                    Log.w(LOG_TAG, "LP get method. save ownerId: " + userID + " and token: " + token + " to TrackList");
                    generalTrackList.setOwnerID(userID);
                    generalTrackList.setToken(token);
                }
                callback.onGetTrackListComplete(GET_TRACKS_STATUS_OK, generalTrackList);
            }
        }
    }

    @Override
    public void onGetSavedTracksComplete(TrackList tracks) {

    }

    public interface GetTrackListCallback {
        void onGetTrackListComplete(int status, TrackList trackList);
    }
}
