package com.example.music_zsz;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {

    private List<Song> bannerSongs;

    public BannerAdapter(List<Song> bannerSongs) {
        this.bannerSongs = bannerSongs;
    }

    // 返回一个较大的数，用于无限循环
    @Override
    public int getItemCount() {
        if (bannerSongs == null || bannerSongs.size() == 0) {
            return 0;
        }
        return Integer.MAX_VALUE;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        int realPosition = position % bannerSongs.size();
        Song song = bannerSongs.get(realPosition);
        Glide.with(holder.itemView)
                .load(song.getCoverUrl())
                .error(R.drawable.ic_02)
                .into(holder.imageView);
        holder.itemView.setOnClickListener(v -> {
            SongRepository.getInstance().addSong(song);
            Toast.makeText(
                    v.getContext(),
                    "将 " + song.getName() + " 添加到音乐列表",
                    Toast.LENGTH_SHORT
            ).show();
        });

        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();

            List<Song> newPlaylist = new ArrayList<>(bannerSongs);
            SongRepository.getInstance().setPlaylist(newPlaylist);

            SongRepository.getInstance().setCurrentSong(song);

            if (context instanceof MainActivity) {
                MainActivity activity = (MainActivity) context;
                MusicService musicService = activity.getMusicService();
                // 假设你在 MainActivity 里写了 getMusicService() 返回绑定的 Service
                if (musicService != null) {
                    // 让 service 使用新列表
                    musicService.setSongList(newPlaylist);

                    // 找到当前歌曲在 newPlaylist 里的位置
                    int index = findIndexInList(song, newPlaylist);
                    // 让 Service 真正开始播放该歌曲
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

    public void updateData(List<Song> newSongs) {
        this.bannerSongs = newSongs;
        notifyDataSetChanged();
    }

    // 返回实际数据个数
    public int getRealCount() {
        return bannerSongs == null ? 0 : bannerSongs.size();
    }

    // 给 BannerViewHolder 调用，用于取真实位置
    public int getRealPosition(int position) {
        if (bannerSongs == null || bannerSongs.size() == 0) {
            return 0;
        }
        return position % bannerSongs.size();
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        BannerViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.bannerImageView);
        }
    }
}

