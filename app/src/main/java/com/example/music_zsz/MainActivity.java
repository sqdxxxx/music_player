package com.example.music_zsz;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.music_zsz.databinding.ActivityMainBinding;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private MainRecyclerAdapter mainAdapter;

    private View  floatingView;
    private ImageView floatingCover, floatingPlayPause, floatingPlaylist;
    private TextView floatingSongName, floatingSingerName;

    private int currentPage = 1;
    private boolean isLoading = false;

    private Handler bannerHandler = new Handler();
    private Runnable bannerRunnable;

    private MusicService musicService;
    private boolean serviceBound = false;

    public MusicService getMusicService() {
        return serviceBound ? musicService : null;
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            musicService = binder.getService();
            serviceBound = true;

            List<Song> currentList = SongRepository.getInstance().getPlaylist().getValue();
            if (currentList != null) {
                musicService.setSongList(currentList);
                Log.d("setsonglist", "success in MainActivity Service");
            }

            Log.d("MainActivity", "MusicService绑定成功");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            Log.d("MainActivity", "MusicService解绑");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);


        mainAdapter = new MainRecyclerAdapter(this, bannerHandler);


        binding.mainRecyclerView.setAdapter(mainAdapter);
        binding.mainRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        Intent serviceIntent = new Intent(this, MusicService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // 绑定 MusicService
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        binding.mainRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                int lastVisiblePosition = lm.findLastCompletelyVisibleItemPosition();
                int totalCount = mainAdapter.getItemCount();
                // 当滑到最后一个条目时，执行加载更多
                if (!isLoading && lastVisiblePosition == totalCount - 1) {
                    loadMoreData();
                }
            }
        });


        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            binding.swipeRefreshLayout.postDelayed(() -> {
                binding.swipeRefreshLayout.setRefreshing(false);
                currentPage = 1;
                mainAdapter.clearAll(); // 清空所有模块
                loadServerData(currentPage);
                Toast.makeText(MainActivity.this, "刷新成功", Toast.LENGTH_SHORT).show();
            }, 1000);
        });

        // 第一次加载第一页
        loadServerData(currentPage);
        initFloatingViews();
        setClickListeners();

        SongRepository.getInstance().getCurrentSong().observe(this, newSong -> {
            if (newSong != null) {
                floatingView.setVisibility(View.VISIBLE);
                updateUIForFloating(newSong);
                Log.d("MainActivity", "当前播放歌曲更新：" + newSong.getName());
            }else{
                floatingView.setVisibility(View.GONE);
            }
        });

        SongRepository.getInstance().getIsPlayingLiveData().observe(MainActivity.this, isPlaying -> {
            if (isPlaying) {
                floatingPlayPause.setImageResource(R.drawable.ic_black_pause);
            } else {
                floatingPlayPause.setImageResource(R.drawable.ic_black_play);
            }
        });


    }

    private void initFloatingViews(){
        floatingView = findViewById(R.id.floatingView);
        floatingCover = floatingView.findViewById(R.id.floatingCover);
        floatingSongName = floatingView.findViewById(R.id.floatingSongName);
        floatingSingerName = floatingView.findViewById(R.id.floatingSingerName);
        floatingPlayPause = floatingView.findViewById(R.id.floatingPlayButton);
        floatingPlaylist = floatingView.findViewById(R.id.floatingPlaylistButton);
    }

    private void setClickListeners() {
        floatingView.setOnClickListener(v -> {
            Song currentSong = SongRepository.getInstance().getCurrentSong().getValue();
            if (currentSong == null) {
                Toast.makeText(MainActivity.this, "当前暂无播放歌曲", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(MainActivity.this, MusicPlayerActivity.class);
            startActivity(intent);
            // 设置页面从底部划入的动画效果（需在 res/anim 下创建 anim_enter_bottom.xml）
            overridePendingTransition(R.anim.anim_enter_bottom, 0);
        });

        floatingPlayPause.setOnClickListener(v -> {
            if (musicService != null) {
                musicService.togglePlayPause();
            }
        });



        floatingPlaylist.setOnClickListener(v -> {
            if (musicService != null) {
                // 打开BottomSheet
                MusicListDialog dialog = new MusicListDialog(musicService);
                dialog.show(getSupportFragmentManager(), "MusicQueueDialog");
            } else {
                Toast.makeText(MainActivity.this, "音乐服务未启动", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void updateUIForFloating(Song song){
        if (song != null) {
            floatingSongName.setText(song.getName());
            floatingSingerName.setText(song.getSinger());
            Glide.with(this)
                    .load(song.getCoverUrl())
                    .circleCrop()
                    .error(R.drawable.ic_02)
                    .into(floatingCover);
        }

    }



    private void loadMoreData() {
        isLoading = true;
        currentPage++;
        loadServerData(currentPage);
    }

    private void loadServerData(int page) {
        //size代表目前有几个模块，刚开始是4个模块，所以page是1，size是4
        int size = page+3;
        String url = "https://hotfix-service-prod.g.mi.com/music/homePage?current=1" + "&size="+ size;

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                runOnUiThread(() -> {
                    isLoading = false;
                    Toast.makeText(MainActivity.this, "请求失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        isLoading = false;
                        Toast.makeText(MainActivity.this, "请求出错：" + response, Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                String json = response.body().string();


                ParseGson parser = new ParseGson(url);
                ArrayList<Song> allSongs = parser.getSonglist(json);


                ArrayList<Song> bannerSongs = new ArrayList<>();
                ArrayList<Song> specialSongs = new ArrayList<>();
                ArrayList<Song> dailySongs = new ArrayList<>();
                ArrayList<Song> popularSongs = new ArrayList<>();

                for (Song song : allSongs) {
                    int style = song.getStyle();
                    switch (style) {
                        case 1: bannerSongs.add(song); break;
                        case 2: specialSongs.add(song); break;
                        case 3: dailySongs.add(song); break;
                        case 4: popularSongs.add(song); break;
                    }
                }

                runOnUiThread(() -> {
                    if (page == 1) {
                        Log.d("init", "page=" + page
                                + ", bannerSongs=" + bannerSongs.size()
                                + ", specialSongs=" + specialSongs.size()
                                + ", dailySongs=" + dailySongs.size()
                                + ", popularSongs=" + popularSongs.size());

                        List<ModuleItem> initModules = new ArrayList<>();


                        initModules.add(new ModuleItem(ModuleItem.TYPE_SEARCH_BAR, null));

                        initModules.add(new ModuleItem(ModuleItem.TYPE_BANNER, bannerSongs));

                        initModules.add(new ModuleItem(ModuleItem.TYPE_SPECIAL, specialSongs));

                        initModules.add(new ModuleItem(ModuleItem.TYPE_DAILY, dailySongs));

                        initModules.add(new ModuleItem(ModuleItem.TYPE_POPULAR, popularSongs));

                        mainAdapter.setModuleItems(initModules);

                        // 仅在首次打开时执行（如果当前播放歌曲为空）
                        if (SongRepository.getInstance().getCurrentSong().getValue() == null) {
                            List<List<Song>> modules = new ArrayList<>();
                            if (!bannerSongs.isEmpty()) modules.add(bannerSongs);
                            if (!specialSongs.isEmpty()) modules.add(specialSongs);
                            if (!dailySongs.isEmpty()) modules.add(dailySongs);
                            if (!popularSongs.isEmpty()) modules.add(popularSongs);

                            if (!modules.isEmpty()) {
                                Random random = new Random();
                                int moduleIndex = random.nextInt(modules.size());
                                List<Song> selectedModule = modules.get(moduleIndex);
                                // 随机选择该模块中的一首作为当前播放歌曲
                                int songIndex = random.nextInt(selectedModule.size());
                                Song selectedSong = selectedModule.get(songIndex);

                                // 将该模块所有歌曲设置到 SongRepository 中
                                for(Song s:selectedModule){
                                    SongRepository.getInstance().addSong(s);
                                }
                                // 设置当前歌曲
                                SongRepository.getInstance().setCurrentSong(selectedSong);

                                // 如果 MusicService 已经绑定，则更新其播放列表，并播放第一首歌
                                if (musicService != null) {
                                    musicService.setSongList(selectedModule);
                                    // 将选中的歌曲添加到列表的最前端并播放
                                    musicService.addSong(selectedSong);
                                    musicService.playSongAt(0);
                                }
                            }
                        }

                    } else {
                        //上拉加载可自定义添加上面模块，我这里写的是添加每日推荐和热门金曲

                        ArrayList<Song> Daily = new ArrayList<>();
                        Daily.addAll(dailySongs);
                        ArrayList<Song> Popular = new ArrayList<>();
                        Popular.addAll(popularSongs);

                        Log.d("add", "page=" + page
                                + ", dailySongs=" + dailySongs.size()
                                + ", popularSongs=" + popularSongs.size());



                        ModuleItem moreDaily = new ModuleItem(ModuleItem.TYPE_DAILY, Daily);
                        ModuleItem morePopular = new ModuleItem(ModuleItem.TYPE_POPULAR, Popular);

                        mainAdapter.addModuleItem(moreDaily);
                        mainAdapter.addModuleItem(morePopular);
                    }

                    isLoading = false;
                });
            }
        });
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
        if (bannerHandler != null && bannerRunnable != null) {
            bannerHandler.removeCallbacks(bannerRunnable);
        }
    }
}
