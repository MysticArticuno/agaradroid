package edu.georgiasouthern.agara_droid.game;

import java.util.Random;

import edu.georgiasouthern.agara_droid.GameThread;

public class Food extends GameObject {
    public int nutrition = 1;
    public int colorR;
    public int colorG;
    public int colorB;
    public Food(float x, float y) {
        Random colorRandom = new Random();
        this.positionX = x;
        this.positionY = y;
        this.colorR = colorRandom.nextInt(256);
        this.colorG = colorRandom.nextInt(256);
        this.colorB = colorRandom.nextInt(256);
    }

    public Food() {
        this(0, 0);
    }

    public void eat() {
        this.nutrition = 0;
    }
}
