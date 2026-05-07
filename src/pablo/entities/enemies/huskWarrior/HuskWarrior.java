// HuskWarrior.java
package pablo.entities.enemies.huskWarrior;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.scenes.scene2d.Stage;
import pablo.entities.enemies.Enemy;
import pablo.entities.player.Pablo;
import pablo.framework.BaseActor;

public class HuskWarrior extends Enemy
{
    private static final String PATH = "assets/HuskWarrior/";

    // Movement
    private static final float PATROL_SPEED          = 50f;
    private static final float IDLE_DURATION         = 0.8f;
    private static final float RECOIL_SPEED          = 70f;      // backward push on block

    // Shield timings
    private static final float SHIELD_ANTICIPATE_DUR = 0.040f;
    private static final float SHIELD_FRONT_IMPACT   = 0.050f;   // shield(front)1 hold
    private static final float SHIELD_FRONT_HOLD     = 0.300f;   // shield(front)2 hold + recoil
    private static final float SHIELD_TOP_DUR        = 0.166f;   // top1+top2 combined
    private static final float SHIELD_TOP_BUMP_IMPACT= 0.050f;
    private static final float SHIELD_TOP_BUMP_HOLD  = 0.150f;

    // Attack timings
    private static final float WINDUP_INTRO_DUR      = 3 * 0.066f; // attack1-3 = 0.198f
    private static final float WINDUP_TOTAL_DUR      = WINDUP_INTRO_DUR + 0.150f; // + hold attack4
    private static final float STRIKE2_THRESHOLD     = 0.080f;   // attack7 onset (30+50ms)
    private static final float STRIKE3_THRESHOLD     = 0.130f;   // attack8 onset (+50ms)
    private static final float ATTACK_TOTAL_DUR      = 0.210f;   // full combo done
    private static final float ATTACK_COOLDOWN_DUR   = 0.200f;

    // Pogo bounce given to Pablo on top block
    private static final float POGO_BOUNCE_SPEED     = 380f;

    // Death
    private static final float HIT_STOP_DURATION    = 0.100f;
    private static final float DEATH_KNOCKBACK_X    = 320f;
    private static final float DEATH_KNOCKBACK_Y    = 380f;
    private static final float DEATH_GRAVITY        = 1400f;
    private static final float MAX_FALL_SPEED       = 1200f;
    private static final float SPIN_SPEED           = 460f;
    private static final float LAND_TUMBLE_DURATION = 7 * 0.066f;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------
    private HuskWarriorState state;
    private float stateTimer;

    // -------------------------------------------------------------------------
    // Death
    // -------------------------------------------------------------------------
    private float hitStopTimer;
    private float deathKnockbackDir;
    private float spinDir;

    // -------------------------------------------------------------------------
    // Combat — per-strike hit flags reset on enterState
    // -------------------------------------------------------------------------
    private boolean hit1Landed, hit2Landed, hit3Landed;

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
    private Animation animShieldAnticipate;
    private Animation animShieldFrontImpact; // shield(front)1
    private Animation animShieldFrontHold;   // shield(front)2
    private Animation animShieldTop;         // shield(top)1+2
    private Animation animShieldTopImpact;   // shield(topBump)1
    private Animation animShieldTopHold;     // shield(topBump)2
    private Animation animWindupIntro;       // attack1-3
    private Animation animWindupHold;        // attack4 held
    private Animation animAttackStrikes;     // attack5-8
    private Animation animDeathAir;
    private Animation animDeathLand;
    private Animation animDeathCorpse;

