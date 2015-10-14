package com.paulpjryan.bpmplayer;

/**
 * Created by PRyan on 10/14/2015.
 */
public class Song {
    private long id;
    private String title;
    private String artist;

    public Song(long id, String title, String artist) {
        this.id=id;
        this.title=title;
        this.artist=artist;
    }

    public long getID() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }
}
