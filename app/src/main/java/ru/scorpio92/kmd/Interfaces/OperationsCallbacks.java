package ru.scorpio92.kmd.Interfaces;

import java.util.ArrayList;

import ru.scorpio92.kmd.Types.Track;
import ru.scorpio92.kmd.Types.TrackList;

/**
 * Created by scorpio92 on 03.11.16.
 */

public interface OperationsCallbacks {
    void onGetTokenComplete(int status, String token, String userID);
    void onGetTrackListComplete(int status, String response);
    void onResponseParseComplete(TrackList tracks);
    void onGetSavedTracksComplete(TrackList tracks);
    /*
    void onDownloadTrackPercentUpdate(int num, int total, int percentCurrent);
    void onDownloadTrackFinished(int id, int num, int total, int percentCurrent);
    void onDownloadTracksFinished(int count);
    */
    void onWriteTrackListToDBComplete(int count);
    void onUpdateDownloadInfoComplete(int count, int action);
    void onScanTaskComplete(ArrayList<Track> tracks);
}
