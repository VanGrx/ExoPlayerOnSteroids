package com.grki.exoplayeronsteroids;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES32;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Surface;

import com.google.android.exoplayer2.SimpleExoPlayer;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static org.opencv.core.Core.flip;


public class OpenCvRenderer implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {


    public static final int screenWidth = 1920;
    public static final int screenHeight = 1080;

    private IntBuffer mPboIds;
    private IntBuffer texFBO;
    private IntBuffer mFBO;
    private boolean mInitRecord;
    private int mPboIndex = 0;
    private int mPboNewIndex = 1;
    private int mPboSize;

    private CascadeClassifier mJavaDetector;

    private File mCascadeFile;


    int mPrevWidth = 0;
    int mPrevHeight = 0;

    ByteBuffer byteBuffer;

    private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);

    private float mRelativeFaceSize = 0.2f;
    private int mAbsoluteFaceSize = 0;


    public OpenCvRenderer(Context context) {
        OpenCVLoader.initDebug();
        byteBuffer = ByteBuffer.allocate(screenWidth * screenHeight);
        mFBO = IntBuffer.allocate(1);
        texFBO = IntBuffer.allocate(2);
        GLES32.glGenFramebuffers(1, mFBO);
        Matrix.setIdentityM(mSTMatrix, 0);



        InputStream is = context.getResources().openRawResource(R.raw.lbpcascade_frontalface);
        File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
        mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
        try {
            FileOutputStream os = new FileOutputStream(mCascadeFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
    }

    public void onPause() {
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void onResume() {
        mLastTime = SystemClock.elapsedRealtimeNanos();
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void onDrawFrame(GL10 glUnused) {


        if (mPlayer.getVideoFormat() != null) {
            int width = mPlayer.getVideoFormat().width;
            int height = mPlayer.getVideoFormat().height;

            if (width < 200 || height < 200)
                return;

            logFrame();

//            if(mPrevWidth!=width || mPrevHeight != height) {
//                mPboSize = width * height * 3;
//                mPboIds = IntBuffer.allocate(2);
//                GLES32.glGenBuffers(2, mPboIds);
//                GLES32.glBindBuffer(GLES32.GL_PIXEL_PACK_BUFFER, mPboIds.get(0));
//                GLES32.glBufferData(GLES32.GL_PIXEL_PACK_BUFFER, mPboSize, null, GLES32.GL_STATIC_READ);
//                checkGlError("Gen1");
//
//                GLES32.glBindBuffer(GLES32.GL_PIXEL_PACK_BUFFER, mPboIds.get(1));
//                GLES32.glBufferData(GLES32.GL_PIXEL_PACK_BUFFER, mPboSize, null, GLES32.GL_STATIC_READ);
//                GLES32.glBindBuffer(GLES32.GL_PIXEL_PACK_BUFFER, 0);
//                checkGlError("Gen2");
//                mPrevWidth = width;
//                mPrevHeight = height;
//            }

            Log.e("IGOR", "WIDTH " + width + " HEIGHT " + height);


            synchronized (this) {
                if (updateSurface) {
                    mSurface.updateTexImage();
                    mSurface.getTransformMatrix(mSTMatrix);
                    updateSurface = false;
                }
            }

            GLES32.glClear(GLES32.GL_DEPTH_BUFFER_BIT | GLES32.GL_COLOR_BUFFER_BIT);


            GLES32.glBindFramebuffer(GLES32.GL_FRAMEBUFFER, mFBO.get(0));

            Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -1, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
            Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mViewMatrix, 0);

            mSourceVideo.draw(mMVPMatrix, mSTMatrix);


            long startTime = System.nanoTime();


            ByteBuffer mTempBuffer = bindPixelBuffer(screenWidth, screenHeight);


            long stopTime = System.nanoTime();

            long totalTime = stopTime - startTime;

            mTempBuffer.position(0);


            GLES32.glBindFramebuffer(GLES32.GL_FRAMEBUFFER, 0);
            GLES32.glBindFramebuffer(GLES32.GL_READ_FRAMEBUFFER, mFBO.get(0));
            checkGlError("glBindFramebuffer");
//            GLES32.glBlitFramebuffer(0, 0, 800, 600, 0, 0, 800, 600, GLES32.GL_COLOR_BUFFER_BIT, GLES32.GL_NEAREST);
//            checkGlError("glBlitFramebuffer");


            ByteBuffer openCVResult = openCVMagic(mTempBuffer, screenWidth, screenHeight);

           mOpenCVVideo.draw(mMVPMatrix, mSTMatrix, openCVResult, screenWidth, screenHeight);
        }


    }

    public static void overlayImage(Mat background, Mat foreground, Mat output, Point location) {

        background.copyTo(output);

        for (int y = (int) Math.max(location.y, 0); y < background.rows(); ++y) {

            int fY = (int) (y - location.y);

            if (fY >= foreground.rows())
                break;

            for (int x = (int) Math.max(location.x, 0); x < background.cols(); ++x) {
                int fX = (int) (x - location.x);
                if (fX >= foreground.cols()) {
                    break;
                }

                double opacity;
                double[] finalPixelValue = new double[4];

                opacity = foreground.get(fY, fX)[3];

                finalPixelValue[0] = background.get(y, x)[0];
                finalPixelValue[1] = background.get(y, x)[1];
                finalPixelValue[2] = background.get(y, x)[2];
                finalPixelValue[3] = background.get(y, x)[3];

                for (int c = 0; c < output.channels(); ++c) {
                    if (opacity > 0) {
                        double foregroundPx = foreground.get(fY, fX)[c];
                        double backgroundPx = background.get(y, x)[c];

                        float fOpacity = (float) (opacity / 255);
                        finalPixelValue[c] = ((backgroundPx * (1.0 - fOpacity)) + (foregroundPx * fOpacity));
                        if (c == 3) {
                            finalPixelValue[c] = foreground.get(fY, fX)[3];
                        }
                    }
                }
                output.put(y, x, finalPixelValue);
            }
        }
    }

    private ByteBuffer openCVMagic(ByteBuffer mTempBuffer, int width, int height) {

        byte[] data = new byte[mTempBuffer.capacity()];
        byte[] newData = new byte[mTempBuffer.capacity()];
        ((ByteBuffer) mTempBuffer.duplicate().clear()).get(data);
        Mat mat = new Mat(height, width, CvType.CV_8UC3);
        mat.put(0, 0, data);


        Mat destination = new Mat();

        flip(mat, mat, 0);

        int x = 50;
        int y = 150;

        Rect rec = new Rect(x, y, screenWidth - x, screenHeight - y);

        Mat cropped = mat.submat(rec);


        Mat mGray = new Mat();

        Imgproc.cvtColor(cropped, mGray, Imgproc.COLOR_BGR2GRAY);

        Mat croppedAlpha = new Mat();

        Imgproc.cvtColor(cropped, croppedAlpha, Imgproc.COLOR_BGR2BGRA);

        int h = mGray.rows();
        if (Math.round(height * mRelativeFaceSize) > 0) {
            mAbsoluteFaceSize = Math.round(h * mRelativeFaceSize);
        }

        MatOfRect faces = new MatOfRect();

        mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());


        Size sz = new Size(1920, 1080);


        Mat endProduct = new Mat();


        Rect[] facesArray = faces.toArray();

        for (int i = 0; i < facesArray.length; i++) {
            Imgproc.rectangle(cropped, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);
            // overlayImage(croppedAlpha,img,croppedAlpha,facesArray[i].tl());

        }


        //Imgproc.cvtColor(cropped,destination,Imgproc.COLOR_BGRA2BGR);


        Imgproc.resize(cropped, destination, sz);

        destination.get(0, 0, newData);


        ByteBuffer newBuffer = ByteBuffer.wrap(newData);

        return newBuffer;
    }


    int k = 0;

    private ByteBuffer bindPixelBuffer(int width, int height) {

//        k++;
//
//        if(k%60!=0) {
//
//            return byteBuffer;
//        }

        GLES32.glBindBuffer(GLES32.GL_PIXEL_PACK_BUFFER, mPboIds.get(mPboIndex));
        checkGlError("glBindBuffer");
        GLES32.glReadPixels(0, 0, width, height, GLES32.GL_RGB, GLES32.GL_UNSIGNED_BYTE, 0);
        //GLES32.glReadPixels(0, 0, width, height, GLES32.GL_RGB, GLES32.GL_UNSIGNED_BYTE, byteBuffer);
        checkGlError("glReadPixels");
        if (mInitRecord) {
            unbindPixelBuffer();
            mInitRecord = false;
            return null;
        }

//        GLES32.glBindBuffer(GLES32.GL_PIXEL_PACK_BUFFER, mPboIds.get(mPboNewIndex));
//        checkGlError("glBindBuffer");

        byteBuffer = (ByteBuffer) GLES32.glMapBufferRange(GLES32.GL_PIXEL_PACK_BUFFER, 0, mPboSize, GLES32.GL_MAP_READ_BIT);
        checkGlError("glMapBufferRange");
        GLES32.glUnmapBuffer(GLES32.GL_PIXEL_PACK_BUFFER);
        checkGlError("glUnmapBuffer");
        unbindPixelBuffer();

        GLES32.glViewport(0, 0, screenWidth, screenHeight);


        return byteBuffer;
    }

    private void unbindPixelBuffer() {
        GLES32.glBindBuffer(GLES32.GL_PIXEL_PACK_BUFFER, 0);
        checkGlError("glBindBuffer");
        mPboIndex = (mPboIndex + 1) % 2;
        mPboNewIndex = (mPboNewIndex + 1) % 2;
    }


    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        // Ignore the passed-in GL10 interface, and use the GLES32
        // class's static methods instead.
        GLES32.glViewport(0, 0, screenWidth, screenHeight);
        mRatio = (float) screenWidth / screenHeight;
        Matrix.frustumM(mProjMatrix, 0, -mRatio, mRatio, -1, 1, 1, 10);
    }


    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {

        GLES32.glEnable(GLES32.GL_BLEND);
        GLES32.glBlendFunc(GLES32.GL_SRC_ALPHA, GLES32.GL_ONE_MINUS_SRC_ALPHA);
        GLES32.glClearColor(0, 0, 0, 1.0f);

        GLES32.glBindFramebuffer(GLES32.GL_FRAMEBUFFER, mFBO.get(0));
        checkGlError("glBindFramebuffer");

        mSourceVideo = new SourceVideo();

        int[] textures = new int[1];
        GLES32.glGenTextures(1, textures, 0);

        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, textures[0]);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_NEAREST);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_NEAREST);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_S, GLES32.GL_CLAMP_TO_EDGE);
        GLES32.glTexParameteri(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_T, GLES32.GL_CLAMP_TO_EDGE);


        GLES32.glTexImage2D(
                GLES32.GL_TEXTURE_2D, 0, GLES32.GL_RGB, 800, 600, 0, GLES32.GL_RGB, GLES32.GL_UNSIGNED_BYTE, null
        );
        checkGlError("glTexImage2D");

        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, 0);


