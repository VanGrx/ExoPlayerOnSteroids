package com.grki.exoplayeronsteroids;

import android.opengl.GLES32;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

class SourceVideo {

    private static final int VERTICES_DATA_POS_OFFSET = 0;
    private static final int VERTICES_DATA_UV_OFFSET = 3;

    private final short[] drawOrder = {0, 1, 2, 3};

    private static final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;


    private final FloatBuffer vertexBuffer;

    private final ShortBuffer drawListBuffer;

    private int maPositionHandle;
    private int maTextureHandle;
    private int muSTMatrixHandle;

    final int mTextureID;


    private int mMVPMatrixHandle;

    private static final int COORDS_PER_VERTEX = 3;


    private final int mProgram;

    private FloatBuffer mVertices;

    private static final int FLOAT_SIZE_BYTES = 4;

    private static final int VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
    private int mTextureUniformHandle;


    SourceVideo() {

        float[] coords = {
                // X, Y, Z, U, V
                3.7f, -2.0f, 1.0f, 0.f, 0.f,
                -1.2f, -2.0f, 1.0f, 1.f, 0.f,
                3.7f, 0f, 1.0f, 0.f, 1.f,
                -1.2f, 0f, 1.0f, 1.f, 1.f,
        };
        mVertices = ByteBuffer.allocateDirect(coords.length
                * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertices.put(coords).position(0);
        float[] mSTMatrix = new float[16];
        Matrix.setIdentityM(mSTMatrix, 0);

        ByteBuffer bb = ByteBuffer.allocateDirect(
                coords.length * 4);
        bb.order(ByteOrder.nativeOrder());

        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(coords);
        vertexBuffer.position(0);

        ByteBuffer dlb = ByteBuffer.allocateDirect(
                drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        String vertexShaderCode = "uniform mat4 uMVPMatrix;\n" +
                "uniform mat4 uSTMatrix;\n" +
                "attribute vec4 aPosition;\n" +
                "attribute vec4 aTextureCoord;\n" +
                "varying vec2 vTextureCoord;\n" +
                "void main() {\n" +
                "  gl_Position = uMVPMatrix * aPosition;\n" +
                "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
                "}\n";
        int vertexShader = MyRenderer.loadShader(
                GLES32.GL_VERTEX_SHADER, vertexShaderCode);
        String fragmentShaderCode = "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;\n" +
                "varying vec2 vTextureCoord;\n" +
                "uniform samplerExternalOES sTexture;\n" +
                "void main() {\n" +
                "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                "}\n";
        int fragmentShader = MyRenderer.loadShader(
                GLES32.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLES32.glCreateProgram();
        GLES32.glAttachShader(mProgram, vertexShader);
        GLES32.glAttachShader(mProgram, fragmentShader);
        GLES32.glLinkProgram(mProgram);

        maPositionHandle = GLES32.glGetAttribLocation(mProgram, "aPosition");
        MyRenderer.checkGlError("glGetAttribLocation aPosition");
        if (maPositionHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aPosition");
        }
        maTextureHandle = GLES32.glGetAttribLocation(mProgram, "aTextureCoord");
        MyRenderer.checkGlError("glGetAttribLocation aTextureCoord");
        if (maTextureHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aTextureCoord");
        }

        mMVPMatrixHandle = GLES32.glGetUniformLocation(mProgram, "uMVPMatrix");
        MyRenderer.checkGlError("glGetUniformLocation uMVPMatrix");
        if (mMVPMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uMVPMatrix");
        }

        mTextureUniformHandle = GLES32.glGetUniformLocation(mProgram, "sTexture");
        MyRenderer.checkGlError("glGetUniformLocation uMVPMatrix");
        if (mMVPMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for sTexture");
        }

        muSTMatrixHandle = GLES32.glGetUniformLocation(mProgram, "uSTMatrix");
        MyRenderer.checkGlError("glGetUniformLocation uSTMatrix");
        if (muSTMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uSTMatrix");
        }

        int[] textures = new int[1];
        GLES32.glGenTextures(1, textures, 0);
        mTextureID = textures[0];
        GLES32.glActiveTexture(GLES32.GL_TEXTURE1);
        GLES32.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID);
        MyRenderer.checkGlError("glBindTexture mTextureID");

        GLES32.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES32.GL_TEXTURE_MIN_FILTER,
                GLES32.GL_LINEAR);
        GLES32.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES32.GL_TEXTURE_MAG_FILTER,
                GLES32.GL_LINEAR);
        GLES32.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES32.GL_TEXTURE_WRAP_S,
                GLES32.GL_CLAMP_TO_EDGE);
        GLES32.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES32.GL_TEXTURE_WRAP_T,
                GLES32.GL_CLAMP_TO_EDGE);
        MyRenderer.checkGlError("glTexParameteri mTextureID");


    }

    public void draw(float[] mvpMatrix, float[] mSTMatrix) {
        GLES32.glUseProgram(mProgram);

        int mPositionHandle = GLES32.glGetAttribLocation(mProgram, "aPosition");

        GLES32.glEnableVertexAttribArray(mPositionHandle);

        int vertexStride = COORDS_PER_VERTEX * 4;
        GLES32.glVertexAttribPointer(
                mPositionHandle, COORDS_PER_VERTEX,
                GLES32.GL_FLOAT, false,
                vertexStride, vertexBuffer);

        mVertices.position(VERTICES_DATA_POS_OFFSET);
        GLES32.glVertexAttribPointer(maPositionHandle, 3, GLES32.GL_FLOAT, false,
                VERTICES_DATA_STRIDE_BYTES, mVertices);
        MyRenderer.checkGlError("glVertexAttribPointer maPosition");
        GLES32.glEnableVertexAttribArray(maPositionHandle);
        MyRenderer.checkGlError("glEnableVertexAttribArray maPositionHandle");

        mVertices.position(VERTICES_DATA_UV_OFFSET);
        GLES32.glVertexAttribPointer(maTextureHandle, 3, GLES32.GL_FLOAT, false,
                VERTICES_DATA_STRIDE_BYTES, mVertices);
        MyRenderer.checkGlError("glVertexAttribPointer maTextureHandle");
        GLES32.glEnableVertexAttribArray(maTextureHandle);
        MyRenderer.checkGlError("glEnableVertexAttribArray maTextureHandle");

        maTextureHandle = GLES32.glGetAttribLocation(mProgram, "aTextureCoord");
        MyRenderer.checkGlError("glGetAttribLocation aTextureCoord");
        if (maTextureHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aTextureCoord");
        }
        GLES32.glUniform1i(mTextureUniformHandle, 1);
        MyRenderer.checkGlError("glUniform1i maTextureHandle");

        mMVPMatrixHandle = GLES32.glGetUniformLocation(mProgram, "uMVPMatrix");
        MyRenderer.checkGlError("glGetUniformLocation uMVPMatrix");

        GLES32.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GLES32.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0);
        MyRenderer.checkGlError("glUniformMatrix4fv");

        GLES32.glDrawElements(
                GLES32.GL_TRIANGLE_STRIP, drawOrder.length,
                GLES32.GL_UNSIGNED_SHORT, drawListBuffer);

        GLES32.glDisableVertexAttribArray(mPositionHandle);
    }
}
