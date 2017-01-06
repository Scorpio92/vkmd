package ru.scorpio92.kmd.Receivers;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;

import ru.scorpio92.kmd.Services.AudioService;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * Created by scorpio92 on 13.11.16.
 */

public class LockScreenReceiver extends BroadcastReceiver {

    final String LOG_TAG = "LockScreenReceiver";
    AudioService audioService;
    ServiceConnection sConn;

    @Override
    public void onReceive(Context context, Intent intent) {
        initAndStartBindingWithAudioService(context.getApplicationContext(), intent);
    }

    void initAndStartBindingWithAudioService(final Context context, final Intent intent) {
        sConn = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder binder) {
                Log.w(LOG_TAG, "AudioService onServiceConnected");
                audioService = ((AudioService.MyBinder) binder).getService();

                if(!audioService.isStarted()) {
                    return;
                }
                if (intent.getAction().equals(Intent.ACTION_MEDIA_BUTTON)) {
                    final KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

                    if (event != null && event.getAction() == KeyEvent.ACTION_UP) {
                        if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY) {
                            audioService.pauseOrPlayTrack();
                        } else if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_NEXT) {
                            audioService.nextTrack();
                        } else if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
                            audioService.prevTrack();
                        }
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