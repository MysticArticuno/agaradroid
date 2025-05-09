package edu.georgiasouthern.agara_droid;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceHolder;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;

import edu.georgiasouthern.agara_droid.game.Cell;
import edu.georgiasouthern.agara_droid.game.Enemy;
import edu.georgiasouthern.agara_droid.game.Food;
import edu.georgiasouthern.agara_droid.game.Player;

public class GameThread extends Thread{
    Handler handler;
    public Random rand = new Random();
    final SurfaceHolder holder;
    public Context context;
    public boolean running = true;
    final int TICKS_PER_SECOND = 60;
    final int SKIP_TICKS = 1000 / TICKS_PER_SECOND;
    float startTouchX = -1;
    float startTouchY = -1;
    float currentTouchX = -1;
    float currentTouchY = -1;
    public int boardSizeX;
    public int boardSizeY;
    public float cameraX;
    public float cameraY;
    public float cameraZoom;
    float cameraMinX;
    float cameraMinY;
    float cameraMaxX;
    float cameraMaxY;
    public int screenSizeX;
    public int screenSizeY;
    public int enemyCount;
    public long onDeathWaitTime = -1;
    public final int GRID_SPACING = 100;
    public final int MAX_FOOD = 1000;
    public boolean won = false;
    public Player player;
    public ArrayList<Cell> players;
    public ArrayList<Food> food;

    public GameThread(Context context, SurfaceHolder holder, int width, int height) {
        handler = new Handler(Looper.getMainLooper());
        this.context = context;
        this.holder = holder;
        screenSizeX = width;
        screenSizeY = height;
    }

