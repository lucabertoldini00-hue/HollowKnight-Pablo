// Crawlid.java
// Peaceful ground patroller. Turns at edges and walls.
// On death: hit-stop → knockback launch → airborne spin → ground impact → hold corpse.

package pablo.entities.enemies.crawlid;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.scenes.scene2d.Stage;
import pablo.entities.enemies.Enemy;
import pablo.entities.player.Pablo;
import pablo.framework.BaseActor;

public class Crawlid extends Enemy
{
    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------
    private static final String PATH = "assets/Crawlid/";

    private static final float SPEED             = 60f;
    private static final float DEATH_KNOCKBACK_X = 320f;
    private static final float DEATH_KNOCKBACK_Y = 420f;
    private static final float DEATH_GRAVITY     = 1400f;  // heavier than normal gravity
    private static final float MAX_FALL_SPEED    = 1200f;
    private static final float SPIN_SPEED        = 480f;   // degrees/sec while airborne
    private static final float HIT_STOP_DURATION = 0.075f;
    private static final float LAND1_HOLD        = 0.065f; // how long death(land)1 is shown

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------
    private CrawlidState state;
    private float stateTimer;

    // -------------------------------------------------------------------------
    // Death fields
    // -------------------------------------------------------------------------
    private float hitStopTimer;
    private float deathKnockbackDir; // +1 or -1 based on Pablo's position
    private float spinDir;           // +1 CCW, -1 CW

    // -------------------------------------------------------------------------
    // Player reference — needed only for death knockback direction
    // -------------------------------------------------------------------------
    private Pablo pablo;

    // -------------------------------------------------------------------------
    // Animations
    // -------------------------------------------------------------------------
    private Animation animWalk;
    private Animation animTurn;
    private Animation animDeathAir;   // 3 frames; holds on frame 3 (PlayMode.NORMAL)
    private Animation animDeathLand1; // squash impact — held briefly
    private Animation animDeathLand2; // final corpse — held forever

    // =========================================================================
    // Constructor
    // =========================================================================
    public Crawlid(float x, float y, Stage stage, Pablo pablo)
    {
        super(x, y, stage);
        this.pablo = pablo;

        health = 8;

        // --- Walk: 4 frames, ~83ms each, looping ---
        animWalk = loadAnimationFromFiles(new String[]{
                PATH+"walk1.png", PATH+"walk2.png",
                PATH+"walk3.png", PATH+"walk4.png"
        }, 0.083f, true);

        // --- Turn: 2 frames, ~100ms each, plays once then we flip and patrol ---
        animTurn = loadAnimationFromFiles(new String[]{
                PATH+"turn1.png", PATH+"turn2.png"
        }, 0.100f, false);

        // --- Death air: 3 frames at 50ms, sticks on frame 3 (NORMAL = no loop) ---
        animDeathAir = loadAnimationFromFiles(new String[]{
                PATH+"death(air)1.png",
                PATH+"death(air)2.png",
                PATH+"death(air)3.png"
        }, 0.050f, false);

        // --- Death land 1: squash impact, held for LAND1_HOLD seconds ---
        animDeathLand1 = loadAnimationFromFiles(new String[]{
                PATH+"death(land)1.png"
        }, LAND1_HOLD, false);

        // --- Death land 2: corpse at rest, held indefinitely ---
        animDeathLand2 = loadAnimationFromFiles(new String[]{
                PATH+"death(land)2.png"
        }, 1f, true);

        setBoundaryRectangle();

        // Below sensor — detects ground under feet
        belowSensor = new BaseActor(0, 0, stage);
        belowSensor.loadTexture("assets/white.png");
        belowSensor.setSize(getWidth() - 8f, 6f);
        belowSensor.setBoundaryRectangle();
        belowSensor.setVisible(false);

        // Edge sensor — detects if ground exists one step ahead
        edgeSensor = new BaseActor(0, 0, stage);
        edgeSensor.loadTexture("assets/white.png");
        edgeSensor.setSize(6f, 6f);
        edgeSensor.setBoundaryRectangle();
        edgeSensor.setVisible(false);

        // Posiziona i sensori subito per il primo frame
        updateSensorPositions();

        enterState(CrawlidState.PATROL);
    }

