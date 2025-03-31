package com.example.music_zsz;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MusicListDialog extends BottomSheetDialogFragment {

    private RecyclerView rvMusicQueue;
    private TextView tvPlayMode, tvMusicCount;
    private ImageView ivPlayMode;
    private MusicListAdapter adapter;

    private MusicService musicService;

    public MusicListDialog(MusicService service) {
        this.musicService = service;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_music_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ivPlayMode = view.findViewById(R.id.ivPlayMode);
        tvPlayMode = view.findViewById(R.id.tvPlayMode);
        tvMusicCount = view.findViewById(R.id.tvMusicCount);
        rvMusicQueue = view.findViewById(R.id.rvMusicQueue);


        MusicService.PlayMode mode = musicService.getPlayMode();
        updatePlayModeText(mode);


        List<Song> songs = SongRepository.getInstance().getPlaylist().getValue();
        if (songs == null) songs = new ArrayList<>();
        tvMusicCount.setText("(" + songs.size() + ")");


        adapter = new MusicListAdapter(songs, musicService);
        rvMusicQueue.setLayoutManager(new LinearLayoutManager(getContext()));
        rvMusicQueue.setAdapter(adapter);


        adapter.setOnItemClickListener((song, position) -> {
            if (musicService != null) {
                musicService.playSongAt(position);
            }
        });


        adapter.setOnItemDeleteClickListener((song, position) -> {
            removeSong(song, position);
        });


    }

    private void removeSong(Song song, int position) {

        List<Song> currentList = SongRepository.getInstance().getPlaylist().getValue();
        if (currentList == null) return;

        currentList.remove(position);
        SongRepository.getInstance().setPlaylist(currentList);
        musicService.setSongList(currentList);


        adapter.updateData(currentList);
        tvMusicCount.setText("(" + currentList.size() + ")");


        Song currentSong = SongRepository.getInstance().getCurrentSong().getValue();
        if (currentSong != null
                && currentSong.getMusicUrl().equals(song.getMusicUrl())) {


            if (currentList.isEmpty()) {

                SongRepository.getInstance().setCurrentSong(null);
                SongRepository.getInstance().setIsPlaying(false);
                musicService.clearSongListAndStop();

                dismiss();

            } else {

                int nextIndex = 0;
                switch (musicService.getPlayMode()) {
                    case LOOP:
                    case SINGLE:

                        nextIndex = position >= currentList.size() ? 0 : position;
                        break;
                    case SHUFFLE:
                        nextIndex = new Random().nextInt(currentList.size());
                        break;
                }
                musicService.playSongAt(nextIndex);
            }
        }
    }

    private void updatePlayModeText(MusicService.PlayMode mode) {
        switch (mode) {
            case LOOP:
                tvPlayMode.setText("顺序播放");
                ivPlayMode.setImageResource(R.drawable.ic_play_grey_01);
                break;
            case SINGLE:
                tvPlayMode.setText("单曲循环");
                ivPlayMode.setImageResource(R.drawable.ic_play_grey_02);
                break;
            case SHUFFLE:
                tvPlayMode.setText("随机播放");
                ivPlayMode.setImageResource(R.drawable.ic_play_grey_03);
                break;
        }
    }
}
