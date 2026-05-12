// FalseKnight.java

package pablo.entities.enemies.falseKnight;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import pablo.entities.player.Pablo;
import pablo.framework.BaseActor;

public class FalseKnight extends BaseActor {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------
    private static final String PATH = "assets/FalseKnight/";

    private static final float GRAVITY          = 900f;
    private static final float MAX_FALL_SPEED   = 1200f;
    private static final float PATROL_SPEED     = 80f;
    private static final float STAGGER_SPEED    = 130f;
    private static final float JUMP_SPEED_Y     = 700f;
    private static final float JUMP_SPEED_X     = 150f;
    private static final float IDLE_DURATION    = 1.5f;   // seconds before patrolling
    private static final float PATROL_DURATION  = 3.0f;   // seconds before attacking
    private static final float JUMP_ATTACK_DIST = 250f;   // px: trigger jump attack instead of ground smash
    private static final float AGGRO_RANGE      = 700f;
    private static final float STOP_RANGE       = 60f;
    private static final float ATTACK_COOLDOWN  = 1.2f;
    private static final float FALL_ACCEL       = 200f;
    private static final float TURN_COOLDOWN    = 0.25f;
    private static final float WALL_HIT_JUMP_WINDOW = 0.25f;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------
    private FalseKnightState state;
    private float stateTimer;

    // -------------------------------------------------------------------------
    // Physics
    // -------------------------------------------------------------------------
    private int   facingDir    = 1;   // 1 = right, -1 = left
    private boolean onGround   = false;

    private float attackCooldown = 0f;
    private float lastDt = 0f;
    private float turnCooldown = 0f;
    private float wallHitTimer = 0f;

    // -------------------------------------------------------------------------
    // Combat
    // -------------------------------------------------------------------------
    private int     hp              = 50;
    private boolean isDead          = false;
    private float   hitStopTimer    = 0f;
    private boolean hitStopFired    = false;   // prevents re-triggering per attack state
    private boolean maceActive      = false;
    private BaseActor maceHitbox;

    // -------------------------------------------------------------------------
    // Jump attack
    // -------------------------------------------------------------------------
    private float jumpTargetX = 0f;

    // -------------------------------------------------------------------------
    // Screen shake callback (set from LevelScreen)
    // -------------------------------------------------------------------------
    private Runnable onScreenShake;

    // -------------------------------------------------------------------------
    // Reference to player for targeting
    // -------------------------------------------------------------------------
    private Pablo pablo;

    // -------------------------------------------------------------------------
    // Animations
    // -------------------------------------------------------------------------
    private Animation animIdle;
    private Animation animTurn;
    private Animation animRunAnticipate;
    private Animation animRun;
    private Animation animAttackAnticipate;
    private Animation animAttack;
    private Animation animAttackRecover;
    private Animation animJumpAnticipate;
    private Animation animJumpUp;
    private Animation animJumpAttackFall;
    private Animation animJumpLand;
    private Animation animStunRoll;
    private Animation animStunEnd;

    // =========================================================================
    // Constructor
    // =========================================================================
    public FalseKnight(float x, float y, Stage s, Pablo pablo) {
        super(x, y, s);
        this.pablo = pablo;

        loadAnimations();

        setBoundaryRectangle();

        // Mace hitbox — separate actor, always in the scene but only "active" during strike
        maceHitbox = new BaseActor(0, 0, s);
        maceHitbox.loadTexture("assets/white.png");
        maceHitbox.setSize(90, 50);
        maceHitbox.setBoundaryRectangle();
        maceHitbox.setColor(Color.RED);
        maceHitbox.setVisible(false);

        enterState(FalseKnightState.IDLE);
    }