    public HuskWarrior(float x, float y, Stage stage, Pablo pablo)
    {
        super(x, y, stage);
        this.pablo = pablo;

        health = 16; // 4 unshielded hits from Pablo (4 dmg each)

        animIdle = loadAnimationFromFiles(new String[]{
                PATH+"idle1.png", PATH+"idle2.png", PATH+"idle3.png",
                PATH+"idle4.png", PATH+"idle5.png", PATH+"idle6.png"
        }, 0.083f, true);

        animWalk = loadAnimationFromFiles(new String[]{
                PATH+"walk1.png", PATH+"walk2.png", PATH+"walk3.png",
                PATH+"walk4.png", PATH+"walk5.png", PATH+"walk6.png",
                PATH+"walk7.png"
        }, 0.083f, true);

        animTurn = loadAnimationFromFiles(new String[]{
                PATH+"turn1.png", PATH+"turn2.png"
        }, 0.083f, false);

        // Shield anticipate: single frame, 40ms
        animShieldAnticipate = loadAnimationFromFiles(new String[]{
                PATH+"shield(anticipate).png"
        }, 0.040f, false);

        // Shield front: impact frame, then hold frame (separate anims for clean phasing)
        animShieldFrontImpact = loadAnimationFromFiles(new String[]{
                PATH+"shield(front)1.png"
        }, 0.050f, false);

        animShieldFrontHold = loadAnimationFromFiles(new String[]{
                PATH+"shield(front)2.png"
        }, 1f, true);

        // Shield top: raise sequence plays once
        animShieldTop = loadAnimationFromFiles(new String[]{
                PATH+"shield(top)1.png", PATH+"shield(top)2.png"
        }, 0.083f, false);

        animShieldTopImpact = loadAnimationFromFiles(new String[]{
                PATH+"shield(topBump)1.png"
        }, 0.050f, false);

        animShieldTopHold = loadAnimationFromFiles(new String[]{
                PATH+"shield(topBump)2.png"
        }, 1f, true);

        // Windup: attack1-3 intro, then attack4 held (same split as HuskHornhead anticipate)
        animWindupIntro = loadAnimationFromFiles(new String[]{
                PATH+"attack1.png", PATH+"attack2.png", PATH+"attack3.png"
        }, 0.066f, false);

        animWindupHold = loadAnimationFromFiles(new String[]{
                PATH+"attack4.png"
        }, 1f, true);

        // Three strikes: average frame duration (~52ms); hitboxes spawned by stateTimer
        animAttackStrikes = loadAnimationFromFiles(new String[]{
                PATH+"attack5.png", PATH+"attack6.png",
                PATH+"attack7.png", PATH+"attack8.png"
        }, 0.052f, false);

        animDeathAir = loadAnimationFromFiles(new String[]{
                PATH+"death(air).png"
        }, 1f, true);

        animDeathLand = loadAnimationFromFiles(new String[]{
                PATH+"death(land)1.png", PATH+"death(land)2.png",
                PATH+"death(land)3.png", PATH+"death(land)4.png",
                PATH+"death(land)5.png", PATH+"death(land)6.png",
                PATH+"death(land)7.png"
        }, 0.066f, false);

        animDeathCorpse = loadAnimationFromFiles(new String[]{
                PATH+"death(land)8.png"
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

        enterState(HuskWarriorState.IDLE);
    }

    @Override
    public void act(float dt)
    {
        super.act(dt);

        if (!canUpdateAI())
            return;

        if (state == HuskWarriorState.HIT_STOP)
        {
            hitStopTimer -= dt;
            if (hitStopTimer <= 0f) launchDeath();
            return;
        }

        stateTimer += dt;

        switch (state)
        {
            case IDLE:              tickIdle(dt);            break;
            case PATROL:            tickPatrol(dt);          break;
            case TURNING:           tickTurning(dt);         break;
            case SHIELD_ANTICIPATE: tickShieldAnticipate(dt);break;
            case SHIELD_FRONT:      tickShieldFront(dt);     break;
            case SHIELD_TOP:        tickShieldTop(dt);       break;
            case SHIELD_TOP_BUMP:   tickShieldTopBump(dt);   break;
            case ATTACK_WINDUP:     tickAttackWindup(dt);    break;
            case ATTACKING:         tickAttacking(dt);       break;
            case ATTACK_COOLDOWN:   tickAttackCooldown(dt);  break;
            case DEAD_AIR:          tickDeadAir(dt);         break;
            case DEAD_LAND:         tickDeadLand(dt);        break;
        }
    }

    private void tickIdle(float dt)
    {
        applyGravity(dt);
        velocityVec.x = 0;
        moveBy(0, velocityVec.y * dt);
        updateSensorPositions();

        setAnimation(animIdle);
        faceDirection(direction);

        if (stateTimer >= IDLE_DURATION)
            enterState(HuskWarriorState.PATROL);
    }

    private void tickPatrol(float dt)
    {
        applyGravity(dt);
        updateSensorPositions();

        if (isOnGround() && !edgeAheadHasGround())
        {
            enterState(HuskWarriorState.TURNING);
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
            enterState(HuskWarriorState.PATROL);
        }
    }

    private void tickShieldAnticipate(float dt)
    {
        applyGravity(dt);
        velocityVec.x = 0;
        moveBy(0, velocityVec.y * dt);
        updateSensorPositions();

        setAnimation(animShieldAnticipate);
        faceDirection(direction);

        if (stateTimer >= SHIELD_ANTICIPATE_DUR)
            enterState(HuskWarriorState.SHIELD_FRONT);
    }

    private void tickShieldFront(float dt)
    {
        applyGravity(dt);
        updateSensorPositions();

        float total = SHIELD_FRONT_IMPACT + SHIELD_FRONT_HOLD;

        if (stateTimer < SHIELD_FRONT_IMPACT)
        {
            // Impact frame — planted
            setAnimation(animShieldFrontImpact);
            velocityVec.x = 0;
        }
        else
        {
            // Hold frame — recoil pushback away from Pablo
            setAnimation(animShieldFrontHold);
            float recoilDir;

            if (pablo.getX() > getX())
                recoilDir = -1f;
            else
                recoilDir = 1f;
            velocityVec.x   = recoilDir * RECOIL_SPEED;
        }

        moveBy(velocityVec.x * dt, velocityVec.y * dt);
        syncFacingToHorizontalMovement();

        if (stateTimer >= total)
            enterState(HuskWarriorState.ATTACK_WINDUP);
    }

    private void tickShieldTop(float dt)
    {
        applyGravity(dt);
        velocityVec.x = 0;
        moveBy(0, velocityVec.y * dt);
        updateSensorPositions();

        setAnimation(animShieldTop);
        faceDirection(direction);

        if (stateTimer >= SHIELD_TOP_DUR)
            enterState(HuskWarriorState.SHIELD_TOP_BUMP);
    }

    private void tickShieldTopBump(float dt)
    {
        applyGravity(dt);
        velocityVec.x = 0;
        moveBy(0, velocityVec.y * dt);
        updateSensorPositions();

        float total = SHIELD_TOP_BUMP_IMPACT + SHIELD_TOP_BUMP_HOLD;

        if (stateTimer < SHIELD_TOP_BUMP_IMPACT)
            setAnimation(animShieldTopImpact);
        else
            setAnimation(animShieldTopHold);

        faceDirection(direction);

        if (stateTimer >= total)
            enterState(HuskWarriorState.PATROL);
    }

    private void tickAttackWindup(float dt)
    {
        applyGravity(dt);
        velocityVec.x = 0;
        moveBy(0, velocityVec.y * dt);
        updateSensorPositions();

        if (stateTimer < WINDUP_INTRO_DUR)
            setAnimation(animWindupIntro); // attack1-3 at 66ms each
        else
            setAnimation(animWindupHold);  // attack4 held for 150ms

        faceDirection(direction);

        if (stateTimer >= WINDUP_TOTAL_DUR)
            enterState(HuskWarriorState.ATTACKING);
    }

    private void tickAttacking(float dt)
    {
        applyGravity(dt);
        velocityVec.x = 0;
        moveBy(0, velocityVec.y * dt);
        updateSensorPositions();

        setAnimation(animAttackStrikes);
        faceDirection(direction);

        // Strike 1 — attack5+6 window (~0-80ms)
        if (!hit1Landed)
        {
            if (overlaps(pablo)) pablo.takeDamage(1);
            hit1Landed = true;
        }

        // Strike 2 — attack7 window (~80-130ms)
        if (!hit2Landed && stateTimer >= STRIKE2_THRESHOLD)
        {
            if (overlaps(pablo)) pablo.takeDamage(1);
            hit2Landed = true;
        }

        // Strike 3 — attack8 window (~130-210ms)
        if (!hit3Landed && stateTimer >= STRIKE3_THRESHOLD)
        {
            if (overlaps(pablo)) pablo.takeDamage(1);
            hit3Landed = true;
        }

        if (stateTimer >= ATTACK_TOTAL_DUR)
            enterState(HuskWarriorState.ATTACK_COOLDOWN);
    }

    private void tickAttackCooldown(float dt)
    {
        // Punish window: warrior rests, shield is DOWN, all hits land
        applyGravity(dt);
        velocityVec.x = 0;
        moveBy(0, velocityVec.y * dt);
        updateSensorPositions();

        setAnimation(animIdle);
        faceDirection(direction);

        if (stateTimer >= ATTACK_COOLDOWN_DUR)
            enterState(HuskWarriorState.IDLE);
    }

    private void tickDeadAir(float dt)
    {
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
            setRotation(0); // snap on impact
            enterState(HuskWarriorState.DEAD_LAND);
        }
    }

    private void tickDeadLand(float dt)
    {
        if (stateTimer < LAND_TUMBLE_DURATION)
        {
            // Frames 1-7: tumble with decelerating slide
            float progress   = stateTimer / LAND_TUMBLE_DURATION;
            float slideSpeed = 60f * (1f - progress);
            velocityVec.x = deathKnockbackDir * slideSpeed;
            moveBy(velocityVec.x * dt, 0);
            syncFacingToHorizontalMovement();
            setAnimation(animDeathLand);
        }
        else
        {
            // Frame 8: hold forever — no remove()
            velocityVec.set(0, 0);
            setAnimation(animDeathCorpse);
        }
    }

    @Override
    public void takeDamage(int amount)
    {
        // Ignore during death
        if (state == HuskWarriorState.HIT_STOP ||
                state == HuskWarriorState.DEAD_AIR   ||
                state == HuskWarriorState.DEAD_LAND)
            return;

        // Ignore during active combat (committed to attack)
        if (state == HuskWarriorState.ATTACK_WINDUP ||
                state == HuskWarriorState.ATTACKING)
            return;

        // Already in a shield state — ignore repeat hits
        if (state == HuskWarriorState.SHIELD_ANTICIPATE ||
                state == HuskWarriorState.SHIELD_FRONT    ||
                state == HuskWarriorState.SHIELD_TOP      ||
                state == HuskWarriorState.SHIELD_TOP_BUMP)
            return;

        // Blocking only active while patrolling — cooldown is the punish window
        boolean canBlock = (state == HuskWarriorState.IDLE    ||
                state == HuskWarriorState.PATROL   ||
                state == HuskWarriorState.TURNING);

        if (canBlock && isPogoFromAbove())
        {
            pablo.velocityVec.y = POGO_BOUNCE_SPEED; // bounce Pablo upward
            enterState(HuskWarriorState.SHIELD_TOP);
            return; // no damage
        }

        if (canBlock && isShieldFacingPablo())
        {
            enterState(HuskWarriorState.SHIELD_ANTICIPATE); // block → retaliate
            return; // no damage
        }

        // Unshielded hit (from behind, or during cooldown)
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
            spawnShieldProp();
            enterState(HuskWarriorState.HIT_STOP);
        }
    }

