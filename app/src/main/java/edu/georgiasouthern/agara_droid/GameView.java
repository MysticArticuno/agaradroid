package edu.georgiasouthern.agara_droid;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    GameThread thread;
    float startTouchX = -1;
    float startTouchY = -1;
    float currentTouchX = -1;
    float currentTouchY = -1;

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
    }

    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        thread = new GameThread(getContext(), holder, getWidth(), getHeight());
        thread.start();
    }

    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

    }

    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        thread.running = false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            thread.startTouchX = event.getX();
            thread.startTouchY = event.getY();
            thread.currentTouchX = event.getX();
            thread.currentTouchY = event.getY();
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            thread.currentTouchX = event.getX();
            thread.currentTouchY = event.getY();
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            thread.startTouchX = -1;
            thread.startTouchY = -1;
            thread.currentTouchX = -1;
            thread.currentTouchY = -1;
            System.out.println("Touch Ended");
        }
        return true;
    }
}