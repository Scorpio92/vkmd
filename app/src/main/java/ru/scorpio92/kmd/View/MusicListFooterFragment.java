package ru.scorpio92.kmd.View;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import ru.scorpio92.kmd.Interfaces.ActivityWatcher;
import ru.scorpio92.kmd.Interfaces.FooterFragmentWatcher;
import ru.scorpio92.kmd.Operations.UpdateDownloadInfo;
import ru.scorpio92.kmd.R;
import ru.scorpio92.kmd.Services.AudioService;
import ru.scorpio92.kmd.Types.Track;
import ru.scorpio92.kmd.Types.TrackList;

import static android.content.Context.BIND_AUTO_CREATE;
import static ru.scorpio92.kmd.Utils.CommonUtils.getHumanTimeFromMilliseconds;

/**
 * Created by scorpio92 on 23.10.16.
 */

public class MusicListFooterFragment extends Fragment implements ActivityWatcher {

    final String LOG_TAG = "MusicListFooterFragment";

    AudioService audioService;
    BroadcastReceiver br;
    ServiceConnection sConn;

    TextView currentTrackName, timePlay, timeDuration;
    SeekBar progressBar;
    ImageButton repeatButton, previousButton, stopButton, pauseButton, playButton, nextButton, moreButton;

    FooterFragmentWatcher footerFragmentWatcher;

    int trackID;