    private void launchDeath()
    {
        velocityVec.x = deathKnockbackDir * DEATH_KNOCKBACK_X;
        velocityVec.y = DEATH_KNOCKBACK_Y;
        enterState(HuskWarriorState.DEAD_AIR);
    }

    @Override
    public void onWallHit()
    {
        if (state == HuskWarriorState.PATROL)
            flip();
        // All other states: walls are ignored
    }

    private void spawnShieldProp()
    {
        // Shield flies opposite to knockback direction and arcs upward
        float shieldVelX = -deathKnockbackDir * 190f;
        float shieldVelY = 310f;
        float shieldSpin = deathKnockbackDir * 380f; // degrees/sec
        new ShieldProp(getX(), getY() + getHeight() * 0.5f,
                getStage(), shieldVelX, shieldVelY, shieldSpin);
    }

    // Returns true if the shield side of the warrior is the side Pablo is on
    private boolean isShieldFacingPablo()
    {
        float pabloCenterX = pablo.getX() + pablo.getWidth()  / 2f;
        float selfCenterX  = getX()       + getWidth()        / 2f;
        if (direction > 0)
            return pabloCenterX > selfCenterX;
        else
            return pabloCenterX < selfCenterX;
    }

    // True when Pablo is falling from above the warrior's midpoint
    private boolean isPogoFromAbove()
    {
        float warriorMidY  = getY() + getHeight() * 0.5f;
        float pabloCenterY = pablo.getY() + pablo.getHeight() / 2f;
        return pabloCenterY > warriorMidY && pablo.velocityVec.y < 0;
    }

    private void enterState(HuskWarriorState next)
    {
        state       = next;
        stateTimer  = 0f;
        elapsedTime = 0f;
        hit1Landed  = false;
        hit2Landed  = false;
        hit3Landed  = false;
    }

    // =========================================================================
    // Shield Prop — independent physics actor spawned at moment of death
    // =========================================================================
    private static class ShieldProp extends BaseActor
    {
        private float velX, velY;
        private float spinRate;
        private static final float GRAVITY = 1000f;

        ShieldProp(float x, float y, Stage stage, float velX, float velY, float spinRate)
        {
            super(x, y, stage);
            this.velX     = velX;
            this.velY     = velY;
            this.spinRate = spinRate;
            loadTexture("assets/HuskWarrior/death(shield).png");
            setBoundaryRectangle();
        }

        @Override
        public void act(float dt)
        {
            super.act(dt);
            velY -= GRAVITY * dt;
            moveBy(velX * dt, velY * dt);
            setRotation(getRotation() + spinRate * dt);
            if (getY() < -200f) remove(); // self-cleanup when off-screen
        }
    }
}
