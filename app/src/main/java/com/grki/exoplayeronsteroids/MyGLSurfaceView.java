package com.grki.exoplayeronsteroids;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import com.google.android.exoplayer2.SimpleExoPlayer;


public class MyGLSurfaceView extends GLSurfaceView {

    MyRenderer mRenderer;

    public MyGLSurfaceView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    void init(SimpleExoPlayer player, FPSListener fpsListener) {
        setEGLContextClientVersion(2);
        mRenderer = new MyRenderer();
        setRenderer(mRenderer);
        mRenderer.setPlayer(player);
        mRenderer.setFPSListener(fpsListener);
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
