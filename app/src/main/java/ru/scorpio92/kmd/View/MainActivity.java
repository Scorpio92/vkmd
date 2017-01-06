package ru.scorpio92.kmd.View;


import android.annotation.TargetApi;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import ru.scorpio92.kmd.Adapters.TracksListAdapter;
import ru.scorpio92.kmd.Interfaces.ActivityWatcher;
import ru.scorpio92.kmd.Interfaces.FooterFragmentWatcher;
import ru.scorpio92.kmd.Interfaces.OperationsCallbacks;
import ru.scorpio92.kmd.Operations.ScanForSavedTracks;
import ru.scorpio92.kmd.Operations.UpdateDownloadInfo;
import ru.scorpio92.kmd.R;
import ru.scorpio92.kmd.Services.AudioService;
import ru.scorpio92.kmd.Services.DownloadService;
import ru.scorpio92.kmd.Types.Track;
import ru.scorpio92.kmd.Types.TrackList;
import ru.scorpio92.kmd.Utils.CommonUtils;

public class MainActivity extends Activity implements OperationsCallbacks, TracksListAdapter.TracksListAdapterCallbacks, FooterFragmentWatcher {

    final String LOG_TAG = "MainActivity";

    ImageButton relogin, search, sort, musicVisibility, moreActions;
    TextView selectedInfo;

    ListView tracksList;
    TracksListAdapter adapter;

    FragmentManager fragmentManager;
    ActivityWatcher activityWatcher;

    ServiceConnection sConn;
    AudioService audioService;

    BroadcastReceiver br;


