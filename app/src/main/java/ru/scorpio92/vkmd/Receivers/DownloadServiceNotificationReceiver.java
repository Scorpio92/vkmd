package ru.scorpio92.vkmd.Receivers;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import ru.scorpio92.vkmd.Services.DownloadService;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * Created by scorpio92 on 11.12.16.
 */

public class DownloadServiceNotificationReceiver extends BroadcastReceiver {

    final String LOG_TAG = "DownloadServiceNotificationReceiver";
    DownloadService downloadService;
    ServiceConnection sConn;


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.w(LOG_TAG,"action: " + action);
        initAndStartBindingWithDownloadService(context.getApplicationContext(), action);
    }

    void initAndStartBindingWithDownloadService(final Context context, final String action) {
        sConn = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder binder) {
                Log.w(LOG_TAG, "onServiceConnected");
                downloadService = ((DownloadService.MyBinder) binder).getService();
                if(action.equals(DownloadService.NOTIFICATION_ACTION_PLAY_PAUSE)) {
                    downloadService.setDownloadPause();
                }
                if(action.equals(DownloadService.NOTIFICATION_ACTION_STOP)) {
                    downloadService.setStopDownload();
                }
                if(action.equals(DownloadService.NOTIFICATION_ACTION_RESCAN)) {
                    downloadService.setRescan(true);
                }
                unbind(context);
            }

            public void onServiceDisconnected(ComponentName name) {
                Log.w(LOG_TAG, "onServiceDisconnected");
            }
        };

        context.bindService(new Intent(context, DownloadService.class), sConn, BIND_AUTO_CREATE);
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

