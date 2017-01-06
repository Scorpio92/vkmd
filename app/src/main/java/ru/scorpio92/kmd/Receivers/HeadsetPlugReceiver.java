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
 * Created by scorpio92 on 10.11.16.
 */

public class HeadsetPlugReceiver extends BroadcastReceiver {

    final String LOG_TAG = "HeadsetPlugReceiver";
    AudioService audioService;
    ServiceConnection sConn;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
            return;
        }

        boolean connectedHeadphones = (intent.getIntExtra("state", 0) == 1);
        //boolean connectedMicrophone = (intent.getIntExtra("microphone", 0) == 1) && connectedHeadphones;

        Log.w(LOG_TAG,"connectedHeadphones: " + Boolean.toString(connectedHeadphones));
        initAndStartBindingWithAudioService(context.getApplicationContext(), connectedHeadphones);
    }

    void initAndStartBindingWithAudioService(final Context context, final Boolean connectedHeadphones) {
        sConn = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder binder) {
                Log.w(LOG_TAG, "AudioService onServiceConnected");
                audioService = ((AudioService.MyBinder) binder).getService();
                if(!connectedHeadphones) {
                    if(audioService.getMediaPlayer().isPlaying()) {
                        audioService.pauseOrPlayTrack();
                    }
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
            context.unbindService(sConn);
        } catch (Exception e) {e.printStackTrace();}
        sConn = null;
    }
}