    void initGUI() {
        fragmentManager = getFragmentManager();

        showFooterFragment(false);

        relogin = (ImageButton) findViewById(R.id.relogin);

        relogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showReloginDialog();
            }
        });

        search = (ImageButton) findViewById(R.id.search);

        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSearchDialog();
            }
        });

        sort = (ImageButton) findViewById(R.id.sort);

        sort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSortContextMenu(view);
            }
        });

        musicVisibility = (ImageButton) findViewById(R.id.musicVisibility);

        musicVisibility.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMusicVisibilityContextMenu(view);
            }
        });

        moreActions = (ImageButton) findViewById(R.id.moreActions);

        moreActions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMoreActionsMenu(view);
            }
        });

        tracksList = (ListView) findViewById(R.id.trackList);

        tracksList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                try {
                    Log.w(LOG_TAG, "setOnItemClickListener " + adapter.getTrackList().getAllTracks().get(i).LOCAL_PATH);
                    fragmentManager.beginTransaction()
                            .show(fragmentManager.findFragmentById(R.id.footer))
                            .commit();
                    activityWatcher.onItemSelected(adapter.getTrackListBackup(), adapter.getTrackList().getAllTracks().get(i).ID);
                } catch (Exception e) {e.printStackTrace();}

            }
        });

        tracksList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (!adapter.getSelectedTracksID().contains(i)) {
                    adapter.getSelectedTracksID().add(i); //добавляем в список выбранных файлов
                }
                adapter.notifyDataSetChanged2();
                if(adapter.getSelectedTracksID().size() > 0) {
                    showTrackContextMenu(view);
                    showSelectedTracksCount();
                }

                return true;
            }
        });

        selectedInfo = (TextView) findViewById(R.id.selectedInfo);
    }

    void initAdapter(TrackList trackList) {
        adapter = new TracksListAdapter(MainActivity.this, trackList);
        tracksList.setAdapter(adapter);
    }

    void initAndStartBindingWithAudioService(final boolean isOnResume) {
        sConn = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder binder) {
                Log.w(LOG_TAG, "AudioService onServiceConnected");
                audioService = ((AudioService.MyBinder) binder).getService();
                if(isOnResume) {
                    audioService.setMainActivityIsStopped(false);
                    if (audioService.isStarted()) {
                        Log.w(LOG_TAG, "AudioService is running");
                        if (adapter == null) {
                            Log.w(LOG_TAG, "trackList == null, get tracks from service");
                            initAdapter(audioService.getTrackList());
                        }
                        showFooterFragment(true);
                    } else {
                        Log.w(LOG_TAG, "AudioService is not running");
                        if (adapter == null) {
                            Log.w(LOG_TAG, "trackList == null, get tracks from intent");
                            TrackList trackList = (TrackList) getIntent().getParcelableExtra("TrackList");
                            try {
                                if (trackList.getAllTracks().size() > 0) {
                                    initAdapter(trackList);
                                } else {
                                    relogin();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                relogin();
                            }
                        }
                        showFooterFragment(false);
                    }
                } else {
                    if (audioService.isStarted()) {
                        Log.w(LOG_TAG, "AudioService is running, say AudioService that MainActivity stopped");
                        audioService.setMainActivityIsStopped(true);
                    }
                }

                try {
                    Log.w(LOG_TAG, "unbindService");
                    if(sConn != null) {
                        unbindService(sConn);
                        sConn = null;
                    }
                    audioService = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void onServiceDisconnected(ComponentName name) {
                Log.w(LOG_TAG, "AudioService onServiceDisconnected");
            }
        };

        bindService(new Intent(this, AudioService.class), sConn, BIND_AUTO_CREATE);
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

                switch (action) {
                    case DownloadService.ACTION_UPDATE_DOWNLOAD_PROGRESS:
                        break;
                    case DownloadService.ACTION_DOWNLOAD_TRACK_FINISH:
                        try {
                            if(adapter != null) {
                                adapter.getTrackListBackup().setPathAfterDownload(track); //для сервиса
                                if (adapter.getTrackList().setPathAfterDownload(track)) {
                                    adapter.notifyDataSetChanged2();
                                }
                            }
                        } catch (Exception e) {e.printStackTrace();}
                        break;
                    case DownloadService.ACTION_DOWNLOAD_COMPLETE:
                        if(downloadedCount < 0) {
                            Toast.makeText(getApplicationContext(), R.string.download_error, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Log.w(LOG_TAG, "onDownloadTracksFinished. Downloaded " + Integer.toString(downloadedCount) + " track(s)");
                        Toast.makeText(getApplicationContext(), R.string.tracks_download_finished, Toast.LENGTH_SHORT).show();
                        break;
                    case DownloadService.ACTION_DOWNLOAD_ERROR:
                        break;
                }
            }
        };
        // создаем фильтр для BroadcastReceiver
        IntentFilter intFilt = new IntentFilter(DownloadService.BROADCAST_ACTION);
        // регистрируем (включаем) BroadcastReceiver
        registerReceiver(br, intFilt);
    }


    protected boolean shouldAskPermissions() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    @TargetApi(23)
    protected void askPermissions() {
        String[] permissions = {
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE"
        };
        int requestCode = 200;
        requestPermissions(permissions, requestCode);
    }

    void showTrackContextMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.inflate(R.menu.track_operations_menu);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                ArrayList<Integer> selectedIDs;

                switch (item.getItemId()) {

                    case R.id.select_all:
                        adapter.setSelection(true);
                        showSelectedTracksCount();
                        break;
                    case R.id.unselect_all:
                        adapter.setSelection(false);
                        showSelectedTracksCount();
                        break;
                    case R.id.download_selected:
                        selectedIDs = new ArrayList<>(adapter.getSelectedTracksID());
                        new UpdateDownloadInfo(MainActivity.this, adapter.getTrackList().getTracksArrayByArrayID(selectedIDs), UpdateDownloadInfo.ACTION_INSERT);
                        adapter.getSelectedTracksID().clear();
                        adapter.notifyDataSetChanged2();
                        showSelectedTracksCount();
                        break;
                    case R.id.delete_selected:
                        selectedIDs = new ArrayList<>(adapter.getSelectedTracksID());
                        new UpdateDownloadInfo(MainActivity.this, adapter.getTrackList().getTracksArrayByArrayID(selectedIDs), UpdateDownloadInfo.ACTION_DELETE);
                        adapter.getTrackList().setWasDownloadedToFalse(selectedIDs);
                        adapter.getSelectedTracksID().clear();
                        adapter.notifyDataSetChanged2();
                        showSelectedTracksCount();
                }

                return true;
            }
        });

        popupMenu.show();
    }

    void showReloginDialog() {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        alertDialog.setTitle(getString(R.string.relogin_dialog_title));


        alertDialog.setPositiveButton(getString(android.R.string.ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        relogin();
                    }
                });

        alertDialog.setNegativeButton(getString(android.R.string.no),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        final AlertDialog dialog = alertDialog.create();
        dialog.show();
    }

    void relogin() {
        Log.w(LOG_TAG, "relogin!!!");

        try {
            if(sConn != null) {
                unbindService(sConn);
                sConn = null;
            }
            audioService = null;
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if(br != null)
                unregisterReceiver(br);
        } catch (Exception e) {e.printStackTrace();}

        br = null;

        MainActivity.this.finish();

        Intent intent = new Intent(MainActivity.this, AuthActivity.class);
        intent.putExtra("autoCheckMediaLibrary", false);
        intent.putExtra("isRelogin", true);
        startActivity(intent);
    }

    void showSearchDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        alertDialog.setTitle(getString(R.string.search_dialog_tittle));

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialoglayout = inflater.inflate(R.layout.search_dialog, null);
        alertDialog.setView(dialoglayout);

        final EditText searchInput = (EditText) dialoglayout.findViewById(R.id.searchInput);
        final RadioGroup searchByGroup = (RadioGroup) dialoglayout.findViewById(R.id.searchByGroup);
        final RadioButton searchByTrackNameRadio = (RadioButton) dialoglayout.findViewById(R.id.searchByTrackNameRadio);
        final RadioButton searchByArtistRadio = (RadioButton) dialoglayout.findViewById(R.id.searchByArtistRadio);

        alertDialog.setPositiveButton(getString(R.string.search_button),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Do nothing here because we override this button later to change the close behaviour.
                        //However, we still need this because on older versions of Android unless we
                        //pass a handler the button doesn't get instantiated
                    }
                });

        final AlertDialog dialog = alertDialog.create();
        //dialog.setCancelable(false);
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(searchInput.getText().toString().trim().length() > 0) {
                    if(searchByGroup.getCheckedRadioButtonId() == searchByArtistRadio.getId())
                        adapter.changeTrackListMode(TracksListAdapter.SHOW_TRACKS_BY_ARTIST, searchInput.getText().toString().trim());
                    if(searchByGroup.getCheckedRadioButtonId() == searchByTrackNameRadio.getId())
                        adapter.changeTrackListMode(TracksListAdapter.SHOW_TRACKS_BY_TITLE, searchInput.getText().toString().trim());

                    dialog.cancel();
                } else {
                    Toast.makeText(MainActivity.this, R.string.empty_field_warning, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    void showSortContextMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.inflate(R.menu.sort_menu);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {

                    case R.id.sort_by_artist:
                        adapter.getTrackList().sortByArtists();
                        adapter.notifyDataSetChanged2();
                        break;
                    case R.id.sort_by_track_name:
                        adapter.getTrackList().sortByTrackName();
                        adapter.notifyDataSetChanged2();
                        break;
                }

                return true;
            }
        });

        popupMenu.show();
    }

    void showMusicVisibilityContextMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.inflate(R.menu.music_visibility_menu);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.visibility_all:
                        adapter.changeTrackListMode(TracksListAdapter.SHOW_ALL_TRACKS);
                        break;
                    case R.id.visibility_not_downloaded:
                        adapter.changeTrackListMode(TracksListAdapter.SHOW_NOT_DOWNLOADED_TRACKS);
                        break;
                    case R.id.visibility_downloaded:
                        if(CommonUtils.getBooleanSetting(MainActivity.this, Settings.SETTING_AUTO_SCAN_SAVED_KEY, false))
                            new ScanForSavedTracks(MainActivity.this, adapter.getTrackListBackup());
                        else
                            adapter.changeTrackListMode(TracksListAdapter.SHOW_DOWNLOADED_TRACKS);
                        break;
                }

                return true;
            }
        });

        popupMenu.show();
    }

    void showMoreActionsMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.inflate(R.menu.more_actions_menu);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {

                    case R.id.downloadManager:
                        startActivity(new Intent(MainActivity.this, DownloadManagerActivity.class));
                        break;
                    case R.id.settings:
                        startActivity(new Intent(MainActivity.this, Settings.class));
                        break;
                    case R.id.about:
                        startActivity(new Intent(MainActivity.this, About.class));
                        break;
                }

                return true;
            }
        });

        popupMenu.show();
    }

    void showFooterFragment(boolean showFooter) {
        try {
            if (showFooter) {
                fragmentManager.beginTransaction()
                        .show(fragmentManager.findFragmentById(R.id.footer))
                        .commit();
                activityWatcher.onFooterRestore();
            } else {
                fragmentManager.beginTransaction()
                        .hide(fragmentManager.findFragmentById(R.id.footer))
                        .commit();
            }
        } catch (Exception e) {e.printStackTrace();}

    }

    void showSelectedTracksCount() {
        if(adapter != null) {
            selectedInfo.setText(getString(R.string.selected) + adapter.getSelectedTracksID().size() + getString(R.string.from) + adapter.getTrackList().getAllTracks().size());
            if (adapter.getSelectedTracksID().size() > 0)
                selectedInfo.setVisibility(View.VISIBLE);
            else
                selectedInfo.setVisibility(View.GONE);
        } else {
            selectedInfo.setVisibility(View.GONE);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.w(LOG_TAG, "onCreate");

        setContentView(R.layout.main_activity);

        initGUI();

        registerDownloadBroadcastReceiver();

        if (shouldAskPermissions()) {
            askPermissions();
        }
    }

    @Override
    public void onGetTokenComplete(int status, String token, String userID) {

    }

    @Override
    public void onGetTrackListComplete(int status, String response) {

    }

    @Override
    public void onResponseParseComplete(TrackList tracks) {

    }

    @Override
    public void onGetSavedTracksComplete(TrackList tracks) {

    }

    @Override
    public void onWriteTrackListToDBComplete(int count) {
    }

    @Override
    public void onUpdateDownloadInfoComplete(int count, int action) {
        if(action == UpdateDownloadInfo.ACTION_INSERT) {
            if (count < 0) {
                Toast.makeText(getApplicationContext(), R.string.download_error, Toast.LENGTH_SHORT).show();
                return;
            }

            Log.w(LOG_TAG, "onUpdateDownloadInfoComplete. Set for download " + Integer.toString(count) + " track(s)");
            String folder = CommonUtils.getStringSetting(MainActivity.this, Settings.SETTING_KMD_FOLDER_KEY, DownloadService.DEFAULT_DOWNLOAD_FOLDER);
            Log.w(LOG_TAG, "SETTING_KMD_FOLDER_KEY: " + folder);
            startService(new Intent(MainActivity.this, DownloadService.class).putExtra(DownloadService.downloadFolderIntentKey, folder));
        }
    }

    @Override
    public void onScanTaskComplete(ArrayList<Track> tracks) {
        Log.w(LOG_TAG, "onScanTaskComplete");
        if(!tracks.isEmpty()) {
            Log.w(LOG_TAG, Integer.toString(tracks.size()));
            for(Track track : tracks) {
                adapter.getTrackListBackup().setPathAfterDownload(track);
            }
        }
        adapter.changeTrackListMode(TracksListAdapter.SHOW_DOWNLOADED_TRACKS);
    }

    @Override
    public void onCheckTrack() {
        showSelectedTracksCount();
    }

    @Override
    public void onAttached(Object object) {
        this.activityWatcher = (ActivityWatcher) object;
    }

    @Override
    public void onStartPlay(int position) {
        adapter.notifyDataSetChanged(position, TracksListAdapter.ON_PLAY_TRACK);
    }

    @Override
    public void onPrepareStart(int position) {
        adapter.notifyDataSetChanged(position, TracksListAdapter.ON_PREPARE_TRACK);
    }

    @Override
    public void onStopTrack() {
        adapter.notifyDataSetChanged(0, TracksListAdapter.ON_STOP_TRACK);
        showFooterFragment(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.w(LOG_TAG, "onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.w(LOG_TAG, "onResume");

        showFooterFragment(false);

        initAndStartBindingWithAudioService(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.w(LOG_TAG, "onStop");

        initAndStartBindingWithAudioService(false);

        /*
        try {
            if(sConn != null) {
                unbindService(sConn);
            }
        } catch (Exception e) {e.printStackTrace();}

        sConn = null;
        audioService = null;
        */
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        try {
            if(br != null)
                unregisterReceiver(br);
        } catch (Exception e) {e.printStackTrace();}

    }
}
