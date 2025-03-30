package com.example.music_zsz;

import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LyricFragment extends Fragment {

    private RecyclerView lyricRecyclerView;
    private LyricAdapter lyricAdapter;
    private List<LyricAdapter.LyricLine> lyricLines = new ArrayList<>();
    private Handler lyricHandler = new Handler();
    private Runnable lyricUpdateRunnable;

    public LyricFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lyric, container, false);
        lyricRecyclerView = view.findViewById(R.id.lyricRecyclerView);
        lyricRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        lyricAdapter = new LyricAdapter(lyricLines);
        lyricRecyclerView.setAdapter(lyricAdapter);

    
        SongRepository.getInstance().getCurrentSong().observe(getViewLifecycleOwner(), song -> {
            if (song != null) {
                String lyricUrl = song.getLyricUrl();
                if (!TextUtils.isEmpty(lyricUrl)) {
                    fetchLyrics(lyricUrl);
                } else {
                    lyricLines.clear();
                    lyricAdapter.notifyDataSetChanged();
                }
            }
        });
        return view;
    }


    private void fetchLyrics(String url) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback(){
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if(getActivity()!=null){
                    getActivity().runOnUiThread(() -> {
                        lyricLines.clear();
                        lyricAdapter.notifyDataSetChanged();
                    });
                }
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful() && response.body() != null){
                    final String lyricsText = response.body().string();
                    List<LyricAdapter.LyricLine> parsedLines = parseLyrics(lyricsText);
                    if(getActivity()!=null){
                        getActivity().runOnUiThread(() -> {
                            lyricLines.clear();
                            lyricLines.addAll(parsedLines);
                            lyricAdapter.notifyDataSetChanged();
                        });
                    }
                }
            }
        });
    }

    private List<LyricAdapter.LyricLine> parseLyrics(String content) {
        List<LyricAdapter.LyricLine> list = new ArrayList<>();
        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("[") && line.contains("]")) {
                int closingBracket = line.indexOf("]");
                String timeStr = line.substring(1, closingBracket);
                String text = line.substring(closingBracket + 1).trim();
                int timeMs = parseTimeToMs(timeStr);
                if (timeMs >= 0) {
                    list.add(new LyricAdapter.LyricLine(timeMs, text));
                }
            }
        }
        return list;
    }


    private int parseTimeToMs(String timeStr) {
        try {
            String[] parts = timeStr.split("[:\\.]");
            if (parts.length >= 3) {
                int minutes = Integer.parseInt(parts[0]);
                int seconds = Integer.parseInt(parts[1]);
                int hundredths = Integer.parseInt(parts[2]);
                return minutes * 60000 + seconds * 1000 + hundredths * 10;
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public void onResume() {
        super.onResume();
        startLyricUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopLyricUpdates();
    }

    private void startLyricUpdates() {
        lyricUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (getActivity() instanceof MusicPlayerActivity && !lyricLines.isEmpty()) {
                    MusicService service = ((MusicPlayerActivity) getActivity()).getMusicService();
                    if (service != null) {
                        int currentTime = service.getCurrentPosition();
                        int currentIndex = getCurrentLyricIndex(currentTime);
                        if (currentIndex != lyricAdapter.getSelectedIndex()) {
                            lyricAdapter.setSelectedIndex(currentIndex);
                            lyricAdapter.notifyDataSetChanged();
                           
                            lyricRecyclerView.smoothScrollToPosition(currentIndex);
                        }
                    }
                }
                lyricHandler.postDelayed(this, 500);
            }
        };
        lyricHandler.post(lyricUpdateRunnable);
    }

    private void stopLyricUpdates() {
        if (lyricUpdateRunnable != null) {
            lyricHandler.removeCallbacks(lyricUpdateRunnable);
        }
    }


    private int getCurrentLyricIndex(int currentTime) {
        int index = 0;
        for (int i = 0; i < lyricLines.size(); i++) {
            if (currentTime >= lyricLines.get(i).time) {
                index = i;
            } else {
                break;
            }
        }
        return index;
    }
}
