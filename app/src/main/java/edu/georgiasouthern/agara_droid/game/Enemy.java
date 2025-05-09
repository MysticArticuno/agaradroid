package edu.georgiasouthern.agara_droid.game;

import edu.georgiasouthern.agara_droid.GameThread;

public class Enemy extends Cell {

    GameThread parent;
    states state;
    GameObject target = null;
    public int fleeSightRange = 350;
    public int chaseSightRange = 750;
    public float movementX = 0;
    public float movementY = 0;
    public int interestTime;
    public int chaseTimer;
    public enum states {
        IDLE,
        CHASING,
        FLEEING,
        EATING
    }
    public Enemy(GameThread thread, float x, float y, int startingMass) {
        this.state = states.IDLE;
        this.mass = startingMass;
        this.positionX = x;
        this.positionY = y;
        this.parent = thread;
        eat(0);
    }
    public void eat(float nutrition) {
        if (dead)
            return;
        mass += (nutrition / 2);
        radius = (float)(200 * Math.log10(mass)) - 280;
    }

    public float getSpeed() {
        return 0.019f;
    }

    public void enemyLogic() {
        switch (state) {
            case IDLE:
                //target = null;
                for (Cell cell : parent.players) {
                    if (cell == this)
                        continue;
                    if (cell.mass > this.mass * 1.2 && this.distanceTo(cell) <= fleeSightRange && !cell.dead) {
                        this.state = states.FLEEING;
                        this.target = cell;
                    } else {
                        if ((cell.mass * 1.2) < this.mass && this.distanceTo(cell) <= chaseSightRange && !cell.dead) {
                            this.state = states.CHASING;
                            this.target = cell;
                            interestTime = (int) (11 + (10 * parent.rand.nextFloat()));
                            chaseTimer = 0;
                        }
                    }
                }
                if (target == null) {
                    if (parent.food.isEmpty())
                        return;
                    Food closestFood = parent.food.get(0);
                    for (Food food : parent.food) {
                        if (distanceTo(food) <= distanceTo(closestFood)) {
                            closestFood = food;
                        }
                    }
                    target = closestFood;
                    state = states.EATING;
                }
                return;

            case CHASING:
                if (target == null) {
                    state = states.IDLE;
                    return;
                }
                if (!parent.players.contains(target)) {
                    state = states.IDLE;
                    return;
                }
                if (target == parent.player && parent.player.dead) {
                    state = states.IDLE;
                    return;
                }
                moveToTarget();
                if (distanceTo(target) >= chaseSightRange * 1.5) {
                    state = states.IDLE;
                    target = null;
                }

                if (chaseTimer > interestTime) {
                    state = states.IDLE;
                    target = null;
                }
                return;

            case FLEEING:
                moveFromTarget();
                if (distanceTo(target) >= fleeSightRange * 5) {
                    state = states.IDLE;
                    target = null;
                }
                return;

            case EATING:
                if (target == null) {
                    state = states.IDLE;
                    return;
                }
                for (Cell cell : parent.players) {
                    if (cell == this)
                        continue;
                    if (cell.mass > this.mass * 1.2 && this.distanceTo(cell) <= fleeSightRange && !cell.dead) {
                        this.state = states.FLEEING;
                        this.target = cell;
                    }
                    if ((cell.mass * 1.2) < this.mass && this.distanceTo(cell) <= chaseSightRange && !cell.dead) {
                        this.state = states.CHASING;
                        this.target = cell;
                        interestTime = (int) (11 + (10 * parent.rand.nextFloat()));
                        chaseTimer = 0;
                    }
                }
                if (distanceTo(target) <= radius) {
                    target = null;
                    state = states.IDLE;
                }
                moveToTarget();
                return;
        }
    }

    public void moveToTarget() {
        if (target == null) {
            movementX = 0;
            movementY = 0;
            return;
        }
        moveToPosition(target.positionX, target.positionY);
    }
    public void moveToPosition(float x, float y) {
        float movementVectorMagnitude = (float)(Math.sqrt(Math.pow(x - positionX, 2) + Math.pow(y - positionY, 2)));
        float scaleFactor = 300/movementVectorMagnitude;
        float speed = getSpeed();
        movementX = speed * ((x - positionX) * scaleFactor);
        movementY = speed * ((y - positionY) * scaleFactor);
    }

    public void moveFromTarget() {
        if (target == null) {
            movementX = 0;
            movementY = 0;
            return;
        }
        float movementVectorMagnitude = (float)(Math.sqrt(Math.pow(target.positionX - positionX, 2) + Math.pow(target.positionY - positionY, 2)));
        float scaleFactor = 300/movementVectorMagnitude;
        float speed = getSpeed();
        movementX = speed * ((target.positionX - positionX) * scaleFactor);
        movementX += -1;
        movementY = speed * ((target.positionY - positionY) * scaleFactor);
        movementY *= -1;
        // Prevent cell from entering out-of-bounds
        if (Math.abs(positionY + movementY) > (float) parent.boardSizeY /2) {
            moveToPosition(positionX >= 0 ? (float) (parent.boardSizeX) / 2 : (float) -1 * parent.boardSizeX / 2, positionY);
        }
        if (Math.abs(positionX + movementX) > (float) parent.boardSizeX /2) {
            moveToPosition(positionX, positionY >= 0 ? (float) (parent.boardSizeY) / 2 : (float) -1 *parent.boardSizeY / 2);
        }
    }
}