    void initGUI(View view) {
        currentTrackName = (TextView) view.findViewById(R.id.currentTrackName);
        timePlay = (TextView) view.findViewById(R.id.timePlay);
        timeDuration = (TextView) view.findViewById(R.id.timeDuration);
        progressBar = (SeekBar) view.findViewById(R.id.progressBar);
        repeatButton = (ImageButton) view.findViewById(R.id.repeatButton);
        previousButton = (ImageButton) view.findViewById(R.id.previousButton);
        stopButton = (ImageButton) view.findViewById(R.id.stopButton);
        pauseButton = (ImageButton) view.findViewById(R.id.pauseButton);
        playButton = (ImageButton) view.findViewById(R.id.playButton);
        nextButton = (ImageButton) view.findViewById(R.id.nextButton);
        moreButton = (ImageButton) view.findViewById(R.id.moreButton);

        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.w(LOG_TAG, "onStopTrackingTouch " + seekBar.getProgress());
                audioService.seekTo(seekBar.getProgress());
            }
        });

        repeatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(audioService.setLoop())
                    Toast.makeText(getActivity(), R.string.loop_is_enabled, Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(getActivity(), R.string.loop_is_disabled, Toast.LENGTH_SHORT).show();
            }
        });

        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(audioService.prevTrack() == AudioService.PLAY_PREV_HEAD_LIST)
                    Toast.makeText(getActivity(), R.string.you_in_the_head_playlist, Toast.LENGTH_SHORT).show();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    audioService.stopService(getActivity().getApplicationContext());
                    audioService = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(audioService.getMediaPlayer() != null) {
                    if(audioService.getMediaPlayer().isPlaying()) {
                        audioService.pauseOrPlayTrack();
                    }
                }
            }
        });

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(audioService.getMediaPlayer() != null) {
                    audioService.pauseOrPlayTrack();
                }
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(audioService.nextTrack() == AudioService.PLAY_NEXT_END_LIST)
                    Toast.makeText(getActivity(), R.string.you_in_the_end_playlist, Toast.LENGTH_SHORT).show();
            }
        });

        moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMoreButtonDialog(view);
            }
        });
    }

    void registerBroadcastReceiver() {
        br = new BroadcastReceiver() {
            // действия при получении сообщений
            public void onReceive(Context context, Intent intent) {
                int action = intent.getIntExtra(AudioService.PARAM_ACTION, -1);
                //Log.w(LOG_TAG, "status = " + action);

                switch (action) {
                    case AudioService.ACTION_SET_NEW_PLAY:
                        setGuiForPlay();
                        break;
                    case AudioService.ACTION_UPDATE_PROGRESS:
                        int result = intent.getIntExtra(AudioService.PARAM_RESULT, 0);
                        progressBar.setProgress(result);
                        timePlay.setText(getHumanTimeFromMilliseconds(result));
                        break;
                    case AudioService.ACTION_PAUSE:
                        pauseButton.setVisibility(View.GONE);
                        playButton.setVisibility(View.VISIBLE);
                        break;
                    case AudioService.ACTION_PLAY:
                        playButton.setVisibility(View.GONE);
                        pauseButton.setVisibility(View.VISIBLE);
                        break;
                    case AudioService.ACTION_STOP:
                        footerFragmentWatcher.onStopTrack();
                        try {
                            getActivity().unregisterReceiver(br);
                        } catch (Exception e) {e.printStackTrace();}
                        audioService = null;
                        break;
                    case AudioService.ACTION_ERROR:
                        Toast.makeText(getActivity().getApplicationContext(), R.string.play_error, Toast.LENGTH_SHORT).show();
                        break;
                    case AudioService.ACTION_PLAY_STARTED:
                        playButton.setVisibility(View.GONE);
                        pauseButton.setVisibility(View.VISIBLE);
                        footerFragmentWatcher.onStartPlay(audioService.getCurrentTrackID());
                        break;
                }
            }
        };
        // создаем фильтр для BroadcastReceiver
        IntentFilter intFilt = new IntentFilter(AudioService.BROADCAST_ACTION);
        // регистрируем (включаем) BroadcastReceiver
        getActivity().registerReceiver(br, intFilt);
    }

    void initAndStartBindingWithAudioService(final TrackList trackList, final boolean startPlayAfterServiceConnected) {
        sConn = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder binder) {
                Log.w(LOG_TAG, "AudioService onServiceConnected");
                audioService = ((AudioService.MyBinder) binder).getService();
                if(startPlayAfterServiceConnected) {
                    startNewPlay(trackList, true, trackID);
                } else {
                    setGuiForPlay();
                    footerFragmentWatcher.onStartPlay(audioService.getCurrentTrackID());
                }
                try {
                    getActivity().unbindService(sConn);
                } catch (Exception e) {e.printStackTrace();}
            }

            public void onServiceDisconnected(ComponentName name) {
                Log.w(LOG_TAG, "AudioService onServiceDisconnected");
            }
        };

        getActivity().bindService(new Intent(getActivity(), AudioService.class), sConn, BIND_AUTO_CREATE);
    }

    void showMoreButtonDialog(View v) {
        PopupMenu popupMenu = new PopupMenu(getActivity(), v);
        popupMenu.inflate(R.menu.more_button_footer_menu);

        MenuItem random_mode = popupMenu.getMenu().findItem(R.id.random_mode);
        random_mode.setChecked(audioService.getRandomMode());

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                switch (item.getItemId()) {

                    case R.id.download_its_track:
                        ArrayList<Track> tracks = new ArrayList<>();
                        tracks.add(audioService.getCurrentTrack());
                        new UpdateDownloadInfo(getActivity(), tracks, UpdateDownloadInfo.ACTION_INSERT);
                        break;
                    case R.id.random_mode:
                        if(audioService.setRandomMode()) {
                            Toast.makeText(getActivity(), R.string.random_mode_enabled, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getActivity(), R.string.random_mode_disabled, Toast.LENGTH_SHORT).show();
                        }
                        break;
                }

                return true;
            }
        });

        popupMenu.show();
    }

    void setGuiForPlay() {
        Track track = audioService.getCurrentTrack();
        footerFragmentWatcher.onPrepareStart(audioService.getCurrentTrackID());
        currentTrackName.setText(track.ARTIST + " - " + track.TITLE);
        progressBar.setMax(track.DURATION * 1000);
        timeDuration.setText(getHumanTimeFromMilliseconds(track.DURATION * 1000));
        if(audioService.getMediaPlayer() != null) {
            int currentProgress = audioService.getMediaPlayer().getCurrentPosition();
            progressBar.setProgress(currentProgress);
            timePlay.setText(getHumanTimeFromMilliseconds(currentProgress));
            if (audioService.getMediaPlayer().isPlaying()) {
                Log.w(LOG_TAG, "setGuiForPlay, play now");
                playButton.setVisibility(View.GONE);
                pauseButton.setVisibility(View.VISIBLE);
            } else {
                Log.w(LOG_TAG, "setGuiForPlay, not play now");
                playButton.setVisibility(View.VISIBLE);
                pauseButton.setVisibility(View.GONE);
            }
        } else {
            Log.w(LOG_TAG, "setGuiForPlay, not play now (first run)");
            progressBar.setProgress(0);
            timePlay.setText(getHumanTimeFromMilliseconds(0));
            playButton.setVisibility(View.VISIBLE);
            pauseButton.setVisibility(View.GONE);
        }
    }

    void startNewPlay(TrackList trackList, boolean startService, int trackID) {
        if(startService) {
            Log.w(LOG_TAG, "startNewPlay with startService");
            audioService.setTrackList(trackList);
            audioService.setCurrentTrackID(trackID);
            getActivity().getBaseContext().startService(new Intent(getActivity().getBaseContext(), AudioService.class));
        } else {
            Log.w(LOG_TAG, "startNewPlay without startService");
            audioService.setCurrentTrackID(trackID);
            audioService.runNewTrackPlaying();
        }

        setGuiForPlay();
    }


    @Override
    public void onAttach(Activity context) {
        Log.w(LOG_TAG, "onAttach");
        footerFragmentWatcher = (FooterFragmentWatcher) context;
        footerFragmentWatcher.onAttached(this);
        super.onAttach(context);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        Log.w(LOG_TAG, "onHiddenChanged: " + Boolean.toString(hidden));
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.w(LOG_TAG, "onDetach");
        try {
            if(br != null)
                getActivity().unregisterReceiver(br);
        } catch (Exception e) {e.printStackTrace();}
        try {
            if(sConn != null)
                getActivity().unbindService(sConn);
        } catch (Exception e) {e.printStackTrace();}
        audioService = null;
        sConn = null;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.w(LOG_TAG, "onCreateView");
        View view = inflater.inflate(R.layout.music_list_footer, container);

        initGUI(view);

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onItemSelected(TrackList trackList, int trackID) {

        this.trackID = trackID;

        if(audioService == null) {
            Log.w(LOG_TAG, "audioService not running");

            //создаем BroadcastReceiver для сервиса AudioService
            registerBroadcastReceiver();

            //создаем биндинг к сервису и запускаем его автоматически после инициализации соединения
            initAndStartBindingWithAudioService(trackList, true);
        } else {
            Log.w(LOG_TAG, "audioService is running");
            if(trackID != audioService.getCurrentTrackID()) {
                startNewPlay(null, false, trackID);
            }
        }

    }

    @Override
    public void onFooterRestore() {
        Log.w(LOG_TAG, "onFooterRestore");
        //создаем BroadcastReceiver для сервиса AudioService
        registerBroadcastReceiver();

        //создаем биндинг к сервису и запускаем его автоматически после инициализации соединения
        initAndStartBindingWithAudioService(null, false);
    }


}
