package com.paulpjryan.bpmplayer;

/**
 * Created by PRyan on 10/14/2015.
 */
public class Song {
    private long id;
    private String title;
    private String artist;
    private int bpm;

    public Song(long id, String title, String artist) {
        this.id=id;
        this.title=title;
        this.artist=artist;
        this.bpm = -1;
    }

    public Song(long id, String title, String artist, int bpm) {
        this.id=id;
        this.title=title;
        this.artist=artist;
        this.bpm = bpm;
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

    public void setBpm(int bpm) {
        this.bpm = bpm;
    }

    public int getBpm() { return bpm; }
}
