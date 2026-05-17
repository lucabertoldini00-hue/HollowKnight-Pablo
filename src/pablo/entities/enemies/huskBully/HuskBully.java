// HuskBully.java
// Heavy ground enemy. Slow patrol, telegraphed leap attack, long landing recovery.
// On death: hit-stop → knockback launch → airborne spin → tumble landing → hold corpse.

package pablo.entities.enemies.huskBully;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.scenes.scene2d.Stage;
import pablo.entities.enemies.Enemy;
import pablo.entities.player.Pablo;
import pablo.framework.BaseActor;

public class HuskBully extends Enemy
{
    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------
    private static final String PATH = "assets/HuskBully/";

    // Patrol
    private static final float PATROL_SPEED      = 45f;
    private static final float DETECTION_RADIUS  = 250f;

    // Anticipation timing — frames 1-3 intro, frame 4 held
    private static final float ANTICIPATE_INTRO_DUR = 3 * 0.040f; // 120ms (3 frames × 40ms)
    private static final float ANTICIPATE_HOLD_DUR  = 0.175f;     // hold peak frame 175ms

    // Leap physics — intentionally clunky
    private static final float LEAP_SPEED_Y      = 560f;
    private static final float LEAP_SPEED_X      = 160f;
    private static final float LEAP_GRAVITY      = 1100f; // heavier than normal for clumsy arc
    private static final float MAX_LEAP_FALL     = 900f;

    // Landing cooldown — punish window
    private static final float COOLDOWN_DURATION = 0.350f;

    // Death
    private static final float HIT_STOP_DURATION    = 0.100f;
    private static final float DEATH_KNOCKBACK_X    = 330f;
    private static final float DEATH_KNOCKBACK_Y    = 420f;
    private static final float DEATH_GRAVITY        = 1400f;
    private static final float MAX_FALL_SPEED       = 1200f;
    private static final float SPIN_SPEED           = 470f;
    private static final float LAND_TUMBLE_DURATION = 7 * 0.066f; // frames 1-7 at 66ms each

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------
    private HuskBullyState state;
    private float stateTimer;

    // -------------------------------------------------------------------------
    // Death fields
    // -------------------------------------------------------------------------
    private float hitStopTimer;
    private float deathKnockbackDir; // +1 or -1
    private float spinDir;

    // -------------------------------------------------------------------------
    // Leap fields
    // -------------------------------------------------------------------------
    private float leapDir; // direction locked at launch (+1 or -1)

    // -------------------------------------------------------------------------
    // Player reference
    // -------------------------------------------------------------------------
    private Pablo pablo;

    // -------------------------------------------------------------------------
    // Animations
    // -------------------------------------------------------------------------
    private Animation animIdle;
    private Animation animWalk;
    private Animation animTurn;
    private Animation animAnticipateIntro; // attack(anticipate)1-3 at 40ms each
    private Animation animAnticipateHold;  // attack(anticipate)4 held via loop
    private Animation animLunge1;          // ascending phase
    private Animation animLunge2;          // descending phase
    private Animation animCooldown;        // attack(cooldown)1 held
    private Animation animDeathAir;        // death(air) held
    private Animation animDeathLand;       // death(land)1-7 tumble
    private Animation animDeathCorpse;     // death(land)8 held forever