    // =========================================================================
    // act()
    // =========================================================================
    @Override
    public void act(float dt)
    {
        super.act(dt);

        if (!canUpdateAI())
            return;

        // Hit-stop: freeze in place, then launch
        if (state == CrawlidState.HIT_STOP)
        {
            hitStopTimer -= dt;
            if (hitStopTimer <= 0f)
                launchDeath();
            return; // no movement, no animation switch
        }

        stateTimer += dt;

        switch (state)
        {
            case PATROL:    tickPatrol(dt);   break;
            case TURNING:   tickTurning(dt);  break;
            case DEAD_AIR:  tickDeadAir(dt);  break;
            case DEAD_LAND: tickDeadLand();   break;
        }
    }

    // =========================================================================
    // State ticks
    // =========================================================================

    private void tickPatrol(float dt)
    {
        applyGravity(dt);
        updateSensorPositions();

        boolean onGround = isOnGround();
        com.badlogic.gdx.Gdx.app.log("CrawlidDebug", "x=" + getX() + " y=" + getY()
            + " onGround=" + onGround
            + " belowSensor=" + (belowSensor != null ? belowSensor.getX() + "," + belowSensor.getY() : "null"));

        // At a ledge with no ground ahead → start turn
        if (onGround && !edgeAheadHasGround())
        {
            enterState(CrawlidState.TURNING);
            return;
        }

        velocityVec.x = direction * SPEED;
        moveBy(velocityVec.x * dt, velocityVec.y * dt);
        updateSensorPositions();

        setAnimation(animWalk);
        syncFacingToHorizontalMovement();
    }

    private void tickTurning(float dt)
    {
        applyGravity(dt);
        velocityVec.x = 0;
        moveBy(0, velocityVec.y * dt);
        updateSensorPositions();

        setAnimation(animTurn);

        // Once both turn frames finish: flip horizontally, resume patrol
        if (animTurn.isAnimationFinished(stateTimer))
        {
            flip();
            enterState(CrawlidState.PATROL);
        }
    }

    private void tickDeadAir(float dt)
    {
        // Heavier-than-normal gravity for a satisfying fall arc
        velocityVec.y -= DEATH_GRAVITY * dt;
        velocityVec.y  = Math.max(velocityVec.y, -MAX_FALL_SPEED);

        moveBy(velocityVec.x * dt, velocityVec.y * dt);
        updateSensorPositions();

        // Spin the sprite while airborne — rotation accumulates each frame
        setRotation(getRotation() + spinDir * SPIN_SPEED * dt);

        // Animation: plays through frames 1→2→3, then holds on 3 automatically
        setAnimation(animDeathAir);
        syncFacingToHorizontalMovement();

        // Land detection via below sensor
        if (isOnGround())
        {
            velocityVec.set(0, 0);
            enterState(CrawlidState.DEAD_LAND);
        }
    }

    private void tickDeadLand()
    {
        // Stop spinning the moment the corpse hits the floor
        setRotation(0);
        velocityVec.set(0, 0);

        // Show squash frame briefly, then hold the corpse frame forever
        if (stateTimer < LAND1_HOLD)
            setAnimation(animDeathLand1);
        else
            setAnimation(animDeathLand2);

        // Intentionally no remove() — corpse stays on the floor
    }

    // =========================================================================
    // Damage
    // =========================================================================

    @Override
    public void takeDamage(int amount)
    {
        // Ignore hits during death sequence
        if (state == CrawlidState.HIT_STOP ||
                state == CrawlidState.DEAD_AIR ||
                state == CrawlidState.DEAD_LAND)
            return;

        health -= amount;

        if (health <= 0)
        {
            health = 0;

            if (pablo.getX() < getX())
                deathKnockbackDir = 1f;
            else
                deathKnockbackDir = -1f;

            hitStopTimer = HIT_STOP_DURATION;
            enterState(CrawlidState.HIT_STOP);
        }
        // If health > 0 (future multi-hit scenarios): could add a stun here
    }

    // Called after hit-stop expires — applies launch velocity and enters DEAD_AIR
    private void launchDeath()
    {
        spinDir        = deathKnockbackDir; // spin direction matches knockback side
        velocityVec.x  = deathKnockbackDir * DEATH_KNOCKBACK_X;
        velocityVec.y  = DEATH_KNOCKBACK_Y;
        enterState(CrawlidState.DEAD_AIR);
    }

    // =========================================================================
    // Wall hit override
    // =========================================================================

    @Override
    public void onWallHit()
    {
        // Only flip direction during normal patrol — ignore walls while flying dead
        if (state == CrawlidState.PATROL)
            flip();
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private void enterState(CrawlidState next)
    {
        state       = next;
        stateTimer  = 0f;
        elapsedTime = 0f; // reset BaseActor animation clock
    }
}

