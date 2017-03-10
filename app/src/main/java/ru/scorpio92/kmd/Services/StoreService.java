package ru.scorpio92.kmd.Services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import ru.scorpio92.kmd.Types.MultiTrackList;
import ru.scorpio92.kmd.Types.TrackList;

/**
 * Created by scorpio92 on 1/7/17.
 */

public class StoreService extends Service {

    private final String LOG_TAG = "StoreService";

    private MyBinder binder = new MyBinder();
    private TrackList trackList;
    private MultiTrackList multiTrackList;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class MyBinder extends Binder {
        public StoreService getService() {
            return StoreService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.w(LOG_TAG, "service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.w(LOG_TAG, "onStartCommand");
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.w(LOG_TAG, "onDestroy");
    }


    public TrackList getTrackList() {
        return trackList;
    }

    public void setTrackList(TrackList trackList) {
        this.trackList = trackList;
    }

    public void setMultiTrackList(MultiTrackList multiTrackList) {
        this.multiTrackList = multiTrackList;
        //Log.w(LOG_TAG, "setMultiTrackList: " + multiTrackList.getTrackList(MultiTrackList.CURRENT_TRACKLIST).getAllTracks().get(0).getFullTrackName());
    }

    public MultiTrackList getMultiTrackList() {
        return multiTrackList;
    }
}
