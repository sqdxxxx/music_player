package com.example.music_zsz;



import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;


public class CoverFragment extends Fragment {

    private FrameLayout rootLayout;
    private ImageView fragmentCoverImageView;
    private ObjectAnimator animator;

    public CoverFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cover, container, false);
        rootLayout = view.findViewById(R.id.coverFragmentRoot);
        fragmentCoverImageView = view.findViewById(R.id.fragmentCoverImageView);

        SongRepository.getInstance().getCurrentSong().observe(getViewLifecycleOwner(), song -> {
            if (song != null) {
                Glide.with(requireContext())
                        .load(song.getCoverUrl())
                        .circleCrop()
                        .error(R.drawable.ic_02)
                        .into(fragmentCoverImageView);



            }
        });

        SongRepository.getInstance().getIsPlayingLiveData().observe(getViewLifecycleOwner(), isPlaying -> {
            if (animator != null) {
                if (isPlaying) {
                    // 如果处于播放状态则恢复动画
                    if (animator.isPaused()) {
                        animator.resume();
                    }
                } else {
                    // 暂停状态下暂停动画
                    if (animator.isRunning()) {
                        animator.pause();
                    }
                }
            }
        });

        startCoverRotation();
        return view;
    }

    private void startCoverRotation() {
        animator = ObjectAnimator.ofFloat(fragmentCoverImageView, "rotation", 0f, 360f);
        animator.setDuration(20000);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.start();
    }

    public void pauseAnimation() {
        if (animator != null && animator.isRunning()) {
            animator.pause();
        }
    }

    public void resumeAnimation() {
        if (animator != null && animator.isPaused()) {
            animator.resume();
        }
    }
}
