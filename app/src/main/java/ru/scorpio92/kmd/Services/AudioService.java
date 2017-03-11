package ru.scorpio92.kmd.Services;


import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.File;
import java.util.Random;
import java.util.Timer;

import ru.scorpio92.kmd.R;
import ru.scorpio92.kmd.Receivers.HeadsetPlugReceiver;
import ru.scorpio92.kmd.Receivers.LockScreenReceiver;
import ru.scorpio92.kmd.Types.MultiTrackList;
import ru.scorpio92.kmd.Types.Track;
import ru.scorpio92.kmd.View.MainActivity;

/**
 * Created by scorpio92 on 05.11.16.
 */

public class AudioService extends Service implements AudioManager.OnAudioFocusChangeListener {

    private final String LOG_TAG = "AudioService";

    private boolean isStarted = false;
    private boolean mMainActivityIsStopped;

    private MyBinder binder = new MyBinder();

    public final static String BROADCAST_ACTION = "ru.scorpio92.kmd.View.MusicListFooterFragment";
    public final static String PARAM_ACTION = "action";
    public final static int ACTION_SET_NEW_PLAY = 0;
    public final static int ACTION_UPDATE_PROGRESS = 1;
    public final static int ACTION_PAUSE = 2;
    public final static int ACTION_PLAY = 3;
    public final static int ACTION_STOP = 4;
    public final static int ACTION_ERROR = 5;
    public final static int ACTION_PLAY_STARTED = 6;
    public final static int ACTION_TRACK_DELETED = 7;
    public final static String PARAM_RESULT = "result";

    public final static int PLAY_PREV_OK = 0;
    public final static int PLAY_PREV_HEAD_LIST = 1;
    public final static int PLAY_NEXT_OK = 0;
    public final static int PLAY_NEXT_END_LIST = 1;

    private MediaPlayer mediaPlayer;
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;

    private int currentTrackID;

    private MultiTrackList multiTrackList;
    private int ownerID;
    private String token;

    private Timer mTimer;
    private TimerTask mTimerTask;
    private boolean stopPlay;
    private boolean loopActivated;
    private boolean randomMode;
    private boolean circularPaying;

    private HeadsetPlugReceiver headsetPlugReceiver;

    private int NOTIFICATION_ID = 666;
    public final static String NOTIFICATION_ACTION_PREV = "ru.scorpio92.kmd.Services.AudioService.PREV";
    public final static String NOTIFICATION_ACTION_PLAY_PAUSE = "ru.scorpio92.kmd.Services.AudioService.PLAY_PAUSE";
    public final static String NOTIFICATION_ACTION_NEXT = "ru.scorpio92.kmd.Services.AudioService.NEXT";
    public final static String NOTIFICATION_ACTION_STOP = "ru.scorpio92.kmd.Services.AudioService.STOP";

    private ComponentName remoteComponentName;
    private RemoteControlClient remoteControlClient;
    AudioManager audioManager;

