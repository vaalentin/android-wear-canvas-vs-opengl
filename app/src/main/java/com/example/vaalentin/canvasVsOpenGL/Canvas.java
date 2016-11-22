package com.example.vaalentin.canvasVsOpenGL;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.view.SurfaceHolder;

/**
 * Created by Valentin on 22/11/2016.
 */

public class Canvas extends CanvasWatchFaceService {
    private final static String TAG = "Canvas";

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        private final int PARTICLES_X = 50;
        private final int PARTICLES_Y = 50;

        private float mBrightnesses[];
        private float mInfluences[];

        private float mTime = 0f;

        private Paint mPaint;

        @Override
        public void onCreate(SurfaceHolder holder) {
           super.onCreate(holder);

            final int particlesCount = PARTICLES_X * PARTICLES_Y;

            mBrightnesses = new float[particlesCount];
            mInfluences = new float[particlesCount];

            int i = 0;

            for(int x = 0; x < PARTICLES_X; ++x) {
                for(int y = 0; y < PARTICLES_Y; ++y) {
                    mBrightnesses[i] = (float) Math.random();
                    mInfluences[i] = (float) Math.random();

                    i++;
                }
            }

            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setColor(Color.WHITE);
            mPaint.setStyle(Paint.Style.FILL);
        }

        @Override
        public void onDraw(android.graphics.Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);

            mTime += 0.05;

            canvas.drawColor(Color.BLACK);

            final int width = bounds.width();
            final int height = bounds.height();

            int i = 0;

            for(int x = 0; x < PARTICLES_X; ++x) {
                for(int y = 0; y < PARTICLES_Y; ++y) {
                    float ax = (float) x / (PARTICLES_X - 1);
                    float ay = (float) y / (PARTICLES_Y - 1);

                    ax *= width;
                    ay *= height;

                    float normBrightness = mBrightnesses[i] * map((float) Math.sin(mTime * mInfluences[i]), -1f, 1f, 0f, 1f);
                    int brightness = (int) Math.floor(normBrightness * 255);

                    mPaint.setARGB(255, brightness, brightness, brightness);

                    canvas.drawRect(
                            ax - 2f,
                            ay - 2f,
                            ax + 2f,
                            ay + 2f,
                            mPaint
                    );

                    i++;
                }
            }

            invalidate();
        }

        private float map(float value, float inMin, float inMax, float outMin, float outMax) {
            return (value - inMin) / (inMax - inMin) * (outMax - outMin) + outMin;
        }
    }
}