    // =========================================================================
    // Animation loading — timings match the spec exactly
    // =========================================================================
    private void loadAnimations() {

        // IDLE — breathing loop
        animIdle = loadAnimationFromFiles(new String[]{
                PATH+"idle2.png", PATH+"idle3.png", PATH+"idle4.png", PATH+"idle5.png",
                PATH+"idle4.png", PATH+"idle3.png", PATH+"idle2.png"
        }, 0.083f, true);

        // TURN — single frame, held ~40ms (handled by timer in tickTurn)
        animTurn = loadAnimationFromFiles(new String[]{
                PATH+"turn.png"
        }, 0.040f, false);

        // RUN ANTICIPATE
        animRunAnticipate = loadAnimationFromFiles(new String[]{
                PATH+"run(anticipate)1.png", PATH+"run(anticipate)2.png"
        }, 0.060f, false);

        // RUN
        animRun = loadAnimationFromFiles(new String[]{
                PATH+"run1.png", PATH+"run2.png", PATH+"run3.png",
                PATH+"run4.png", PATH+"run5.png"
        }, 0.050f, true);

        // ATTACK ANTICIPATE — frame 5 duplicated to fake the hold
        animAttackAnticipate = loadAnimationFromFiles(new String[]{
                PATH+"attack(anticipate)1.png", PATH+"attack(anticipate)2.png",
                PATH+"attack(anticipate)3.png", PATH+"attack(anticipate)4.png",
                PATH+"attack(anticipate)5.png", PATH+"attack(anticipate)5.png",
                PATH+"attack(anticipate)5.png"
        }, 0.050f, false);

        // ATTACK — lightning fast
        animAttack = loadAnimationFromFiles(new String[]{
                PATH+"attack1.png", PATH+"attack2.png", PATH+"attack3.png"
        }, 0.030f, false);

        // ATTACK RECOVER — recover5 held by duplicates
        animAttackRecover = loadAnimationFromFiles(new String[]{
                PATH+"attack(recover)1.png", PATH+"attack(recover)2.png",
                PATH+"attack(recover)3.png", PATH+"attack(recover)4.png",
                PATH+"attack(recover)5.png", PATH+"attack(recover)5.png",
                PATH+"attack(recover)5.png"
        }, 0.060f, false);

        // JUMP ANTICIPATE
        animJumpAnticipate = loadAnimationFromFiles(new String[]{
                PATH+"jump(anticipate)1.png", PATH+"jump(anticipate)2.png",
                PATH+"jump(anticipate)3.png"
        }, 0.040f, false);

        // JUMP UP — in-air phases
        animJumpUp = loadAnimationFromFiles(new String[]{
                PATH+"jump1.png", PATH+"jump2.png", PATH+"jump3.png", PATH+"jump4.png"
        }, 0.060f, false);

        // JUMP ATTACK FALL — apex hold then rapid fall
        animJumpAttackFall = loadAnimationFromFiles(new String[]{
                PATH+"jumpAttack1.png", PATH+"jumpAttack1.png",
                PATH+"jumpAttack2.png"
        }, 0.050f, false);

        // JUMP LAND
        animJumpLand = loadAnimationFromFiles(new String[]{
                PATH+"land1.png",
                PATH+"land2.png", PATH+"land2.png",
                PATH+"land3.png", PATH+"land3.png"
        }, 0.030f, false);

        // STUN ROLL
        animStunRoll = loadAnimationFromFiles(new String[]{
                PATH+"stun(roll)1.png", PATH+"stun(roll)2.png", PATH+"stun(roll)3.png",
                PATH+"stun(roll)4.png", PATH+"stun(roll)5.png"
        }, 0.066f, false);

        // STUN END — stun(end)5 held by duplicates
        animStunEnd = loadAnimationFromFiles(new String[]{
                PATH+"stun(end)1.png", PATH+"stun(end)2.png", PATH+"stun(end)3.png",
                PATH+"stun(end)4.png",
                PATH+"stun(end)5.png", PATH+"stun(end)5.png", PATH+"stun(end)5.png"
        }, 0.066f, false);
    }

    // =========================================================================
    // act() — main loop
    // =========================================================================
    @Override
    public void act(float dt) {
        super.act(dt);

        if (isDead) return;

        // Hit-stop: freeze the boss completely while timer runs
        if (hitStopTimer > 0) {
            hitStopTimer -= dt;
            return;
        }

        lastDt = dt;
        attackCooldown = Math.max(0f, attackCooldown - dt);
        turnCooldown = Math.max(0f, turnCooldown - dt);
        wallHitTimer = Math.max(0f, wallHitTimer - dt);

        applyGravity(dt);
        stateTimer += dt;

        switch (state) {
            case IDLE:               tickIdle();              break;
            case PATROL:             tickPatrol();            break;
            case TURN:               tickTurn();              break;
            case ANTICIPATE_ATTACK:  tickAnticipateAttack();  break;
            case ATTACK:             tickAttack();            break;
            case RECOVER_ATTACK:     tickRecoverAttack();     break;
            case JUMP_ANTICIPATE:    tickJumpAnticipate();    break;
            case JUMP_UP:            tickJumpUp();            break;
            case JUMP_ATTACK_FALL:   tickJumpAttackFall();    break;
            case JUMP_LAND:          tickJumpLand();          break;
            case STAGGER:            tickStagger();           break;
            case DEAD:               tickDead();              break;
        }

        // Move
        moveBy(velocityVec.x * dt, velocityVec.y * dt);

        // Sync mace hitbox position every frame
        syncMaceHitbox();

        // Sprite flip to match facing direction
        syncFacingToHorizontalMovement();
    }

