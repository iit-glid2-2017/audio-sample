package com.iit.mediaplayer.musicplayerapp;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class AudioPlayerService extends Service {

    public static final String MEDIA_PLAYER_STARTED_KEY = "started";

    public static final int MEDIA_PLAYER_SERVICE_STARTED = 10;
    public static final int MEDIA_PLAYER_CONTROL_START = 21;
    public static final int MEDIA_PLAYER_CONTROL_PAUSE = 22;
    public static final int MEDIA_PLAYER_CONTROL_STOP = 23;

    public static final int MEDIA_PLAYER_CONTROL_PROGRESS = 24;

    public static final int MEDIA_PLAYER_SERVICE_CLIENT_UNBOUND = 30;


    private MediaPlayer mMediaPlayer;

    private Messenger mServiceMessenger;
    private Messenger messengerToApp;

    private ServiceHandler mHandler;

    private boolean isServiceBound = false;


    @Override
    public IBinder onBind(Intent arg0) {
        isServiceBound = true;
        return mServiceMessenger.getBinder();
    }

    @Override
    public void onCreate() {

        Log.v("log_iit", "onCreate in service");
        mMediaPlayer = new MediaPlayer();

        mHandler = new ServiceHandler(this);
        mServiceMessenger = new Messenger(mHandler);
        loadMusic();


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int stratdId) {
        Log.v("log_iit", "onStartCommand in service");
        if (intent != null) {

            messengerToApp = intent
                    .getParcelableExtra(MainActivity.MEDIA_PLAYER_APP_MESSENGER_KEY);
            if (messengerToApp != null) {
                try {
                    Message message = Message.obtain();
                    message.what = MEDIA_PLAYER_SERVICE_STARTED;
                    messengerToApp.send(message);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isServiceBound = false;
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
        }
        mMediaPlayer.reset();
        mMediaPlayer.release();
    }


    private void loadMusic() {

        try {
            mMediaPlayer.setDataSource("/sdcard/Download/music.mp3");
            mMediaPlayer.prepare();

        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    private void playPerform() {
        Log.v("log_iit", "start requested in service");
        mMediaPlayer.start();

        try {
            Message messagePlay = Message.obtain();
            messagePlay.what = MEDIA_PLAYER_CONTROL_START;
            messengerToApp.send(messagePlay);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        followProgress();

    }

    private void followProgress() {
        Log.v("log_iit", "followProgress call");
        try {
            Message messagePlay = Message.obtain();
            messagePlay.what = MEDIA_PLAYER_CONTROL_PROGRESS;
            messagePlay.arg1 = mMediaPlayer.getDuration();
            messagePlay.arg2 = mMediaPlayer.getCurrentPosition();
            messengerToApp.send(messagePlay);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                followProgress();
            }
        }, 1000);

    }


    /***********************************************************/
    /***************** private classes *************************/
    /***********************************************************/

    private static class ServiceHandler extends Handler {

        private final WeakReference<AudioPlayerService> mTarget;

        private ServiceHandler(AudioPlayerService target) {
            mTarget = new WeakReference<AudioPlayerService>(target);
        }

        @Override
        public void handleMessage(Message message) {

            AudioPlayerService target = mTarget.get();

            switch (message.what) {

                case AudioPlayerService.MEDIA_PLAYER_CONTROL_START:
                    target.playPerform();
                    break;
                case AudioPlayerService.MEDIA_PLAYER_CONTROL_PAUSE:
                    target.mMediaPlayer.pause();
                    try {
                        Message messagePause = Message.obtain();
                        messagePause.what = MEDIA_PLAYER_CONTROL_PAUSE;
                        target.messengerToApp.send(messagePause);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case AudioPlayerService.MEDIA_PLAYER_CONTROL_STOP:
                    target.mHandler.removeCallbacksAndMessages(null);
                    //just a workaround
                    target.mMediaPlayer.pause();
                    target.mMediaPlayer.seekTo(0);

                    try {
                        Message messageStop = Message.obtain();
                        messageStop.what = MEDIA_PLAYER_CONTROL_STOP;
                        target.messengerToApp.send(messageStop);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case AudioPlayerService.MEDIA_PLAYER_CONTROL_PROGRESS:
                    int progress = message.arg1;
                    int audioLength = target.mMediaPlayer.getDuration();
                    int audioPosition = audioLength * progress / 100;
                    target.mMediaPlayer.seekTo(audioPosition);
                    break;
                case AudioPlayerService.MEDIA_PLAYER_SERVICE_CLIENT_UNBOUND:

                    target.isServiceBound = false;

                    break;
            }
        }
    }

}