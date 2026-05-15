// Tiktik.java
// Brisk ground patroller. Stuns briefly when hit but survives.
// On death: hit-stop → launch → looping tumble anim → squash land → hold corpse.
// Tumbling is drawn into the sprites — no engine rotation needed.

package pablo.entities.enemies.tiktik;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.scenes.scene2d.Stage;
import pablo.entities.enemies.Enemy;
import pablo.entities.enemies.crawlid.CrawlidState;
import pablo.entities.player.Pablo;
import pablo.framework.BaseActor;

public class Tiktik extends Enemy
{
    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------
    private static final String PATH = "assets/Tiktik/";

    private static final float SPEED             = 90f;
    private static final float DEATH_KNOCKBACK_X = 380f;
    private static final float DEATH_KNOCKBACK_Y = 450f;
    private static final float DEATH_GRAVITY     = 1400f;
    private static final float MAX_FALL_SPEED    = 1200f;
    private static final float HIT_STOP_DURATION = 0.125f; // spec: 100ms–150ms, use midpoint
    private static final float STUN_ANIM_TOTAL   = 0.200f; // stun1 + stun2 = 2 × 100ms
    private static final float LAND1_HOLD        = 0.050f; // squash frame: 50ms

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------
    private TiktikState state;
    private float stateTimer;

    // -------------------------------------------------------------------------
    // Death fields
    // -------------------------------------------------------------------------
    private float hitStopTimer;
    private float deathKnockbackDir; // +1 or -1

    // -------------------------------------------------------------------------
    // Player reference — only used to determine knockback direction on death
    // -------------------------------------------------------------------------
    private Pablo pablo;

    // -------------------------------------------------------------------------
    // Animations
    // -------------------------------------------------------------------------
    private Animation animWalk;
    private Animation animStun;       // stun1 + stun2, plays once
    private Animation animDeathAir;   // 7-frame LOOP — sprites draw the tumble
    private Animation animDeathLand1; // squash impact, held ~50ms
    private Animation animDeathLand2; // final corpse, held forever

