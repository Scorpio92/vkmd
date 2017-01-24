package ru.scorpio92.kmd.Operations;

import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

import ru.scorpio92.kmd.Services.DownloadService;
import ru.scorpio92.kmd.Types.MainDB;
import ru.scorpio92.kmd.Types.Track;
import ru.scorpio92.kmd.Types.TrackList;
import ru.scorpio92.kmd.Utils.DBUtils;
import ru.scorpio92.kmd.Utils.KMDUtils;

/**
 * Created by scorpio92 on 30.12.16.
 */

public class ScanForSavedTracks {

    private final String LOG_TAG = "ScanForSavedTracks";

    private Context context;
    private ScanForSavedTracksCallback callback;
    private TrackList trackList;
    private ArrayList<Track> tracks;

    public ScanForSavedTracks(Context context, TrackList trackList) {
        tracks = new ArrayList<>();
        if(context != null && trackList != null) {
            this.context = context;
            callback = (ScanForSavedTracksCallback) context;
            this.trackList = trackList;
            new ScanTask().execute();
        }
    }

    private class ScanTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            ContentValues newValues = new ContentValues();
            ArrayList<File> files = KMDUtils.getSavedMP3FilesList(DownloadService.DEFAULT_DOWNLOAD_PATH);
            for (File f : files) {
                Track track = trackList.containsTrack(f.getName());
                if(track != null) {
                    String query = "SELECT * FROM " + MainDB.MEDIA_TABLE + " WHERE " + MainDB.MEDIA_TABLE_TRACK_ID_COLUMN + "=" + "'" + track.ID + "'";
                    ArrayList<String> als = new ArrayList<>();
                    als.add(MainDB.MEDIA_TABLE_DOWNLOAD_PATH_COLUMN);
                    ArrayList<String> result = DBUtils.select_from_db(context, query, als, false);
                    if(result.isEmpty()) {
                        Log.w(LOG_TAG, "add track with name: " + f.getName());
                        track.IS_DOWNLOADED = true;
                        track.LOCAL_PATH = f.getAbsolutePath();
                        tracks.add(track);

                        try {
                            newValues.put(MainDB.MEDIA_TABLE_OWNER_ID_COLUMN, track.OWNER_ID);
                            newValues.put(MainDB.MEDIA_TABLE_TRACK_ID_COLUMN, track.ID);
                            newValues.put(MainDB.MEDIA_TABLE_ARTISTS_COLUMN, track.ARTIST);
                            newValues.put(MainDB.MEDIA_TABLE_TRACK_NAME_COLUMN, track.TITLE);
                            newValues.put(MainDB.MEDIA_TABLE_DURATION_COLUMN, track.DURATION);
                            newValues.put(MainDB.MEDIA_TABLE_URL_COLUMN, track.URL);
                            newValues.put(MainDB.MEDIA_TABLE_NEED_DOWNLOAD_COLUMN, "0");
                            newValues.put(MainDB.MEDIA_TABLE_WAS_DOWNLOADED_COLUMN, "1");
                            newValues.put(MainDB.MEDIA_TABLE_DOWNLOAD_PATH_COLUMN, f.getAbsolutePath());
                            DBUtils.insert_update_delete(context, MainDB.MEDIA_TABLE, newValues, null, DBUtils.ACTION_INSERT);
                            newValues.clear();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            callback.onScanTaskComplete(tracks);
        }
    }

    public interface ScanForSavedTracksCallback {
        void onScanTaskComplete(ArrayList<Track> tracks);
    }
}
