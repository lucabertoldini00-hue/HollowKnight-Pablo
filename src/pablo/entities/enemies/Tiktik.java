// Tiktik.java
// Crawls along surfaces (floor only in this phase — wall/ceiling crawling is a future iteration).
// Slightly faster than Crawlid. When struck, stuns in place for 0.5 seconds.
// Stun is triggered by calling takeDamage() — combat hookup comes in a later phase.

package pablo.entities.enemies;

import com.badlogic.gdx.scenes.scene2d.Stage;
import pablo.framework.BaseActor;

public class Tiktik extends Enemy
{
    private static final float SPEED         = 90f;
    private static final float STUN_DURATION = 0.5f;

    // --- stun state ---
    private boolean isStunned  = false;
    private float   stunTimer  = 0f;

    public Tiktik(float x, float y, Stage stage)
    {
        super(x, y, stage);

        health = 2;

        // Load texture — replace with your actual asset path
        loadTexture("assets/tiktik.png");
        setBoundaryRectangle();

        // belowSensor: thin strip directly under Tiktik's feet
        belowSensor = new BaseActor(0, 0, stage);
        belowSensor.loadTexture("assets/white.png");
        belowSensor.setSize(getWidth() - 8f, 6f);
        belowSensor.setBoundaryRectangle();
        belowSensor.setVisible(false);

        // edgeSensor: tiny square one step ahead at ground level
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

        // Always apply gravity so Tiktik falls if knocked off a platform
        applyGravity(dt);

        // While stunned: freeze in place, count down timer
        if (isStunned)
        {
            stunTimer -= dt;
            velocityVec.x = 0;

            if (stunTimer <= 0f)
                isStunned = false;

            // Still move vertically (gravity), but no horizontal patrol
            moveBy(velocityVec.x * dt, velocityVec.y * dt);
            updateSensorPositions();
            return;
        }

        // Normal patrol behaviour (identical to Crawlid, different speed)
        updateSensorPositions();

        if (isOnGround() && !edgeAheadHasGround())
            flip();

        velocityVec.x = direction * SPEED;

        moveBy(velocityVec.x * dt, velocityVec.y * dt);
        updateSensorPositions();

        setScaleX(direction);
    }

    /**
     * Override: apply damage AND trigger stun.
     * When health reaches zero the enemy is removed from the Stage.
     */
    @Override
    public void takeDamage(int amount)
    {
        health -= amount;

        if (health <= 0)
        {
            remove();
            return;
        }

        // Stun: freeze patrol for STUN_DURATION seconds
        isStunned  = true;
        stunTimer  = STUN_DURATION;
        velocityVec.x = 0;
    }
}