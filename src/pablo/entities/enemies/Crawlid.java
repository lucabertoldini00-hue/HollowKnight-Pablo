// Crawlid.java
// Meanders left/right on the ground. Does not chase Pablo.
// Flips direction when it detects an edge ahead or hits a wall.
// All shared physics/sensor logic lives in Enemy.java.

package pablo.entities.enemies;

import com.badlogic.gdx.scenes.scene2d.Stage;
import pablo.framework.BaseActor;

public class Crawlid extends Enemy
{
    private static final float SPEED = 60f;

    public Crawlid(float x, float y, Stage stage)
    {
        super(x, y, stage);

        health = 2;

        // Load texture — replace with your actual asset path
        loadTexture("assets/crawlid.png");
        setBoundaryRectangle();

        // belowSensor: thin strip directly under the enemy's feet
        belowSensor = new BaseActor(0, 0, stage);
        belowSensor.loadTexture("assets/white.png");
        belowSensor.setSize(getWidth() - 8f, 6f);
        belowSensor.setBoundaryRectangle();
        belowSensor.setVisible(false);

        // edgeSensor: tiny square at ground level, one step ahead in current direction
        edgeSensor = new BaseActor(0, 0, stage);
        edgeSensor.loadTexture("assets/white.png");
        edgeSensor.setSize(6f, 6f);
        edgeSensor.setBoundaryRectangle();
        edgeSensor.setVisible(false);
    }

    @Override
    public void act(float dt)
    {
        super.act(dt);

        // 1. Apply gravity (defined in Enemy)
        applyGravity(dt);

        // 2. Sync sensors before edge check
        updateSensorPositions();

        // 3. At edge with no ground ahead → flip
        if (isOnGround() && !edgeAheadHasGround())
            flip();

        // 4. Horizontal movement
        velocityVec.x = direction * SPEED;

        // 5. Move
        moveBy(velocityVec.x * dt, velocityVec.y * dt);

        // 6. Re-sync sensors after movement
        updateSensorPositions();

        // 7. Face direction of travel
        setScaleX(direction);
    }
}