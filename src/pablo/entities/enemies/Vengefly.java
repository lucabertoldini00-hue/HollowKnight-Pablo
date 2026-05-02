// Vengefly.java
// Hovers with sine-wave bob. Detects Pablo → startle squawk → homing chase.
// On death: hit-stop → launch → engine-spun death3 frame → land → hold corpse.
// Death rotation is engine-applied (like Crawlid), NOT sprite-drawn (like Tiktik).

package pablo.entities.enemies;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import pablo.entities.player.Pablo;
import pablo.framework.BaseActor;

public class Vengefly extends Enemy
{
    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------
    private static final String PATH = "assets/Venegefly/"; // folder name has double-e

    private static final float HOVER_AMPLITUDE   = 12f;    // px, sine bob height
    private static final float HOVER_FREQUENCY   = 1.5f;   // oscillations/sec
    private static final float PATROL_RANGE      = 40f;    // px each side of spawn (cos wave)
    private static final float CHASE_SPEED       = 140f;   // max homing speed px/sec
    private static final float CHASE_LERP        = 4f;     // velocity smoothing factor
    private static final float DETECTION_RADIUS  = 200f;   // px; triggers startle

    // Startle timing — spec: 4 × 66ms intro, then 300-400ms hold loop
    private static final float STARTLE_INTRO     = 0.264f; // 4 frames × 66ms
    private static final float STARTLE_HOLD      = 0.350f; // extra loop before chase

    private static final float TURN_DURATION     = 0.132f; // 2 frames × 66ms

    private static final float HIT_STOP_DURATION = 0.100f;
    private static final float DEATH_KNOCKBACK_X = 300f;
    private static final float DEATH_KNOCKBACK_Y = 280f;
    private static final float DEATH_GRAVITY     = 1400f;  // heavy fall arc
    private static final float MAX_FALL_SPEED    = 1200f;
    private static final float SPIN_SPEED        = 520f;   // degrees/sec while dead

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------
    private enum State { HOVERING, TURNING, STARTLE, CHASE, HIT_STOP, DEAD_AIR, DEAD_GROUND }
    private State state;
    private float stateTimer;

    // -------------------------------------------------------------------------
    // Hover anchor
    // -------------------------------------------------------------------------
    private float spawnX;
    private float spawnY;

    // -------------------------------------------------------------------------
    // Death fields
    // -------------------------------------------------------------------------
    private float hitStopTimer;
    private float deathKnockbackDir; // +1 or -1
    private float spinDir;

    // -------------------------------------------------------------------------
    // Player reference
    // -------------------------------------------------------------------------
    private Pablo pablo;

    // -------------------------------------------------------------------------
    // Animations
    // -------------------------------------------------------------------------
    private Animation animIdle;         // idle1-5, 50ms, loop
    private Animation animTurn;         // turn1-2, 66ms, once
    private Animation animStartle;      // startle1-4, 66ms, once (intro)
    private Animation animStartleHold;  // startle3-4, 66ms, loop (mandible hold)
    private Animation animChase;        // chase1-4, 41ms, loop
    private Animation animDeath;        // death1-3, 40ms, NORMAL (holds on death3)