    // =========================================================================
    // Constructor
    // =========================================================================
    public HuskBully(float x, float y, Stage stage, Pablo pablo)
    {
        super(x, y, stage);
        this.pablo = pablo;

        health = 14;

        // --- Idle: 6 frames at 83ms, looping ---
        animIdle = loadAnimationFromFiles(new String[]{
                PATH+"idle1.png", PATH+"idle2.png", PATH+"idle3.png",
                PATH+"idle4.png", PATH+"idle5.png", PATH+"idle6.png"
        }, 0.083f, true);

        // --- Walk: 7 frames at 83ms, looping ---
        animWalk = loadAnimationFromFiles(new String[]{
                PATH+"walk1.png", PATH+"walk2.png", PATH+"walk3.png",
                PATH+"walk4.png", PATH+"walk5.png", PATH+"walk6.png",
                PATH+"walk7.png"
        }, 0.083f, true);

        // --- Turn: 2 frames at 83ms, plays once ---
        animTurn = loadAnimationFromFiles(new String[]{
                PATH+"turn1.png", PATH+"turn2.png"
        }, 0.083f, false);

        // --- Anticipate intro: frames 1-3 at 40ms each, plays once ---
        animAnticipateIntro = loadAnimationFromFiles(new String[]{
                PATH+"attack(anticipate)1.png",
                PATH+"attack(anticipate)2.png",
                PATH+"attack(anticipate)3.png"
        }, 0.040f, false);

        // --- Anticipate hold: frame 4 looped so it stays frozen ---
        animAnticipateHold = loadAnimationFromFiles(new String[]{
                PATH+"attack(anticipate)4.png"
        }, 1f, true);

        // --- Lunge ascending: held via loop ---
        animLunge1 = loadAnimationFromFiles(new String[]{
                PATH+"attack(lunge)1.png"
        }, 1f, true);

        // --- Lunge descending: held via loop ---
        animLunge2 = loadAnimationFromFiles(new String[]{
                PATH+"attack(lunge)2.png"
        }, 1f, true);

        // --- Cooldown: single frame, held via loop ---
        animCooldown = loadAnimationFromFiles(new String[]{
                PATH+"attack(cooldown)1.png"
        }, 1f, true);

        // --- Death air: single frame, held via loop ---
        animDeathAir = loadAnimationFromFiles(new String[]{
                PATH+"death(air).png"
        }, 1f, true);

        // --- Death land tumble: frames 1-7 at 66ms, plays once ---
        animDeathLand = loadAnimationFromFiles(new String[]{
                PATH+"death(land)1.png", PATH+"death(land)2.png",
                PATH+"death(land)3.png", PATH+"death(land)4.png",
                PATH+"death(land)5.png", PATH+"death(land)6.png",
                PATH+"death(land)7.png"
        }, 0.066f, false);

        // --- Death corpse: frame 8, held forever ---
        animDeathCorpse = loadAnimationFromFiles(new String[]{
                PATH+"death(land)8.png"
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

        enterState(HuskBullyState.IDLE);
    }

    // =========================================================================
    // act()
    // =========================================================================
    @Override
    public void act(float dt)
    {
        super.act(dt);

        if (!canUpdateAI() && !isInDeathSequence())
            return;

        // Hit-stop: freeze completely, then launch into death arc
        if (state == HuskBullyState.HIT_STOP)
        {
            hitStopTimer -= dt;
            if (hitStopTimer <= 0f)
                launchDeath();
            return;
        }

        stateTimer += dt;

        switch (state)
        {
            case IDLE:              tickIdle(dt);       break;
            case PATROL:            tickPatrol(dt);     break;
            case TURNING:           tickTurning(dt);    break;
            case ATTACK_ANTICIPATE: tickAnticipate(dt); break;
            case ATTACK_LUNGE:      tickLunge(dt);      break;
            case ATTACK_COOLDOWN:   tickCooldown(dt);   break;
            case DEAD_AIR:          tickDeadAir(dt);    break;
            case DEAD_LAND:         tickDeadLand(dt);   break;
        }
    }

    // =========================================================================
    // State ticks
    // =========================================================================

    private void tickIdle(float dt)
    {
        applyGravity(dt);
        velocityVec.x = 0;
        moveBy(0, velocityVec.y * dt);
        updateSensorPositions();

        setAnimation(animIdle);
        faceDirection(direction);

        // Aggro check — face and leap
        if (isInDetectionRange())
        {
            faceTarget();
            enterState(HuskBullyState.ATTACK_ANTICIPATE);
        }
    }

    private void tickPatrol(float dt)
    {
        applyGravity(dt);
        updateSensorPositions();

        // Edge ahead with no ground → turn
        if (isOnGround() && !edgeAheadHasGround())
        {
            enterState(HuskBullyState.TURNING);
            return;
        }

        // Spotted Pablo → begin leap wind-up
        if (isInDetectionRange())
        {
            faceTarget();
            enterState(HuskBullyState.ATTACK_ANTICIPATE);
            return;
        }

        velocityVec.x = direction * PATROL_SPEED;
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

        // Once both frames finish: flip horizontally, resume patrol
        if (animTurn.isAnimationFinished(stateTimer))
        {
            flip();
            enterState(HuskBullyState.PATROL);
        }
    }

    private void tickAnticipate(float dt)
    {
        // Stand still, apply gravity to stay grounded
        applyGravity(dt);
        velocityVec.x = 0;
        moveBy(0, velocityVec.y * dt);
        updateSensorPositions();

        float total = ANTICIPATE_INTRO_DUR + ANTICIPATE_HOLD_DUR;

        if (stateTimer < ANTICIPATE_INTRO_DUR)
        {
            // Fast crouch: attack(anticipate)1-3 at 40ms each
            setAnimation(animAnticipateIntro);
        }
        else if (stateTimer < total)
        {
            // Hold peak anticipation frame for readability
            setAnimation(animAnticipateHold);
        }
        else
        {
            // Launch! Lock direction toward Pablo, apply impulse
            if (pablo.getX() > getX())
                leapDir = 1f;
            else
                leapDir = -1f;
            direction = leapDir;
            faceDirection(leapDir);

            velocityVec.x = leapDir * LEAP_SPEED_X;
            velocityVec.y = LEAP_SPEED_Y;

            enterState(HuskBullyState.ATTACK_LUNGE);
        }

        faceDirection(direction);
    }

    private void tickLunge(float dt)
    {
        // Custom heavier gravity — intentionally clumsy arc
        velocityVec.y -= LEAP_GRAVITY * dt;
        velocityVec.y  = Math.max(velocityVec.y, -MAX_LEAP_FALL);

        moveBy(velocityVec.x * dt, velocityVec.y * dt);
        updateSensorPositions();

        // Ascending → lunge1, descending → lunge2
        if (velocityVec.y > 0)
            setAnimation(animLunge1);
        else
            setAnimation(animLunge2);

        syncFacingToHorizontalMovement();

        // Land detection: only when descending and sensor hits ground
        // Small grace period (0.08s) so we don't snap to cooldown on launch frame
        if (velocityVec.y <= 0 && isOnGround() && stateTimer > 0.08f)
        {
            velocityVec.set(0, 0);
            enterState(HuskBullyState.ATTACK_COOLDOWN);
        }
    }

    private void tickCooldown(float dt)
    {
        // Long punish window — no movement, enemy is fully vulnerable
        applyGravity(dt);
        velocityVec.x = 0;
        moveBy(0, velocityVec.y * dt);
        updateSensorPositions();

        setAnimation(animCooldown);
        faceDirection(direction);

        if (stateTimer >= COOLDOWN_DURATION)
        {
            // Recover: attack again if still in range, otherwise patrol
            if (isInDetectionRange())
            {
                faceTarget();
                enterState(HuskBullyState.ATTACK_ANTICIPATE);
            }
            else
            {
                enterState(HuskBullyState.PATROL);
            }
        }
    }

    private void tickDeadAir(float dt)
    {
        // Heavy death arc
        velocityVec.y -= DEATH_GRAVITY * dt;
        velocityVec.y  = Math.max(velocityVec.y, -MAX_FALL_SPEED);

        moveBy(velocityVec.x * dt, velocityVec.y * dt);
        updateSensorPositions();

        setAnimation(animDeathAir);
        setRotation(getRotation() + spinDir * SPIN_SPEED * dt);
        syncFacingToHorizontalMovement();

        if (isOnGround())
        {
            velocityVec.set(0, 0);
            setRotation(0); // snap rotation on impact
            enterState(HuskBullyState.DEAD_LAND);
        }
    }

    private void tickDeadLand(float dt)
    {
        if (stateTimer < LAND_TUMBLE_DURATION)
        {
            // Frames 1-7: tumble with decelerating slide friction
            float progress   = stateTimer / LAND_TUMBLE_DURATION;
            float slideSpeed = 70f * (1f - progress);
            velocityVec.x = deathKnockbackDir * slideSpeed;
            moveBy(velocityVec.x * dt, 0);
            syncFacingToHorizontalMovement();
            setAnimation(animDeathLand);
        }
        else
        {
            // Frame 8: hold forever — no remove(), corpse stays
            velocityVec.set(0, 0);
            setAnimation(animDeathCorpse);
        }
    }

    // =========================================================================
    // Damage
    // =========================================================================

    @Override
    public void takeDamage(int amount)
    {
        // Ignore during death sequence
        if (state == HuskBullyState.HIT_STOP  ||
                state == HuskBullyState.DEAD_AIR   ||
                state == HuskBullyState.DEAD_LAND)
            return;

        health -= amount;

        if (health <= 0)
        {
            health = 0;

            // Knockback direction: opposite of Pablo's position relative to bully
            if (pablo.getX() < getX())
                deathKnockbackDir = 1f;
            else
                deathKnockbackDir = -1f;
            spinDir           = deathKnockbackDir;
            hitStopTimer      = HIT_STOP_DURATION;

            enterState(HuskBullyState.HIT_STOP);
        }
    }

    // Called after hit-stop expires — applies launch velocity
    private void launchDeath()
    {
        velocityVec.x = deathKnockbackDir * DEATH_KNOCKBACK_X;
        velocityVec.y = DEATH_KNOCKBACK_Y;
        enterState(HuskBullyState.DEAD_AIR);
    }

    // =========================================================================
    // Wall hit override
    // =========================================================================

    @Override
    public void onWallHit()
    {
        if (state == HuskBullyState.PATROL)
        {
            // Normal patrol: flip direction
            flip();
        }
        else if (state == HuskBullyState.ATTACK_LUNGE)
        {
            // Hit a wall during leap → abort, enter cooldown (punishable)
            velocityVec.x = 0;
            enterState(HuskBullyState.ATTACK_COOLDOWN);
        }
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
        float dx     = pabloX - selfX;
        float dy     = pabloY - selfY;
        return dx * dx + dy * dy < DETECTION_RADIUS * DETECTION_RADIUS;
    }

    private void faceTarget()
    {
        if (pablo.getX() > getX())
            direction = 1f;
        else
            direction = -1f;
        faceDirection(direction);
    }

    private void enterState(HuskBullyState next)
    {
        state       = next;
        stateTimer  = 0f;
        elapsedTime = 0f; // reset BaseActor animation clock
    }

    private boolean isInDeathSequence()
    {
        return state == HuskBullyState.HIT_STOP ||
                state == HuskBullyState.DEAD_AIR ||
                state == HuskBullyState.DEAD_LAND;
    }
}
