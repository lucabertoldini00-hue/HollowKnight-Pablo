// Enemy.java
package pablo.entities.enemies;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.math.Vector2;
import pablo.Object;
import pablo.framework.BaseActor;

public abstract class Enemy extends BaseActor
{
    private static final float CAMERA_ACTIVATION_MARGIN = 320f;
    protected static final float VOID_Y = -300f;

    private static final float SETTLE_TIME = 0.5f;
    private float settleTimer = SETTLE_TIME;

    protected float gravity          = 700f;
    protected float maxVerticalSpeed = 700f;

    protected int   health;
    protected float direction = 1f;

    protected BaseActor belowSensor;
    protected BaseActor edgeSensor;

    protected boolean useGravityWhenOffScreen = true;

    // -----------------------------------------------------------------------
    // Damage Flash QoL
    // -----------------------------------------------------------------------
    private static final float FLASH_DURATION = 0.15f;
    private float flashTimer = 0f;
    private boolean isFlashing = false;

    // -----------------------------------------------------------------------
    // Damage Knockback
    // -----------------------------------------------------------------------
    private float knockbackTimer = 0f;
    private float knockbackSpeedX = 0f;
    private static final float KNOCKBACK_DURATION = 0.15f;

    public Enemy(float x, float y, Stage stage)
    {
        super(x, y, stage);
    }

    @Override
    public void act(float dt)
    {
        super.act(dt);

        if (isFlashing)
        {
            flashTimer -= dt;
            if (flashTimer <= 0f)
            {
                isFlashing = false;
                setColor(com.badlogic.gdx.graphics.Color.WHITE);
            }
        }

        if (knockbackTimer > 0f) {
            knockbackTimer -= dt;
            moveBy(knockbackSpeedX * dt, 0);
            updateSensorPositions();
            
            // Only stop X movement if we are handling it here, subclasses handle X movement usually.
            // Temporary block subclass X movement if needed, but since we are in `super.act()`, 
            // subclasses usually set velocityVec.x later. So let's rely on knockbackTimer in subclasses if needed,
            // or just add it here and subclasses will add to it.
        }

        // Fase di settle: applica solo gravità + collisioni, niente AI.
        // Garantisce che tutti i nemici siano a terra prima che l'AI parta,
        // indipendentemente dalla posizione della camera.
        if (settleTimer > 0f)
        {
            settleTimer -= dt;
            if (useGravityWhenOffScreen)
            {
                applyGravity(dt);
                moveBy(0, velocityVec.y * dt);
                updateSensorPositions();
            }
            velocityVec.x = 0;
            removeIfBelowVoid();
            return;
        }

        if (!isActiveNearCamera())
        {
            if (useGravityWhenOffScreen)
            {
                applyGravity(dt);
                moveBy(0, velocityVec.y * dt);
                updateSensorPositions();
            }
            velocityVec.x = 0;
            removeIfBelowVoid();
            return;
        }

        removeIfBelowVoid();
    }

    @Override
    public void setAnimation(Animation<TextureRegion> anim)
    {
        super.setAnimation(anim);
        // Mantiene i bounds coerenti se le frame hanno dimensioni diverse.
        setBoundaryRectangle();
        updateSensorPositions();
    }

    public boolean isSettling()
    {
        return settleTimer > 0f;
    }

    protected boolean canUpdateAI()
    {
        return isActiveNearCamera() && !isSettling();
    }

    public boolean isActiveNearCamera()
    {
        if (getStage() == null)
            return false;

        Camera cam = getStage().getCamera();
        float left   = cam.position.x - cam.viewportWidth  / 2f - CAMERA_ACTIVATION_MARGIN;
        float right  = cam.position.x + cam.viewportWidth  / 2f + CAMERA_ACTIVATION_MARGIN;
        float bottom = cam.position.y - cam.viewportHeight / 2f - CAMERA_ACTIVATION_MARGIN;
        float top    = cam.position.y + cam.viewportHeight / 2f + CAMERA_ACTIVATION_MARGIN;

        return getX() + getWidth() >= left
                && getX() <= right
                && getY() + getHeight() >= bottom
                && getY() <= top;
    }

