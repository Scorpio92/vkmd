package ru.scorpio92.kmd.Receivers;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import ru.scorpio92.kmd.Services.AudioService;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * Created by scorpio92 on 08.11.16.
 */

public class AudioServiceNotificationReceiver extends BroadcastReceiver {

    final String LOG_TAG = "AudioServiceNotificationReceiver";
    AudioService audioService;
    ServiceConnection sConn;


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.w(LOG_TAG,"action: " + action);
        initAndStartBindingWithAudioService(context.getApplicationContext(), action);
    }

    void initAndStartBindingWithAudioService(final Context context, final String action) {
        sConn = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder binder) {
                Log.w(LOG_TAG, "AudioService onServiceConnected");
                audioService = ((AudioService.MyBinder) binder).getService();
                if(action.equals(AudioService.NOTIFICATION_ACTION_PREV)) {
                    audioService.prevTrack();
                }
                if(action.equals(AudioService.NOTIFICATION_ACTION_PLAY_PAUSE)) {
                    audioService.pauseOrPlayTrack();
                }
                if(action.equals(AudioService.NOTIFICATION_ACTION_NEXT)) {
                    audioService.nextTrack();
                }
                if(action.equals(AudioService.NOTIFICATION_ACTION_STOP)) {
                    //audioService.stopService(context);
                    audioService.stopPlay();
                }
                unbind(context);
            }

            public void onServiceDisconnected(ComponentName name) {
                Log.w(LOG_TAG, "AudioService onServiceDisconnected");
            }
        };

        context.bindService(new Intent(context, AudioService.class), sConn, BIND_AUTO_CREATE);
    }

    void unbind(Context context) {
        Log.w(LOG_TAG, "unbind");
        try {
            if(sConn != null)
                context.unbindService(sConn);
        } catch (Exception e) {e.printStackTrace();}
        sConn = null;
    }
}
