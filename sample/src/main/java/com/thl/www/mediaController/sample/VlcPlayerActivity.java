package com.thl.www.mediaController.sample;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.thl.www.mediaController.BrightnessHelper;
import com.thl.www.mediaController.PlayerControlView;
import com.thl.www.mediaController.ShowChangeLayout;
import com.thl.www.mediaController.VideoGestureRelativeLayout;

import org.videolan.vlc.VlcVideoView;
import org.videolan.vlc.listener.MediaListenerEvent;
import org.videolan.vlc.util.VLCInstance;

public class VlcPlayerActivity extends AppCompatActivity implements VideoGestureRelativeLayout.VideoGestureListener {

    private VlcVideoView mVlcVideoView;

    private final String TAG = "gesturetestm";
    private VideoGestureRelativeLayout ly_VG;
    private ShowChangeLayout scl;
    private AudioManager mAudioManager;
    private int maxVolume = 0, oldVolume = 0;
    private int newProgress = 0, oldProgress = 0;
    private BrightnessHelper mBrightnessHelper;
    private float brightness = 1;
    private Window mWindow;
    private WindowManager.LayoutParams mLayoutParams;
    private PlayerControlView mPlayerControlView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //屏幕常亮
        setFullScreen(this);
        setContentView(R.layout.activity_vlc_player);
        mVlcVideoView = (VlcVideoView) findViewById(R.id.VlcVideoView);

        if (VLCInstance.testCompatibleCPU(this)) {
            Log.i("taohaili", "support   cpu");
        } else {
            Log.i("taohiali", "not support  cpu");
        }


        mVlcVideoView.startPlay("http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4");
        mPlayerControlView = (PlayerControlView) findViewById(R.id.PlayerControlView);

        mPlayerControlView.setNextListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(VlcPlayerActivity.this, "onClick Next", Toast.LENGTH_SHORT).show();
            }
        });

        mPlayerControlView.setPrevListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(VlcPlayerActivity.this, "onClick Prev", Toast.LENGTH_SHORT).show();
            }
        });
        mPlayerControlView.setPlayer(mVlcVideoView);

        ly_VG = (VideoGestureRelativeLayout) findViewById(R.id.ly_VG);
        ly_VG.setVideoGestureListener(this);

        scl = (ShowChangeLayout) findViewById(R.id.scl);

        //初始化获取音量属性
        mAudioManager = (AudioManager) getSystemService(Service.AUDIO_SERVICE);
        maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        //初始化亮度调节
        mBrightnessHelper = new BrightnessHelper(this);

        //下面这是设置当前APP亮度的方法配置
        mWindow = getWindow();
        mLayoutParams = mWindow.getAttributes();
        brightness = mLayoutParams.screenBrightness;
        mVlcVideoView.setMediaListenerEvent(new MediaListenerEvent() {
            @Override
            public void eventBuffing(int i, float v) {

            }

            @Override
            public void eventPlayInit(boolean b) {

            }

            @Override
            public void eventStop(boolean b) {

            }

            @Override
            public void eventError(int i, boolean b) {

            }

            @Override
            public void eventPlay(boolean b) {

            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mVlcVideoView.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        mVlcVideoView.pause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mVlcVideoView.onStop();
    }

    public void setFullScreen(Context context) {
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            WindowManager.LayoutParams params = activity.getWindow().getAttributes();
            params.flags |= 1024;
            activity.getWindow().setAttributes(params);
            activity.getWindow().addFlags(512);
        }
    }


    @Override
    public void onDown(MotionEvent e) {
        //每次按下的时候更新当前亮度和音量，还有进度
        oldProgress = newProgress;
        oldVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        brightness = mLayoutParams.screenBrightness;
        if (brightness == -1) {
            //一开始是默认亮度的时候，获取系统亮度，计算比例值
            brightness = mBrightnessHelper.getBrightness() / 255f;
        }
    }

    @Override
    public void onEndFF_REW(MotionEvent e) {
        int i = newProgress * mVlcVideoView.getDuration() / 100;
        mVlcVideoView.seekTo(i);
    }

    @Override
    public void onVolumeGesture(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        int value = ly_VG.getHeight() / maxVolume;
        int newVolume = (int) ((e1.getY() - e2.getY()) / value + oldVolume);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, AudioManager.FLAG_PLAY_SOUND);
        //要强行转Float类型才能算出小数点，不然结果一直为0
        int volumeProgress = (int) (newVolume / Float.valueOf(maxVolume) * 100);
        if (volumeProgress >= 50) {
            scl.setImageResource(R.drawable.volume_higher_w);
        } else if (volumeProgress > 0) {
            scl.setImageResource(R.drawable.volume_lower_w);
        } else {
            scl.setImageResource(R.drawable.volume_off_w);
        }
        scl.setProgress(volumeProgress);
        scl.show();
    }

    @Override
    public void onBrightnessGesture(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        //下面这是设置当前APP亮度的方法
        float newBrightness = (e1.getY() - e2.getY()) / ly_VG.getHeight();
        newBrightness += brightness;
        if (newBrightness < 0) {
            newBrightness = 0;
        } else if (newBrightness > 1) {
            newBrightness = 1;
        }
        mLayoutParams.screenBrightness = newBrightness;
        mWindow.setAttributes(mLayoutParams);
        scl.setProgress((int) (newBrightness * 100));
        scl.setImageResource(R.drawable.brightness_w);
        scl.show();
    }

    @Override
    public void onFF_REWGesture(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        float offset = e2.getX() - e1.getX();
        //根据移动的正负决定快进还是快退
        if (offset > 0) {
            scl.setImageResource(R.drawable.ff);
            newProgress = (int) (oldProgress + offset / ly_VG.getWidth() * 100);
            if (newProgress > 100) {
                newProgress = 100;
            }
        } else {
            scl.setImageResource(R.drawable.fr);
            newProgress = (int) (oldProgress + offset / ly_VG.getWidth() * 100);
            if (newProgress < 0) {
                newProgress = 0;
            }
        }
        scl.setProgress(newProgress);
        scl.show();
    }

    @Override
    public void onSingleTapGesture(MotionEvent e) {
        if (!mPlayerControlView.isShowing())
            mPlayerControlView.show();
        else
            mPlayerControlView.hide();
    }

    @Override
    public void onDoubleTapGesture(MotionEvent e) {
        Log.d(TAG, "onDoubleTapGesture: ");
        makeToast("DoubleTap");
    }

    private void makeToast(String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }
}
