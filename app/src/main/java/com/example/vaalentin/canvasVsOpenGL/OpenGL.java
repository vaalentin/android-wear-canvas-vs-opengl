package com.example.vaalentin.canvasVsOpenGL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.support.wearable.watchface.Gles2WatchFaceService;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by Valentin on 22/11/2016.
 */

public class OpenGL extends Gles2WatchFaceService {
    private final static String TAG = "OpenGL";

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends Gles2WatchFaceService.Engine {
        private final int PARTICLES_X = 50;
        private final int PARTICLES_Y = 50;
        private final int PARTICLES_COUNT = PARTICLES_X * PARTICLES_Y;
        private final int BYTES_PER_FLOAT = 4;

        private FloatBuffer mPositions;
        private int mPositionsLoc;

        private FloatBuffer mBrightnesses;
        private int mBrightnessesLoc;

        private FloatBuffer mInfluences;
        private int mInfluencesLoc;

        private int mProgram;

        private int mTextures[] = new int[1];

        private int mTimeLoc;
        private float mTime = 0f;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            float positions[] = new float[PARTICLES_COUNT * 2];
            float brightnesses[] = new float[PARTICLES_COUNT];
            float influences[] = new float[PARTICLES_COUNT];

            int i = 0;
            int j = 0;

            for(int x = 0; x < PARTICLES_X; ++x) {
                for(int y = 0; y < PARTICLES_Y; ++y) {
                    positions[i++] = (float) x / (PARTICLES_X - 1);
                    positions[i++] = (float) y / (PARTICLES_Y - 1);

                    brightnesses[j] = (float) Math.random();
                    influences[j] = (float) Math.random();

                    j++;
                }
            }

            mPositions = ByteBuffer.allocateDirect(positions.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
            mPositions.put(positions);
            mPositions.position(0);

            mBrightnesses = ByteBuffer.allocateDirect(brightnesses.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
            mBrightnesses.put(brightnesses);
            mBrightnesses.position(0);

            mInfluences = ByteBuffer.allocateDirect(influences.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
            mInfluences.put(influences);
            mInfluences.position(0);
        }

        @Override
        public void onGlContextCreated() {
            super.onGlContextCreated();

            // shaders
            String vertShaderSrc = ""
                    + "precision mediump float;"
                    + ""
                    + "float map(float value, float inMin, float inMax, float outMin, float outMax) {"
                    + "  return (value - inMin) / (inMax - inMin) * (outMax - outMin) + outMin;"
                    + "}"
                    + ""
                    + "attribute vec2 aPosition;"
                    + "attribute float aBrightness;"
                    + "attribute float aInfluence;"
                    + ""
                    + "uniform float uTime;"
                    + "uniform sampler2D uTexture;"
                    + ""
                    + "varying float vBrightness;"
                    + ""
                    + "void main() {"
                    + "  vec2 uv = aPosition;"
                    + "  uv.y = 1.0 - uv.y;"
                    + "  vec4 texColor = texture2D(uTexture, uv);"
                    + "  vBrightness = aBrightness * map(sin(uTime * aInfluence), -1.0, 1.0, 0.3, 1.0);"
                    + "  vBrightness *= 1.0 - texColor.r;"
                    + "  gl_PointSize = 4.0;"
                    + "  vec2 position = aPosition * 2.0 - 1.0;"
                    + "  gl_Position = vec4(position, 0.0, 1.0);"
                    + "}";

            int vertShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
            GLES20.glShaderSource(vertShader, vertShaderSrc);
            GLES20.glCompileShader(vertShader);

            String fragShaderSrc = ""
                    + "precision mediump float;"
                    + ""
                    + "varying float vBrightness;"
                    + ""
                    + "void main() { "
                    + "  gl_FragColor = vec4(vec3(vBrightness), 1.0);"
                    + "}";

            int fragShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
            GLES20.glShaderSource(fragShader, fragShaderSrc);
            GLES20.glCompileShader(fragShader);

            // program
            mProgram = GLES20.glCreateProgram();
            GLES20.glAttachShader(mProgram, vertShader);
            GLES20.glAttachShader(mProgram, fragShader);
            GLES20.glLinkProgram(mProgram);

            int[] status = new int[1];
            GLES20.glGetProgramiv(mProgram, GLES20.GL_LINK_STATUS, status, 0);

            if(status[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "Error linking program");
                Log.e(TAG, GLES20.glGetProgramInfoLog(mProgram));
            }

            // attributes and uniforms
            mPositionsLoc = GLES20.glGetAttribLocation(mProgram, "aPosition");
            mBrightnessesLoc = GLES20.glGetAttribLocation(mProgram, "aBrightness");
            mInfluencesLoc = GLES20.glGetAttribLocation(mProgram, "aInfluence");

            mTimeLoc = GLES20.glGetUniformLocation(mProgram, "uTime");

            // textures
            InputStream stream = getResources().openRawResource(R.raw.texture);
            Bitmap bitmap = BitmapFactory.decodeStream(stream);

            GLES20.glGenTextures(1, mTextures, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);

            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

            bitmap.recycle();
        }

        @Override
        public void onGlSurfaceCreated(int width, int height) {
            super.onGlSurfaceCreated(width, height);

            GLES20.glClearColor(0f, 0f, 0f, 0f);
            GLES20.glViewport(0, 0, width, height);
        }

        @Override
        public void onDraw() {
            super.onDraw();

            mTime += 0.05;

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            GLES20.glUseProgram(mProgram);

            GLES20.glUniform1f(mTimeLoc, mTime);

            mPositions.position(0);
            GLES20.glVertexAttribPointer(mPositionsLoc, 2, GLES20.GL_FLOAT, false, 0, mPositions);
            GLES20.glEnableVertexAttribArray(mPositionsLoc);

            mBrightnesses.position(0);
            GLES20.glVertexAttribPointer(mBrightnessesLoc, 1, GLES20.GL_FLOAT, false, 0, mBrightnesses);
            GLES20.glEnableVertexAttribArray(mBrightnessesLoc);

            mInfluences.position(0);
            GLES20.glVertexAttribPointer(mInfluencesLoc, 1, GLES20.GL_FLOAT, false, 0, mInfluences);
            GLES20.glEnableVertexAttribArray(mInfluencesLoc);

            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, PARTICLES_COUNT);

            invalidate();
        }
    }
}
