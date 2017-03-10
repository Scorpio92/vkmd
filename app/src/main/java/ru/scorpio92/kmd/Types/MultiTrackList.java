package ru.scorpio92.kmd.Types;

import android.util.Log;

import java.util.HashMap;

/**
 * Created by scorpio92 on 1/12/17.
 */

public class MultiTrackList {

    public static final int CURRENT_TRACKLIST = 0;
    public static final int MAIN_TRACKLIST = 1;

    private HashMap<Integer, TrackList> map;

    public MultiTrackList(TrackList trackList) {
        map = new HashMap<Integer, TrackList>();
        map.put(CURRENT_TRACKLIST, trackList);
        map.put(MAIN_TRACKLIST, trackList);
    }

    public MultiTrackList(MultiTrackList multiTrackList) {
       map = new HashMap<Integer, TrackList>();
       map.putAll(multiTrackList.getHashMap());
    }

    public void addTrackList(int type, TrackList trackList) {
        map.put(type, trackList);
    }

    public void removeTrackList(int type) {
        map.remove(type);
    }

    public void replaceTrackList(int type, TrackList trackList) {
        removeTrackList(type);
        addTrackList(type,trackList);
    }

    public TrackList getTrackList(int type) {
        if(map.containsKey(type))
            return map.get(type);
        else
            return null;
    }

    private HashMap<Integer, TrackList> getHashMap () {
        return map;
    }
}
