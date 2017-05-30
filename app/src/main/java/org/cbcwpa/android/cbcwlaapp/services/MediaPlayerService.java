package org.cbcwpa.android.cbcwlaapp.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSessionManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.cbcwpa.android.cbcwlaapp.R;
import org.cbcwpa.android.cbcwlaapp.activities.HomeActivity;
import org.cbcwpa.android.cbcwlaapp.activities.SermonActivity;
import org.cbcwpa.android.cbcwlaapp.utils.PlaybackStatus;
import org.cbcwpa.android.cbcwlaapp.xml.Sermon;

import java.io.IOException;

public class MediaPlayerService extends Service implements
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener,
        MediaPlayer.OnBufferingUpdateListener,
        AudioManager.OnAudioFocusChangeListener {


    private static final String TAG = "MediaPlayerService";

    private MediaPlayer mediaPlayer;

    private Sermon sermon;

    private int resumePosition;

    private AudioManager audioManager;

    public static boolean isRunning = false;


    /****************** service ***********************/

    private final IBinder iBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    public class LocalBinder extends Binder {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Perform one-time setup procedures

        // Manage incoming phone calls during playback.
        // Pause MediaPlayer on incoming call,
        // Resume on hangup.
        callStateListener();

        //ACTION_AUDIO_BECOMING_NOISY -- change in audio outputs -- BroadcastReceiver
        registerBecomingNoisyReceiver();

        //Listen for new Audio to play -- BroadcastReceiver
        registerAudioControlBroadcasts();

        isRunning = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Request audio focus
        if (!requestAudioFocus()) {
            // Could not gain focus
            stopSelf();
        }

        if (mediaSessionManager == null) {
            try {
                initMediaSession();
            } catch (RemoteException e) {
                e.printStackTrace();
                stopSelf();
            }
        }

        //Handle Intent action from MediaSession.TransportControls
        handleIncomingActions(intent);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            stopMedia();
            mediaPlayer.release();
        }

        removeAudioFocus();

        // disable phone state listener
        if (phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        removeNotification();

        Log.i(TAG, "MediaPlayerService destroyed properly");

        // unregister broadcast receivers
        unregisterReceiver(becomingNoisyReceiver);
        unregisterAudioControlBroadcasts();

        // remove notifications
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(NOTIFICATION_ID);

        isRunning = false;

    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // property shutdown service when swipe killed the app
        // requires setting stopWithTask to false in manifest
        stopSelf();
    }

    /*************** headphone removed ******************/

    // Receive when audio output has changed (i.e. headphone removed)
    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //pause audio on ACTION_AUDIO_BECOMING_NOISY
            pauseMedia();
            buildNotification(PlaybackStatus.PAUSED);
        }
    };

    private void registerBecomingNoisyReceiver() {
        // register after getting audio focus
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, intentFilter);
    }

    /***************** phone call monitor *****************/

    //Handle incoming phone calls
    private PhoneStateListener phoneStateListener;

    private TelephonyManager telephonyManager;

    private void callStateListener() {
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        phoneStateListener = new PhoneStateListener() {

            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    // pause media player if on a call or phone is rining
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        break;
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (mediaPlayer != null) {
                            pauseMedia();
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        if (mediaPlayer != null) {
                            resumeMedia();
                        }
                        break;
                }
            }
        };

        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }



    /***************** player control *******************/

    private void initMediaPlayer() {
        Log.i(TAG, "init media player");

        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();

            //Set up MediaPlayer event listeners
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnErrorListener(this);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnBufferingUpdateListener(this);
            mediaPlayer.setOnSeekCompleteListener(this);
            mediaPlayer.setOnInfoListener(this);
        }

        // Reset so that the MediaPlayer is not pointing to another data source
        mediaPlayer.reset();

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            // Set the data source to the mediaFile location
            mediaPlayer.setDataSource(sermon.getAudioPath());
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }
        mediaPlayer.prepareAsync();
    }

    private void playMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
        if (client != null)
            client.playing(sermon.getId());
    }

    private void stopMedia() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        if (client != null)
            client.stopped();
    }

    private void pauseMedia() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            resumePosition = mediaPlayer.getCurrentPosition();
        }
        if (client != null)
            client.paused();
    }

    private void resumeMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(resumePosition);
            mediaPlayer.start();
        }
        if (client != null)
            client.playing(sermon.getId());
    }

    private void skipToNext() {

//        stopMedia();
//        //reset mediaPlayer
//        mediaPlayer.reset();
//        initMediaPlayer();
    }

    private void skipToPrevious() {

//        stopMedia();
//        //reset mediaPlayer
//        mediaPlayer.reset();
//        initMediaPlayer();
    }

    private void rewind() {
        if (mediaPlayer.isPlaying()) {
            int pos = mediaPlayer.getCurrentPosition();
            int newpos = ((pos - 5000)  > 0) ? pos-5000 : 0;
            mediaPlayer.seekTo(newpos);
        }
    }

    private void fastforward() {
        int dur = mediaPlayer.getDuration();
        int pos = mediaPlayer.getCurrentPosition();
        int newpos = ((pos + 5000) < dur) ? pos+5000 : dur;
        mediaPlayer.seekTo(newpos);
    }

    /***************** MediaPlayer listeners *******************/

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {

    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        stopMedia();
        stopSelf();
        // TODO how to restart service when play is clicked
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        switch (i) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d("MediaPlayer Error", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + i1);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d("MediaPlayer Error", "MEDIA ERROR SERVER DIED " + i1);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d("MediaPlayer Error", "MEDIA ERROR UNKNOWN " + i1);
                break;
        }
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        playMedia();
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {

    }

    /****************** audio focus **********************/

    private PlaybackStatus playbackStatusBeforeFocusLoss;

    @Override
    public void onAudioFocusChange(int i) {

        switch (i) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (mediaPlayer == null) initMediaPlayer();
                if (!mediaPlayer.isPlaying() && playbackStatusBeforeFocusLoss == PlaybackStatus.PLAYING)
                    mediaPlayer.start();
                else if (mediaPlayer.isPlaying() && playbackStatusBeforeFocusLoss == PlaybackStatus.PAUSED)
                    mediaPlayer.pause();
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mediaPlayer.isPlaying()){
                    mediaPlayer.stop();
                    playbackStatusBeforeFocusLoss = PlaybackStatus.PLAYING;
                } else {
                    playbackStatusBeforeFocusLoss = PlaybackStatus.PAUSED;
                }
                mediaPlayer.release();
                mediaPlayer = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    playbackStatusBeforeFocusLoss = PlaybackStatus.PAUSED;
                } else {
                    playbackStatusBeforeFocusLoss = PlaybackStatus.PLAYING;
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.setVolume(0.1f, 0.1f);
                    playbackStatusBeforeFocusLoss = PlaybackStatus.PLAYING;
                } else {
                    playbackStatusBeforeFocusLoss = PlaybackStatus.PAUSED;
                }
                break;
        }
    }

    private boolean requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //Focus gained
            return true;
        }
        //Could not gain focus
        return false;
    }

    private boolean removeAudioFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                audioManager.abandonAudioFocus(this);
    }

    /******************** receive play new audio broadcast ******************/

    private BroadcastReceiver playNewAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            // reset media player and play new audio
            if (mediaPlayer != null) {
                stopMedia();
                mediaPlayer.reset();
            }

            sermon = (Sermon) intent.getParcelableExtra("sermon");

            initMediaPlayer();
            buildNotification(PlaybackStatus.PLAYING);
        }
    };

    private BroadcastReceiver pauseAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            pauseMedia();
            buildNotification(PlaybackStatus.PAUSED);
        }
    };

    private BroadcastReceiver resumeAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            resumeMedia();
            buildNotification(PlaybackStatus.PLAYING);
        }
    };

    private void registerAudioControlBroadcasts() {

        // register to receive play new audio broadcast
        IntentFilter filter = new IntentFilter(SermonActivity.Broadcast_PLAY_NEW_AUDIO);
        registerReceiver(playNewAudio, filter);

        // register to receive pause audio broadcast
        filter = new IntentFilter(SermonActivity.Broadcast_PAUSE_AUDIO);
        registerReceiver(pauseAudio, filter);

        // register to receive start audio broadcast
        filter = new IntentFilter(SermonActivity.Broadcast_RESUME_AUDIO);
        registerReceiver(resumeAudio, filter);
    }

    private void unregisterAudioControlBroadcasts() {
        unregisterReceiver(playNewAudio);
        unregisterReceiver(resumeAudio);
        unregisterReceiver(pauseAudio);
    }


    /********************* media session (interaction with media controller) *******/

    public static final String ACTION_PLAY = "org.cbcwla.android.ACTION_PLAY";
    public static final String ACTION_PAUSE = "org.cbcwla.android.ACTION_PAUSE";
    public static final String ACTION_PREVIOUS = "org.cbcwla.android.ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "org.cbcwla.android.ACTION_NEXT";
    public static final String ACTION_STOP = "org.cbcwla.android.ACTION_STOP";
    public static final String ACTION_RW = "org.cbcwla.android.ACTION_RW";
    public static final String ACTION_FF = "org.cbcwla.android.ACTION_FF";

    // MediaSession
    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;

    // AudioPlayer notification ID
    private static final int NOTIFICATION_ID = 101;

    private void initMediaSession() throws RemoteException {
        if (mediaSessionManager != null) return; //mediaSessionManager exists

        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        // Create a new MediaSession
        mediaSession = new MediaSessionCompat(getApplicationContext(), "AudioPlayer");
        //Get MediaSessions transport controls
        transportControls = mediaSession.getController().getTransportControls();
        //set MediaSession -> ready to receive media commands
        mediaSession.setActive(true);
        //indicate that the MediaSession handles transport control commands
        // through its MediaSessionCompat.Callback.
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Attach Callback to receive MediaSession updates
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            // Implement callbacks
            @Override
            public void onPlay() {
                super.onPlay();
                resumeMedia();
                buildNotification(PlaybackStatus.PLAYING);
//                if (client != null)
//                    client.playing();
            }

            @Override
            public void onPause() {
                super.onPause();
                pauseMedia();
                buildNotification(PlaybackStatus.PAUSED);
//                if (client != null)
//                    client.paused();
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                skipToNext();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                skipToPrevious();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onFastForward() {
                super.onFastForward();
                fastforward();
            }

            @Override
            public void onRewind() {
                super.onRewind();
                rewind();
            }

            @Override
            public void onStop() {
                super.onStop();
                removeNotification();
                //Stop the service
                stopSelf();
//                if (client != null)
//                    client.paused();
            }

            @Override
            public void onSeekTo(long position) {
                super.onSeekTo(position);
            }
        });
    }

    /************** notification ****************/


    private void buildNotification(PlaybackStatus playbackStatus) {

        int notificationAction = android.R.drawable.ic_media_pause;//needs to be initialized
        PendingIntent play_pauseAction = null;

        //Build a new notification according to the current state of the MediaPlayer
        if (playbackStatus == PlaybackStatus.PLAYING) {
            notificationAction = android.R.drawable.ic_media_pause;
            //create the pause action
            play_pauseAction = playbackAction(1);
        } else if (playbackStatus == PlaybackStatus.PAUSED) {
            notificationAction = android.R.drawable.ic_media_play;
            //create the play action
            play_pauseAction = playbackAction(0);
        }

        Intent homeIntent = new Intent(this, HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                homeIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_sermon); //replace with your own image

        // Create a new Notification
        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setShowWhen(false)
                .setContentIntent(pendingIntent)
                .setStyle(new NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.getSessionToken())
                // Show our playback controls in the compact notification view.
                .setShowActionsInCompactView(0, 1, 2))