    // =========================================================================
    // Constructor
    // =========================================================================
    public Tiktik(float x, float y, Stage stage, Pablo pablo)
    {
        super(x, y, stage);
        this.pablo = pablo;

        health = 8; // takes 2 hits from Pablo (4 dmg each) — stun visible on first hit

        // --- Walk: 4 frames, 83ms each, looping ---
        animWalk = loadAnimationFromFiles(new String[]{
                PATH+"walk1.png", PATH+"walk2.png",
                PATH+"walk3.png", PATH+"walk4.png"
        }, 0.083f, true);

        // --- Stun: 2 frames at 100ms each, plays once ---
        animStun = loadAnimationFromFiles(new String[]{
                PATH+"stun1.png", PATH+"stun2.png"
        }, 0.100f, false);

        // --- Death air: 7 frames at 40ms, LOOPING — sprites handle the spin ---
        animDeathAir = loadAnimationFromFiles(new String[]{
                PATH+"death(air)1.png", PATH+"death(air)2.png",
                PATH+"death(air)3.png", PATH+"death(air)4.png",
                PATH+"death(air)5.png", PATH+"death(air)6.png",
                PATH+"death(air)7.png"
        }, 0.040f, true); // true = loop

        // --- Death land 1: squash, held 50ms ---
        animDeathLand1 = loadAnimationFromFiles(new String[]{
                PATH+"death(land)1.png"
        }, LAND1_HOLD, false);

        // --- Death land 2: corpse, held forever ---
        animDeathLand2 = loadAnimationFromFiles(new String[]{
                PATH+"death(land)2.png"
        }, 1f, true);

        // RIPRISTINA L'ANIMAZIONE DI BASE PER AVERE I BOUNDS CORRETTI!
        setAnimation(animWalk);

        setBoundaryRectangle();

        belowSensor = new BaseActor(0, 0, stage);
        belowSensor.loadTexture("assets/white.png");
        belowSensor.setSize(getWidth() - 8f, 6f);
        belowSensor.setBoundaryRectangle();
        belowSensor.setVisible(false);

        edgeSensor = new BaseActor(0, 0, stage);
        edgeSensor.loadTexture("assets/white.png");
        edgeSensor.setSize(6f, 6f);
        edgeSensor.setBoundaryRectangle();
        edgeSensor.setVisible(false);

        // Posiziona i sensori subito per il primo frame
        updateSensorPositions();

        enterState(TiktikState.PATROL);
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

        if (removeIfBelowVoid())
            return;

        // Hit-stop: freeze everything, then launch into death
        if (state == TiktikState.HIT_STOP)
        {
            hitStopTimer -= dt;
            if (hitStopTimer <= 0f)
                launchDeath();
            return;
        }

        stateTimer += dt;

        switch (state)
        {
            case PATROL:    tickPatrol(dt);   break;
            case TURNING:   tickTurning(dt);  break;
            case STUNNED:   tickStunned(dt);  break;
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

        if (isOnGround() && !edgeAheadHasGround())
        {
            enterState(TiktikState.TURNING);
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
        // Brief edge pause — reuse stun anim timing (200ms) then flip
        applyGravity(dt);
        velocityVec.x = 0;
        moveBy(0, velocityVec.y * dt);
        updateSensorPositions();

        setAnimation(animWalk); // hold walk frame while turning (no separate turn sprite)

        if (stateTimer >= 0.10f) // very short pause before flipping
        {
            flip();
            enterState(TiktikState.PATROL);
        }
    }

    private void tickStunned(float dt)
    {
        // Play stun1→stun2, brief recoil slide, then resume patrol reversed
        applyGravity(dt);

        // Small recoil slide opposite to travel direction — decelerates naturally
        float recoilSpeed = Math.max(0f, 60f - (stateTimer / STUN_ANIM_TOTAL) * 60f);
        velocityVec.x = -direction * recoilSpeed;

        moveBy(velocityVec.x * dt, velocityVec.y * dt);
        updateSensorPositions();

        setAnimation(animStun);
        syncFacingToHorizontalMovement();

        if (stateTimer >= STUN_ANIM_TOTAL)
        {
            flip(); // reverse direction after stun
            enterState(TiktikState.PATROL);
        }
    }

    private void tickDeadAir(float dt)
    {
        velocityVec.y -= DEATH_GRAVITY * dt;
        velocityVec.y  = Math.max(velocityVec.y, -MAX_FALL_SPEED);

        moveBy(velocityVec.x * dt, velocityVec.y * dt);
        updateSensorPositions();

        // Looping 7-frame tumble — sprite art handles the visual spin, no setRotation needed
        setAnimation(animDeathAir);
        syncFacingToHorizontalMovement();

        if (isOnGround())
        {
            velocityVec.set(0, 0);
            enterState(TiktikState.DEAD_LAND);
        }
    }

    private void tickDeadLand()
    {
        velocityVec.set(0, 0);

        // Show squash for 50ms, then hold corpse forever
        if (stateTimer < LAND1_HOLD)
            setAnimation(animDeathLand1);
        else
            setAnimation(animDeathLand2);

        // No remove() — corpse stays on the floor
    }

    // =========================================================================
    // Damage
    // =========================================================================

    @Override
    public void takeDamage(int amount)
    {
        // Ignore hits during death sequence
        if (state == TiktikState.HIT_STOP ||
                state == TiktikState.DEAD_AIR  ||
                state == TiktikState.DEAD_LAND)
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
            enterState(TiktikState.HIT_STOP);
        }
        else
        {
            // Survived the hit — play stun anim and recoil
            enterState(TiktikState.STUNNED);
        }
    }

    // Applies launch velocity after hit-stop expires
    private void launchDeath()
    {
        velocityVec.x = deathKnockbackDir * DEATH_KNOCKBACK_X;
        velocityVec.y = DEATH_KNOCKBACK_Y;
        enterState(TiktikState.DEAD_AIR);
    }

    // =========================================================================
    // Wall hit override
    // =========================================================================

    @Override
    public void onWallHit()
    {
        // Only flip during normal patrol — ignore walls while stunned or dead
        if (state == TiktikState.PATROL)
            flip();
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private void enterState(TiktikState next)
    {
        state       = next;
        stateTimer  = 0f;
        elapsedTime = 0f;
    }

    // Tiktik sprite faces RIGHT by default (unlike all other enemies).
    @Override
    protected void faceDirection(float horizontalDirection)
    {
        if (horizontalDirection > 0f)
            setScaleX(1f);    // no flip — sprite already faces right
        else if (horizontalDirection < 0f)
            setScaleX(-1f);   // flip to face left
    }
}

