package ru.scorpio92.kmd.View;


import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.kobakei.ratethisapp.RateThisApp;

import java.util.ArrayList;

import ru.scorpio92.kmd.Adapters.TracksListAdapter;
import ru.scorpio92.kmd.Constants;
import ru.scorpio92.kmd.Interfaces.ActivityWatcher;
import ru.scorpio92.kmd.Interfaces.FooterFragmentWatcher;
import ru.scorpio92.kmd.Operations.AddTrack;
import ru.scorpio92.kmd.Operations.DeleteTrack;
import ru.scorpio92.kmd.Operations.GetTrackListFromResponseOrDB;
import ru.scorpio92.kmd.Operations.ScanForSavedTracks;
import ru.scorpio92.kmd.Operations.SearchTracks;
import ru.scorpio92.kmd.Operations.UpdateDownloadInfo;
import ru.scorpio92.kmd.R;
import ru.scorpio92.kmd.Services.AudioService;
import ru.scorpio92.kmd.Services.DownloadService;
import ru.scorpio92.kmd.Services.StoreService;
import ru.scorpio92.kmd.Types.MultiTrackList;
import ru.scorpio92.kmd.Types.Track;
import ru.scorpio92.kmd.Types.TrackList;
import ru.scorpio92.kmd.Utils.CommonUtils;