//        GLES32.glFramebufferTexture2D(GLES32.GL_FRAMEBUFFER,GLES32.GL_COLOR_ATTACHMENT0,GLES32.GL_TEXTURE_2D,textures[0],0);
//        checkGlError("glFramebufferTexture");

        mOpenCVVideo = new OpenCVVideo();

        texFBO.put(1, mOpenCVVideo.mTextureID);

//
//        GLES32.glFramebufferTexture(GLES32.GL_FRAMEBUFFER,GLES32.GL_COLOR_ATTACHMENT0+1,texFBO.get(1),0);
//        checkGlError("glFramebufferTexture");


        Matrix.setLookAtM(mVMatrix, 0, 0, 0, 4f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        synchronized (this) {
            updateSurface = false;
        }

        mSurface = new SurfaceTexture(mSourceVideo.mTextureID);
        mSurface.setOnFrameAvailableListener(this);
        Surface surface = new Surface(mSurface);
        mPlayer.setVideoSurface(surface);

        int width = screenWidth;
        int height = screenHeight;
        mPboSize = width * height * 3;
        mPboIds = IntBuffer.allocate(2);
        GLES32.glGenBuffers(2, mPboIds);
        GLES32.glBindBuffer(GLES32.GL_PIXEL_PACK_BUFFER, mPboIds.get(0));
        GLES32.glBufferData(GLES32.GL_PIXEL_PACK_BUFFER, mPboSize, null, GLES32.GL_STATIC_READ);
        checkGlError("Gen1");

        GLES32.glBindBuffer(GLES32.GL_PIXEL_PACK_BUFFER, mPboIds.get(1));
        GLES32.glBufferData(GLES32.GL_PIXEL_PACK_BUFFER, mPboSize, null, GLES32.GL_STATIC_READ);
        GLES32.glBindBuffer(GLES32.GL_PIXEL_PACK_BUFFER, 0);
        checkGlError("Gen2");
        mPrevWidth = width;
        mPrevHeight = height;


    }

    @Override
    synchronized public void onFrameAvailable(SurfaceTexture surface) {
        /* For simplicity, SurfaceTexture calls here when it has new
         * data available.  Call may come in from some random thread,
         * so let's be safe and use synchronize. No OpenGL calls can be done here.
         */
        updateSurface = true;
        //Log.v(TAG, "onFrameAvailable " + surface.getTimestamp());
    }

    public static int loadShader(int shaderType, String source) {

        // create a vertex shader type (GLES32.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES32.GL_FRAGMENT_SHADER)
        int shader = GLES32.glCreateShader(shaderType);

        // add the source code to the shader and compile it
        GLES32.glShaderSource(shader, source);
        GLES32.glCompileShader(shader);

        return shader;
    }

    public static void checkGlError(String op) {
        int error;
        while ((error = GLES32.glGetError()) != GLES32.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }

    private final float[] mViewMatrix = new float[16];

    private float[] mMVPMatrix = new float[16];
    private float[] mProjMatrix = new float[16];
    private float[] mMMatrix = new float[16];
    private float[] mVMatrix = new float[16];
    private float[] mSTMatrix = new float[16];

    private float mRatio = 1.0f;
    private SurfaceTexture mSurface;
    private SurfaceTexture mSurfaceRes;
    private boolean updateSurface = false;
    private long mLastTime = -1;
    private long mRunTime = 0;
    private SourceVideo mSourceVideo;
    private OpenCVVideo mOpenCVVideo;


    private SimpleExoPlayer mPlayer;

    public void setPlayer(SimpleExoPlayer player) {
        mPlayer = player;
    }

    private static final String TAG = "OpenCvRenderer";

    private FPSListener mFPSListener;

    private long startTime = System.nanoTime();
    private int frames = 0;

    private void logFrame() {
        frames++;
        if (System.nanoTime() - startTime >= 1000000000) {
            mFPSListener.onFPSMeasured(frames);
            frames = 0;
            startTime = System.nanoTime();
        }
    }

    void setFPSListener(FPSListener fps) {
        mFPSListener = fps;
    }

}
