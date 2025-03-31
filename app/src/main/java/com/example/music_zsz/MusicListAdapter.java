package com.example.music_zsz;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import android.widget.TextView;

import androidx.annotation.NonNull;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MusicListAdapter extends RecyclerView.Adapter<MusicListAdapter.QueueViewHolder> {

    private List<Song> songList;
    private final MusicService musicService;

    private OnItemClickListener onItemClickListener;
    private OnItemDeleteClickListener onItemDeleteClickListener;

    public MusicListAdapter(List<Song> songList, MusicService musicService) {
        this.songList = songList;
        this.musicService = musicService;
    }

    @NonNull
    @Override
    public QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_music_list, parent, false);
        return new QueueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QueueViewHolder holder, int position) {
        Song song = songList.get(position);
        holder.tvSongName.setText(song.getName());
        holder.tvSinger.setText(song.getSinger());



        Song currentSong = SongRepository.getInstance().getCurrentSong().getValue();
        if (currentSong != null && currentSong.getMusicUrl().equals(song.getMusicUrl())) {

            holder.itemView.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.blue));
        } else {

            holder.itemView.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), android.R.color.transparent));
        }

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(song, position);
                notifyDataSetChanged();
            }
        });


        holder.ivDelete.setOnClickListener(v -> {
            if (onItemDeleteClickListener != null) {
                onItemDeleteClickListener.onDeleteClick(song, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return songList != null ? songList.size() : 0;
    }

    public void updateData(List<Song> newList) {
        this.songList = newList;
        notifyDataSetChanged();
    }

    // setter for onItemClickListener
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }
    public void setOnItemDeleteClickListener(OnItemDeleteClickListener listener) {
        this.onItemDeleteClickListener = listener;
    }

    // 接口
    public interface OnItemClickListener {
        void onItemClick(Song song, int position);
    }
    public interface OnItemDeleteClickListener {
        void onDeleteClick(Song song, int position);
    }

    static class QueueViewHolder extends RecyclerView.ViewHolder {
        TextView tvSongName, tvSinger;
        ImageView ivDelete;

        public QueueViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSongName = itemView.findViewById(R.id.tvSongName);
            tvSinger = itemView.findViewById(R.id.tvSinger);
            ivDelete = itemView.findViewById(R.id.ivDelete);
        }
    }
}