public class MainActivity extends Activity implements
        TracksListAdapter.TracksListAdapterCallbacks,
        FooterFragmentWatcher,
        SearchTracks.SearchTracksCallback,
        AddTrack.AddTrackCallback,
        DeleteTrack.DeleteTrackCallback,
        GetTrackListFromResponseOrDB.GetTrackListFromResponseOrDBCallback,
        UpdateDownloadInfo.UpdateDownloadInfoCallback,
        ScanForSavedTracks.ScanForSavedTracksCallback {

    final String LOG_TAG = "MainActivity";

    ImageButton relogin, search, sort, musicVisibility, moreActions;
    TextView selectedInfo;

    ListView tracksList;
    TracksListAdapter adapter;

    FragmentManager fragmentManager;
    ActivityWatcher activityWatcher;

    ServiceConnection sConn;
    ServiceConnection sConnStoreService;
    AudioService audioService;

    BroadcastReceiver br;

    private final int BIND_WITH_AUDIO_SERVICE_ON_RESUME = 0;
    private final int BIND_WITH_AUDIO_SERVICE_ON_STOP = 1;
    private final int BIND_WITH_AUDIO_SERVICE_ON_MANUAL_RELOGIN = 2;

    AlertDialog searchDialog;
    boolean stopSearch;

    boolean isRelogin = false;
    boolean isExit = false;
    boolean isManualTrackSelect = false;


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
                    isManualTrackSelect = true;
                    fragmentManager.beginTransaction()
                            .show(fragmentManager.findFragmentById(R.id.footer))
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                            .commit();
                    activityWatcher.onItemSelected(adapter.getMultiTrackList(), adapter.getCurrentTrackList().getAllTracks().get(i).ID);
                } catch (Exception e) {
                    //isManualTrackSelect = false;
                    e.printStackTrace();
                }

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
                    showTrackContextMenu(view, i);
                    showSelectedTracksCount();
                }

                return true;
            }
        });

        selectedInfo = (TextView) findViewById(R.id.selectedInfo);
    }

    void initAdapter(MultiTrackList multiTrackList) {
        adapter = new TracksListAdapter(MainActivity.this, multiTrackList);
        tracksList.setAdapter(adapter);
    }

    void initAndStartBindingWithAudioService(final int action) {
        sConn = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder binder) {
                Log.w(LOG_TAG, "AudioService onServiceConnected");
                audioService = ((AudioService.MyBinder) binder).getService();

                switch (action) {
                    case BIND_WITH_AUDIO_SERVICE_ON_RESUME:
                        audioService.setMainActivityIsStopped(false);
                        if (audioService.isStarted()) {
                            Log.w(LOG_TAG, "AudioService is running");
                            if (adapter == null) {
                                Log.w(LOG_TAG, "trackList == null, get tracks from service");
                                initAdapter(audioService.getMultiTrackList());
                            }
                            showFooterFragment(true);
                        } else {
                            showFooterFragment(false);
                            if (adapter == null) {
                                Log.w(LOG_TAG, "trackList == null, get tracks from store service");
                                getTracksFromStoreService();
                            }
                        }
                        break;
                    case BIND_WITH_AUDIO_SERVICE_ON_STOP:
                        if (audioService.isStarted()) {
                            Log.w(LOG_TAG, "AudioService is running, say AudioService that MainActivity stopped");
                            audioService.setMainActivityIsStopped(true);
                        }
                        break;
                    case BIND_WITH_AUDIO_SERVICE_ON_MANUAL_RELOGIN:
                        //if(audioService.isStarted()) {
                            Log.w(LOG_TAG, "manual relogin. stop service");
                            audioService.stopService(MainActivity.this);
                        //}
                        relogin(false);
                        break;
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

    void getTracksFromStoreService() {
        sConnStoreService = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder binder) {
                Log.w(LOG_TAG, "StoreService onServiceConnected");
                StoreService storeService = ((StoreService.MyBinder) binder).getService();
                try {
                    TrackList trackList = new TrackList(storeService.getTrackList().getAllTracks());
                    trackList.setOwnerID(Integer.toString(storeService.getTrackList().getOwnerID()));
                    trackList.setToken(storeService.getTrackList().getToken());

                    try {
                        Log.w(LOG_TAG, "unbindService");
                        unbindService(sConnStoreService);
                        sConnStoreService = null;
                        storeService = null;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    stopService(new Intent(MainActivity.this, StoreService.class));

                    try {
                        if (trackList.getAllTracks().size() > 0) {
                            initAdapter(new MultiTrackList(trackList));
                        } else {
                            relogin(false);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        relogin(false);
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                    relogin(false);
                }
            }

            public void onServiceDisconnected(ComponentName name) {
                Log.w(LOG_TAG, "StoreService onServiceDisconnected");
            }
        };

        bindService(new Intent(this, StoreService.class), sConnStoreService, BIND_AUTO_CREATE);
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
                                adapter.getMainTrackList().setPathAfterDownload(track); //для сервиса
                                if (adapter.getCurrentTrackList().setPathAfterDownload(track)) {
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


    void showTrackContextMenu(View v, final int i) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.inflate(R.menu.track_operations_menu);

        final ArrayList<Integer> selectedIDs = new ArrayList<>(adapter.getSelectedTracksID());

        MenuItem download_selected = popupMenu.getMenu().findItem(R.id.download_selected);
        MenuItem delete_selected_from_cache = popupMenu.getMenu().findItem(R.id.delete_selected_from_cache);
        boolean showDownload = false;
        boolean showDeleteFromCache = false;
        for (int id: selectedIDs) {
            if(!adapter.getCurrentTrackList().getAllTracks().get(id).IS_DOWNLOADED) {
                showDownload = true;
                //break;
            } else
                showDeleteFromCache = true;
        }
        download_selected.setVisible(showDownload);
        delete_selected_from_cache.setVisible(showDeleteFromCache);

        MenuItem add_this_track = popupMenu.getMenu().findItem(R.id.add_this_track);
        MenuItem delete_from_my_audios = popupMenu.getMenu().findItem(R.id.delete_from_my_audios);

        final Track currentTrack = adapter.getCurrentTrackList().getAllTracks().get(i);
        final String token = adapter.getMainTrackList().getToken();
        if(adapter.getSelectedTracksID().size() == 1) {
            if(!token.equals(TrackList.EMPTY_TOKEN)) {
                Track track = adapter.getMainTrackList().containsTrack(currentTrack.getFullTrackName());
                if(track == null) {
                    add_this_track.setVisible(true);
                    delete_from_my_audios.setVisible(false);
                } else {
                    add_this_track.setVisible(false);
                    delete_from_my_audios.setVisible(true);
                }
            } else {
                add_this_track.setVisible(false);
                delete_from_my_audios.setVisible(false);
            }
        } else {
            add_this_track.setVisible(false);
            delete_from_my_audios.setVisible(false);
        }

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

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
                        new UpdateDownloadInfo(MainActivity.this, adapter.getCurrentTrackList().getTracksArrayByArrayID(selectedIDs), UpdateDownloadInfo.ACTION_INSERT);
                        adapter.getSelectedTracksID().clear();
                        adapter.notifyDataSetChanged2();
                        showSelectedTracksCount();
                        break;
                    case R.id.add_this_track:
                        new AddTrack(MainActivity.this, currentTrack, token);
                        break;
                    case R.id.delete_selected_from_cache:
                        new UpdateDownloadInfo(MainActivity.this, adapter.getCurrentTrackList().getTracksArrayByArrayID(selectedIDs), UpdateDownloadInfo.ACTION_DELETE);
                        adapter.getCurrentTrackList().setWasDownloadedToFalse(selectedIDs);
                        adapter.getSelectedTracksID().clear();
                        adapter.notifyDataSetChanged2();
                        showSelectedTracksCount();
                        break;
                    case R.id.delete_from_my_audios:
                        new DeleteTrack(MainActivity.this, currentTrack, token);
                        break;
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
                        relogin(true);
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

    void showExitDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        alertDialog.setTitle(getString(R.string.exit_dialog_title));


        alertDialog.setPositiveButton(getString(android.R.string.ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        isExit = true;
                        relogin(true);
                    }
                });

        alertDialog.setNegativeButton(getString(android.R.string.no),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        isExit = false;
                        dialog.cancel();
                    }
                });

        final AlertDialog dialog = alertDialog.create();
        dialog.show();
    }

    void relogin(boolean isManualRelogin) {
        Log.w(LOG_TAG, "relogin!!! isManualRelogin: " + isManualRelogin);

        isRelogin = true;

        if(isManualRelogin) {
            initAndStartBindingWithAudioService(BIND_WITH_AUDIO_SERVICE_ON_MANUAL_RELOGIN);
        } else {

            try {
                if (sConn != null) {
                    unbindService(sConn);
                    sConn = null;
                }
                audioService = null;
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                if (br != null) {
                    unregisterReceiver(br);
                    br = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            MainActivity.this.finish();
        }
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
        final CheckBox onlineSearch = (CheckBox) dialoglayout.findViewById(R.id.onlineSearch);
        final ProgressBar searchProgress = (ProgressBar) dialoglayout.findViewById(R.id.searchProgress);

        alertDialog.setPositiveButton(getString(R.string.search_button),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Do nothing here because we override this button later to change the close behaviour.
                        //However, we still need this because on older versions of Android unless we
                        //pass a handler the button doesn't get instantiated
                    }
                });

        alertDialog.setNegativeButton(getString(android.R.string.no),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Do nothing here because we override this button later to change the close behaviour.
                        //However, we still need this because on older versions of Android unless we
                        //pass a handler the button doesn't get instantiated
                    }
                });

        //final AlertDialog dialog = alertDialog.create();
        searchDialog = alertDialog.create();
        //dialog.setCancelable(false);
        searchDialog.show();

        searchDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(searchInput.getText().toString().trim().length() > 0) {
                    if(onlineSearch.isChecked()) {
                        stopSearch = false;
                        searchInput.setEnabled(false);
                        //searchByGroup.setEnabled(false);
                        searchByTrackNameRadio.setEnabled(false);
                        searchByArtistRadio.setEnabled(false);
                        onlineSearch.setEnabled(false);
                        searchProgress.setVisibility(View.VISIBLE);
                        v.setEnabled(false); //блокируем конпку Поиск
                        if (searchByGroup.getCheckedRadioButtonId() == searchByArtistRadio.getId())
                            new SearchTracks(MainActivity.this, searchInput.getText().toString().trim(), true, Constants.ACCESS_TOKEN_PUBLIC);
                        if (searchByGroup.getCheckedRadioButtonId() == searchByTrackNameRadio.getId())
                            new SearchTracks(MainActivity.this, searchInput.getText().toString().trim(), false, Constants.ACCESS_TOKEN_PUBLIC);
                    } else {
                        if (searchByGroup.getCheckedRadioButtonId() == searchByArtistRadio.getId())
                            adapter.changeTrackListMode(TracksListAdapter.SHOW_TRACKS_BY_ARTIST, searchInput.getText().toString().trim());
                        if (searchByGroup.getCheckedRadioButtonId() == searchByTrackNameRadio.getId())
                            adapter.changeTrackListMode(TracksListAdapter.SHOW_TRACKS_BY_TITLE, searchInput.getText().toString().trim());

                        searchDialog.cancel();

                    }
                    //dialog.cancel();
                } else {
                    searchProgress.setVisibility(View.INVISIBLE);
                    Toast.makeText(MainActivity.this, R.string.empty_field_warning, Toast.LENGTH_SHORT).show();
                }
            }
        });

        searchDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.w(LOG_TAG, "online search stopped");
                stopSearch = true;
                searchDialog.cancel();
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
                        adapter.getCurrentTrackList().sortByArtists();
                        adapter.notifyDataSetChanged2();
                        break;
                    case R.id.sort_by_track_name:
                        adapter.getCurrentTrackList().sortByTrackName();
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
                            new ScanForSavedTracks(MainActivity.this, adapter.getMainTrackList());
                        else
                            adapter.changeTrackListMode(TracksListAdapter.SHOW_DOWNLOADED_TRACKS);
                        break;
                    case R.id.visibility_all_downloaded:
                        new GetTrackListFromResponseOrDB(GetTrackListFromResponseOrDB.IS_GET_SAVED_TRACKLIST_FROM_DB, MainActivity.this, null);
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
        Log.w(LOG_TAG, "showFooter: " + showFooter);
        try {
            if (showFooter) {
                fragmentManager.beginTransaction()
                        .show(fragmentManager.findFragmentById(R.id.footer))
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit();
                activityWatcher.onFooterRestore();
            } else {
                fragmentManager.beginTransaction()
                        .hide(fragmentManager.findFragmentById(R.id.footer))
                        //.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                        .commit();
            }
        } catch (Exception e) {e.printStackTrace();}

    }

    void showSelectedTracksCount() {
        if(adapter != null) {
            selectedInfo.setText(getString(R.string.selected) + adapter.getSelectedTracksID().size() + getString(R.string.from) + adapter.getCurrentTrackList().getAllTracks().size());
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

        RateThisApp.Config config = new RateThisApp.Config(3, 3);
        RateThisApp.init(config);
    }

    @Override
    public void onResponseParseComplete(TrackList tracks) {
        if(!tracks.getAllTracks().isEmpty()) {
            Log.w(LOG_TAG, "show online search result");
            adapter.showOnlineSearchResult(tracks);
        } else {
            Toast.makeText(getApplicationContext(), R.string.nothing_founded, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onGetSavedTracksComplete(TrackList tracks) {
        if(!tracks.getAllTracks().isEmpty()) {
            Log.w(LOG_TAG, "show all saved tracks");
            adapter.showAllSavedTracks(tracks);
        } else {
            Toast.makeText(getApplicationContext(), R.string.nothing_founded, Toast.LENGTH_SHORT).show();
        }
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
                adapter.getMainTrackList().setPathAfterDownload(track);
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
    public void onStartPlay(int trackID) {
        Log.w(LOG_TAG, "onStartPlay");
        adapter.notifyDataSetChanged(trackID, TracksListAdapter.ON_PLAY_TRACK);
        boolean b = CommonUtils.getBooleanSetting(MainActivity.this, Settings.SETTING_FOLLOW_ON_PLAY, false);
        Log.w(LOG_TAG, "b: " + b + ", isManualTrackSelect: " + isManualTrackSelect);
        if(b && !isManualTrackSelect) {
            try {
                tracksList.setSelection(adapter.getCurrentTrackList().getTrackPositionByID(trackID));
                Log.w(LOG_TAG, "onStartPlay. setSelection");
            } catch (Exception e) {}
        }
        isManualTrackSelect = false;
        //Log.w(LOG_TAG, "onStartPlay. isManualTrackSelect = false");
        Log.w(LOG_TAG, "b: " + b + ", isManualTrackSelect: " + isManualTrackSelect);

    }

    @Override
    public void onPrepareStart(int trackID) {
        adapter.notifyDataSetChanged(trackID, TracksListAdapter.ON_PREPARE_TRACK);
    }

    @Override
    public void onStopTrack() {
        adapter.notifyDataSetChanged(0, TracksListAdapter.ON_STOP_TRACK);
        showFooterFragment(false);
    }

    @Override
    public void onDeleteTrackFromAdapter(boolean needDeleteFromAdapter, Track track) {
        if(needDeleteFromAdapter) {
            adapter.getCurrentTrackList().removeTrack(track);
            adapter.getMainTrackList().removeTrack(track);
        }
        adapter.notifyDataSetChanged2();
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

        if(!isRelogin) {
            showFooterFragment(false);

            initAndStartBindingWithAudioService(BIND_WITH_AUDIO_SERVICE_ON_RESUME);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.w(LOG_TAG, "onStop");

        if(!isRelogin)
            initAndStartBindingWithAudioService(BIND_WITH_AUDIO_SERVICE_ON_STOP);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.w(LOG_TAG, "onDestroy");

        try {
            if(br != null)
                unregisterReceiver(br);
        } catch (Exception e) {e.printStackTrace();}

        if(!isExit && isRelogin) {
            Intent intent = new Intent(MainActivity.this, AuthActivity.class);
            intent.putExtra("autoCheckMediaLibrary", false);
            intent.putExtra("isRelogin", true);
            startActivity(intent);
        }
    }

    @Override
    public void onSearchTracksComplete(int code, String response) {
        if(!stopSearch) {
            switch (code) {
                case SearchTracks.SEARCH_TRACKS_STATUS_OK:
                    Log.w(LOG_TAG, response);
                    new GetTrackListFromResponseOrDB(GetTrackListFromResponseOrDB.IS_GET_TRACKLIST_FROM_ONLINE_SEARCH_RESPONSE, MainActivity.this, response);
                    break;
                case SearchTracks.SEARCH_TRACKS_STATUS_FAIL:
                    Toast.makeText(this, R.string.problems_with_parsing_response, Toast.LENGTH_SHORT).show();
                    break;
                case SearchTracks.SEARCH_TRACKS_NO_INTERNET:
                    Toast.makeText(this, R.string.problems_with_internet, Toast.LENGTH_SHORT).show();
                    break;
            }

            if (searchDialog != null) {
                try {
                    searchDialog.cancel();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            Log.w(LOG_TAG, "ignore online search result");
        }
    }

    @Override
    public void OnAddTrack(int code, Track track) {
        switch (code) {
            case AddTrack.ADD_TRACK_STATUS_OK:
                Log.w(LOG_TAG, "track with ID " + track.ID + " was added to user tracks");
                //audioService.getMultiTrackList().getTrackList(MultiTrackList.MAIN_TRACKLIST).addTrackToTop(track);
                adapter.getMainTrackList().addTrackToTop(track);
                Toast.makeText(this, R.string.track_was_added, Toast.LENGTH_SHORT).show();
                break;
            case AddTrack.ADD_TRACK_STATUS_FAIL:
                Toast.makeText(this, R.string.problems_with_parsing_response, Toast.LENGTH_SHORT).show();
                break;
            case AddTrack.ADD_TRACK_NO_INTERNET:
                Toast.makeText(this, R.string.problems_with_internet, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void OnDeleteTrack(int code, Track track) {
        adapter.getSelectedTracksID().clear();
        switch (code) {
            case DeleteTrack.DELETE_TRACK_STATUS_OK:
                Log.w(LOG_TAG, "track with ID " + track.ID + " was deleted from user tracks");
                Toast.makeText(this, R.string.track_was_deleted, Toast.LENGTH_SHORT).show();
                activityWatcher.onDeleteTrack(track);
                break;
            case DeleteTrack.DELETE_TRACK_STATUS_FAIL:
                Toast.makeText(this, R.string.problems_with_parsing_response, Toast.LENGTH_SHORT).show();
                break;
            case DeleteTrack.DELETE_TRACK_NO_INTERNET:
                Toast.makeText(this, R.string.problems_with_internet, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        showExitDialog();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Monitor launch times and interval from installation
        RateThisApp.onStart(this);
        // If the criteria is satisfied, "Rate this app" dialog will be shown
        RateThisApp.showRateDialogIfNeeded(this);
    }
}
