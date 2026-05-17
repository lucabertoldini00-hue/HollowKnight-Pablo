// HuskHornhead.java
package pablo.entities.enemies.huskHornhead;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.scenes.scene2d.Stage;
import pablo.entities.enemies.Enemy;
import pablo.entities.player.Pablo;
import pablo.framework.BaseActor;

public class HuskHornhead extends Enemy
{
    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------
    private static final String PATH = "assets/HuskHornhead/";

    private static final float PATROL_SPEED       = 55f;
    private static final float LUNGE_SPEED        = 360f;
    private static final float LUNGE_MAX_DURATION = 1.2f;
    private static final float DETECTION_RADIUS   = 220f;

    private static final float ANTICIPATE_INTRO_DUR = 0.160f;
    private static final float ANTICIPATE_HOLD_DUR  = 0.175f;

    private static final float COOLDOWN_DURATION = 0.250f;

    private static final float DEATH_KNOCKBACK_X = 340f;
    private static final float DEATH_KNOCKBACK_Y = 400f;
    private static final float DEATH_GRAVITY     = 1400f;
    private static final float MAX_FALL_SPEED    = 1200f;
    private static final float SPIN_SPEED        = 460f;
    private static final float HIT_STOP_DURATION = 0.125f;

    private static final float LAND_TUMBLE_DURATION = 7 * 0.066f;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------
    private HuskHornheadState state;
    private float stateTimer;

    private float hitStopTimer;
    private float deathKnockbackDir;
    private float spinDir;
    private float chargeDir;

    private Pablo pablo;

    // -------------------------------------------------------------------------
    // Animations
    // -------------------------------------------------------------------------
    private Animation animIdle;
    private Animation animWalk;
    private Animation animTurn;
    private Animation animAnticipateIntro;
    private Animation animAnticipateHold;
    private Animation animLunge;
    private Animation animCooldown;
    private Animation animDeathAir;
    private Animation animDeathLand;
    private Animation animDeathCorpse;

    // =========================================================================
    // Constructor
    // =========================================================================
    public HuskHornhead(float x, float y, Stage stage, Pablo pablo)
    {
        super(x, y, stage);
        this.pablo = pablo;

        health = 12;

        animIdle = loadAnimationFromFiles(new String[]{
                PATH + "idle1.png", PATH + "idle2.png", PATH + "idle3.png",
                PATH + "idle4.png", PATH + "idle5.png", PATH + "idle6.png"
        }, 0.083f, true);

        animWalk = loadAnimationFromFiles(new String[]{
                PATH + "walk1.png", PATH + "walk2.png", PATH + "walk3.png",
                PATH + "walk4.png", PATH + "walk5.png", PATH + "walk6.png",
                PATH + "walk7.png"
        }, 0.083f, true);

        animTurn = loadAnimationFromFiles(new String[]{
                PATH + "turn1.png", PATH + "turn2.png"
        }, 0.083f, false);

        animAnticipateIntro = loadAnimationFromFiles(new String[]{
                PATH + "attack(anticipate)1.png",
                PATH + "attack(anticipate)2.png",
                PATH + "attack(anticipate)3.png",
                PATH + "attack(anticipate)4.png"
        }, 0.040f, false);

        animAnticipateHold = loadAnimationFromFiles(new String[]{
                PATH + "attack(anticipate)5.png"
        }, 1f, true);

        animLunge = loadAnimationFromFiles(new String[]{
                PATH + "attack(lunge)1.png",
                PATH + "attack(lunge)2.png",
                PATH + "attack(lunge)3.png",
                PATH + "attack(lunge)4.png"
        }, 0.050f, true);

        animCooldown = loadAnimationFromFiles(new String[]{
                PATH + "attack(cooldown).png"
        }, 1f, true);

        animDeathAir = loadAnimationFromFiles(new String[]{
                PATH + "death(air).png"
        }, 1f, true);

        animDeathLand = loadAnimationFromFiles(new String[]{
                PATH + "death(land)1.png",
                PATH + "death(land)2.png",
                PATH + "death(land)3.png",
                PATH + "death(land)4.png",
                PATH + "death(land)5.png",
                PATH + "death(land)6.png",
                PATH + "death(land)7.png"
        }, 0.066f, false);

        animDeathCorpse = loadAnimationFromFiles(new String[]{
                PATH + "death(land)8.png"
        }, 1f, true);

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

        enterState(HuskHornheadState.IDLE);
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
        if (state == HuskHornheadState.HIT_STOP)
        {
            hitStopTimer -= dt;
            if (hitStopTimer <= 0f)
                launchDeath();
            return;
        }

        stateTimer += dt;

        switch (state)
        {
            case IDLE:
                tickIdle(dt);
                break;

            case PATROL:
                tickPatrol(dt);
                break;

            case TURNING:
                tickTurning(dt);
                break;

            case ANTICIPATE:
                tickAnticipate(dt);
                break;

            case LUNGE:
                tickLunge(dt);
                break;

            case COOLDOWN:
                tickCooldown(dt);
                break;

            case DEAD_AIR:
                tickDeadAir(dt);
                break;

            case DEAD_LAND:
                tickDeadLand(dt);
                break;
        }
    }

    // =========================================================================
    // Ticks
    // =========================================================================
    private void tickIdle(float dt)
    {
        applyGravity(dt);

        velocityVec.x = 0;
        moveBy(0, velocityVec.y * dt);

        updateSensorPositions();

        setAnimation(animIdle);
        faceDirection(direction);

        if (isInDetectionRange())
        {
            faceTarget();
            enterState(HuskHornheadState.ANTICIPATE);
        }
    }

