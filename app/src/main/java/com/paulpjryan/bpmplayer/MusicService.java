package com.paulpjryan.bpmplayer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.MediaController;

import java.util.ArrayList;

public class MusicService extends Service implements
    MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
    MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener {

    private MediaPlayer player;     //media player
    private ArrayList<Song> songs;  //songs
    private int songPos;            //current song position
    boolean wasPlaying = false;     //was music playing when audio focus was lost?

    private String songTitle = "";
    private static final int NOTIFY_ID = 1;

    private final IBinder musicBind = new MusicBinder();

    private MediaController controller;
    AudioManager am;

    public MusicService() {
        //onCreate();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        am = (AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        int result = am.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        if(result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            songPos = 0;
            player = new MediaPlayer();
            initMusicPlayer();
        }
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        super.onDestroy();
    }

    public void initMusicPlayer() {
        //set player properties
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);

        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
    }

    public void setList(ArrayList<Song> aSongs) {
        songs = aSongs;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            // Pause playback, will resume when regains
            pausePlayer();
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            // Resume playback
            if(wasPlaying)
                play();
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            //am.unregisterMediaButtonEventReceiver(RemoteControlReceiver);
            am.abandonAudioFocus(this);
            // Stop playback, will not resume when regains
            wasPlaying = false;
            pausePlayer();
        }
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    public void playSong() {
        //play a song
        player.reset();
        Song toPlay = songs.get(songPos);

        songTitle = toPlay.getTitle();

        long currSong = toPlay.getID();
        Uri trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currSong);

        try {
            player.setDataSource(getApplicationContext(), trackUri);
        } catch(Exception e) {
            Log.e("MusicService", "Error setting data source", e);
        }

        player.prepareAsync();
        wasPlaying = true;
    }

    public void setSong(int songIndex) {
        songPos = songIndex;
    }

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
    /* Code to play next on song finished
           if(player.getCurrentPosition()&gt;0){
                mp.reset();
                playNext();
           }
     */
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i2) {
        mediaPlayer.reset();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mediaPlayer.start();

        //if(!controller.isShowing())
            controller.show(0);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pend = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentIntent(pend)
                .setSmallIcon(R.drawable.ic_audiotrack_white_48dp)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentTitle("Playing")
                .setContentText(songTitle);
        Notification not = builder.build();

        startForeground(NOTIFY_ID, not);
    }

    public void setController(MediaController controller) {
        this.controller = controller;
    }

    public int getPosition() {
        return player.getCurrentPosition();
    }

    public int getDuration() {
        return player.getDuration();
    }

    public boolean isPlaying() {
        return player.isPlaying();
    }

    public void pausePlayer() {
        player.pause();
    }

    public void controlledPause() {
        wasPlaying = false;
        pausePlayer();
    }

    public void seek(int pos) {
        player.seekTo(pos);
    }

    public void play() {
        player.start();
    }

    public boolean playPrev() {
        //Log.d("MusicService", "Position = " + getPosition() + " checking against " + "5000");
        boolean ret = false;
        if(getPosition() < 3000) {
            songPos--;
            if (songPos < 0) {
                songPos = songs.size() - 1;
            }
            ret = true;
        }
        playSong();
        return ret;
    }

    public void playNext() {
        songPos++;
        if(songPos >= songs.size()) {
            songPos = 0;
        }
        playSong();
    }
}