    // =========================================================================
    // Constructor
    // =========================================================================
    public Vengefly(float x, float y, Stage stage, Pablo pablo)
    {
        super(x, y, stage);
        this.pablo  = pablo;
        this.spawnX = x;
        this.spawnY = y;

        health = 4; // one hit from Pablo's 4-dmg nail

        // --- Idle: 5 frames, 50ms each, looping ---
        animIdle = loadAnimationFromFiles(new String[]{
                PATH+"idle1.png", PATH+"idle2.png", PATH+"idle3.png",
                PATH+"idle4.png", PATH+"idle5.png"
        }, 0.050f, true);

        // --- Turn: 2 frames, 66ms each, plays once ---
        animTurn = loadAnimationFromFiles(new String[]{
                PATH+"turn1.png", PATH+"turn2.png"
        }, 0.066f, false);

        // --- Startle intro: 4 frames, 66ms each, plays once ---
        animStartle = loadAnimationFromFiles(new String[]{
                PATH+"startle1.png", PATH+"startle2.png",
                PATH+"startle3.png", PATH+"startle4.png"
        }, 0.066f, false);

        // --- Startle hold: startle3+4 alternating to keep mandibles alive ---
        animStartleHold = loadAnimationFromFiles(new String[]{
                PATH+"startle3.png", PATH+"startle4.png"
        }, 0.066f, true);

        // --- Chase: 4 frames, 41ms each, looping ---
        animChase = loadAnimationFromFiles(new String[]{
                PATH+"chase1.png", PATH+"chase2.png",
                PATH+"chase3.png", PATH+"chase4.png"
        }, 0.041f, true);

        // --- Death: 3 frames, 40ms each, NORMAL — holds on death3 automatically ---
        animDeath = loadAnimationFromFiles(new String[]{
                PATH+"death1.png", PATH+"death2.png", PATH+"death3.png"
        }, 0.040f, false);

        setBoundaryRectangle();

        // belowSensor — only active during DEAD_AIR to detect landing
        belowSensor = new BaseActor(0, 0, stage);
        belowSensor.loadTexture("assets/white.png");
        belowSensor.setSize(getWidth() - 8f, 6f);
        belowSensor.setBoundaryRectangle();
        belowSensor.setVisible(false);

        // edgeSensor not needed — Vengefly never walks on surfaces
        edgeSensor = null;

        enterState(State.HOVERING);
    }

    // =========================================================================
    // act()
    // =========================================================================
    @Override
    public void act(float dt)
    {
        super.act(dt);

        // Hit-stop: freeze, then launch into death arc
        if (state == State.HIT_STOP)
        {
            hitStopTimer -= dt;
            if (hitStopTimer <= 0f)
                launchDeath();
            return;
        }

        stateTimer += dt;

        switch (state)
        {
            case HOVERING:    tickHovering(dt);    break;
            case TURNING:     tickTurning();       break;
            case STARTLE:     tickStartle(dt);     break;
            case CHASE:       tickChase(dt);       break;
            case DEAD_AIR:    tickDeadAir(dt);     break;
            case DEAD_GROUND: tickDeadGround();    break;
        }
    }

    // =========================================================================
    // State ticks
    // =========================================================================

    private void tickHovering(float dt)
    {
        // Engine drives position via sine/cos — not velocityVec
        float targetY = spawnY + MathUtils.sin(elapsedTime * HOVER_FREQUENCY) * HOVER_AMPLITUDE;
        float targetX = spawnX + MathUtils.cos(elapsedTime * 0.5f) * PATROL_RANGE;

        float newX = getX() + (targetX - getX()) * 3f * dt;
        float newY = getY() + (targetY - getY()) * 3f * dt;
        setPosition(newX, newY);

        belowSensor.setPosition(getX() + 4f, getY() - 6f);

        setAnimation(animIdle);

        // Face direction of current horizontal drift
        float dx = targetX - getX();
        if (Math.abs(dx) > 0.5f)
        {
            if (dx >= 0)
                setScaleX(1f);
            else
                setScaleX(-1f);
        }

        // Aggro check every frame
        if (isInDetectionRange())
            enterState(State.STARTLE);
    }

    private void tickTurning()
    {
        // Brief mid-air pivot used when explicitly triggered
        setAnimation(animTurn);
        velocityVec.set(0, 0);

        if (stateTimer >= TURN_DURATION)
        {
            direction *= -1f;
            setScaleX(direction);
            enterState(State.HOVERING);
        }
    }

    private void tickStartle(float dt)
    {
        // Freeze in place while squawking
        velocityVec.set(0, 0);

        if (stateTimer < STARTLE_INTRO)
        {
            // Phase 1: startle1 → startle2 → startle3 → startle4 (once)
            setAnimation(animStartle);
        }
        else if (stateTimer < STARTLE_INTRO + STARTLE_HOLD)
        {
            // Phase 2: loop startle3↔4 to keep mandibles open
            setAnimation(animStartleHold);
        }
        else
        {
            // Phase 3: snap into chase
            enterState(State.CHASE);
        }
    }

