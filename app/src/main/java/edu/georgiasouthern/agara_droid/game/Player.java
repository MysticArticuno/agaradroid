package edu.georgiasouthern.agara_droid.game;

import edu.georgiasouthern.agara_droid.GameThread;

public class Player extends Cell{

    GameThread parent;
    public Player(GameThread thread, float x, float y, int startingMass) {
        this.mass = startingMass;
        this.positionX = x;
        this.positionY = y;
        this.parent = thread;
        eat(0);
    }

    public void die() {
        dead = true;
        // mass = 0;
        // radius = 0;
        parent.endGame();
    }

}
