package com.example.music_zsz;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.ArrayList;
import java.util.List;

public class SongRepository {
    private static SongRepository instance;
    private final MutableLiveData<List<Song>> playlistLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Song> currentSongLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isPlayingLiveData = new MutableLiveData<>(false); // 默认暂停

    private SongRepository() { }

    public static synchronized SongRepository getInstance() {
        if (instance == null) {
            instance = new SongRepository();
        }
        return instance;
    }

    public LiveData<List<Song>> getPlaylist() {
        return playlistLiveData;
    }

    public void setPlaylist(List<Song> songs) {
        playlistLiveData.setValue(songs);
    }

    public void addSong(Song song) {
        List<Song> list = playlistLiveData.getValue();
        if (list == null) {
            list = new ArrayList<>();
        }
        list.add(song);
        playlistLiveData.setValue(list);
    }

    public LiveData<Song> getCurrentSong() {
        return currentSongLiveData;
    }

    public void setCurrentSong(Song song) {
        currentSongLiveData.setValue(song);
    }

    public void toggleLikeForCurrentSong() {
        Song song = currentSongLiveData.getValue();
        if (song != null) {
            song.setLike(!song.getLike());
            currentSongLiveData.setValue(song);
        }
    }


    public LiveData<Boolean> getIsPlayingLiveData() {
        return isPlayingLiveData;
    }

    public void setIsPlaying(boolean isPlaying) {
        isPlayingLiveData.setValue(isPlaying);
    }
}