    public void run() {
        // Init
        Canvas canvas = null;
        restartGame();

        // Game Loop
        double next_tick = System.currentTimeMillis();
        while (running) {
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    synchronized (holder) {
                        // Game Logic
                        while (System.currentTimeMillis() > next_tick) {
                            gameLogic();
                            next_tick += SKIP_TICKS;
                        }

                        // Render
                        renderBoard(canvas);

                        // End Game
                        if (onDeathWaitTime != -1 && System.currentTimeMillis() > onDeathWaitTime) {
                            onDeathWaitTime = -1;
                            restartGame();
                        }
                    }
                }
            } finally {
                if (canvas != null)
                    holder.unlockCanvasAndPost(canvas);
            }
        }
    }

    public void gameLogic() {

        // Spawn food if less than the limit
        if (food.size() < MAX_FOOD)
            addFood();

        // Player Input
        float movementX = 0;
        float movementY = 0;
        if (startTouchX != -1 && startTouchY != -1) {
            float playerSpeed = player.getSpeed();
            float movementVectorMagnitude = (float) (Math.sqrt(Math.pow(currentTouchX - startTouchX, 2) + Math.pow(currentTouchY - startTouchY, 2)));
            if (movementVectorMagnitude <= 300) {
                movementX = playerSpeed * (movementVectorMagnitude / 300) * (currentTouchX - startTouchX);
                movementY = playerSpeed * (movementVectorMagnitude / 300) * (currentTouchY - startTouchY);
            } else {
                float scaleFactor = 300/movementVectorMagnitude;
                movementX = playerSpeed * ((currentTouchX - startTouchX) * scaleFactor);
                movementY = playerSpeed * ((currentTouchY - startTouchY) * scaleFactor);
            }
        }
        if (!player.dead) {
            player.positionX += movementX;
            player.positionY += movementY;
        }

        // Prevent player from entering the border
        if (player.positionX > (float)boardSizeX/2)
            player.positionX = (float)boardSizeX/2;
        if (player.positionX < -1 * (float)boardSizeX/2)
            player.positionX = -1 * (float)boardSizeX/2;
        if (player.positionY > (float)boardSizeY/2)
            player.positionY = (float)boardSizeY/2;
        if (player.positionY < -1 * (float)boardSizeY/2)
            player.positionY = -1 * (float)boardSizeY/2;

        // Enemy Logic
        for (Cell cell : players) {
            if (cell instanceof Enemy) {
                ((Enemy) cell).enemyLogic();
                cell.positionX += ((Enemy) cell).movementX;
                cell.positionY += ((Enemy) cell).movementY;
//                if (cell.positionX > ((float)boardSizeX/2) + 100)
//                    cell.positionX = (float)boardSizeX;
//                if (cell.positionX < -1 * ((float)boardSizeX/2) + 100)
//                    cell.positionX = (float)boardSizeX;
//                if (cell.positionY > ((float)boardSizeY/2) + 100)
//                    cell.positionY = (float)boardSizeY/2;
//                if (cell.positionY < -1 * ((float)boardSizeY/2) + 100)
//                    cell.positionY = (float)boardSizeY/2;
            }

        }

        // Eating Logic
        ArrayList<Food> eatenFood = new ArrayList<Food>();
        for (Food food : food) {
            for (Cell cell : players) {
                if (cell.distanceTo(food) < cell.radius) {
                    cell.eat(food.nutrition);
                    food.nutrition = 0;
                    eatenFood.add(food);
                }
            }
        }
        food.removeAll(eatenFood);

        // Cell Eating Logic
        ArrayList<Cell> eatenPlayers = new ArrayList<Cell>();
        for (Cell eater : players) {
            if (eater.dead)
                eatenPlayers.add(eater);
            for (Cell eaten : players) {
                if (eater == eaten)
                    continue;
                if (!eaten.dead && !eater.dead && eater.mass > eaten.mass * 1.2){
                    if (eater.distanceTo(eaten) < eater.radius / 1.8) {
                        eater.eat(eaten.mass/2);
                        eaten.die();
                        eatenPlayers.add(eaten);
                    }
                }
            }
        }
        players.removeAll(eatenPlayers);
        checkDeadPlayers();



        players.sort(new Comparator<Cell>() {
            @Override
            public int compare(Cell c1, Cell c2) {
                return Float.compare(c1.mass, c2.mass);
            }
        });
        if (players.size() == 1 && !player.dead)
            winGame();
    }
    public void renderBoard(Canvas canvas) {
        // Calculate camera zoom
        cameraZoom = (float) (((double) (player.mass) / 500) + 0.55);

        // Draw Background
        Paint BGPaint = new Paint();
        BGPaint.setColor(Color.WHITE);
        canvas.drawRect(0, 0, screenSizeX, screenSizeY, BGPaint);

        // Move Camera to Player
        cameraX = player.positionX;
        cameraY = player.positionY;
        cameraMinX = cameraX - ((float)screenSizeX/2) * cameraZoom;
        cameraMinY = cameraY - ((float)screenSizeY/2) * cameraZoom;
        cameraMaxX = cameraX + ((float)screenSizeX/2) * cameraZoom;
        cameraMaxY = cameraY + ((float)screenSizeY/2) * cameraZoom;
        //System.out.println("Camera Position: ("+cameraX+", "+cameraY+"), Camera bounds: X=("+cameraMinX+", "+cameraMaxX+"), Y=("+cameraMinY+", "+cameraMaxY+")");

        // Render Grid Lines
        Paint linePaint = new Paint();
        linePaint.setARGB(255, 242, 242, 242);
        // Horizontal
        for(int i = (int) Math.floor(cameraMinY); i < cameraMaxY; i++) {
            if(i % GRID_SPACING == 0) {
                canvas.drawRect(
                        0,
                        (((i-1) - cameraY) / cameraZoom) + ((float)screenSizeY/2),
                        screenSizeX-1,
                        (((i+1) - cameraY) / cameraZoom) + ((float)screenSizeY/2),
                        linePaint);
            }
        }
        // Vertical
        for(int i = (int) Math.floor(cameraMinX); i < cameraMaxX; i++) {
            if(i % GRID_SPACING == 0) {
                canvas.drawRect(
                        (((i-1) - cameraX) / cameraZoom) + ((float)screenSizeX/2),
                        0,
                        (((i+1) - cameraX) / cameraZoom) + ((float)screenSizeX/2),
                        screenSizeY-1, linePaint);
            }
        }


        // Draw Map Border
        Paint borderPaint = new Paint();
        borderPaint.setARGB(100, 150, 0, 0);
        // Bottom
        if(cameraMaxY > (float)boardSizeY/2)
            canvas.drawRect(
                    0,
                    ((((float)boardSizeY/2) - cameraY) / cameraZoom) + ((float)screenSizeY/2),
                    screenSizeX-1,
                    screenSizeY-1,
                    borderPaint);
        // Top
        if(cameraMinY < -1 * (float)boardSizeY/2)
            canvas.drawRect(
                    0,
                    0,
                    screenSizeX-1,
                    (((float)(-1 * boardSizeY/2) - cameraY) / cameraZoom) + ((float)screenSizeY/2),
                    borderPaint);
        // Left
        if(cameraMinX < -1 * (float)boardSizeX/2)
            canvas.drawRect(
                    0,
                    (((float)(-1 * boardSizeY/2) - cameraY) / cameraZoom) + ((float)screenSizeY/2),
                    (((float)(-1 * boardSizeX/2) - cameraX) / cameraZoom) + ((float)screenSizeX/2),
                    (((float)(boardSizeY/2) - cameraY) / cameraZoom) + ((float)screenSizeY/2),
                    borderPaint);
        // Right
        if(cameraMaxX > (float)boardSizeX/2)
            canvas.drawRect(
                    (((float)(boardSizeX/2) - cameraX) / cameraZoom) + ((float)screenSizeX/2),
                    (((float)(-1 * boardSizeY/2) - cameraY) / cameraZoom) + ((float)screenSizeY/2),
                    screenSizeX-1,
                    (((float)(boardSizeY/2) - cameraY) / cameraZoom) + ((float)screenSizeY/2),
                    borderPaint);


        // Render Food
        Paint foodPaint = new Paint();
        for(Food food: food) {
            if(food.positionX > cameraMinX - 50 && food.positionX < cameraMaxX + 50)
                if(food.positionY > cameraMinY - 50 && food.positionY < cameraMaxY + 50) {
                    foodPaint.setARGB(255, food.colorR, food.colorG, food.colorB);
                    drawCircleWithCamera(canvas, food.positionX, food.positionY, 10, foodPaint);

                }
        }

        // Render Players
        for (Cell cell : players) {
            if (cell.dead)
                continue;
            drawCircleWithCamera(canvas, cell.positionX, cell.positionY, cell.radius, cell.paint, 6, cell.borderPaint);
        }

//        // Render Player
//        if (!player.dead)
//            drawCircleWithCamera(canvas, player.positionX, player.positionY, player.radius, player.paint, 6, player.borderPaint);

        // Render Joystick
        if (startTouchX != -1 && startTouchY != -1) {
            Paint joystickPaint = new Paint();
            joystickPaint.setStyle(Paint.Style.STROKE);
            joystickPaint.setStrokeWidth(15);
            joystickPaint.setARGB(100, 100, 100, 100);
            canvas.drawCircle(startTouchX, startTouchY, 300, joystickPaint);
            joystickPaint.setStyle(Paint.Style.FILL);
            float movementVectorMagnitude = (float) (Math.sqrt(Math.pow(currentTouchX - startTouchX, 2) + Math.pow(currentTouchY - startTouchY, 2)));
            float joyX = currentTouchX;
            float joyY = currentTouchY;
            if (movementVectorMagnitude <= 300) {
                joyX = currentTouchX - startTouchX;
                joyY = currentTouchY - startTouchY;
            } else {
                float scaleFactor = 300/movementVectorMagnitude;
                joyX = (currentTouchX - startTouchX) * scaleFactor;
                joyY = (currentTouchY - startTouchY) * scaleFactor;
            }
            canvas.drawCircle(startTouchX + joyX, startTouchY + joyY, 100, joystickPaint);
        }

    }

    public void drawCircleWithCamera(Canvas canvas, float cx, float cy, float radius, Paint paint) {
        drawCircleWithCamera(canvas, cx, cy, radius, paint, 0, paint);
    }

    public void drawCircleWithCamera(Canvas canvas, float cx, float cy, float radius, Paint paint, int borderExpandRadius, Paint borderPaint) {
        if (cx > cameraMinX - radius && cx < cameraMaxX + radius)
            if (cy > cameraMinY - radius && cy < cameraMaxY + radius) {
                canvas.drawCircle(
                        ((cx - cameraX) / cameraZoom) + ((float) screenSizeX / 2),
                        ((cy - cameraY) / cameraZoom) + ((float) screenSizeY / 2),
                        (radius + borderExpandRadius) / cameraZoom,
                        borderPaint
                );
                canvas.drawCircle(
                        ((cx - cameraX) / cameraZoom) + ((float) screenSizeX / 2),
                        ((cy - cameraY) / cameraZoom) + ((float) screenSizeY / 2),
                        (radius) / cameraZoom,
                        paint
                );
            }
    }

    public void addFood() {
        food.add(new Food(
                (rand.nextFloat() * boardSizeX) - ((float)boardSizeX/2),
                (rand.nextFloat() * boardSizeY) - ((float)boardSizeY/2)
        ));
    }

    public void addFood(float x, float y) {
        food.add(new Food(
                x,
                y
        ));
    }

    public void addEnemy() {
        players.add(new Enemy(
                this,
                rand.nextFloat() * (boardSizeX-((float)boardSizeX/2)),
                rand.nextFloat() * (boardSizeY-((float)boardSizeY/2)),
                //(int)((player.mass * 0.6) + ((player.mass * 0.8) * rand.nextFloat()))
                50
        ));
    }




    public void addEnemy(float x, float y, int startingMass) {
        players.add(new Enemy(
                this,
                x,
                y,
                startingMass
        ));
    }

    public void addPlayer() {
        player = new Player(
                this,
                0,
                0,
                50
        );
        players.add(player);
    }

    public void endGame() {
        onDeathWaitTime = System.currentTimeMillis() + 5000;
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, "Game Over!", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void winGame() {
        if (!won) {
            won = true;
            onDeathWaitTime = System.currentTimeMillis() + 5000;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "You Win!", Toast.LENGTH_LONG).show();
                }
            });
        }

    }

    public void checkDeadPlayers() {
        int alive = 0;
        for (Cell cell : players) {
            if (cell == player) {
                continue;
            }
            if (Math.abs(cell.positionX) > boardSizeX || Float.isNaN(cell.positionX) || Float.isNaN(cell.positionY)) {
                cell.dead = true;
            }
            if (Math.abs(cell.positionY) > boardSizeY) {
                cell.dead = true;
            }
            if (!cell.dead) {
                alive += 1;
                //System.out.println(cell.positionX+" "+cell.positionY);
            }

        }
        //System.out.println(alive);
        if (alive == 0)
            winGame();
    }

    public void restartGame() {
        // Init Stuff
        players = new ArrayList<Cell>(0);
        food = new ArrayList<Food>(0);
        won = false;

        // Set up board
        boardSizeX = 5000;
        boardSizeY = 5000;
        cameraX = (float) boardSizeX /2;
        cameraY = (float) boardSizeY /2;

        // Create Player
        addPlayer();


        // Fill board with food
        for (int i=0;i<MAX_FOOD;i++) {
            addFood();
        }

        // Add Enemies
        enemyCount = 12;
        for (int i=0;i<enemyCount;i++) {
            addEnemy();
        }
    }
}
