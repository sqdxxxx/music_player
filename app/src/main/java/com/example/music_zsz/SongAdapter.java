package com.example.music_zsz;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private List<Song> songs;
    private MainViewModel viewModel;
    private ArrayList<Song> songWaitingToPlay;
    private int layoutId; // 用于区分不同布局

    public SongAdapter(List<Song> songs, @LayoutRes int layoutId) {
        this.songs = songs;
        this.layoutId = layoutId;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 根据传入的 layoutId 加载不同布局
        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songs.get(position);


        Glide.with(holder.itemView.getContext())
                .load(song.getCoverUrl())
                .error(R.drawable.ic_02)
                .into(holder.coverImageView);

        Log.d("IMAGE", "Loading URL: " + song.getCoverUrl());
        holder.songNameTextView.setText(song.getName());
        holder.singerTextView.setText(song.getSinger());


        holder.playImageView.setOnClickListener(v -> {
            List<Song> playlist = SongRepository.getInstance().getPlaylist().getValue();
            if (playlist == null) {
                playlist = new ArrayList<>();
            }
            boolean found = false;
            for (Song s : playlist) {
                if (s.getMusicUrl().equals(song.getMusicUrl())) {
                    found = true;
                    break;
                }
            }
            if (found) {

                Toast.makeText(
                        v.getContext(),
                        "播放列表中已存在 " + song.getName(),
                        Toast.LENGTH_SHORT
                ).show();
            } else {

                SongRepository.getInstance().addSong(song);
                Toast.makeText(
                        v.getContext(),
                        "已添加 " + song.getName() + " 到播放列表",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });


        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();

            List<Song> newPlaylist = new ArrayList<>(songs);
            SongRepository.getInstance().setPlaylist(newPlaylist);

            SongRepository.getInstance().setCurrentSong(song);

            if (context instanceof MainActivity) {
                MainActivity activity = (MainActivity) context;
                MusicService musicService = activity.getMusicService();

                if (musicService != null) {

                    musicService.setSongList(newPlaylist);


                    int index = findIndexInList(song, newPlaylist);

                    musicService.playSongAt(index);
                }
            }

            Intent intent = new Intent(context, MusicPlayerActivity.class);



            context.startActivity(intent);

            Toast.makeText(context, "正在准备播放：" + song.getName(), Toast.LENGTH_SHORT).show();
        });

    }

    private int findIndexInList(Song target, List<Song> list) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getMusicUrl().equals(target.getMusicUrl())) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getItemCount() {
        return songs != null ? songs.size() : 0;
    }


    public void updateData(List<Song> newSongs) {
        this.songs = newSongs;
        notifyDataSetChanged();
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        ImageView coverImageView;
        ImageView playImageView;
        TextView songNameTextView;
        TextView singerTextView;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            coverImageView = itemView.findViewById(R.id.coverImageView);
            playImageView = itemView.findViewById(R.id.playImageView);
            songNameTextView = itemView.findViewById(R.id.songNameTextView);
            singerTextView = itemView.findViewById(R.id.singerTextView);
        }
    }
}
