package com.paulpjryan.bpmplayer;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.TextView;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

public class MainActivity extends ActionBarActivity implements MediaController.MediaPlayerControl {

    //region notes
        /*
            TODO: fix play/pause button when returning to activity from notification (bug)
            TODO: speed up BPM functionality (bug)
                AsyncTask for BPM calculation
                Cache BPMs (done)
                Sort by BPM available after all songs have been processed
                    Done by default
            TODO: Style controller and list (feature)
            TODO: Add sort selector
            TODO: implement filters (feature)
            TODO: fix MediaController-keyboard conflict
                Cant open keyboard while MediaController is shown
        */
    //endregion

    private RecyclerView songView;
    private ArrayList<Song> songList;

    private MusicService musicService;
    private Intent playIntent;
    private boolean musicBound = false;

    private MusicController controller;

    private boolean paused = false, playbackPaused = false;
    SongAdapter mSongAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        songView = (RecyclerView)findViewById(R.id.song_list);
        songList = new ArrayList<>();

        //EditTexts for BPM filter
        EditText et_filter_low = (EditText)findViewById(R.id.filter_low);
        EditText et_filter_high = (EditText)findViewById(R.id.filter_high);

        et_filter_low.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //Change low limit of filter
                int lowLim = -2;
                if(count > 0) {
                    lowLim = Integer.parseInt(s.toString());
                }
                mSongAdapter.setLowLim(lowLim);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        et_filter_high.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //Change high limit of filter
                int highLim = 999;
                if(count > 0) {
                    highLim = Integer.parseInt(s.toString());
                }
                mSongAdapter.setHighLim(highLim);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        final LinearLayoutManager layoutManager = new LinearLayoutManager(getBaseContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        songView.setLayoutManager(layoutManager);

        getSongList();
        //Log.d("Song_List", "Got " + songList.size() + " songs");

        /*Collections.sort(songList, new Comparator<Song>() {
            @Override
            public int compare(Song a, Song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });
        */

        Collections.sort(songList, new Comparator<Song>() {
            @Override
            public int compare(Song a, Song b) {
                // a > b, return +
                // b > a, return -
                // a = b, return comparison of artists
                if(a.getBpm() < 0 && b.getBpm() < 0) {
                    //return equal
                    //return 0;
                    return a.getArtist().compareTo(b.getArtist());
                } else if(a.getBpm() < 0) {
                    //return b
                    return 1;
                } else if(b.getBpm() < 0) {
                    //return a
                    return -1;
                } else if(a.getBpm() == b.getBpm()) {
                    return a.getArtist().compareTo(b.getArtist());
                }
                return a.getBpm() - b.getBpm();
            }
        });

        mSongAdapter = new SongAdapter(songList, this);
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

        switch(id) {
            case R.id.action_settings:
                return true;
                 //break;
            case R.id.action_stopmusic:
                //FOR DEBUGGING ONLY
                stopService(playIntent);
                musicService = null;
                System.exit(0);
                break;
            case R.id.action_cleardb:
                clearSavedBpms();
                break;
            case R.id.log_sharedPrefs:
                logSharedPrefs();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    //retrieve song info
    public void getSongList() {
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String selectionMimeType = MediaStore.Files.FileColumns.MIME_TYPE + "=?";
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND " + selectionMimeType;
        String sortOrder = null;
        String[] projection = null;
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("mp3");
        String[] selectionArgsMp3 = new String[] { mimeType };


        Cursor musicCursor = musicResolver.query(musicUri, projection, selection, selectionArgsMp3, sortOrder);

        if(musicCursor != null && musicCursor.moveToFirst()) {
            int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);

            do {
                long id = musicCursor.getLong(idColumn);
                String title = musicCursor.getString(titleColumn);
                String artist = musicCursor.getString(artistColumn);

                //bpm processing
                int bpm = getBpm(id);

                songList.add(new Song(id, title, artist, bpm));
            } while (musicCursor.moveToNext());
        }
        if(musicCursor != null)
            musicCursor.close();
    }

    private int getBpmFromFile(long id) {
        int bpm = -1;
        Uri uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.toString(id));
        try {
            Mp3File file = new Mp3File(getRealPathFromURI(getApplicationContext(), uri));
            if (file.hasId3v2Tag()) {
                ID3v2 id3v2Tag = file.getId3v2Tag();
                bpm = id3v2Tag.getBPM();
                Log.d("MP3AGIC", "Got BPM for track: " + id + ": " + bpm);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bpm;
    }

    private int getBpmFromSharedPrefs(long id) {
        SharedPreferences sp = getApplicationContext().getSharedPreferences(getString(R.string.bpm_sharedprefskey), Context.MODE_PRIVATE);
        int bpm = sp.getInt("" + id, -2);

        Log.d("getBpmFromSharedPrefs", "Got BPM of " + bpm + " for id " + id);

        return bpm;
    }

    private void saveBpmToSharedPrefs(long id, int bpm) {
        SharedPreferences sp = getApplicationContext().getSharedPreferences(getString(R.string.bpm_sharedprefskey), Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putInt("" + id, bpm);
        ed.commit();

        Log.d("SavetoSharedPrefs", "Saved bpm of " + bpm + " for id " + id);
    }

    public int getBpm(long id) {
        int bpm = getBpmFromSharedPrefs(id);

        //Not in sharedprefs
        if(bpm < -1) {
            Log.d("getBPM", "Bpm not in sharedPrefs");
            bpm = getBpmFromFile(id);
            saveBpmToSharedPrefs(id, bpm);
        } else
            Log.d("getBPM", "Bpm in sharedPrefs");

        return bpm;
    }

    public void logSharedPrefs() {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(getString(R.string.bpm_sharedprefskey), Context.MODE_PRIVATE);
        Map<String,?> keys = prefs.getAll();

        for(Map.Entry<String,?> entry : keys.entrySet()){
            Log.d("map values",entry.getKey() + ": " +
                    entry.getValue().toString());
        }
    }

    private void clearSavedBpms() {
        SharedPreferences sp = getApplicationContext().getSharedPreferences(getString(R.string.bpm_sharedprefskey), Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.clear();
        ed.commit();
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

        mSongAdapter.selectNext();
        //controller.show(0);
    }

    public void playPrev() {
        musicService.playPrev();

        if(playbackPaused) {
            setupController();
            playbackPaused = false;
        }

        mSongAdapter.selectPrev();
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

    public String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
