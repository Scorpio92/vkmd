package ru.scorpio92.vkmd.Services;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import ru.scorpio92.vkmd.R;
import ru.scorpio92.vkmd.Types.MainDB;
import ru.scorpio92.vkmd.Types.Track;
import ru.scorpio92.vkmd.Utils.DBUtils;
import ru.scorpio92.vkmd.View.DownloadManagerActivity;
import ru.scorpio92.vkmd.View.MainActivity;

/**
 * Created by scorpio92 on 23.11.16.
 */

public class DownloadService extends IntentService {

    private final String LOG_TAG = "DownloadService";

    private final int DEFAULT_CONNECTION_TIMEOUT = 30 * 1000;
    private final int DEFAULT_FILE_BUFFER = 8192;

    public static final String DEFAULT_DOWNLOAD_FOLDER = "vkmd";
    public static final String DEFAULT_DOWNLOAD_PATH = System.getenv("EXTERNAL_STORAGE") + "/" + DEFAULT_DOWNLOAD_FOLDER;
    public static final String downloadFolderIntentKey = "downloadFolder";

    public final static String BROADCAST_ACTION = "ru.scorpio92.vkmd.View.MainActivity";
    public final static String PARAM_ACTION = "action";
    public final static String PARAM_CURRENT_NUM = "current_num";
    public final static String PARAM_TOTAL_COUNT = "total_count";
    public final static String PARAM_PERCENT_PROGRESS = "percent_progress";
    public final static String PARAM_DOWNLOADED_COUNT = "downloaded_count";
    public final static String PARAM_TRACK = "track";
    public final static String PARAM_PAUSE = "pause";
    public final static int ACTION_UPDATE_DOWNLOAD_PROGRESS = 0;
    public final static int ACTION_DOWNLOAD_TRACK_FINISH = 1;
    public final static int ACTION_DOWNLOAD_COMPLETE = 2;
    public final static int ACTION_DOWNLOAD_ERROR = 3;
    public final static int ACTION_NOTIFY_DM = 4;

    private int NOTIFICATION_ID = 777;
    public final static String NOTIFICATION_ACTION_PLAY_PAUSE = "ru.scorpio92.vkmd.Services.DownloadService.PLAY_PAUSE";
    public final static String NOTIFICATION_ACTION_STOP = "ru.scorpio92.vkmd.Services.DownloadService.STOP";
    public final static String NOTIFICATION_ACTION_RESCAN = "ru.scorpio92.vkmd.Services.DownloadService.RESCAN";
    private final int NOTIFICATION_UPDATE_INTERVAL = 3;
    private final int NOTIFICATION_UPDATE_INTERVAL_FOR_DM = 500;

    private boolean downloadPause = false;
    private boolean stopDownload = false;

    private Timer mTimer;
    private TimerTask mTimerTask;

    private ArrayList<Track> tracksForDownload;
    private boolean rescan;


    private DownloadService.MyBinder binder = new DownloadService.MyBinder();

    public DownloadService() {
        super("DownloadService");
    }

    public DownloadService(String name) {
        super(name);
    }

