package com.example.music_zsz;

import android.os.Parcel;
import android.os.Parcelable;

public class Song {
    private String name;
    private String singer;
    private String cover_url;
    private String music_url;
    private String lyric_url;
    private int style;

    private boolean isLike;
    public Song(String name, String singer, String cover_url, String music_url,String lyric_url,int style) {
        this.name = name;
        this.singer = singer;
        this.cover_url = cover_url;
        this.music_url = music_url;
        this.lyric_url = lyric_url;
        this.style = style;
        this.isLike = false;
    }






    public void setLike(boolean like) {
        isLike = like;
    }

    public int getStyle() {
        return style;
    }

    public String getName() {
        return name;
    }

    public String getSinger() {
        return singer;
    }

    public String getCoverUrl() {
        return cover_url;
    }

    public String getMusicUrl() {
        return music_url;
    }
    public String getLyricUrl() {
        return lyric_url;
    }

    public boolean getLike() {
        return isLike;
    }


}
