package com.example.music_zsz;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class LyricAdapter extends RecyclerView.Adapter<LyricAdapter.ViewHolder> {

    private List<LyricLine> lyricLines;

    private int selectedIndex = 0;


    public LyricAdapter(List<LyricLine> lyricLines) {
        this.lyricLines = lyricLines;
    }

    public void setSelectedIndex(int index) {
        selectedIndex = index;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lyric, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LyricLine line = lyricLines.get(position);
        holder.lyricTextView.setText(line.text);

        if (position == selectedIndex) {
            holder.lyricTextView.setTextColor(Color.WHITE);
            holder.lyricTextView.setTextSize(20);
        } else {
            holder.lyricTextView.setTextColor(Color.GRAY);
            holder.lyricTextView.setTextSize(16);
        }
    }

    @Override
    public int getItemCount() {
        return lyricLines.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView lyricTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            lyricTextView = itemView.findViewById(R.id.itemLyricTextView);
        }
    }





        public static class LyricLine {
            public int time;

            public String text;

            public LyricLine(int time, String text) {
                this.time = time;
                this.text = text;
            }
        }

}
