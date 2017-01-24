package ru.scorpio92.kmd.View;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import ru.scorpio92.kmd.Adapters.DownloadManagerListAdapter;
import ru.scorpio92.kmd.Operations.GetTrackListFromResponseOrDB;
import ru.scorpio92.kmd.Operations.UpdateDownloadInfo;
import ru.scorpio92.kmd.R;
import ru.scorpio92.kmd.Services.DownloadService;
import ru.scorpio92.kmd.Types.Track;
import ru.scorpio92.kmd.Types.TrackList;

/**
 * Created by scorpio92 on 25.12.16.
 */

public class DownloadManagerActivity extends Activity implements
        DownloadManagerListAdapter.DownloadManagerListAdapterCallbacks,
        GetTrackListFromResponseOrDB.GetTrackListFromResponseOrDBCallback,
        UpdateDownloadInfo.UpdateDownloadInfoCallback {

    final String LOG_TAG = "DownloadManagerActivity";

    ImageButton tracksSelector, stop_download, start_pause_download, delete_from_download;
    LinearLayout currentDownloadContainer;
    TextView generalDownloadProgressText, currentDownloadTrack, currentDownloadTrackPercent, track_for_download_count;
    ProgressBar downloadProgress;
    ListView downloadList;

    DownloadManagerListAdapter downloadManagerListAdapter;

    BroadcastReceiver br;

    private boolean downloadServiceIsStarted = false;
    private boolean updateAfterDownloadComplete = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.w(LOG_TAG, "onCreate");

        setContentView(R.layout.download_manager);

        initGUI();

        registerDownloadBroadcastReceiver();

        new GetTrackListFromResponseOrDB(GetTrackListFromResponseOrDB.IS_GET_TRACKLIST_FOR_DOWNLOAD_FROM_DB, DownloadManagerActivity.this, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(br != null)
            unregisterReceiver(br);
    }

    void initGUI() {
        tracksSelector = (ImageButton) findViewById(R.id.tracksSelector);

        tracksSelector.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean b = downloadManagerListAdapter.setSelection();
                Log.w(LOG_TAG, "setSelection: " + b);
                show_hide_track_count();
            }
        });

        stop_download = (ImageButton) findViewById(R.id.stop_download);
        stop_download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(downloadServiceIsStarted)
                    sendBroadcast(new Intent(DownloadService.NOTIFICATION_ACTION_STOP));
                else
                    Toast.makeText(DownloadManagerActivity.this, R.string.download_not_running, Toast.LENGTH_SHORT).show();
            }
        });

        start_pause_download = (ImageButton) findViewById(R.id.start_pause_download);
        start_pause_download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(downloadServiceIsStarted) {
                    sendBroadcast(new Intent(DownloadService.NOTIFICATION_ACTION_PLAY_PAUSE));
                } else {
                    startService(new Intent(DownloadManagerActivity.this, DownloadService.class));
                }
            }
        });

        delete_from_download = (ImageButton) findViewById(R.id.delete_from_download);
        delete_from_download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ArrayList<Integer> selectedIDs = new ArrayList<Integer>(downloadManagerListAdapter.getSelectedTracksID());
                new UpdateDownloadInfo(DownloadManagerActivity.this, downloadManagerListAdapter.getTrackList().getTracksArrayByArrayID(selectedIDs), UpdateDownloadInfo.ACTION_DELETE);
                downloadManagerListAdapter.getSelectedTracksID().clear();
                downloadManagerListAdapter.notifyDataSetChanged();
            }
        });


        //--container--
        currentDownloadContainer = (LinearLayout) findViewById(R.id.currentDownloadContainer);
        show_hide_download_container(false);

        generalDownloadProgressText = (TextView) findViewById(R.id.generalDownloadProgressText);

        currentDownloadTrack = (TextView) findViewById(R.id.currentDownloadTrack);

        currentDownloadTrackPercent = (TextView) findViewById(R.id.currentDownloadTrackPercent);

        downloadProgress = (ProgressBar) findViewById(R.id.downloadProgress);
        //--container--


        downloadList = (ListView) findViewById(R.id.downloadList);

        track_for_download_count = (TextView) findViewById(R.id.track_for_download_count);
        show_hide_track_count();
    }

    void initList(TrackList tracks) {
        downloadManagerListAdapter = new DownloadManagerListAdapter(DownloadManagerActivity.this, tracks);
        downloadList.setAdapter(downloadManagerListAdapter);
        downloadManagerListAdapter.notifyDataSetChanged();
    }

    void show_hide_download_container(boolean show) {
        downloadServiceIsStarted = show;
        if(show) {
            currentDownloadContainer.setVisibility(View.VISIBLE);
            showPausePlay(false);
        } else {
            currentDownloadContainer.setVisibility(View.GONE);
            showPausePlay(true);
        }
    }

    void showPausePlay(boolean show_pause) {
        if(!show_pause) {
            start_pause_download.setImageDrawable(getResources().getDrawable(R.drawable.pause));
        } else {
            start_pause_download.setImageDrawable(getResources().getDrawable(R.drawable.play));
        }
    }

    void show_hide_track_count() {
        if(downloadManagerListAdapter != null) {
            track_for_download_count.setText(getString(R.string.selected) + downloadManagerListAdapter.getSelectedTracksID().size() + getString(R.string.from) + downloadManagerListAdapter.getTrackList().getAllTracks().size());
            if (downloadManagerListAdapter.getSelectedTracksID().size() > 0)
                track_for_download_count.setVisibility(View.VISIBLE);
            else
                track_for_download_count.setVisibility(View.GONE);
        } else {
            track_for_download_count.setVisibility(View.GONE);
        }
    }

    void updateCurrentTrackContainer(Track track, int num, int total, int percentCurrent) {
        generalDownloadProgressText.setText(getString(R.string.generalDownloadProgressText) + " " + (num-1) + "/" + total);
        currentDownloadTrack.setText(track.ARTIST + " - " + track.TITLE);
        downloadProgress.setMax(100);
        downloadProgress.setProgress(percentCurrent);
        currentDownloadTrackPercent.setText(percentCurrent + "%");
    }

    void registerDownloadBroadcastReceiver() {
        br = new BroadcastReceiver() {
            // действия при получении сообщений
            public void onReceive(Context context, Intent intent) {
                int action = intent.getIntExtra(DownloadService.PARAM_ACTION, -1);
                int num = intent.getIntExtra(DownloadService.PARAM_CURRENT_NUM, 0);
                int total = intent.getIntExtra(DownloadService.PARAM_TOTAL_COUNT, 0);
                int percentCurrent = intent.getIntExtra(DownloadService.PARAM_PERCENT_PROGRESS, 0);
                int downloadedCount = intent.getIntExtra(DownloadService.PARAM_DOWNLOADED_COUNT, 0);
                Track track = intent.getParcelableExtra(DownloadService.PARAM_TRACK);
                boolean pause = intent.getBooleanExtra(DownloadService.PARAM_PAUSE, true);

                switch (action) {
                    case DownloadService.ACTION_UPDATE_DOWNLOAD_PROGRESS:
                        show_hide_download_container(true);
                        updateCurrentTrackContainer(track, num, total, percentCurrent);
                        break;
                    case DownloadService.ACTION_DOWNLOAD_TRACK_FINISH:
                        updateAfterDownloadComplete = true;
                        new GetTrackListFromResponseOrDB(GetTrackListFromResponseOrDB.IS_GET_TRACKLIST_FOR_DOWNLOAD_FROM_DB, DownloadManagerActivity.this, null);
                        break;
                    case DownloadService.ACTION_DOWNLOAD_COMPLETE:
                        updateAfterDownloadComplete = true;
                        new GetTrackListFromResponseOrDB(GetTrackListFromResponseOrDB.IS_GET_TRACKLIST_FOR_DOWNLOAD_FROM_DB, DownloadManagerActivity.this, null);
                        show_hide_download_container(false);
                        Log.w(LOG_TAG, "onDownloadTracksFinished. Downloaded " + Integer.toString(downloadedCount) + " track(s)");
                        break;
                    case DownloadService.ACTION_DOWNLOAD_ERROR:
                        show_hide_download_container(false);
                        break;
                    case DownloadService.ACTION_NOTIFY_DM:
                        downloadServiceIsStarted = true;
                        showPausePlay(pause);
                        break;
                }
            }
        };
        // создаем фильтр для BroadcastReceiver
        IntentFilter intFilt = new IntentFilter(DownloadService.BROADCAST_ACTION);
        // регистрируем (включаем) BroadcastReceiver
        registerReceiver(br, intFilt);
    }


    @Override
    public void onResponseParseComplete(TrackList tracks) {

    }

    @Override
    public void onGetSavedTracksComplete(TrackList tracks) {
        //Log.w(LOG_TAG, "onGetSavedTracksComplete: " + Integer.toString(tracks.getAllTracks().size()));
        initList(tracks);
        if(tracks.getAllTracks().size() == 0 && !updateAfterDownloadComplete) {
            Toast.makeText(getApplicationContext(), R.string.no_tracks_for_download, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onUpdateDownloadInfoComplete(int count, int action) {
        Log.w(LOG_TAG, "deleted " + count + " tracks from download table, refresh adapter");
        sendBroadcast(new Intent(DownloadService.NOTIFICATION_ACTION_RESCAN));
        new GetTrackListFromResponseOrDB(GetTrackListFromResponseOrDB.IS_GET_TRACKLIST_FOR_DOWNLOAD_FROM_DB, DownloadManagerActivity.this, null);
    }

    @Override
    public void onCheckTrack() {
        show_hide_track_count();
    }
}