    public class MyBinder extends Binder {
        public DownloadService getService() {
            return DownloadService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.w(LOG_TAG, "onCreate");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.w(LOG_TAG, "onHandleIntent");
        String downloadFolder = intent.getStringExtra(downloadFolderIntentKey);
        String downloadPath = null;

        try {
            if(downloadFolder != null) {
                if (downloadFolder.length() == 0)
                    downloadPath = DEFAULT_DOWNLOAD_PATH;
                else
                    downloadPath = System.getenv("EXTERNAL_STORAGE") + "/" + downloadFolder;
            } else {
                downloadPath = DEFAULT_DOWNLOAD_PATH;
            }
        } catch (Exception e) {
            //e.printStackTrace();
            downloadPath = DEFAULT_DOWNLOAD_PATH;
        }

        try {
            File vkmdFolder = new File(downloadPath);
            if (!vkmdFolder.exists()) {
                if(vkmdFolder.mkdir())
                    downloadTracks(downloadPath);
                else
                    sendBroadcast(ACTION_DOWNLOAD_ERROR, 0, 0, 0, null);
            } else
                downloadTracks(downloadPath);
        } catch (Exception e) {
            e.printStackTrace();
            sendBroadcast(ACTION_DOWNLOAD_ERROR, 0, 0, 0, null);
        }

        Log.w(LOG_TAG, "stopForeground");
        stopForeground(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.w(LOG_TAG, "onDestroy");
    }


    public void setDownloadPause() {
        downloadPause = !downloadPause;
    }

    public void setStopDownload() {
        stopDownload = true;
    }

    public void setRescan(boolean rescan) {
        this.rescan = rescan;
    }

    private ArrayList<Track> getArrayTracksForDownload() {
        ArrayList<Track> tracks = new ArrayList<>();
        ArrayList<String> result, als;
        result = new ArrayList<>();
        als = new ArrayList<>();
        try {
            Log.w(LOG_TAG, "search tracksForDownload with parameter: need_download = 1");
            String selectAllTracks = "SELECT * FROM " + MainDB.MEDIA_TABLE + " WHERE " + MainDB.MEDIA_TABLE_NEED_DOWNLOAD_COLUMN + " = " + "'1'";
            als.add(MainDB.MEDIA_TABLE_OWNER_ID_COLUMN);
            als.add(MainDB.MEDIA_TABLE_TRACK_ID_COLUMN);
            als.add(MainDB.MEDIA_TABLE_ARTISTS_COLUMN);
            als.add(MainDB.MEDIA_TABLE_TRACK_NAME_COLUMN);
            als.add(MainDB.MEDIA_TABLE_DURATION_COLUMN);
            als.add(MainDB.MEDIA_TABLE_URL_COLUMN);
            result = DBUtils.select_from_db(getBaseContext(), selectAllTracks, als, true);
            Log.w(LOG_TAG, "search tracksForDownload cancel");
        } catch (Exception e) {
            e.printStackTrace();
            sendBroadcast(ACTION_DOWNLOAD_ERROR, 0, 0, 0, null);
        }

        if(!result.isEmpty()) {
            Log.w(LOG_TAG, "add tracksForDownload to TRACK Array. start");
            for (int i=0; i<result.size();i=i+als.size()) {
                if(stopDownload) {
                    Log.w(LOG_TAG, "downloadTracks stopped");
                    sendBroadcast(ACTION_DOWNLOAD_COMPLETE, 0, 0, 0, null);
                }
                Track track = new Track(Integer.valueOf(result.get(i)), Integer.valueOf(result.get(i+1)), result.get(i+2), result.get(i+3), Integer.valueOf(result.get(i+4)), result.get(i+5), null, false);
                tracks.add(track);
            }
            Log.w(LOG_TAG, "add tracksForDownload to TRACK Array. end");
        } else {
            sendBroadcast(ACTION_DOWNLOAD_COMPLETE, 0, 0, 0, null);
        }
        return tracks;
    }

    private void downloadTracks(String downloadPath) {

        Log.w(LOG_TAG, "downloadPath: " + downloadPath);

        mTimer = new Timer();
        mTimerTask = new TimerTask();
        mTimer.schedule(mTimerTask, 0, NOTIFICATION_UPDATE_INTERVAL_FOR_DM);

        tracksForDownload = new ArrayList<>(getArrayTracksForDownload());

        if(tracksForDownload.isEmpty()) {
            Log.w(LOG_TAG, "tracksForDownload.isEmpty");
            return;
        }

        int currentNum, tracksCount, percentCurrent, downloadedTracksCount;

        InputStream input = null;
        OutputStream output = null;

        currentNum = 1;
        downloadedTracksCount = 0;

        ContentValues newValues = new ContentValues();

        while (!tracksForDownload.isEmpty()) {
            try {
                tracksCount = tracksForDownload.size() + downloadedTracksCount;

                Track track = tracksForDownload.get(0);

                if(stopDownload) {
                    Log.w(LOG_TAG, "downloadTracks stopped");
                    sendBroadcast(ACTION_DOWNLOAD_COMPLETE, downloadedTracksCount, 0, 0, null);
                    return;
                }

                while (downloadPause) {
                    try {
                        Log.w(LOG_TAG, "downloadTracks pause");
                        Thread.sleep(1000);
                    } catch (Exception e) {e.printStackTrace();}
                }

                Log.w(LOG_TAG, "start for downloadTracks track with ID: " + track.ID);
                URL url = new URL(track.URL);
                URLConnection connection = url.openConnection();
                connection.setConnectTimeout(DEFAULT_CONNECTION_TIMEOUT);
                connection.setDoOutput(false);
                connection.connect();

                int lenghtOfFile = connection.getContentLength();

                input = new BufferedInputStream(url.openStream(), DEFAULT_FILE_BUFFER);

                String fileName = track.getFullTrackName();
                try {
                    output = new FileOutputStream(downloadPath + "/" + fileName);
                } catch (Exception e) {
                    fileName = track.getShortTrackName();
                    output = new FileOutputStream(downloadPath + "/" + fileName);
                }
                track.LOCAL_PATH = downloadPath + "/" + fileName;

                byte data[] = new byte[1024];

                long total = 0;
                int count;
                percentCurrent = 0;

                sentNotificationInForeground(currentNum, tracksCount, percentCurrent);
                long lastTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());

                while ((count = input.read(data)) != -1) {

                    if(stopDownload) {
                        Log.w(LOG_TAG, "downloadTracks stopped");
                        sendBroadcast(ACTION_DOWNLOAD_COMPLETE, downloadedTracksCount, 0, 0, null);
                        return;
                    }

                    while (downloadPause) {
                        try {
                            if(stopDownload) {
                                Log.w(LOG_TAG, "downloadTracks stopped");
                                sendBroadcast(ACTION_DOWNLOAD_COMPLETE, downloadedTracksCount, 0, 0, null);
                                return;
                            }
                            Log.w(LOG_TAG, "downloadTracks pause");
                            sentNotificationInForeground(currentNum, tracksCount, percentCurrent);
                            Thread.sleep(1000);
                        } catch (Exception e) {e.printStackTrace();}
                    }
                    //Log.w(LOG_TAG, "after pause");
                    total += count;
                    int percent = (int) ((total * 100) / lenghtOfFile);
                    if(percentCurrent != percent) {
                        percentCurrent = percent;
                        sendBroadcast(ACTION_UPDATE_DOWNLOAD_PROGRESS, currentNum, tracksCount, percentCurrent, track);
                        if(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - lastTime > NOTIFICATION_UPDATE_INTERVAL) {
                            lastTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
                            Log.w(LOG_TAG, "lastTime: " + Long.toString(lastTime));
                            sentNotificationInForeground(currentNum, tracksCount, percentCurrent);
                        }
                    }
                    output.write(data, 0, count);
                }

                //finish downloadTracks track
                Log.w(LOG_TAG, "finish downloadTracks track with ID: " + track.ID);
                sendBroadcast(ACTION_DOWNLOAD_TRACK_FINISH, currentNum, tracksCount, percentCurrent, track);
                sentNotificationInForeground(currentNum, tracksCount, percentCurrent);
                downloadedTracksCount++;

                try {
                    Log.w(LOG_TAG, "start set parameter: was_download = 1 for track with ID: " + track.ID);
                    newValues.clear();
                    newValues.put(MainDB.MEDIA_TABLE_NEED_DOWNLOAD_COLUMN, "0");
                    newValues.put(MainDB.MEDIA_TABLE_WAS_DOWNLOADED_COLUMN, "1");
                    newValues.put(MainDB.MEDIA_TABLE_DOWNLOAD_PATH_COLUMN, downloadPath + "/" + fileName);
                    String where = MainDB.MEDIA_TABLE_TRACK_ID_COLUMN + "=" + "'" + track.ID + "'";
                    DBUtils.insert_update_delete(getBaseContext(), MainDB.MEDIA_TABLE, newValues, where, DBUtils.ACTION_UPDATE);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } catch(Exception e){
                e.printStackTrace();
                downloadedTracksCount--;
            } finally {

                tracksForDownload.remove(0);

                if(rescan) {
                    Log.w(LOG_TAG, "rescan media table");
                    tracksForDownload = new ArrayList<>(getArrayTracksForDownload());
                    rescan = false;
                }

                currentNum++;

                // flushing output
                try {
                    output.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // closing streams
                try {
                    output.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    input.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        Log.w(LOG_TAG, "tracksForDownload downloadTracks complete");

        sendBroadcast(ACTION_DOWNLOAD_COMPLETE, downloadedTracksCount, 0, 0, null);
    }

    private void sendBroadcast(int action, int num, int total, int percentCurrent, Track track) {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra(PARAM_ACTION, action);
        switch (action) {
            case ACTION_UPDATE_DOWNLOAD_PROGRESS:
                intent.putExtra(PARAM_CURRENT_NUM, num);
                intent.putExtra(PARAM_TOTAL_COUNT, total);
                intent.putExtra(PARAM_PERCENT_PROGRESS, percentCurrent);
                intent.putExtra(PARAM_TRACK, track);
                break;
            case ACTION_DOWNLOAD_TRACK_FINISH:
                intent.putExtra(PARAM_TRACK, track);
                break;
            case ACTION_DOWNLOAD_COMPLETE:
                if (mTimer != null) {
                    mTimer.cancel();
                }
                intent.putExtra(PARAM_DOWNLOADED_COUNT, num);
                break;
            case ACTION_DOWNLOAD_ERROR:
                if (mTimer != null) {
                    mTimer.cancel();
                }
                break;

        }
        sendBroadcast(intent);
    }

    private void sendNotifyForDM() {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra(PARAM_ACTION, ACTION_NOTIFY_DM);
        intent.putExtra(PARAM_PAUSE, downloadPause);
        sendBroadcast(intent);
        Log.w(LOG_TAG, "sendNotifyForDM");
    }

    private void sentNotificationInForeground(int num, int total, int percentCurrent) {
        try {
            Intent notificationIntent = new Intent(this, DownloadManagerActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
            PendingIntent pendingIntentPlay_Pause = PendingIntent.getBroadcast(this, 0, new Intent().setAction(NOTIFICATION_ACTION_PLAY_PAUSE), PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent pendingIntentStop = PendingIntent.getBroadcast(this, 0, new Intent().setAction(NOTIFICATION_ACTION_STOP), PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
            builder
                    //.setContentTitle(getPackageName())
                    //.setContentText(getString(R.string.notification_download_text))
                    .setContentTitle(getString(R.string.notification_download_text))
                    .setSubText(num + getString(R.string.from) + total + " (" + percentCurrent + "%)")
                    .setProgress(total, percentCurrent, true)
                    .setSmallIcon(R.drawable.download)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setContentIntent(pendingIntent);

            if (downloadPause) {
                builder.addAction(R.drawable.play, "Resume", pendingIntentPlay_Pause);
            } else {
                builder.addAction(R.drawable.pause, "Pause", pendingIntentPlay_Pause);
            }

            builder.addAction(R.drawable.close, "Stop", pendingIntentStop);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.setVisibility(Notification.VISIBILITY_PUBLIC);
            }

            Notification notification = builder.build();
            startForeground(NOTIFICATION_ID, notification);

        } catch (Exception e) {e.printStackTrace();}
    }

    class TimerTask extends java.util.TimerTask {
        @Override
        public void run() {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        sendNotifyForDM();
                    } catch (Exception e) {e.printStackTrace();}
                }
            });
        }

    }
}
