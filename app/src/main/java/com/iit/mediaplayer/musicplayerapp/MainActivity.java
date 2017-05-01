package com.iit.mediaplayer.musicplayerapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    public static final String MEDIA_PLAYER_APP_MESSENGER_KEY = "app_messenger";
    private static final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;
    private static final int REQUEST_APP_SETTINGS = 168;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private Button mPlayButton;
    private Button mStopButton;
    private SeekBar mSeekBar;
    private boolean isSeek = false;


    private AppHandler mHandler;
    private Messenger mAppMessenger;
    private MediaPlayerServiceConnection mConnection = new MediaPlayerServiceConnection();
    private Messenger messengerToService;

    private boolean isServiceConnected = false;

    private boolean isPlaying = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPlayButton = (Button) findViewById(R.id.play_button);
        mPlayButton.setOnClickListener(this);

        mStopButton = (Button) findViewById(R.id.stop_button);
        mStopButton.setOnClickListener(this);

        mSeekBar = (SeekBar) findViewById(R.id.seek_bar);
        mSeekBar.setOnSeekBarChangeListener(this);

        mHandler = new AppHandler(this);
        mAppMessenger = new Messenger(mHandler);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (hasPermissions(REQUIRED_PERMISSIONS)) {
                Log.v("test", "onCreate:hasPermissions");
                initService();
            } else {
                //Ask for permission: Type 1
                checkPermissions();
            }
        } else {
            initService();
        }


    }

    private void initService() {
        Intent serviceIntent = new Intent(this,
                AudioPlayerService.class);
        serviceIntent.putExtra(MEDIA_PLAYER_APP_MESSENGER_KEY, mAppMessenger);
        startService(serviceIntent);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.play_button:
                if (!isPlaying) {
                    playAudio();
                } else {
                    pauseAudio();
                }
                break;
            case R.id.stop_button:
                stopAudio();
                break;
        }
    }


    private void playAudio() {
        if (messengerToService != null) {
            try {
                Message message = Message.obtain();
                message.what = AudioPlayerService.MEDIA_PLAYER_CONTROL_START;
                messengerToService.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

    }

    private void pauseAudio() {
        if (messengerToService != null) {
            try {
                Message message = Message.obtain();
                message.what = AudioPlayerService.MEDIA_PLAYER_CONTROL_PAUSE;
                messengerToService.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

    }

    private void stopAudio() {
        if (messengerToService != null) {
            try {
                Message message = Message.obtain();
                message.what = AudioPlayerService.MEDIA_PLAYER_CONTROL_STOP;
                messengerToService.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

    }


    private void doBind() {
        Log.v("log_iit", "request service bind in activity");
        bindService(
                new Intent(this, AudioPlayerService.class),
                mConnection, Context.BIND_AUTO_CREATE);

    }

    private void doUnbindService() {
        if (messengerToService != null) {
            try {
                Message message = Message.obtain();
                message.what = AudioPlayerService.MEDIA_PLAYER_SERVICE_CLIENT_UNBOUND;
                messengerToService.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        unbindService(mConnection);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    private void updatePlayButton() {
        isPlaying = true;
        mPlayButton.setText("Pause");

    }

    private void updatePauseButton() {
        isPlaying = false;
        mPlayButton.setText("Play");
    }

    private void stopPerformed() {
        isPlaying = false;
        mPlayButton.setText("Play");
        mSeekBar.setProgress(0);
    }

    private void checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<String>();

        final List<String> permissionsList = new ArrayList<String>();
        if (!addPermission(permissionsList, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            permissionsNeeded.add("READ_EXTERNAL_STORAGE");
        }

        if (permissionsList.size() > 0) {
            if (permissionsNeeded.size() > 0) {
                // Need Rationale
                String message = "You need to grant access to " + permissionsNeeded.get(0);
                for (int i = 1; i < permissionsNeeded.size(); i++)
                    message = message + ", " + permissionsNeeded.get(i);

                showMessageOKCancel(message,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        permissionsList.toArray(new String[permissionsList.size()]),
                                        REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
                            }
                        });
                return;
            }
            ActivityCompat.requestPermissions(this, permissionsList.toArray(
                    new String[permissionsList.size()]),
                    REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
            return;
        }
        initService();
    }

    public boolean hasPermissions(@NonNull String... permissions) {
        for (String permission : permissions)
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(
                    getApplicationContext(), permission))
                return false;
        return true;
    }

    private boolean addPermission(List<String> permissionsList, String permission) {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), permission) !=
                PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission);
            // Check for Rationale Option
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission))
                return false;
        }
        return true;
    }


    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, okListener)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //
                    }
                })
                .create()
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS: {
                Map<String, Integer> perms = new HashMap<String, Integer>();
                // Initial
                perms.put(Manifest.permission.READ_EXTERNAL_STORAGE,
                        PackageManager.PERMISSION_GRANTED);
                // Fill with results
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);
                // Check for ACCESS_FINE_LOCATION
                if (perms.get(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_GRANTED) {
                    // All Permissions Granted
                    initService();
                } else {
                    //
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void goToSettings() {
        Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
        myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
        myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityForResult(myAppSettings, REQUEST_APP_SETTINGS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.e("test", "On result, " + requestCode + "," + resultCode);
        if (resultCode == REQUEST_APP_SETTINGS) {

            if (hasPermissions(REQUIRED_PERMISSIONS)) {
                initService();
            } else {
                //
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void seekAudio(int progress) {
        if (messengerToService != null) {
            try {
                Message message = Message.obtain();
                message.what = AudioPlayerService.MEDIA_PLAYER_CONTROL_PROGRESS;
                message.arg1 = progress;
                messengerToService.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

    }

    private void progressPerformed(int duration, int currentPosition) {
        int progress = (int) ((float) currentPosition / (float) duration * 100);
        mSeekBar.setProgress(progress);
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (isSeek) {
            seekAudio(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        isSeek = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        isSeek = false;
    }

    /***********************************************************/
    /***************** private classes *************************/
    /**
     * *******************************************************
     */

    private class MediaPlayerServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {

            isServiceConnected = true;
            messengerToService = new Messenger(binder);

            Log.v("log_iit", "service connected");

            //try {
            //Message message = Message.obtain();
            //message.what = MediaPlayerService.MEDIA_PLAYER_GET_PODCASTS;
            //messengerToService.send(message);
            //} catch (RemoteException e1) {
            //  e1.printStackTrace();
            //}
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            messengerToService = null;
        }
    }

    private static class AppHandler extends Handler {

        private final WeakReference<MainActivity> mTarget;

        private AppHandler(MainActivity target) {
            mTarget = new WeakReference<MainActivity>(target);
        }

        @Override
        public void handleMessage(Message message) {

            MainActivity target = mTarget.get();
            Bundle bundle;
            switch (message.what) {
                case AudioPlayerService.MEDIA_PLAYER_SERVICE_STARTED:
                    target.doBind();
                    break;
                case AudioPlayerService.MEDIA_PLAYER_CONTROL_START:
                    target.updatePlayButton();
                    break;
                case AudioPlayerService.MEDIA_PLAYER_CONTROL_PAUSE:
                    target.updatePauseButton();
                    break;
                case AudioPlayerService.MEDIA_PLAYER_CONTROL_STOP:
                    target.stopPerformed();
                    break;
                case AudioPlayerService.MEDIA_PLAYER_CONTROL_PROGRESS:
                    Log.v("log_iit", "progress in app with duration = " + message.arg1 + " and current position = " + message.arg2);
                    target.progressPerformed(message.arg1, message.arg2);
                    break;
            }
        }
    }
}
