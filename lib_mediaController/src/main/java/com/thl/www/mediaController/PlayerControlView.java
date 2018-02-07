package com.thl.www.mediaController;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class PlayerControlView extends FrameLayout {

    public static final int DEFAULT_FAST_REWIND_MS = 5_000;
    public static final int DEFAULT_FAST_FORWARD_MS = 15_000;
    public static final int DEFAULT_SHOW_TIMEOUT_MS = 5_000;

    private final ViewHolder viewHolder;

    boolean alwaysShow;

    private MediaController.MediaPlayerControl player;
    private boolean showing;
    private boolean dragging;
    private int fastRewindMs;
    private int fastForwardMs;
    private int showTimeoutMs;
    public LinearLayout llt_top;
    public LinearLayout llt_bottom;
    private TextView tv_time;

    private OnClickListener nextListener, prevListener;
    private OnClickListener backListener, shareListener, smallListener;
    private OnVisibilityChangedListener onVisibilityChangedListener;

    private Runnable updateProgressRunnable = new Runnable() {
        @Override
        public void run() {
            int pos = updateProgress();
            if (!dragging && showing && player != null && player.isPlaying()) {
                postDelayed(updateProgressRunnable, 1000 - (pos % 1000));
            }
        }
    };
    private Runnable hideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    public PlayerControlView(Context context) {
        this(context, null);
    }

    public PlayerControlView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PlayerControlView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(getContext(), R.layout.player_control_view, this);
        llt_top = (LinearLayout) findViewById(R.id.llt_top);
        tv_time = (TextView) findViewById(R.id.tv_time);
        llt_bottom = (LinearLayout) findViewById(R.id.llt_bottom);
        mBatteryView = (BatteryView) findViewById(R.id.mBatteryView);
        viewHolder = new ViewHolder(this);
        fastRewindMs = DEFAULT_FAST_REWIND_MS;
        fastForwardMs = DEFAULT_FAST_FORWARD_MS;
        showTimeoutMs = DEFAULT_SHOW_TIMEOUT_MS;
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.PlayerControlView, 0, 0);
            fastRewindMs = a.getInt(R.styleable.PlayerControlView_pcv_fast_rewind_ms, DEFAULT_FAST_REWIND_MS);
            fastForwardMs = a.getInt(R.styleable.PlayerControlView_pcv_fast_forward_ms, DEFAULT_FAST_FORWARD_MS);
            showTimeoutMs = a.getInt(R.styleable.PlayerControlView_pcv_show_timeout_ms, DEFAULT_SHOW_TIMEOUT_MS);
            alwaysShow = a.getBoolean(R.styleable.PlayerControlView_pcv_always_show, false);
            a.recycle();
        }
        if (isInEditMode()) {
            return;
        }
        ComponentListener componentListener = new ComponentListener();
        viewHolder.pausePlayButton.setOnClickListener(componentListener);
        viewHolder.fastForwardButton.setOnClickListener(componentListener);
        viewHolder.fastRewindButton.setOnClickListener(componentListener);
        viewHolder.skipPrevButton.setOnClickListener(componentListener);
        viewHolder.skipNextButton.setOnClickListener(componentListener);
        viewHolder.iv_share.setOnClickListener(componentListener);
        viewHolder.iv_back.setOnClickListener(componentListener);
        viewHolder.llt_small.setOnClickListener(componentListener);
        viewHolder.seekBar.setOnSeekBarChangeListener(componentListener);
        viewHolder.seekBar.setMax(1000);

        Drawable pauseDrawable = toStateListDrawable(viewHolder.pausePlayButton.getPauseDrawable());
        viewHolder.pausePlayButton.setPauseDrawable(pauseDrawable);
        Drawable playDrawable = toStateListDrawable(viewHolder.pausePlayButton.getPlayDrawable());
        viewHolder.pausePlayButton.setPlayDrawable(playDrawable);
        viewHolder.fastForwardButton.setImageDrawable(toStateListDrawable(viewHolder.fastForwardButton.getDrawable()));
        viewHolder.fastRewindButton.setImageDrawable(toStateListDrawable(viewHolder.fastRewindButton.getDrawable()));
        viewHolder.skipNextButton.setImageDrawable(toStateListDrawable(viewHolder.skipNextButton.getDrawable()));
        viewHolder.skipPrevButton.setImageDrawable(toStateListDrawable(viewHolder.skipPrevButton.getDrawable()));

        viewHolder.skipNextButton.setVisibility(View.INVISIBLE);
        viewHolder.skipPrevButton.setVisibility(View.INVISIBLE);

        hide();
        initBatteryReceiver(context);
    }

    protected Drawable toStateListDrawable(Drawable drawable) {
        int drawableColor = ContextCompat.getColor(getContext(), android.R.color.white);
        return Util.createStateListDrawable(drawable, drawableColor);
    }

    public void setPlayer(MediaController.MediaPlayerControl player) {
        this.player = player;
        updatePausePlayImage();
    }

    public void attach(Activity activity) {
        ViewGroup rootView = (ViewGroup) activity.findViewById(android.R.id.content);
        attach(rootView);
    }

    public void attach(ViewGroup rootView) {
        rootView.removeView(this);
        if (rootView instanceof RelativeLayout) {
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
            rootView.addView(this, layoutParams);
        } else {
            LayoutParams layoutParams = new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM
            );
            rootView.addView(this, layoutParams);
        }
    }

    public void show() {
        show(showTimeoutMs);
    }

    void show(int showTimeoutMs) {
        tv_time.setText(Util.getNowTime());
        showing = true;
        if (onVisibilityChangedListener != null) {
            onVisibilityChangedListener.onShown(this);
        }
        setVisibility(VISIBLE);
        if (llt_top.getVisibility() == GONE) {
            llt_top.clearAnimation();
            llt_top.setVisibility(VISIBLE);
            Animation animationBottom = AnimationUtils.loadAnimation(getContext(), R.anim.anim_enter_from_top);
            llt_top.startAnimation(animationBottom);

            llt_bottom.clearAnimation();
            llt_bottom.setVisibility(VISIBLE);
            Animation animationBottom2 = AnimationUtils.loadAnimation(getContext(), R.anim.anim_enter_from_bottom);
            llt_bottom.startAnimation(animationBottom2);
        }

        setFocusable(true);
        setFocusableInTouchMode(true);
        setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        requestFocus();

        updateProgress();
        viewHolder.pausePlayButton.requestFocus();
        disableUnsupportedButtons();
        updatePausePlayImage();

        removeCallbacks(updateProgressRunnable);
        post(updateProgressRunnable);

        removeCallbacks(hideRunnable);
        if (!alwaysShow) {
            postDelayed(hideRunnable, showTimeoutMs);
        }
    }

    public boolean isShowing() {
        return showing;
    }

    public void hide() {
        showing = false;
        if (onVisibilityChangedListener != null) {
            onVisibilityChangedListener.onHidden(this);
        }
        removeCallbacks(hideRunnable);
        removeCallbacks(updateProgressRunnable);

        if (llt_top.getVisibility() == VISIBLE) {
            llt_top.clearAnimation();
            llt_top.setVisibility(GONE);
            Animation animationBottom = AnimationUtils.loadAnimation(getContext(), R.anim.anim_exit_from_top);
            llt_top.startAnimation(animationBottom);

            llt_bottom.clearAnimation();
            llt_bottom.setVisibility(GONE);
            Animation animationBottom2 = AnimationUtils.loadAnimation(getContext(), R.anim.anim_exit_from_bottom);
            llt_bottom.startAnimation(animationBottom2);
            animationBottom2.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    setVisibility(GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
        }
    }

    public void toggleVisibility() {
        if (showing) {
            hide();
        } else {
            show();
        }
    }

    private int updateProgress() {
        if (dragging || player == null) {
            return 0;
        }
        updatePausePlayImage();
        int currentTime = player.getCurrentPosition();
        int totalTime = player.getDuration();
        if (totalTime > 0) {
            long position = 1000L * currentTime / totalTime;
            viewHolder.seekBar.setProgress((int) position);
        }
        int percent = player.getBufferPercentage();
        viewHolder.seekBar.setSecondaryProgress(percent * 10);

        viewHolder.currentTimeText.setText(Util.formatTime(currentTime));
        viewHolder.totalTimeText.setText(Util.formatTime(totalTime));
        return currentTime;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (player == null) {
            return super.dispatchKeyEvent(event);
        }
        final boolean uniqueDown = event.getRepeatCount() == 0 && event.getAction() == KeyEvent.ACTION_DOWN;

        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_HEADSETHOOK:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_SPACE:
                if (uniqueDown) {
                    doPauseResume();
                    show();
                    viewHolder.pausePlayButton.requestFocus();
                }
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                if (uniqueDown && !player.isPlaying()) {
                    player.start();
                    show();
                }
                return true;
            case KeyEvent.KEYCODE_MEDIA_STOP:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                if (uniqueDown && player.isPlaying()) {
                    player.pause();
                    show();
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
            case KeyEvent.KEYCODE_CAMERA:
            case KeyEvent.KEYCODE_MENU:
                return super.dispatchKeyEvent(event);
            case KeyEvent.KEYCODE_BACK:
                if (alwaysShow) {
                    return super.dispatchKeyEvent(event);
                }
                if (uniqueDown) {
                    hide();
                }
                return true;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                if (player.canSeekForward()) {
                    player.seekTo(fastForwardMs);
                    show();
                }
                return true;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                if (player.canSeekForward()) {
                    player.seekTo(fastRewindMs);
                    show();
                }
                return true;
            default:
                break;
        }

        show();
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(updateProgressRunnable);
        removeCallbacks(hideRunnable);
    }

    private void updatePausePlayImage() {
        if (player == null) {
            return;
        }
        viewHolder.pausePlayButton.toggleImage(player.isPlaying());
    }

    private void doPauseResume() {
        if (player == null) {
            return;
        }
        if (player.isPlaying()) {
            player.pause();
        } else {
            player.start();
        }
        updatePausePlayImage();
    }

    @Override
    public void setEnabled(boolean enabled) {
        viewHolder.pausePlayButton.setEnabled(enabled);
        viewHolder.fastForwardButton.setEnabled(enabled);
        viewHolder.fastRewindButton.setEnabled(enabled);
        viewHolder.skipNextButton.setEnabled(enabled && nextListener != null);
        viewHolder.skipPrevButton.setEnabled(enabled && prevListener != null);

        viewHolder.iv_share.setEnabled(enabled && shareListener != null);
        viewHolder.iv_back.setEnabled(enabled && backListener != null);
        viewHolder.llt_small.setEnabled(enabled && smallListener != null);

        viewHolder.seekBar.setEnabled(enabled);
        disableUnsupportedButtons();
        super.setEnabled(enabled);
    }

    private void disableUnsupportedButtons() {
        if (player == null) {
            return;
        }
        if (!player.canPause()) {
            viewHolder.pausePlayButton.setEnabled(false);
        }
        if (!player.canSeekBackward()) {
            viewHolder.fastRewindButton.setEnabled(false);
        }
        if (!player.canSeekForward()) {
            viewHolder.fastForwardButton.setEnabled(false);
        }
        if (!player.canSeekBackward() && !player.canSeekForward()) {
            viewHolder.seekBar.setEnabled(false);
        }
    }

    public void setPrevListener(@Nullable OnClickListener prevListener) {
        this.prevListener = prevListener;
        viewHolder.skipPrevButton.setVisibility(prevListener == null ? View.INVISIBLE : View.VISIBLE);
    }

    public void setNextListener(@Nullable OnClickListener nextListener) {
        this.nextListener = nextListener;
        viewHolder.skipNextButton.setVisibility(nextListener == null ? View.INVISIBLE : View.VISIBLE);
    }

    public void setBackListener(@Nullable OnClickListener backListener) {
        this.backListener = backListener;
        viewHolder.iv_back.setVisibility(backListener == null ? View.INVISIBLE : View.VISIBLE);
    }

    public void setShareListener(@Nullable OnClickListener shareListener) {
        this.shareListener = shareListener;
        viewHolder.iv_share.setVisibility(shareListener == null ? View.INVISIBLE : View.VISIBLE);
    }

    public void setSmallListener(@Nullable OnClickListener smallListener) {
        this.smallListener = smallListener;
        viewHolder.llt_small.setVisibility(smallListener == null ? View.INVISIBLE : View.VISIBLE);
    }

    public void setFastRewindMs(int fastRewindMs) {
        this.fastRewindMs = fastRewindMs;
    }

    public void setFastForwardMs(int fastForwardMs) {
        this.fastForwardMs = fastForwardMs;
    }

    public void setShowTimeoutMs(int showTimeoutMs) {
        this.showTimeoutMs = showTimeoutMs;
    }

    public void setAlwaysShow(boolean alwaysShow) {
        this.alwaysShow = alwaysShow;
        if (alwaysShow) {
            removeCallbacks(hideRunnable);
        }
    }

    public void setOnVisibilityChangedListener(OnVisibilityChangedListener listener) {
        this.onVisibilityChangedListener = listener;
    }

    public ViewHolder getViewHolder() {
        return viewHolder;
    }

    public MediaController getMediaControllerWrapper() {
        return new MediaControllerWrapper(this);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return PlayerControlView.class.getName();
    }

    public interface OnVisibilityChangedListener {
        void onShown(PlayerControlView view);

        void onHidden(PlayerControlView view);
    }

    public static final class ViewHolder {

        public final RelativeLayout controlsBackground;
        public final SeekBar seekBar;
        public final TextView totalTimeText;
        public final TextView currentTimeText;
        public final PausePlayButton pausePlayButton;
        public final ImageButton fastForwardButton;
        public final ImageButton fastRewindButton;
        public final ImageButton skipNextButton;
        public final ImageButton skipPrevButton;

        public final ImageView iv_share;
        public final ImageView iv_back;
        public final View llt_small;
        public final TextView tv_title;

        private ViewHolder(View view) {
            controlsBackground = (RelativeLayout) view.findViewById(R.id.controls_background);
            pausePlayButton = (PausePlayButton) view.findViewById(R.id.pause_play);
            fastForwardButton = (ImageButton) view.findViewById(R.id.fast_forward);
            fastRewindButton = (ImageButton) view.findViewById(R.id.fast_rewind);
            skipNextButton = (ImageButton) view.findViewById(R.id.skip_next);
            skipPrevButton = (ImageButton) view.findViewById(R.id.skip_previous);
            seekBar = (SeekBar) view.findViewById(R.id.seek_bar);
            totalTimeText = (TextView) view.findViewById(R.id.total_time_text);
            currentTimeText = (TextView) view.findViewById(R.id.current_time_text);

            tv_title = (TextView) view.findViewById(R.id.tv_title);
            iv_share = (ImageView) view.findViewById(R.id.iv_share);
            iv_back = (ImageView) view.findViewById(R.id.iv_back);
            llt_small = view.findViewById(R.id.llt_small);

        }
    }

    private final class ComponentListener implements SeekBar.OnSeekBarChangeListener, OnClickListener {

        @Override
        public void onClick(View v) {
            if (player == null) {
                return;
            }
            if (v == viewHolder.pausePlayButton) {
                doPauseResume();
            } else if (v == viewHolder.fastRewindButton) {
                int position = player.getCurrentPosition();
                position -= fastRewindMs;
                player.seekTo(position);
                updateProgress();
            } else if (v == viewHolder.fastForwardButton) {
                int position = player.getCurrentPosition();
                position += fastForwardMs;
                player.seekTo(position);
                updateProgress();
            } else if (v == viewHolder.skipPrevButton) {
                if (prevListener != null) {
                    prevListener.onClick(v);
                }
            } else if (v == viewHolder.skipNextButton) {
                if (nextListener != null) {
                    nextListener.onClick(v);
                }
            } else if (v == viewHolder.iv_back) {
                if (backListener != null) {
                    backListener.onClick(v);
                }
            } else if (v == viewHolder.iv_share) {
                if (shareListener != null) {
                    shareListener.onClick(v);
                }
            } else if (v == viewHolder.llt_small) {
                if (smallListener != null) {
                    smallListener.onClick(v);
                }
            }
            show();
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (!fromUser || player == null) {
                return;
            }
            long duration = player.getDuration();
            long newPosition = (duration * progress) / 1000L;
            player.seekTo((int) newPosition);
            viewHolder.currentTimeText.setText(Util.formatTime((int) newPosition));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            show();
            dragging = true;
            removeCallbacks(hideRunnable);
            removeCallbacks(updateProgressRunnable);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            dragging = false;
            show();
            post(updateProgressRunnable);
        }
    }


    protected BatteryView mBatteryView;
    private BatteryReceiver batteryReceiver;

    private void initBatteryReceiver(Context context) {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        batteryReceiver = new BatteryReceiver();
        if (context != null) {
            context.registerReceiver(batteryReceiver, filter);
        }
    }

    public void unRegisterBatteryReceiver(Context mContext) {
        if (batteryReceiver != null && mContext != null) {
            try {
                mContext.unregisterReceiver(batteryReceiver);
                batteryReceiver = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public class BatteryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int current = intent.getExtras().getInt("level");// 获得当前电量
            int total = intent.getExtras().getInt("scale");// 获得总电量
            int percent = current * 100 / total;
            mBatteryView.setBatteryValue(percent);
        }
    }
}