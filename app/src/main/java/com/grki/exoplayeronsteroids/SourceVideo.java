package com.grki.exoplayeronsteroids;

import android.opengl.GLES32;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class SourceVideo {

    private final float[] Coords = {
            // X, Y, Z, U, V
            3.7f,  -2.0f, 1.0f, 0.f, 0.f,
            -1.2f,  -2.0f, 1.0f, 1.f, 0.f,
            3.7f,  0f, 1.0f, 0.f, 1.f,
            -1.2f,  0f, 1.0f, 1.f, 1.f,
    };

    private static final int VERTICES_DATA_POS_OFFSET = 0;
    private static final int VERTICES_DATA_UV_OFFSET = 3;

    private final short drawOrder[] = { 0, 1, 2, 3 }; // order to draw vertices

    private static final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;


    private final FloatBuffer vertexBuffer;

    private final ShortBuffer drawListBuffer;

    private int maPositionHandle;
    private int maTextureHandle;
    private int muSTMatrixHandle;

    public final int mTextureID;


    private int mPositionHandle;
    private int mMVPMatrixHandle;

    static final int COORDS_PER_VERTEX = 3;
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    private final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uSTMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "  gl_Position = uMVPMatrix * aPosition;\n" +
                    "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
                    "}\n";

    private final String fragmentShaderCode =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "}\n";




    private final int mProgram;

    private FloatBuffer mVertices;

    private static final int FLOAT_SIZE_BYTES = 4;

    private static final int VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
    private float[] mSTMatrix = new float[16];
    private int mTextureUniformHandle;


    public SourceVideo(){

        mVertices = ByteBuffer.allocateDirect(Coords.length
                * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertices.put(Coords).position(0);
        Matrix.setIdentityM(mSTMatrix, 0);

        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(
                // (number of coordinate values * 4 bytes per float)
                Coords.length * 4);
        // use the device hardware's native byte order
        bb.order(ByteOrder.nativeOrder());

        // create a floating point buffer from the ByteBuffer
        vertexBuffer = bb.asFloatBuffer();
        // add the coordinates to the FloatBuffer
        vertexBuffer.put(Coords);
        // set the buffer to read the first coordinate
        vertexBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 2 bytes per short)
                drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        // prepare shaders and OpenGL program
        int vertexShader = MyRenderer.loadShader(
                GLES32.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = MyRenderer.loadShader(
                GLES32.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLES32.glCreateProgram();             // create empty OpenGL Program
        GLES32.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES32.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
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

        // Can't do mipmapping with camera source
        GLES32.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES32.GL_TEXTURE_MIN_FILTER,
                GLES32.GL_LINEAR);
        GLES32.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES32.GL_TEXTURE_MAG_FILTER,
                GLES32.GL_LINEAR);
        // Clamp to edge is the only option
        GLES32.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES32.GL_TEXTURE_WRAP_S,
                GLES32.GL_CLAMP_TO_EDGE);
        GLES32.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES32.GL_TEXTURE_WRAP_T,
                GLES32.GL_CLAMP_TO_EDGE);
        MyRenderer.checkGlError("glTexParameteri mTextureID");


    }

    public void draw(float[] mvpMatrix,float[] mSTMatrix) {
        // Add program to OpenGL environment
        GLES32.glUseProgram(mProgram);

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES32.glGetAttribLocation(mProgram, "aPosition");

        // Enable a handle to the triangle vertices
        GLES32.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
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
        GLES32.glUniform1i(mTextureUniformHandle,1);
        MyRenderer.checkGlError("glUniform1i maTextureHandle");

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES32.glGetUniformLocation(mProgram, "uMVPMatrix");
        MyRenderer.checkGlError("glGetUniformLocation uMVPMatrix");

        // Apply the projection and view transformation
        GLES32.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GLES32.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0);
        MyRenderer.checkGlError("glUniformMatrix4fv");

        // Draw the square
        GLES32.glDrawElements(
                GLES32.GL_TRIANGLE_STRIP, drawOrder.length,
                GLES32.GL_UNSIGNED_SHORT, drawListBuffer);

        // Disable vertex array
        GLES32.glDisableVertexAttribArray(mPositionHandle);
    }
}