    private void tickPatrol(float dt)
    {
        applyGravity(dt);
        updateSensorPositions();

        if (isOnGround() && !edgeAheadHasGround())
        {
            enterState(HuskHornheadState.TURNING);
            return;
        }

        if (isInDetectionRange())
        {
            faceTarget();
            enterState(HuskHornheadState.ANTICIPATE);
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

        if (animTurn.isAnimationFinished(stateTimer))
        {
            flip();
            enterState(HuskHornheadState.PATROL);
        }
    }

    private void tickAnticipate(float dt)
    {
        applyGravity(dt);

        velocityVec.x = 0;
        moveBy(0, velocityVec.y * dt);

        updateSensorPositions();

        float total = ANTICIPATE_INTRO_DUR + ANTICIPATE_HOLD_DUR;

        if (stateTimer < ANTICIPATE_INTRO_DUR)
        {
            setAnimation(animAnticipateIntro);
        }
        else if (stateTimer < total)
        {
            setAnimation(animAnticipateHold);
        }
        else
        {
            if (pablo.getX() > getX())
                chargeDir = 1f;
            else
                chargeDir = -1f;
            direction = chargeDir;
            faceDirection(chargeDir);

            enterState(HuskHornheadState.LUNGE);
        }
    }

    private void tickLunge(float dt)
    {
        applyGravity(dt);

        velocityVec.x = chargeDir * LUNGE_SPEED;
        moveBy(velocityVec.x * dt, velocityVec.y * dt);

        updateSensorPositions();

        setAnimation(animLunge);
        syncFacingToHorizontalMovement();

        if (stateTimer >= LUNGE_MAX_DURATION)
            enterState(HuskHornheadState.COOLDOWN);
    }

    private void tickCooldown(float dt)
    {
        applyGravity(dt);

        velocityVec.x = 0;
        moveBy(0, velocityVec.y * dt);

        updateSensorPositions();

        setAnimation(animCooldown);
        faceDirection(direction);

        if (stateTimer >= COOLDOWN_DURATION)
        {
            if (isInDetectionRange())
            {
                faceTarget();
                enterState(HuskHornheadState.ANTICIPATE);
            }
            else
            {
                enterState(HuskHornheadState.PATROL);
            }
        }
    }

    private void tickDeadAir(float dt)
    {
        velocityVec.y -= DEATH_GRAVITY * dt;
        velocityVec.y = Math.max(velocityVec.y, -MAX_FALL_SPEED);

        moveBy(velocityVec.x * dt, velocityVec.y * dt);

        updateSensorPositions();

        setAnimation(animDeathAir);
        setRotation(getRotation() + spinDir * SPIN_SPEED * dt);
        syncFacingToHorizontalMovement();

        if (isOnGround())
        {
            velocityVec.set(0, 0);
            setRotation(0);

            enterState(HuskHornheadState.DEAD_LAND);
        }
    }

    private void tickDeadLand(float dt)
    {
        if (stateTimer < LAND_TUMBLE_DURATION)
        {
            float progress = stateTimer / LAND_TUMBLE_DURATION;
            float slideSpeed = 70f * (1f - progress);

            velocityVec.x = deathKnockbackDir * slideSpeed;
            moveBy(velocityVec.x * dt, 0);
            syncFacingToHorizontalMovement();
            setAnimation(animDeathLand);
        }
        else
        {
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
        if (state == HuskHornheadState.HIT_STOP ||
                state == HuskHornheadState.DEAD_AIR ||
                state == HuskHornheadState.DEAD_LAND)
            return;

        health -= amount;

        if (health <= 0)
        {
            health = 0;

            if (pablo.getX() < getX())
                deathKnockbackDir = 1f;
            else
                deathKnockbackDir = -1f;
            spinDir = deathKnockbackDir;
            hitStopTimer = HIT_STOP_DURATION;

            enterState(HuskHornheadState.HIT_STOP);
        }
    }

    private void launchDeath()
    {
        velocityVec.x = deathKnockbackDir * DEATH_KNOCKBACK_X;
        velocityVec.y = DEATH_KNOCKBACK_Y;

        enterState(HuskHornheadState.DEAD_AIR);
    }

    // =========================================================================
    // Wall hit
    // =========================================================================
    @Override
    public void onWallHit()
    {
        if (state == HuskHornheadState.PATROL)
        {
            flip();
        }
        else if (state == HuskHornheadState.LUNGE)
        {
            velocityVec.x = 0;
            direction = chargeDir;

            enterState(HuskHornheadState.COOLDOWN);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================
    private boolean isInDetectionRange()
    {
        if (overlaps(pablo))
            return true;

        float selfX = getX() + getWidth() / 2f;
        float selfY = getY() + getHeight() / 2f;

        float pabloX = pablo.getX() + pablo.getWidth() / 2f;
        float pabloY = pablo.getY() + pablo.getHeight() / 2f;

        float dx = pabloX - selfX;
        float dy = pabloY - selfY;

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

    private void enterState(HuskHornheadState next)
    {
        state = next;
        stateTimer = 0f;
        elapsedTime = 0f;
    }

    private boolean isInDeathSequence()
    {
        return state == HuskHornheadState.HIT_STOP ||
                state == HuskHornheadState.DEAD_AIR ||
                state == HuskHornheadState.DEAD_LAND;
    }
}
