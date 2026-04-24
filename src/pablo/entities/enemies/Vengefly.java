// Vengefly.java
// Hovers near its spawn point with a gentle sine-wave drift.
// When Pablo enters detection range (200px), switches to CHASING state and homes in.
// No gravity, no floor sensors — this enemy never touches the ground.

package pablo.entities.enemies;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import pablo.entities.player.Pablo;
import pablo.framework.BaseActor;

public class Vengefly extends Enemy
{
    // --- tuning ---
    private static final float HOVER_SPEED       = 30f;  // horizontal drift while hovering
    private static final float CHASE_SPEED       = 120f; // homing speed toward Pablo
    private static final float DETECTION_RADIUS  = 200f; // pixels; triggers chase
    private static final float HOVER_AMPLITUDE   = 12f;  // px up/down sine drift
    private static final float HOVER_FREQUENCY   = 1.5f; // oscillations per second

    private enum State { HOVERING, CHASING }
    private State state;

    // Spawn position used as the anchor for hover drift
    private float spawnX;
    private float spawnY;

    // Reference to Pablo — set once in constructor, never cached differently
    private Pablo pablo;

    public Vengefly(float x, float y, Stage stage, Pablo pablo)
    {
        super(x, y, stage);

        this.pablo  = pablo;
        this.spawnX = x;
        this.spawnY = y;
        this.state  = State.HOVERING;

        health = 3;

        // No floor sensors needed — Vengefly is airborne at all times
        // belowSensor and edgeSensor remain null (handled safely in Enemy)

        // Load texture — replace with your actual asset path
        loadTexture("assets/vengefly.png");
        setBoundaryRectangle();
    }

    @Override
    public void act(float dt)
    {
        super.act(dt);
        // No applyGravity — Vengefly hovers freely

        switch (state)
        {
            case HOVERING: hover(dt); break;
            case CHASING:  chase(dt); break;
        }

        // Check distance to Pablo every frame — if in range switch to chase
        if (state == State.HOVERING && isInDetectionRange())
            state = State.CHASING;
    }

    // ------------------------------------------------------------------
    // Private behaviour methods
    // ------------------------------------------------------------------

    /**
     * Sine-wave vertical drift around the spawn point.
     * Vengefly bobs up and down while slowly patrolling horizontally.
     */
    private void hover(float dt)
    {
        // Vertical position driven entirely by sine wave (no velocityVec.y used here)
        float targetY = spawnY + (float) Math.sin(elapsedTime * HOVER_FREQUENCY) * HOVER_AMPLITUDE;

        // Slow horizontal drift back and forth (uses elapsedTime as phase)
        float targetX = spawnX + (float) Math.cos(elapsedTime * 0.4f) * 30f;

        // Lerp toward the target position for smooth movement
        float newX = getX() + (targetX - getX()) * 3f * dt;
        float newY = getY() + (targetY - getY()) * 3f * dt;

        setPosition(newX, newY);

        // Face the direction of horizontal movement
        float dx = newX - getX();
        if (Math.abs(dx) > 0.5f)
            setScaleX(dx > 0 ? 1f : -1f);
    }

    /**
     * Moves directly toward Pablo's centre at CHASE_SPEED pixels/second.
     */
    private void chase(float dt)
    {
        // Pablo's centre
        float targetX = pablo.getX() + pablo.getWidth()  / 2f;
        float targetY = pablo.getY() + pablo.getHeight() / 2f;

        // This enemy's centre
        float selfX = getX() + getWidth()  / 2f;
        float selfY = getY() + getHeight() / 2f;

        // Direction vector toward Pablo
        Vector2 toPlayer = new Vector2(targetX - selfX, targetY - selfY);

        if (toPlayer.len() > 1f) // avoid division by zero when already on top
        {
            toPlayer.nor(); // normalise to unit vector
            velocityVec.set(toPlayer.x * CHASE_SPEED, toPlayer.y * CHASE_SPEED);
        }
        else
        {
            velocityVec.set(0, 0);
        }

        moveBy(velocityVec.x * dt, velocityVec.y * dt);

        // Face Pablo
        setScaleX(velocityVec.x >= 0 ? 1f : -1f);
    }

    /**
     * Euclidean distance from this enemy's centre to Pablo's centre.
     * Returns true when Pablo is within DETECTION_RADIUS pixels.
     */
    private boolean isInDetectionRange()
    {
        float selfX   = getX() + getWidth()  / 2f;
        float selfY   = getY() + getHeight() / 2f;
        float pabloX  = pablo.getX() + pablo.getWidth()  / 2f;
        float pabloY  = pablo.getY() + pablo.getHeight() / 2f;

        float dx = pabloX - selfX;
        float dy = pabloY - selfY;

        return (dx * dx + dy * dy) < (DETECTION_RADIUS * DETECTION_RADIUS);
    }
}