    BroadcastReceiver downloadBr;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate()
    {
        Log.w(LOG_TAG, "service created");

        stopPlay = false;
        mediaPlayer = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.w(LOG_TAG, "service started with startID: " + startId);

        registerPhoneStateListener();
        registerHeadsetPlugReceiver();
        registerRemoteClient();
        registerDownloadBroadcastReceiver();

        //runNewTrackPlaying();

        isStarted = true;
        mMainActivityIsStopped = false;
        circularPaying = true;

        return Service.START_NOT_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {

        Log.w(LOG_TAG, "onTaskRemoved");
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy()
    {
        Log.w(LOG_TAG, "onDestroy");
        super.onDestroy();
    }

    public void stopPlay() {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra(PARAM_ACTION, ACTION_STOP);
        sendBroadcast(intent);

        Log.w(LOG_TAG, "stop play");

        stopTrackPlaying();

        try {
            if(mediaPlayer != null)
                mediaPlayer.release();
        } catch (Exception e) {e.printStackTrace();}

        try {
            if (headsetPlugReceiver != null) {
                unregisterReceiver(headsetPlugReceiver);
            }
        } catch (Exception e) {e.printStackTrace();}

        try {
            if (downloadBr != null) {
                unregisterReceiver(downloadBr);
            }
        } catch (Exception e) {e.printStackTrace();}

        try {
            if(audioManager!=null)
                audioManager.unregisterRemoteControlClient(remoteControlClient);
        } catch (Exception e) {e.printStackTrace();}

        try {
            if(audioManager!=null)
                audioManager.abandonAudioFocus(this);
        } catch (Exception e) {e.printStackTrace();}

        mediaPlayer = null;
        headsetPlugReceiver = null;
        remoteControlClient = null;
        audioManager = null;
        mediaPlayer = null;
        phoneStateListener = null;
        telephonyManager = null;
        //multiTrackList = null;

        //context.stopService(new Intent(context, AudioService.class));
        stopForeground(true);
        //stopSelf();
    }

    public void stopService() {
        stopPlay();
        Log.w(LOG_TAG, "stop AudioService");
        multiTrackList = null;
        //context.stopService(new Intent(context, AudioService.class));
        //stopForeground(true);
        stopSelf();
    }

    @Override
    public void onAudioFocusChange(int i) {

    }

    void registerPhoneStateListener() {
        if(phoneStateListener == null) {
            //регистрируем слушатель для регистрации событий телефонных звонков
            phoneStateListener = new PhoneStateListener() {
                @Override
                public void onCallStateChanged(int state, String incomingNumber) {
                    try {
                        switch(state) {
                            case TelephonyManager.CALL_STATE_RINGING:
                            case TelephonyManager.CALL_STATE_OFFHOOK:
                                pauseTrack();
                                break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    super.onCallStateChanged(state, incomingNumber);
                }
            };
        }
        if (telephonyManager == null) {
            telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            try {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void registerHeadsetPlugReceiver() {
        if(headsetPlugReceiver == null) {
            headsetPlugReceiver = new HeadsetPlugReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.HEADSET_PLUG");
            registerReceiver(headsetPlugReceiver, intentFilter);
        }
    }

    void registerRemoteClient(){
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            remoteControlClient = null;
            return;
        }
        remoteComponentName = new ComponentName(getApplicationContext(), LockScreenReceiver.class);
        try {
            if(remoteControlClient == null) {
                audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                // Request audio focus for playback
                int result = audioManager.requestAudioFocus(
                        this,
                        // Use the music stream.
                        AudioManager.STREAM_MUSIC,
                        // Request permanent focus.
                        AudioManager.AUDIOFOCUS_GAIN);

                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    audioManager.registerMediaButtonEventReceiver(remoteComponentName);
                    Log.w(LOG_TAG, "AudioManager.AUDIOFOCUS_REQUEST_GRANTED ok");
                    // Start playback.
                } else {
                    Log.w(LOG_TAG, "AudioManager.AUDIOFOCUS_REQUEST_GRANTED fail");
                    remoteControlClient = null;
                    return;
                }
                //audioManager.registerMediaButtonEventReceiver(remoteComponentName);
                Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
                mediaButtonIntent.setComponent(remoteComponentName);
                PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
                remoteControlClient = new RemoteControlClient(mediaPendingIntent);
                audioManager.registerRemoteControlClient(remoteControlClient);
            }
            remoteControlClient.setTransportControlFlags(
                    RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
                            RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
                            RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE |
                            RemoteControlClient.FLAG_KEY_MEDIA_STOP |
                            RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS |
                            RemoteControlClient.FLAG_KEY_MEDIA_NEXT);
            Log.w(LOG_TAG, "registerRemoteClient ok");
        } catch(Exception e) {e.printStackTrace();}
    }

    void registerDownloadBroadcastReceiver() {
        downloadBr = new BroadcastReceiver() {
            // действия при получении сообщений
            public void onReceive(Context context, Intent intent) {
                //Log.w(LOG_TAG, "onReceive");
                int action = intent.getIntExtra(DownloadService.PARAM_ACTION, -1);
                Track track = intent.getParcelableExtra(DownloadService.PARAM_TRACK);

                switch (action) {

                    case DownloadService.ACTION_DOWNLOAD_TRACK_FINISH:
                        if(multiTrackList.getTrackList(MultiTrackList.CURRENT_TRACKLIST).setPathAfterDownload(track))
                            Log.w(LOG_TAG, "track was added to service TrackList after download");
                        break;
                }
            }
        };
        // создаем фильтр для BroadcastReceiver
        IntentFilter intFilt = new IntentFilter(DownloadService.BROADCAST_ACTION);
        // регистрируем (включаем) BroadcastReceiver
        registerReceiver(downloadBr, intFilt);
    }


    public void sentNotificationInForeground() {

        try {
            Track track = getCurrentTrack();
            String title = track.ARTIST + " - " + track.TITLE;

            updateLockscreenMetadata(title);

            Intent notificationIntent = new Intent(this, MainActivity.class);

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
            PendingIntent pendingIntentPlay_Pause = PendingIntent.getBroadcast(this, 0, new Intent().setAction(NOTIFICATION_ACTION_PLAY_PAUSE), PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent pendingIntentStop = PendingIntent.getBroadcast(this, 0, new Intent().setAction(NOTIFICATION_ACTION_STOP), PendingIntent.FLAG_UPDATE_CURRENT);

            PendingIntent pendingIntentPrev = PendingIntent.getBroadcast(this, 0, new Intent().setAction(NOTIFICATION_ACTION_PREV), PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent pendingIntentNext = PendingIntent.getBroadcast(this, 0, new Intent().setAction(NOTIFICATION_ACTION_NEXT), PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
            builder
                    .setContentTitle(track.ARTIST)
                    .setContentText(track.TITLE)
                    .setSmallIcon(R.drawable.track)
                    .setPriority(Notification.PRIORITY_MAX);

            if(mMainActivityIsStopped) {
                builder.setContentIntent(pendingIntent);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.addAction(R.drawable.prev, "Prev", pendingIntentPrev).setColor(Color.BLACK);

                MediaSessionCompat mMediaSession = new MediaSessionCompat(getApplicationContext(), "mMediaSessionTag");
                builder.setStyle(new NotificationCompat.MediaStyle().setShowActionsInCompactView(0,1,2).setMediaSession(mMediaSession.getSessionToken()));
                builder.setVisibility(Notification.VISIBILITY_PUBLIC);
            }

            if (mediaPlayer != null) {
                if (!mediaPlayer.isPlaying()) {
                    builder.addAction(R.drawable.play, "Play", pendingIntentPlay_Pause).setColor(Color.BLACK);
                    setRemoteClientPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
                } else {
                    builder.addAction(R.drawable.pause, "Pause", pendingIntentPlay_Pause).setColor(Color.BLACK);
                    setRemoteClientPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.addAction(R.drawable.next, "Next", pendingIntentNext).setColor(Color.BLACK);
            }

            builder.addAction(R.drawable.exit, "Close", pendingIntentStop).setColor(Color.BLACK);

            Notification notification = builder.build();
            startForeground(NOTIFICATION_ID, notification);
        } catch (Exception e) {e.printStackTrace();}
    }

    void updateLockscreenMetadata(String tittle) {
        if (remoteControlClient == null)
            return;
        RemoteControlClient.MetadataEditor metadataEditor = remoteControlClient.editMetadata(true);
        metadataEditor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, tittle);
        metadataEditor.putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, null);
        metadataEditor.apply();
    }

    void setRemoteClientPlaybackState(int playbackState) {
        if(remoteControlClient == null) {
            return;
        }
        remoteControlClient.setPlaybackState(playbackState);
    }



    //set/get data from binder
    public class MyBinder extends Binder {
        public AudioService getService() {
            return AudioService.this;
        }
    }

    public void setMultiTrackList(MultiTrackList multiTrackList) {
        this.multiTrackList = multiTrackList;
        /*this.multiTrackList = new MultiTrackList(multiTrackList);
        ownerID = this.multiTrackList.getTrackList(MultiTrackList.MAIN_TRACKLIST).getOwnerID();
        token = this.multiTrackList.getTrackList(MultiTrackList.MAIN_TRACKLIST).getToken();*/
    }

    public MultiTrackList getMultiTrackList() {
        return multiTrackList;
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public void setCurrentTrackID(int currentTrackID) {
        this.currentTrackID = currentTrackID;
    }

    public int getCurrentTrackID() {
        return currentTrackID;
    }

    public Track getCurrentTrack() {
        return multiTrackList.getTrackList(MultiTrackList.CURRENT_TRACKLIST).containsTrack(currentTrackID);
    }

    public boolean isStarted() {
        return isStarted;
    }

    public void setMainActivityIsStopped (boolean b) {
        mMainActivityIsStopped = b;
        if(isStarted && mediaPlayer != null) {
            Log.w(LOG_TAG, "setMainActivityIsStopped: " + b + ", sentNotificationInForeground");
            sentNotificationInForeground();
        }
    }

    public int getOwnerID() {
        return ownerID;
    }

    public String getToken() {
        return token;
    }


    //music control
    public void stopTrackPlaying() {
        if(mediaPlayer != null) {
            Log.w(LOG_TAG, "mediaPlayer stop play");

            try {
                mediaPlayer.reset();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (mTimer != null) {
                mTimer.cancel();
                mTimer = null;
            }
        }
    }

    public void runNewTrackPlaying() {

        if(mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();

            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    try {
                        registerPhoneStateListener();
                        registerHeadsetPlugReceiver();

                        mediaPlayer.start();
                        Log.w(LOG_TAG, "mediaPlayer started");

                        sentNotificationInForeground();
                        Intent intent = new Intent(BROADCAST_ACTION);
                        intent.putExtra(PARAM_ACTION, ACTION_PLAY_STARTED);
                        sendBroadcast(intent);

                        if (mTimer != null) {
                            mTimer.cancel();
                        }
                        mTimer = new Timer();
                        mTimerTask = new TimerTask();
                        mTimer.schedule(mTimerTask, 0, 1000);

                    } catch (Exception e) {
                        e.printStackTrace();

                        Log.w(LOG_TAG, "Prepare error");
                        Intent intent_error = new Intent(BROADCAST_ACTION);
                        intent_error.putExtra(PARAM_ACTION, ACTION_ERROR);
                        sendBroadcast(intent_error);
                    }
                }
            });

            //когда трек закончился, автоматом переключаемся на следующий
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    nextTrack();
                }
            });
        }

        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra(PARAM_ACTION, ACTION_SET_NEW_PLAY);
        sendBroadcast(intent);

        stopTrackPlaying();

        sentNotificationInForeground();

        try {
            Track track = getCurrentTrack();

            String path;
            Log.w(LOG_TAG, "track.LOCAL_PATH: " + track.LOCAL_PATH);
            if(track.IS_DOWNLOADED && (new File(track.LOCAL_PATH).exists())) {
                path = track.LOCAL_PATH;
            } else {
                path = track.URL;
            }

            Log.w(LOG_TAG, "mediaPlayer setDataSource " + path);
            mediaPlayer.setDataSource(path);
            Log.w(LOG_TAG, "mediaPlayer start prepare");
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();

            Log.w(LOG_TAG, "Prepare error");
            Intent intent_error = new Intent(BROADCAST_ACTION);
            intent_error.putExtra(PARAM_ACTION, ACTION_ERROR);
            sendBroadcast(intent_error);
        }
    }

    public int prevTrack() {
        int currentTrackPositionInTrackList = multiTrackList.getTrackList(MultiTrackList.CURRENT_TRACKLIST).getTrackPositionByID(currentTrackID);
        if(currentTrackPositionInTrackList > 0) {
            currentTrackID = multiTrackList.getTrackList(MultiTrackList.CURRENT_TRACKLIST).getAllTracks().get(currentTrackPositionInTrackList-1).ID;
            runNewTrackPlaying();
            return PLAY_PREV_OK;
        }
        return PLAY_PREV_HEAD_LIST;
    }

    public void pauseOrPlayTrack() {
        Intent intent = new Intent(BROADCAST_ACTION);

        if(mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            intent.putExtra(PARAM_ACTION, ACTION_PAUSE);
        } else {
            mediaPlayer.start();
            intent.putExtra(PARAM_ACTION, ACTION_PLAY);
        }

        sendBroadcast(intent);
        sentNotificationInForeground();
    }

    public void pauseTrack() {
        try {
            if(mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    Intent intent = new Intent(BROADCAST_ACTION);
                    intent.putExtra(PARAM_ACTION, ACTION_PAUSE);
                    sendBroadcast(intent);
                    sentNotificationInForeground();
                }
            }
        } catch (Exception e) {e.printStackTrace();}
    }

    public int nextTrack() {
        if(!loopActivated) {
            if(randomMode) {
                currentTrackID = multiTrackList.getTrackList(MultiTrackList.CURRENT_TRACKLIST).getAllTracks().get(new Random().nextInt(multiTrackList.getTrackList(MultiTrackList.CURRENT_TRACKLIST).getAllTracks().size())).ID;
            } else {
                int currentTrackPositionInTrackList = multiTrackList.getTrackList(MultiTrackList.CURRENT_TRACKLIST).getTrackPositionByID(currentTrackID);
                if (currentTrackPositionInTrackList < multiTrackList.getTrackList(MultiTrackList.CURRENT_TRACKLIST).getAllTracks().size() - 1) {
                    currentTrackID = multiTrackList.getTrackList(MultiTrackList.CURRENT_TRACKLIST).getAllTracks().get(currentTrackPositionInTrackList + 1).ID;
                } else {
                    if(circularPaying) {
                        currentTrackID = multiTrackList.getTrackList(MultiTrackList.CURRENT_TRACKLIST).getAllTracks().get(0).ID;
                    } else
                        return PLAY_NEXT_END_LIST;
                }
            }
        }
        runNewTrackPlaying();
        return PLAY_NEXT_OK;
    }

    public boolean setLoop() {
        loopActivated = !loopActivated;
        return loopActivated;
    }

    public boolean setRandomMode() {
        randomMode = !randomMode;
        return randomMode;
    }

    public boolean getRandomMode() {
        return randomMode;
    }

    public boolean setCircularPaying() {
        circularPaying = !circularPaying;
        return circularPaying;
    }

    public boolean getCircularPaying() {
        return circularPaying;
    }

    public void seekTo(int value) {
        try {
                Log.w(LOG_TAG, "seekTo " + value);
                mediaPlayer.seekTo(value);
        } catch (Exception e) {e.printStackTrace();}
    }

    public void replay() {
        try {
            mediaPlayer.seekTo(0);
            if(mediaPlayer.isPlaying())
                mediaPlayer.pause();
            pauseOrPlayTrack();
        } catch (Exception e) {e.printStackTrace();}
    }

    public void deleteAndPlayNext(Track track) {
        Log.w(LOG_TAG, "deleteAndPlayNext");
        nextTrack();
        try {
            multiTrackList.getTrackList(MultiTrackList.CURRENT_TRACKLIST).removeTrack(track);
            multiTrackList.getTrackList(MultiTrackList.MAIN_TRACKLIST).removeTrack(track);
            Intent intent_error = new Intent(BROADCAST_ACTION);
            intent_error.putExtra(PARAM_ACTION, ACTION_TRACK_DELETED);
            sendBroadcast(intent_error);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class TimerTask extends java.util.TimerTask {

        @Override
        public void run() {

            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                int currentPosition = mediaPlayer.getCurrentPosition();
                                if (currentPosition > 0) {
                                    Intent intent = new Intent(BROADCAST_ACTION);
                                    intent.putExtra(PARAM_ACTION, ACTION_UPDATE_PROGRESS);
                                    Log.w(LOG_TAG, "PlayProgressTask. currentPosition: " + Integer.toString(currentPosition));
                                    intent.putExtra(PARAM_RESULT, currentPosition);
                                    sendBroadcast(intent);
                                }
                            } catch (Exception e) {e.printStackTrace();}
                        }
                    });
                }
            }
        }
    }
}