    // ✅ Fix: aggiorna facingDir insieme allo scaleX
    private void syncFacingToHorizontalMovement() {
        if (velocityVec.x > 0) {
            setScaleX(1f);
            facingDir = 1;
        } else if (velocityVec.x < 0) {
            setScaleX(-1f);
            facingDir = -1;
        } else {
            faceDirection(facingDir);
        }
    }

    private void faceDirection(float horizontalDirection) {
        if (horizontalDirection > 0f)
            setScaleX(1f);
        else if (horizontalDirection < 0f)
            setScaleX(-1f);
    }

    // =========================================================================
    // State tick methods
    // =========================================================================

    private void tickIdle() {
        setAnimation(animIdle);
        velocityVec.x = 0;
        if (stateTimer > IDLE_DURATION)
            enterState(FalseKnightState.PATROL);
    }

    private void tickPatrol() {
        updateFacingTowardPablo();

        float dist = Math.abs(pablo.getX() - getX());
        boolean inAggro = dist <= AGGRO_RANGE;

        // Short run-anticipation at the very start of patrol
        if (stateTimer < animRunAnticipate.getAnimationDuration()) {
            setAnimation(animRunAnticipate);
            velocityVec.x = 0;
        } else {
            setAnimation(animRun);
            if (inAggro && dist > STOP_RANGE)
                velocityVec.x = PATROL_SPEED * facingDir;
            else
                velocityVec.x = 0f;
        }

        // Decide which attack to use based on distance to Pablo
        if (inAggro && attackCooldown <= 0f && stateTimer > 0.5f) {
            if (dist <= JUMP_ATTACK_DIST) {
                enterState(FalseKnightState.ANTICIPATE_ATTACK);
            }
            else
            {
                enterState(FalseKnightState.JUMP_ANTICIPATE);
            }
            attackCooldown = ATTACK_COOLDOWN;
        }

        // Se siamo bloccati contro un muro, proviamo un salto per superarlo
        if (inAggro && wallHitTimer > 0f && attackCooldown <= 0f) {
            wallHitTimer = 0f;
            enterState(FalseKnightState.JUMP_ANTICIPATE);
            attackCooldown = ATTACK_COOLDOWN;
        }
    }

    private void tickTurn() {
        setAnimation(animTurn);
        velocityVec.x = 0;
        if (stateTimer > 0.040f) {
            facingDir = -facingDir;
            enterState(FalseKnightState.PATROL);
        }
    }

    private void tickAnticipateAttack() {
        updateFacingTowardPablo();
        setAnimation(animAttackAnticipate);
        velocityVec.x = 0;
        if (animAttackAnticipate.isAnimationFinished(stateTimer))
            enterState(FalseKnightState.ATTACK);
    }

    private void tickAttack() {
        setAnimation(animAttack);
        velocityVec.x = 0;

        // Enable mace hitbox at frame 2 onset (~60ms in)
        if (stateTimer >= 0.060f)
            maceActive = true;

        // Impact frame: trigger hit-stop + screen shake once
        if (stateTimer >= 0.060f && !hitStopFired) {
            triggerHitStop(0.075f);
            triggerScreenShake();
            hitStopFired = true;
        }

        if (animAttack.isAnimationFinished(stateTimer)) {
            maceActive = false;
            enterState(FalseKnightState.RECOVER_ATTACK);
        }
    }

    private void tickRecoverAttack() {
        setAnimation(animAttackRecover);
        velocityVec.x = 0;
        if (animAttackRecover.isAnimationFinished(stateTimer))
            enterState(FalseKnightState.IDLE);
    }

    private void tickJumpAnticipate() {
        setAnimation(animJumpAnticipate);
        velocityVec.x = 0;

        if (animJumpAnticipate.isAnimationFinished(stateTimer)) {
            // Face Pablo before jumping
            updateFacingTowardPablo();
            jumpTargetX = pablo.getX();
            velocityVec.y = JUMP_SPEED_Y;
            velocityVec.x = JUMP_SPEED_X * facingDir;
            enterState(FalseKnightState.JUMP_UP);
        }
    }

    private void tickJumpUp() {
        setAnimation(animJumpUp);
        // Transition to fall phase when vertical velocity becomes negative
        if (velocityVec.y < 0)
            enterState(FalseKnightState.JUMP_ATTACK_FALL);
    }

