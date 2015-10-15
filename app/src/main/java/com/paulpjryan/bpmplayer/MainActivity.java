package com.paulpjryan.bpmplayer;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.MediaController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends ActionBarActivity implements MediaController.MediaPlayerControl {

    //region notes
        /*
            TODO: fix play/pause button when returning to activity from notification (bug)
            TODO: Add BPM functionality (feature)
            TODO: Style controller and list (feature)
            TODO: Indicate playing song (feature)
         */
    //endregion

    private RecyclerView songView;
    private ArrayList<Song> songList;

    private MusicService musicService;
    private Intent playIntent;
    private boolean musicBound = false;

    private MusicController controller;

    private boolean paused = false, playbackPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        songView = (RecyclerView)findViewById(R.id.song_list);
        songList = new ArrayList<>();

        final LinearLayoutManager layoutManager = new LinearLayoutManager(getBaseContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        songView.setLayoutManager(layoutManager);

        getSongList();
        Log.d("Song_List", "Got " + songList.size() + " songs");

        Collections.sort(songList, new Comparator<Song>() {
            @Override
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });

        SongAdapter mSongAdapter = new SongAdapter(songList.toArray(new Song[songList.size()]), this);
        songView.setAdapter(mSongAdapter);

        setupController();
    }

    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder)service;
            //get service
            musicService = binder.getService();
            //pass list
            musicService.setList(songList);
            musicBound = true;

            musicService.setController(controller);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        if(playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    @Override
    public void onDestroy() {
        stopService(playIntent);
        unbindService(musicConnection);
        musicService = null;
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
        paused = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(paused) {
            setupController();
            paused = false;
        }

    }

    @Override
    public void onStop() {
        controller.hide();
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if(id == R.id.action_stopmusic) {
            stopService(playIntent);
            musicService = null;
            // TODO: make this not exit the app. Add regular music controls.
            System.exit(0);
        }

        return super.onOptionsItemSelected(item);
    }

    //retrieve song info
    public void getSongList() {
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        Cursor musicCursor = musicResolver.query(musicUri, null, selection, null, null);

        if(musicCursor != null && musicCursor.moveToFirst()) {
            int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);

            do {
                long id = musicCursor.getLong(idColumn);
                String title = musicCursor.getString(titleColumn);
                String artist = musicCursor.getString(artistColumn);
                songList.add(new Song(id, title, artist));
            } while (musicCursor.moveToNext());
        }
        if(musicCursor != null)
            musicCursor.close();
    }

    public void songPicked(View view) {
        int pos = songView.getChildLayoutPosition(view);
        musicService.setSong(pos);
        musicService.playSong();

        if(playbackPaused) {
            setupController();
            playbackPaused = false;
        }

        //controller.show(0);
    }

    public void playNext() {
        musicService.playNext();

        if(playbackPaused) {
            setupController();
            playbackPaused = false;
        }

        //controller.show(0);
    }

    public void playPrev() {
        musicService.playPrev();

        if(playbackPaused) {
            setupController();
            playbackPaused = false;
        }

        //controller.show(0);
    }

    //region MediaController Methods
    @Override
    public void start() {
        musicService.play();
    }

    @Override
    public void pause() {
        playbackPaused = true;
        musicService.pausePlayer();
    }

    @Override
    public int getDuration() {
        if(musicService != null && musicBound && musicService.isPlaying()) {
            return musicService.getDuration();
        }
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        if(musicService != null && musicBound && musicService.isPlaying()) {
            return musicService.getPosition();
        }
        return 0;
    }

    @Override
    public void seekTo(int i) {
        musicService.seek(i);
    }

    @Override
    public boolean isPlaying() {
        if(musicService != null && musicBound && musicService.isPlaying()) {
            return musicService.isPlaying();
        }
        return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }
    //endregion

    //Setup music controller
    private void setupController() {
        if (controller == null)
            controller = new MusicController(this);

        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playPrev();
            }
        });

        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.song_list));
        controller.setEnabled(true);
    }
}
