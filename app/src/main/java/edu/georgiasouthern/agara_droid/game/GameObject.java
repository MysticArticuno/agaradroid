package edu.georgiasouthern.agara_droid.game;

public class GameObject {
    public float positionX;
    public float positionY;

    public float distanceTo(GameObject object) {
        return (float) Math.sqrt(Math.pow((this.positionX - object.positionX), 2) + Math.pow((this.positionY - object.positionY), 2));
    }
}
