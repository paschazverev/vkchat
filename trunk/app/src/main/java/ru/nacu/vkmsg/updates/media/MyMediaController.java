package ru.nacu.vkmsg.updates.media;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import ru.android.common.UiTools;
import ru.nacu.vkmsg.R;
import ru.nacu.vkmsg.VKMessenger;

/**
 * @author quadro
 * @since 7/9/12 7:05 PM
 */
public final class MyMediaController extends RelativeLayout implements View.OnClickListener,
        SeekBar.OnSeekBarChangeListener, MediaListener, Runnable {

    private ImageView btn;
    private TextView artist;
    private TextView track;
    private SeekBar bar;

    private String url;
    private int bufferingPosition;
    private int currentPosition;

    @SuppressWarnings("UnusedDeclaration")
    public MyMediaController(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    @SuppressWarnings("UnusedDeclaration")
    public MyMediaController(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    @SuppressWarnings("UnusedDeclaration")
    public MyMediaController(Context context) {
        super(context);
        init(context);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void init(Context context) {
        btn = new ImageView(context);
        btn.setImageResource(R.drawable.audio_play);
        btn.setId(R.id.btn_play);
        final LayoutParams p1 = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p1.topMargin = (int) UiTools.dpToPix(4, context);
        p1.leftMargin = p1.topMargin;
        p1.addRule(ALIGN_PARENT_TOP);
        p1.addRule(ALIGN_PARENT_LEFT);
        addView(btn, p1);

        btn.setOnClickListener(this);

        artist = new TextView(context, null, R.style.artist);
        artist.setId(R.id.tv_artist);
        artist.setTypeface(Typeface.DEFAULT_BOLD);
        artist.setTextSize(UiTools.dpToPix(10, context));
        artist.setSingleLine();
        artist.setEllipsize(TextUtils.TruncateAt.END);
        final LayoutParams p2 = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p2.topMargin = (int) UiTools.dpToPix(1, context);
        p2.addRule(ALIGN_PARENT_TOP);
        p2.addRule(ALIGN_PARENT_RIGHT);
        p2.addRule(RIGHT_OF, btn.getId());
        p2.leftMargin = p1.topMargin;
        addView(artist, p2);

        track = new TextView(context, null, R.style.track);
        track.setTextSize(UiTools.dpToPix(10, context));
        track.setId(R.id.tv_track);
        track.setSingleLine();
        track.setEllipsize(TextUtils.TruncateAt.END);
        final LayoutParams p3 = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p3.addRule(ALIGN_PARENT_RIGHT);
        p3.addRule(RIGHT_OF, btn.getId());
        p3.addRule(BELOW, artist.getId());
        p3.leftMargin = p1.topMargin;
        addView(track, p3);

        bar = new SeekBar(context);
        bar.setOnSeekBarChangeListener(this);
        bar.setThumb(context.getResources().getDrawable(R.drawable.audio_control));
        bar.setProgressDrawable(context.getResources().getDrawable(R.drawable.seekbar_progress));
        final LayoutParams p4 = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p4.addRule(ALIGN_PARENT_BOTTOM);
        p4.addRule(ALIGN_PARENT_RIGHT);
        p4.addRule(RIGHT_OF, btn.getId());
        p4.addRule(BELOW, track.getId());
        addView(bar, p4);
    }

    public void setTrack(String url, String artist, String track) {
        this.url = url;
        this.artist.setText(artist);
        this.track.setText(track);

        if (url.equals(MediaService.getCurrent())) {
            btn.setImageResource(R.drawable.audio_pause);
            MediaService.setListener(this);
            currentPosition = MediaService.getCurrentPosition();
            bufferingPosition = 100;
            updatePos();
        } else {
            btn.setImageResource(R.drawable.audio_play);
            currentPosition = 0;
            bufferingPosition = 0;
            updatePos();
        }
    }

    @Override
    public void onClick(View view) {
        if (url.equals(MediaService.getCurrent())) {
            MediaService.stop();
        } else {
            MediaService.startPlayback(url, this);
            btn.setImageResource(R.drawable.audio_pause);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int pos, boolean user) {
        if (user) {
            VKMessenger.getHandler().removeCallbacks(this);
            VKMessenger.getHandler().postDelayed(this, 300);
        }
    }

    private volatile boolean moving = false;

    private void updatePos() {
        final int max = MediaService.getDuration();
        bar.setMax(max);
        if (!moving) {
            bar.setProgress(currentPosition);
        }

        bar.setSecondaryProgress((int) ((double) bufferingPosition / 100d * (double) max));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        moving = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        moving = false;
    }

    @Override
    public void onBufferingProgressListener(int progress) {
        bufferingPosition = progress;
        updatePos();
    }

    @Override
    public void onPlayingProgressListener(int progress) {
        currentPosition = progress;
        updatePos();
    }

    @Override
    public void onStop() {
        btn.setImageResource(R.drawable.audio_play);
        currentPosition = 0;
        updatePos();
    }

    @Override
    public void run() {
        MediaService.seekTo(bar.getProgress());
    }
}
