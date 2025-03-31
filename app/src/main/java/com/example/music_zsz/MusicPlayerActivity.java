package com.example.music_zsz;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import androidx.palette.graphics.Palette;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.tencent.mmkv.MMKV;

import java.util.ArrayList;
import java.util.List;

public class MusicPlayerActivity extends AppCompatActivity {

    private MusicService musicService;
    private boolean serviceBound = false;
    private Handler handler = new Handler();
    private ObjectAnimator animator;
    private ViewPager2 viewPager;
    private MusicPagerAdapter pagerAdapter;

    private boolean isFavorited = false;


    private ImageView coverImageView, playModel, prevImageView, playPauseImageView, nextImageView, itemlist, closeButton, likeImageView;
    private TextView songNameTextView, singerTextView, tvCurrentTime, tvTotalTime;
    private SeekBar seekBar;

    // 播放进度更新任务
    private Runnable updateProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if (serviceBound && musicService != null && musicService.isPlaying()) {
                int currentPos = musicService.getCurrentPosition();
                seekBar.setProgress(currentPos);
                tvCurrentTime.setText(formatTime(currentPos));
            }
            handler.postDelayed(this, 1000);
        }
    };


    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            musicService = binder.getService();
            serviceBound = true;
            List<Song> currentList = SongRepository.getInstance().getPlaylist().getValue();
            if (currentList != null) {
                musicService.setSongList(currentList);
                Log.d("setsonglist", "success in onServiceConnected");
            }
            initMusicPlayer();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        viewPager = findViewById(R.id.viewPager);
        pagerAdapter = new MusicPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        initCommonViews();
        setClickListeners();


        Intent serviceIntent = new Intent(this, MusicService.class);



        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);


        SongRepository.getInstance().getCurrentSong().observe(this, newSong -> {
            if (newSong != null) {
                updateUIForSong(newSong);

                Log.d("MusicPlayerActivity", "当前播放歌曲更新：" + newSong.getName());
            }else{
                finish();
            }
        });


        SongRepository.getInstance().getIsPlayingLiveData().observe(this, isPlaying -> {
            if (isPlaying) {
                playPauseImageView.setImageResource(R.drawable.ic_pause);
            } else {
                playPauseImageView.setImageResource(R.drawable.ic_play);
            }
        });

    }






    private void initCommonViews() {
        coverImageView = findViewById(R.id.coverImageView);
        songNameTextView = findViewById(R.id.songNameTextView);
        singerTextView = findViewById(R.id.singerTextView);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        seekBar = findViewById(R.id.seekBar);
        playModel = findViewById(R.id.playModel);
        prevImageView = findViewById(R.id.prevImageView);
        playPauseImageView = findViewById(R.id.playPauseImageView);
        nextImageView = findViewById(R.id.nextImageView);
        likeImageView  =findViewById(R.id.likeImageView);
        itemlist = findViewById(R.id.itemlist);
        closeButton = findViewById(R.id.close);
    }


    private void setClickListeners() {



        if (closeButton != null) {
            closeButton.setOnClickListener(v -> {
                finish();
                overridePendingTransition(0, R.anim.anim_exit_bottom);
            });
        }



        playModel.setOnClickListener(v -> {
            if (serviceBound && musicService != null) {
                MusicService.PlayMode currentMode = musicService.getPlayMode();
                switch (currentMode) {
                    case LOOP:
                        musicService.setPlayMode(MusicService.PlayMode.SINGLE);
                        playModel.setImageResource(R.drawable.ic_play03); // 更新图标
                        Toast.makeText(this, "单曲循环", Toast.LENGTH_SHORT).show();
                        break;
                    case SINGLE:
                        musicService.setPlayMode(MusicService.PlayMode.SHUFFLE);
                        playModel.setImageResource(R.drawable.ic_play02);
                        Toast.makeText(this, "随机播放", Toast.LENGTH_SHORT).show();
                        break;
                    case SHUFFLE:
                        musicService.setPlayMode(MusicService.PlayMode.LOOP);
                        playModel.setImageResource(R.drawable.ic_play01);
                        Toast.makeText(this, "顺序播放", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });


        prevImageView.setOnClickListener(v -> {
            if (serviceBound && musicService != null) {
                musicService.playPrevious();
            }
        });



        playPauseImageView.setOnClickListener(v -> {
            if (serviceBound && musicService != null) {
                musicService.togglePlayPause();

            }
        });


        nextImageView.setOnClickListener(v -> {
            if (serviceBound && musicService != null) {
                musicService.playNext();
            }
        });

        likeImageView.setOnClickListener(v -> {
            MMKV kv = MMKV.defaultMMKV();
            // musicUrl作为 key
            String key = "favorite_" + musicService.getCurrentlyPlayingSong().getMusicUrl();


            if (!isFavorited) {

                Animator animator = AnimatorInflater.loadAnimator(this, R.animator.favorite_animator);
                animator.setTarget(likeImageView);
                animator.start();

                isFavorited = true;
                likeImageView.setImageResource(R.drawable.ic_liked);

                kv.encode(key, true);
            } else {

                Animator animator = AnimatorInflater.loadAnimator(this, R.animator.unfavorite_animator);
                animator.setTarget(likeImageView);
                animator.start();

                isFavorited = false;
                likeImageView.setImageResource(R.drawable.ic_like);

                kv.encode(key, false);
            }
        });



        itemlist.setOnClickListener(v -> {

            if (musicService != null) {
                // 打开BottomSheet
                MusicListDialog dialog = new MusicListDialog(musicService);
                dialog.show(getSupportFragmentManager(), "MusicQueueDialog");
            } else {
                Toast.makeText(MusicPlayerActivity.this, "音乐服务未启动", Toast.LENGTH_SHORT).show();
            }
        });





        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            boolean userSeeking = false;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    tvCurrentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                userSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (serviceBound && musicService != null) {
                    musicService.seekTo(seekBar.getProgress());
                }
                userSeeking = false;
            }
        });
    }

    public void initViewPager(){
        viewPager = findViewById(R.id.viewPager);
        pagerAdapter = new MusicPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
    }


    private void initMusicPlayer() {


        Song currentSong = SongRepository.getInstance().getCurrentSong().getValue();
        if (currentSong == null) {
            Toast.makeText(this, "尚未选择歌曲", Toast.LENGTH_SHORT).show();
            return;
        }


        updateUIForSong(currentSong);


        if (musicService != null) {

            int duration = musicService.getDuration();
            seekBar.setMax(duration);
            tvTotalTime.setText(formatTime(duration));

            int currentPos = musicService.getCurrentPosition();
            seekBar.setProgress(currentPos);
            tvCurrentTime.setText(formatTime(currentPos));
        }


        handler.post(updateProgressRunnable);
    }

    private void updateUIForSong(Song song) {
        MMKV kv = MMKV.defaultMMKV();
        String key = "favorite_" + song.getMusicUrl();
        boolean fav = kv.decodeBool(key, false);
        song.setLike(fav);
        isFavorited = fav;

        songNameTextView.setText(song.getName());
        singerTextView.setText(song.getSinger());

        if (fav) {
            likeImageView.setImageResource(R.drawable.ic_liked);
        } else {
            likeImageView.setImageResource(R.drawable.ic_like);
        }



        ConstraintLayout Layout = findViewById(R.id.activity_music_player);
        Glide.with(this)
                .asBitmap()
                .load(song.getCoverUrl())
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable com.bumptech.glide.request.transition.Transition<? super Bitmap> transition) {
                        Palette.from(resource).generate(palette -> {
                            int dominantColor = palette.getDominantColor(Color.BLACK);
                            Layout.setBackgroundColor(dominantColor);
                        });
                    }
                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) { }
                });
    }

    public MusicService getMusicService() {
        return musicService;
    }





    private String formatTime(int millis) {
        int minutes = millis / 60000;
        int seconds = (millis % 60000) / 1000;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
        handler.removeCallbacks(updateProgressRunnable);
    }
}
