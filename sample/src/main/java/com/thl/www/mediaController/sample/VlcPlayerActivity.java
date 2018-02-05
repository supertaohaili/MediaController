package com.thl.www.mediaController.sample;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.thl.www.mediaController.PlayerControlView;

import org.videolan.vlc.VlcVideoView;
import org.videolan.vlc.util.VLCInstance;

public class VlcPlayerActivity extends AppCompatActivity {

    private VlcVideoView mVlcVideoView;

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
        final PlayerControlView mPlayerControlView = (PlayerControlView) findViewById(R.id.PlayerControlView);

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
        mVlcVideoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mPlayerControlView.isShowing())
                    mPlayerControlView.show();
                else
                    mPlayerControlView.hide();
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
}