    private void tickChase(float dt)
    {
        setAnimation(animChase);

        // Pablo's centre
        float targetX = pablo.getX() + pablo.getWidth()  / 2f;
        float targetY = pablo.getY() + pablo.getHeight() / 2f;

        // Self centre
        float selfX = getX() + getWidth()  / 2f;
        float selfY = getY() + getHeight() / 2f;

        Vector2 toPlayer = new Vector2(targetX - selfX, targetY - selfY);

        if (toPlayer.len() > 1f)
        {
            toPlayer.nor();
            // Lerp velocity — creates a natural swooping arc instead of instant snap
            velocityVec.x += (toPlayer.x * CHASE_SPEED - velocityVec.x) * CHASE_LERP * dt;
            velocityVec.y += (toPlayer.y * CHASE_SPEED - velocityVec.y) * CHASE_LERP * dt;
        }
        else
        {
            velocityVec.set(0, 0);
        }

        moveBy(velocityVec.x * dt, velocityVec.y * dt);
        belowSensor.setPosition(getX() + 4f, getY() - 6f);

        // Face direction of travel
        if (Math.abs(velocityVec.x) > 0.5f)
        {
            if (velocityVec.x >= 0)
                setScaleX(1f);
            else
                setScaleX(-1f);
        }
    }

    private void tickDeadAir(float dt)
    {
        // Heavy gravity for satisfying fall arc
        velocityVec.y -= DEATH_GRAVITY * dt;
        velocityVec.y  = Math.max(velocityVec.y, -MAX_FALL_SPEED);

        moveBy(velocityVec.x * dt, velocityVec.y * dt);
        belowSensor.setPosition(getX() + 4f, getY() - 6f);

        // Hold death3 (NORMAL mode stops there) — engine spins the sprite
        setAnimation(animDeath);
        setRotation(getRotation() + spinDir * SPIN_SPEED * dt);

        if (isOnGround())
        {
            velocityVec.set(0, 0);
            enterState(State.DEAD_GROUND);
        }
    }

    private void tickDeadGround()
    {
        // Stop spinning, hold death3 as the permanent corpse
        setRotation(0);
        velocityVec.set(0, 0);
        setAnimation(animDeath); // death3 held forever
        // No remove() — corpse remains visible
    }

    // =========================================================================
    // Damage
    // =========================================================================

    @Override
    public void takeDamage(int amount)
    {
        if (state == State.HIT_STOP ||
                state == State.DEAD_AIR  ||
                state == State.DEAD_GROUND)
            return;

        health -= amount;

        if (health <= 0)
        {
            health = 0;
            if (pablo.getX() < getX())
                deathKnockbackDir = 1f;
            else
                deathKnockbackDir = -1f;
            spinDir           = deathKnockbackDir;
            hitStopTimer      = HIT_STOP_DURATION;
            enterState(State.HIT_STOP);
        }
    }

    private void launchDeath()
    {
        velocityVec.x = deathKnockbackDir * DEATH_KNOCKBACK_X;
        velocityVec.y = DEATH_KNOCKBACK_Y;
        enterState(State.DEAD_AIR);
    }

    // =========================================================================
    // Wall hit override — bounce during chase; ignore during hover/death
    // =========================================================================

    @Override
    public void onWallHit()
    {
        if (state == State.CHASE)
            velocityVec.x *= -1f;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private boolean isInDetectionRange()
    {
        float selfX  = getX() + getWidth()  / 2f;
        float selfY  = getY() + getHeight() / 2f;
        float pabloX = pablo.getX() + pablo.getWidth()  / 2f;
        float pabloY = pablo.getY() + pablo.getHeight() / 2f;
        float dx = pabloX - selfX;
        float dy = pabloY - selfY;
        return (dx * dx + dy * dy) < (DETECTION_RADIUS * DETECTION_RADIUS);
    }

    private void enterState(State next)
    {
        state       = next;
        stateTimer  = 0f;
        elapsedTime = 0f;
        velocityVec.set(0, 0);
    }
}