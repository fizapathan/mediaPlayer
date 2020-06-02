package com.example.fizapathan.browserplayer;

/**
 * Created by Fiza Pathan on 5/10/2020.
 */

public class Song {
    private long id;
    private String title;
    private String artist;

    public Song(long songId, String songTitle, String songArtist) {
        id = songId;
        title = songTitle;
        artist = songArtist;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
}
