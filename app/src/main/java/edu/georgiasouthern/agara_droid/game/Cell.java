package edu.georgiasouthern.agara_droid.game;

import android.graphics.Color;
import android.graphics.Paint;

import java.util.Random;

public class Cell extends GameObject {
    public boolean dead = false;
    public float mass;
    public float radius;
    public Paint paint = new Paint();
    public Paint borderPaint = new Paint();
    public Cell() {
        Random random = new Random();
        int colorR = random.nextInt(256);
        int colorG = random.nextInt(256);
        int colorB = random.nextInt(256);
        this.paint.setARGB(255, colorR, colorG, colorB);
        colorR -= 40;
        if (colorR < 0)
            colorR = 0;
        colorG -= 40;
        if (colorG < 0)
            colorG = 0;
        colorB -= 40;
        if (colorB < 0)
            colorB = 0;
        this.borderPaint.setARGB(255, colorR, colorG, colorB);
    }

    public void eat(float nutrition) {
        if (dead)
            return;
        mass += nutrition;
        radius = (float)(200 * Math.log10(mass)) - 280;
    }

    public void die() {
        System.out.println(this + " just died lol");
        dead = true;
        mass = 0;
        radius = 0;
    }

    public float getSpeed() {
        return 0.02f;
    }
}
