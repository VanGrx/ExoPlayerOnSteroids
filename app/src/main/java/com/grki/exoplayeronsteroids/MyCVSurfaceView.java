package com.grki.exoplayeronsteroids;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

import com.google.android.exoplayer2.SimpleExoPlayer;


public class MyCVSurfaceView extends GLSurfaceView {

    OpenCvRenderer mRenderer;
    Context mContext;

    public MyCVSurfaceView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        mContext = context;
    }




    void init(SimpleExoPlayer player, FPSListener fpsListener) {
        setEGLContextClientVersion(2);
        mRenderer = new OpenCvRenderer(mContext);
        setRenderer(mRenderer);
        mRenderer.setPlayer(player);
        mRenderer.setFPSListener(fpsListener);
        Log.i("@@@", "setrenderer");
    }

    @Override
    public void onPause() {
        mRenderer.onPause();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mRenderer.onResume();
    }

}