    private void tickJumpAttackFall() {
        setAnimation(animJumpAttackFall);

        // Accelerate downward faster for dramatic slam feel
        velocityVec.y = Math.max(velocityVec.y - FALL_ACCEL * lastDt, -MAX_FALL_SPEED);

        // Enable mace during fall (hitbox below boss)
        maceActive = true;

        // Land detection handled externally via onGroundLanded()
    }

    private void tickJumpLand() {
        setAnimation(animJumpLand);
        maceActive = false;
        velocityVec.x = 0;

        // Impact: hit-stop + screen shake on first frame of landing
        if (!hitStopFired) {
            triggerHitStop(0.075f);
            triggerScreenShake();
            hitStopFired = true;
        }

        if (animJumpLand.isAnimationFinished(stateTimer))
            enterState(FalseKnightState.IDLE);
    }

    private float staggerRollEndTime = -1f;

    private void tickStagger() {
        setAnimation(animStunRoll);
        velocityVec.x = -facingDir * STAGGER_SPEED;
        if (animStunRoll.isAnimationFinished(stateTimer)) {
            velocityVec.x = 0;
            if (staggerRollEndTime < 0f) staggerRollEndTime = stateTimer;
            float endElapsed = stateTimer - staggerRollEndTime;
            setAnimation(animStunEnd);
            if (animStunEnd.isAnimationFinished(endElapsed))
                enterState(FalseKnightState.IDLE);
        }
    }

    private void tickDead() {
        setAnimation(animStunEnd);
        velocityVec.x = 0;
        // Fade out slowly
        setOpacity(Math.max(0f, getColor().a - 0.005f));
        if (getColor().a <= 0f)
            remove();
    }

    // =========================================================================
    // Physics
    // =========================================================================

    // ✅ Fix: salta applyGravity se la gestisci manualmente nello stato
    private void applyGravity(float dt) {
        if (state == FalseKnightState.JUMP_ATTACK_FALL) return; // già gestita nel tick
        velocityVec.y = MathUtils.clamp(
                velocityVec.y - GRAVITY * dt,
                -MAX_FALL_SPEED,
                JUMP_SPEED_Y
        );
    }

    // Called by LevelScreen when a horizontal wall collision is resolved
    public void onWallHit() {
        wallHitTimer = WALL_HIT_JUMP_WINDOW;
        if (state == FalseKnightState.PATROL && turnCooldown <= 0f) {
            turnCooldown = TURN_COOLDOWN;
            enterState(FalseKnightState.TURN);
        }
    }

    // Called by LevelScreen when vertical collision stops the boss (landing)
    public void onGroundLanded() {
        velocityVec.y = 0;
        if (state == FalseKnightState.JUMP_ATTACK_FALL)
            enterState(FalseKnightState.JUMP_LAND);
    }

    // =========================================================================
    // Mace hitbox
    // =========================================================================

    private void syncMaceHitbox() {
        // Position the mace in front of the boss at waist height
        float hx;
        if (facingDir > 0)
            hx = getX() + getWidth();
        else
            hx = getX() - maceHitbox.getWidth();

        float hy = getY() + getHeight() * 0.15f;

        maceHitbox.setPosition(hx, hy);
        maceHitbox.setVisible(maceActive);
    }

    public boolean maceOverlapsPablo() {
        return maceActive && maceHitbox.overlaps(pablo);
    }

    // =========================================================================
    // Damage
    // =========================================================================

    public void takeDamage(int amount) {
        if (isDead) return;
        hp -= amount;
        if (hp <= 0) {
            hp = 0;
            isDead = true;
            enterState(FalseKnightState.DEAD);
        } else {
            enterState(FalseKnightState.STAGGER);
        }
    }

    public int getHp()     { return hp; }
    public boolean isDead() { return isDead; }

    // =========================================================================
    // Hit-stop & screen shake
    // =========================================================================

    private void triggerHitStop(float duration) {
        hitStopTimer = duration;
    }

    private void triggerScreenShake() {
        if (onScreenShake != null)
            onScreenShake.run();
    }

    public void setScreenShakeCallback(Runnable callback) {
        onScreenShake = callback;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void enterState(FalseKnightState next) {
        state         = next;
        stateTimer    = 0f;
        hitStopFired  = false;
        elapsedTime   = 0f;    // reset BaseActor animation clock
        maceActive    = false;
    }

    private void updateFacingTowardPablo() {
        if (pablo == null) return;
        facingDir = (pablo.getX() >= getX()) ? 1 : -1;
    }
}
