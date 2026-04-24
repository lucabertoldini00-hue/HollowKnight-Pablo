// Enemy.java
// Base class for all ground-based enemies (Crawlid, Tiktik).
// Vengefly also extends this but ignores the floor sensors and gravity.
//
// What lives here:  health, direction, gravity, floor sensors, flip(), onWallHit(), takeDamage()
// What stays in subclasses: speed, stun logic, AI state, patrol/hover behaviour

package pablo.entities.enemies;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import pablo.Object;
import pablo.framework.BaseActor;

public abstract class Enemy extends BaseActor
{
    // --- shared physics constants (subclasses can override by setting these in their constructor) ---
    protected float gravity          = 700f;
    protected float maxVerticalSpeed = 700f;

    // --- shared state ---
    protected int   health;
    protected float direction; // 1 = right, -1 = left

    // --- floor sensors (ground-based enemies only; Vengefly leaves these null) ---
    protected BaseActor belowSensor;
    protected BaseActor edgeSensor;

    public Enemy(float x, float y, Stage stage)
    {
        super(x, y, stage);
        direction = 1f; // default: start moving right
    }

    // ------------------------------------------------------------------
    // Shared helpers — called by subclasses from their own act() methods
    // ------------------------------------------------------------------

    /**
     * Applies downward gravity to velocityVec.y and clamps to maxVerticalSpeed.
     * Ground-based enemies call this every frame. Vengefly skips it.
     */
    protected void applyGravity(float dt)
    {
        velocityVec.y -= gravity * dt;
        velocityVec.y  = MathUtils.clamp(velocityVec.y, -maxVerticalSpeed, maxVerticalSpeed);
    }

    /**
     * Reverses horizontal direction and mirrors the sprite accordingly.
     */
    protected void flip()
    {
        direction *= -1f;
        setScaleX(direction);
    }

    /**
     * Called by LevelScreen when preventOverlap() returns an X-dominant offset (wall hit).
     * Default behaviour: flip direction. Subclasses may override.
     */
    public void onWallHit()
    {
        flip();
    }

    /**
     * Stub — wired up to the combat system in a later phase.
     * Subclasses override to add stun, animations, etc.
     */
    public void takeDamage(int amount)
    {
        health -= amount;
        if (health <= 0)
            remove(); // removes this actor from the Stage
    }

    // ------------------------------------------------------------------
    // Sensor helpers — used by ground-based enemies
    // ------------------------------------------------------------------

    /**
     * True if belowSensor overlaps any enabled solid Object.
     * Returns false if belowSensor was never initialised (e.g. Vengefly).
     */
    protected boolean isOnGround()
    {
        if (belowSensor == null) return false;

        for (BaseActor actor : BaseActor.getList(getStage(), Object.class.getName()))
        {
            Object solid = (Object) actor;
            if (belowSensor.overlaps(solid) && solid.isEnable())
                return true;
        }
        return false;
    }

    /**
     * True if edgeSensor overlaps any enabled solid Object ahead of the enemy.
     * When this returns false the enemy is at an edge and should flip.
     * Returns false if edgeSensor was never initialised.
     */
    protected boolean edgeAheadHasGround()
    {
        if (edgeSensor == null) return false;

        for (BaseActor actor : BaseActor.getList(getStage(), Object.class.getName()))
        {
            Object solid = (Object) actor;
            if (edgeSensor.overlaps(solid) && solid.isEnable())
                return true;
        }
        return false;
    }

    /**
     * Repositions both sensors relative to the enemy's current position.
     * Must be called after any movement. Ground-based enemies call this
     * at the top and bottom of act(). Vengefly skips it.
     */
    protected void updateSensorPositions()
    {
        if (belowSensor != null)
            belowSensor.setPosition(getX() + 4f, getY() - 6f);

        if (edgeSensor != null)
        {
            float edgeX = (direction > 0)
                    ? getX() + getWidth()
                    : getX() - 6f;
            edgeSensor.setPosition(edgeX, getY() - 6f);
        }
    }
}