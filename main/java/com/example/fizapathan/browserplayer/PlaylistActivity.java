package com.example.fizapathan.browserplayer;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;
import java.lang.invoke.MutableCallSite;
import java.util.ArrayList;
import java.util.HashMap;

import android.os.IBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.view.MenuItem;
import android.view.View;
import android.widget.MediaController.MediaPlayerControl;
import com.example.fizapathan.browserplayer.MusicService.MusicBinder;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

public class PlaylistActivity extends AppCompatActivity implements MediaPlayerControl {

    ArrayList<Song> songList;
    ListView songView;
    private final int REQUEST_CODE_ASK_PERMISSIONS = 123;

    private MusicService musicService;
    private Intent playIntent;
    private boolean musicBound = false;
    private MusicController controller;

    private boolean paused = false, playbackPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        songList = new ArrayList<Song>();

        if (ContextCompat.checkSelfPermission(getBaseContext(),
                READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            //"android.permission.READ_EXTERNAL_STORAGE"
            ActivityCompat.requestPermissions(
                    PlaylistActivity.this,
                    new String[]{READ_EXTERNAL_STORAGE},
                    REQUEST_CODE_ASK_PERMISSIONS);
        } else {
            getSongList();
        }

        SongAdapter songAdapter = new SongAdapter(PlaylistActivity.this, 0, songList);
        songView = (ListView) findViewById(R.id.song_list);

        songView.setAdapter(songAdapter);


        // Set a click listener to play the audio when the list item is clicked on
        songView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                //musicService.setSong(Integer.parseInt(view.getTag().toString()));
                musicService.setSong(position);
                musicService.playSong();

                if(playbackPaused) {
                    setController();
                    playbackPaused = false;
                }
                controller.show(0);
            }
        });

        setController();

    }

    /*
        songList.add(new Song(12, "Muskuraane", "Arijit Singh"));
        songList.add(new Song(13, "Sanam Re", "Arijit Singh"));
        songList.add(new Song(14, "Hua hai aaj pehli baar", "Armaan Malik"));
        songList.add(new Song(15, "Muskuraane", "Arijit Singh"));


        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        byte[] art;

                try {
                    String songImgPath = musicUri.getPath();
                    Log.i("Song Image PAth", ":     " + songImgPath);
                    metadataRetriever.setDataSource(songImgPath, new HashMap<String, String>());
                    art = metadataRetriever.getEmbeddedPicture();
                    Bitmap songImage = BitmapFactory.decodeByteArray(art, 0, art.length);
                } catch (Exception w) {
                    Log.i("PlaylistActivity", "metadataRetriever created havoc");
                    Log.i("Havoc caught", ":    " + w);
                }
                finally {
                    metadataRetriever.release();
                }

     */

    public void getSongList() {
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        Uri albumUri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
        Cursor albumCursor = musicResolver.query(albumUri, null, null, null, null);

        try {
            if (musicCursor != null && musicCursor.moveToFirst()) {
                if(albumCursor != null && albumCursor.moveToFirst()) {
                    //get columns
                    int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                    int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
                    int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                    int path = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART);
                    //add songs to list
                    do {
                        long thisId = musicCursor.getLong(idColumn);
                        String thisTitle = musicCursor.getString(titleColumn);
                        String thisArtist = musicCursor.getString(artistColumn);
                        String thisPath;
                        try {
                            thisPath = albumCursor.getString(path);
                            Log.i("PlaylistActivity", "Song image path" + path);
                        } catch (Exception e) {
                            Log.i("PlaylistActivity", "Song path is null");
                        } finally {
                            thisPath = "R.drawable.bursting_tape";
                            albumCursor.close();
                        }

                        Log.i("PlaylistActivity", "id: " + idColumn + "\ntitle: " + thisTitle + "\nartist: " + thisArtist);

                        songList.add(new Song(thisId, thisTitle, thisArtist));

                    } while (musicCursor.moveToNext());
                }
            }
        } catch (Exception e) {
            songList.add(new Song(1, "unknown song", "unknown artist"));
            Log.i("PlaylistActivity", "Song path is null");
        } finally {
            try {
                if (musicCursor != null && !musicCursor.isClosed())
                    musicCursor.close();
            } catch (Exception e) {
                Log.i("PlaylistActivity", "Exception while closing cursor");
            }
        }
    }

    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            MusicBinder binder = (MusicBinder) service;

            //get service
            musicService = binder.getService();

            //pass list
            musicService.setList(songList);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            musicBound = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getSongList();
                } else {

                }
                return;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(paused) {
            setController();
            paused = false;
        }
    }

    @Override
    protected void onStop() {
        controller.hide();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        stopService(playIntent);
        musicService = null;
        super.onDestroy();
    }

    /*
    MusicPlayback code starts here
    Below methods are used for media controller
     */
    public void setController() {
        //set the controller up
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

    //play next
    private void playNext() {
        musicService.playNext();
        if(playbackPaused) {
            setController();
            playbackPaused = false;
        }
        controller.show(0);
    }

    //play previous
    private void playPrev() {
        musicService.playPrev();
        if(playbackPaused) {
            setController();
            playbackPaused = false;
        }
        controller.show(0);
    }

    @Override
    public void start() {
        musicService.go();
    }

    @Override
    public void pause() {
        playbackPaused = true;
        musicService.pausePlayer();
    }

    @Override
    public int getDuration() {
        if(musicService != null && musicBound && musicService.isPlng())
            return musicService.getDur();
        else
            return 0;
    }

    @Override
    public int getCurrentPosition() {
        if(musicService != null && musicBound && musicService.isPlng())
            return musicService.getPosition();
        else
            return 0;
    }

    @Override
    public void seekTo(int pos) {
        musicService.seek(pos);
    }

    @Override
    public boolean isPlaying() {
        if(musicService != null && musicBound)
            return musicService.isPlng();
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

    /*
        UI methods for menu item selected
         */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_playlist, menu);
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
        }

        return super.onOptionsItemSelected(item);
    }
}