    protected void applyGravity(float dt)
    {
        velocityVec.y -= gravity * dt;
        velocityVec.y  = MathUtils.clamp(velocityVec.y, -maxVerticalSpeed, maxVerticalSpeed);
    }

    protected void flip()
    {
        direction *= -1f;
        faceDirection(direction);
    }

    // All enemy sprites face LEFT by default in their art assets.
    // scaleX = -1 flips them to face right when moving right.
    protected void faceDirection(float horizontalDirection)
    {
        if (horizontalDirection > 0f)
            setScaleX(-1f);   // flip left-facing sprite to face right
        else if (horizontalDirection < 0f)
            setScaleX(1f);    // no flip — sprite already faces left
    }

    protected void syncFacingToHorizontalMovement()
    {
        if (Math.abs(velocityVec.x) < 0.1f)
            return;
        faceDirection(velocityVec.x);
    }

    protected boolean removeIfBelowVoid()
    {
        if (getY() < VOID_Y)
        {
            remove();
            return true;
        }
        return false;
    }

    public void onWallHit()
    {
        flip();
    }

    public void takeDamage(int amount)
    {
        health -= amount;
        if (health <= 0)
            health = 0;
    }

    public void takeDamage(int amount, float knockbackDir)
    {
        int oldHealth = health;
        takeDamage(amount);

        if (oldHealth > 0 && health <= 0)
        {
            this.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
                com.badlogic.gdx.scenes.scene2d.actions.Actions.delay(3f),
                com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut(1f),
                com.badlogic.gdx.scenes.scene2d.actions.Actions.removeActor()
            ));
        }

        if (oldHealth <= 0) return;

        // Damage Flash QoL
        isFlashing = true;
        flashTimer = FLASH_DURATION;
        setColor(com.badlogic.gdx.graphics.Color.RED);
        
        // Knockback QoL
        if (health > 0) {
            knockbackSpeedX = knockbackDir * 250f;
            knockbackTimer = KNOCKBACK_DURATION;
        }
    }

    protected boolean isOnGround()
    {
        if (belowSensor == null) return false;
        if (getStage() == null) return false;

        for (BaseActor actor : BaseActor.getList(getStage(), Object.class.getName()))
        {
            Object solid = (Object) actor;
            if (belowSensor.overlaps(solid) && solid.isEnable())
                return true;
        }
        return false;
    }

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

    protected void updateSensorPositions()
    {
        if (belowSensor != null)
        {
            float sensorWidth = Math.max(4f, getWidth() - 8f);
            boolean sizeChanged = belowSensor.getWidth() != sensorWidth || belowSensor.getHeight() != 6f;
            if (sizeChanged)
            {
                belowSensor.setSize(sensorWidth, 6f);
                belowSensor.setBoundaryRectangle();
            }
            belowSensor.setPosition(getX() + (getWidth() - sensorWidth) / 2f, getY() - 6f);
        }

        if (edgeSensor != null)
        {
            float edgeX = (direction > 0) ? getX() + getWidth() : getX() - 6f;
            edgeSensor.setPosition(edgeX, getY() - 6f);
        }
    }

    public void snapToGroundIfOverlapping()
    {
        if (belowSensor == null || getStage() == null)
            return;
        if (velocityVec.y > 0)
            return;

        for (BaseActor actor : BaseActor.getList(getStage(), Object.class.getName()))
        {
            Object solid = (Object) actor;
            if (!solid.isEnable())
                continue;

            if (belowSensor.overlaps(solid))
            {
                Vector2 offset = belowSensor.preventOverlap(solid);
                if (offset != null && Math.abs(offset.y) > Math.abs(offset.x) && offset.y > 0f)
                {
                    moveBy(0, offset.y);
                    velocityVec.y = 0;
                    updateSensorPositions();
                }
            }
        }
    }

    public boolean isDead()
    {
        return health <= 0;
    }
}