package com.example.fizapathan.browserplayer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import java.util.ArrayList;
import android.content.ContentUris;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Binder;
import android.os.PowerManager;
import android.util.Log;
import java.util.Random;
import android.app.Notification;
import android.app.PendingIntent;


/**
 * Created by Fiza Pathan on 5/11/2020.
 */

public class MusicService extends Service
        implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener,
        AudioManager.OnAudioFocusChangeListener {
    @Override
    public void onAudioFocusChange(int i) {

    }

    private MediaPlayer player; //media player
    private ArrayList<Song> songs; //song list
    private int songPosition; //current position
    private final IBinder musicBind = new MusicBinder();

    private String songTitle = "";
    private static final int NOTIFY_ID = 1;

    private boolean shuffle = false;
    private Random random;

    private AudioManager mAudioManager;
    //private AudioAttributes mAudioAttributes;
    //private AudioFocusRequest mAudioFocusRequest;
    /** To handle audio interruptions */
    AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener;

    public void onCreate() {
        //create the service
        super.onCreate();
        songPosition = 0;
        player = new MediaPlayer();
        random = new Random();
        initMusicPlayer();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mAudioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mAudioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(mAudioAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(mOnAudioFocusChangeListener)
                    .build();
        }
        */
        mOnAudioFocusChangeListener =
                new AudioManager.OnAudioFocusChangeListener() {
                    @Override
                    public void onAudioFocusChange(int focusChange) {
                        if(focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                            if(!isPlng())
                                go();
                        }
                        else if(focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                            int currentVolume;  // = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                            int interruptedVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_RING);
                            //int setVolume = currentVolume - interruptedVolume;
                            currentVolume = Math.round(interruptedVolume / 2) + 1;
                            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);
                        }
                        else if(focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                            pausePlayer();
                        }
                        else if(focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                            player.stop();
                            releaseMediaPlayer();
                        }
                    }
                };
    }

    public void initMusicPlayer() {
        //setting player properties
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
    }

    public void setList(ArrayList<Song> theSongs) {
        songs = theSongs;
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    public void playSong() {
        // Release the media player if it currently exists because we are about to
        // play a different sound file
        player.reset();

        Song playSong = songs.get(songPosition);
        songTitle = playSong.getTitle();
        long currentSong = playSong.getId();

        Uri trackUri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currentSong);

        int res = mAudioManager.requestAudioFocus(mOnAudioFocusChangeListener,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        if(res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            try {
                player.setDataSource(getApplicationContext(), trackUri);
            } catch (Exception e) {
                Log.i("Music Service", "Error setting data source" + e);
            }
            player.prepareAsync();
        }
    }

    public void setSong(int songIndex) {
        songPosition = songIndex;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        player.stop();
        player.release();
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        if(player.getCurrentPosition() > 0) {
            mediaPlayer.reset();
            playNext();
        }
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
        mediaPlayer.reset();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        //start playback
        mediaPlayer.start();

        Intent notIntent = new Intent(this, PlaylistActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_play_button)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentTitle("Playing")
                .setContentText(songTitle);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            Notification notification = builder.build();
            startForeground(NOTIFY_ID, notification);
        }
    }

    /**
     * Clean up the media player by releasing its resources.
     */
    private void releaseMediaPlayer() {
        // If the media player is not null, then it may be currently playing a sound.
        if (player != null) {
            // Regardless of the current state of the media player, release its resources
            // because we no longer need it.
            player.stop();
            player.release();

            // Set the media player back to null. For our code, we've decided that
            // setting the media player to null is an easy way to tell that the media player
            // is not configured to play an audio file at the moment.
            player = null;

            mAudioManager.abandonAudioFocus(mOnAudioFocusChangeListener);
        }
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        releaseMediaPlayer();
    }

    /*
        MediaPlayback control code starts here
        Below methods are used for media controller
         */
    public int getPosition() {
        return player.getCurrentPosition();
    }

    public int getDur() {
        return player.getDuration();
    }

    public boolean isPlng() {
        return player.isPlaying();
    }

    public void pausePlayer() {
        player.pause();
    }

    public void seek(int position) {
        player.seekTo(position);
    }

    public void go() {
        player.start();
    }

    //skip to previous song
    public void playPrev() {
        songPosition--;
        if(songPosition < 0)
            songPosition = songs.size() - 1;
        playSong();
    }

    //skip to next song
    public void playNext() {
        if(shuffle) {
            int newSong = songPosition;
            while (newSong == songPosition)
                newSong = random.nextInt(songs.size());
            songPosition = newSong;
        } else {
            songPosition++;
            if (songPosition >= songs.size())
                songPosition = 0;
        }
        playSong();
    }

    public void setShuffle() {
        shuffle = !shuffle;
    }
}
