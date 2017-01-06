package ru.scorpio92.kmd.Operations;

import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

import ru.scorpio92.kmd.Interfaces.OperationsCallbacks;
import ru.scorpio92.kmd.Types.MainDB;
import ru.scorpio92.kmd.Types.Track;
import ru.scorpio92.kmd.Utils.DBUtils;

/**
 * Created by scorpio92 on 21.11.16.
 */

public class UpdateDownloadInfo {

    private final String LOG_TAG = "UpdateDownloadInfo";

    public static final int ACTION_INSERT = 0;
    public static final int ACTION_DELETE = 1;
    private int action;

    private Context context;
    private OperationsCallbacks callback;
    private ArrayList<Track> tracks;

    public UpdateDownloadInfo(Context context, ArrayList<Track> tracks, int action) {
        this.context = context;
        callback = (OperationsCallbacks) context;
        this.tracks = tracks;
        this.action = action;
        try {
            new Task().execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class Task extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... params) {
            int writedTracksCount = 0;
            ContentValues newValues = new ContentValues();
            for (Track track : tracks) {
                try {
                    String query = "SELECT * FROM " + MainDB.MEDIA_TABLE + " WHERE " + MainDB.MEDIA_TABLE_TRACK_ID_COLUMN + "=" + "'" + track.ID + "'";
                    ArrayList<String> als = new ArrayList<>();
                    als.add(MainDB.MEDIA_TABLE_DOWNLOAD_PATH_COLUMN);
                    ArrayList<String> result = DBUtils.select_from_db(context, query, als, false);

                    switch (action) {

                        case ACTION_INSERT:
                            if(result.isEmpty()) {
                                try {
                                    Log.w(LOG_TAG, "insert");
                                    newValues.put(MainDB.MEDIA_TABLE_OWNER_ID_COLUMN, track.OWNER_ID);
                                    newValues.put(MainDB.MEDIA_TABLE_TRACK_ID_COLUMN, track.ID);
                                    newValues.put(MainDB.MEDIA_TABLE_ARTISTS_COLUMN, track.ARTIST);
                                    newValues.put(MainDB.MEDIA_TABLE_TRACK_NAME_COLUMN, track.TITLE);
                                    newValues.put(MainDB.MEDIA_TABLE_DURATION_COLUMN, track.DURATION);
                                    newValues.put(MainDB.MEDIA_TABLE_URL_COLUMN, track.URL);
                                    newValues.put(MainDB.MEDIA_TABLE_NEED_DOWNLOAD_COLUMN, "1");
                                    newValues.put(MainDB.MEDIA_TABLE_WAS_DOWNLOADED_COLUMN, "0");
                                    newValues.put(MainDB.MEDIA_TABLE_DOWNLOAD_PATH_COLUMN, "");
                                    DBUtils.insert_update_delete(context, MainDB.MEDIA_TABLE, newValues, null, DBUtils.ACTION_INSERT);
                                    newValues.clear();
                                    writedTracksCount++;
                                } catch (Exception e2) {
                                    e2.printStackTrace();
                                    writedTracksCount--;
                                }
                            }
                            break;

                        case ACTION_DELETE:
                            Log.w(LOG_TAG, "delete");

                            //delete from table
                            try {
                                String where = MainDB.MEDIA_TABLE_TRACK_ID_COLUMN + "=" + "'" + track.ID + "'";
                                DBUtils.insert_update_delete(context, MainDB.MEDIA_TABLE, null, where, DBUtils.ACTION_DELETE);
                                newValues.clear();
                                writedTracksCount++;

                                //delete file from FS
                                try {
                                    if (!result.get(0).equals(""))
                                        if (new File(result.get(0)).delete())
                                            Log.w(LOG_TAG, "track with ID: " + track.ID + " was deleted from " + result.get(0));

                                } catch (Exception e2) {
                                    e2.printStackTrace();
                                }

                            } catch (Exception e2) {
                                e2.printStackTrace();
                                writedTracksCount--;
                            }
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return writedTracksCount;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            callback.onUpdateDownloadInfoComplete(result, action);
        }
    }
}