//               .setColor(getResources().getColor(R.color.colorPrimary))
                .setLargeIcon(largeIcon)
                .setSmallIcon(android.R.drawable.stat_sys_headset)
                // Set Notification content information
                .setContentText(sermon.getAuthor())
                .setContentTitle(sermon.getTitle())
                .setContentInfo(sermon.getPubDate())
                // Add playback actions
//                .addAction(android.R.drawable.ic_media_previous, "previous", playbackAction(3))
                .addAction(android.R.drawable.ic_media_rew, "rewind", playbackAction(4))
                .addAction(notificationAction, "pause", play_pauseAction)
                .addAction(android.R.drawable.ic_media_ff, "fast forward", playbackAction(5));
//                .addAction(android.R.drawable.ic_media_next, "next", playbackAction(2));

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void removeNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private PendingIntent playbackAction(int actionNumber) {
        Intent playbackAction = new Intent(this, MediaPlayerService.class);
        switch (actionNumber) {
            case 0:
                // Play
                playbackAction.setAction(ACTION_PLAY);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 1:
                // Pause
                playbackAction.setAction(ACTION_PAUSE);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 2:
                // Next track
                playbackAction.setAction(ACTION_NEXT);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 3:
                // Previous track
                playbackAction.setAction(ACTION_PREVIOUS);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 4:
                playbackAction.setAction(ACTION_RW);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 5:
                playbackAction.setAction(ACTION_FF);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            default:
                break;
        }
        return null;
    }

    private void handleIncomingActions(Intent playbackAction) {
        if (playbackAction == null || playbackAction.getAction() == null) return;

        String actionString = playbackAction.getAction();
        if (actionString.equalsIgnoreCase(ACTION_PLAY)) {
            transportControls.play();
        } else if (actionString.equalsIgnoreCase(ACTION_PAUSE)) {
            transportControls.pause();
        } else if (actionString.equalsIgnoreCase(ACTION_NEXT)) {
            transportControls.skipToNext();
        } else if (actionString.equalsIgnoreCase(ACTION_PREVIOUS)) {
            transportControls.skipToPrevious();
        } else if (actionString.equalsIgnoreCase(ACTION_STOP)) {
            transportControls.stop();
        } else if (actionString.equalsIgnoreCase(ACTION_RW)) {
            transportControls.rewind();
        } else if (actionString.equalsIgnoreCase(ACTION_FF)) {
            transportControls.fastForward();
        }
    }

    /****************** communicate with activity through callback **********/

    private MediaListener client;

    public void registerClient(MediaListener activity) {
        client = activity;
    }

    public void unregisterClient() {
        client = null;
    }

    public interface MediaListener {
        public void stopped();
        public void paused();
        public void playing(int sermonId);
    }

    public int getCurrentSermonId() {
        if (sermon != null)
            return sermon.getId();
        else
            return -1;
    }

    public PlaybackStatus getStatus() {
        if (mediaPlayer.isPlaying())
            return PlaybackStatus.PLAYING;
        else if (sermon != null)
            return PlaybackStatus.PAUSED;
        else
            return PlaybackStatus.STOPPED;
    }
}
