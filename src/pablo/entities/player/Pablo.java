// Pablo.java

package pablo.entities.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import pablo.Object;
import pablo.framework.BaseActor;
import pablo.entities.combat.Hitbox;

public class Pablo extends BaseActor
{
    private Animation stand;
    private Animation walk;

    private float walkAcceleration;
    private float walkDeceleration;
    private float maxHorizontalSpeed;
    private float gravity;
    private float maxVerticalSpeed;

    // Health
    private int maxHealth;
    private int health;
    private boolean isDead = false;

    // Anima
    public  static final int MAX_SOUL      = 100;
    private static final int SOUL_PER_HEAL = 33;
    private int soul = 0;

    // -----------------------------------------------------------------------
    // Invulnerabilità
    // -----------------------------------------------------------------------
    private static final float INVULN_DURATION = 1.5f;
    private static final float FLASH_INTERVAL  = 0.08f;
    private boolean invulnerable = false;
    private float   invulnTimer  = 0f;
    private float   flashTimer   = 0f;
    private boolean flashVisible = true;

    // -----------------------------------------------------------------------
    // Hitstop — congela Pablo per un istante quando viene colpito
    // -----------------------------------------------------------------------
    private static final float HITSTOP_DURATION = 0.12f;
    private float   hitstopTimer = 0f;
    private boolean hitstopActive = false;

    // -----------------------------------------------------------------------
    // Dash
    // -----------------------------------------------------------------------
    private static final float DASH_SPEED     = 520f;  // velocità orizzontale durante il dash
    private static final float DASH_DURATION  = 0.18f; // secondi di durata del dash
    private static final float DASH_COOLDOWN  = 0.6f;  // secondi prima di poter rifare dash
    private float dashTimer    = 0f;
    private float dashCooldown = 0f;
    private float dashDir      = 1f; // direzione al momento del dash
    private Animation dashAnim;

    // -----------------------------------------------------------------------
    // Doppio salto
    // -----------------------------------------------------------------------
    private boolean canDoubleJump  = false; // diventa true dopo il primo salto
    private boolean doubleJumpUsed = false; // resettato all'atterraggio

    // Animazioni salto
    private Animation jumpUp;
    private Animation jumpLand;
    private Animation frameRise1, frameRise2, frameAltezzaMax, frameDrop1, frameDrop2;

    private float jumpSpeed;
    private BaseActor belowSensor;

    private Playerstate currentState;
    private boolean wasOnSolid = true;

    private float jumpTimer   = 0;
    private float totalJumpTime;

    private Animation attack;

    // Animazione cura
    private Animation healAnim;
    private boolean   healApplied = false;

    // -----------------------------------------------------------------------
    // Costruttore
    // -----------------------------------------------------------------------
    public Pablo(float x, float y, Stage s)
    {
        super(x, y, s);

        currentState = Playerstate.IDLE;

        stand = loadTexture("assets/Pablo/stand.png");

        String[] walkFile = {
                "assets/Pablo/Camminata/walk1.png","assets/Pablo/Camminata/walk2.png","assets/Pablo/Camminata/walk3.png","assets/Pablo/Camminata/walk4.png",
                "assets/Pablo/Camminata/walk5.png","assets/Pablo/Camminata/walk6.png","assets/Pablo/Camminata/walk7.png","assets/Pablo/Camminata/walk8.png","assets/Pablo/Camminata/walk9.png"
        };
        walk = loadAnimationFromFiles(walkFile, 0.10f, true);

        maxHorizontalSpeed = 200;
        walkAcceleration   = 200;
        walkDeceleration   = 200;
        gravity            = 500;
        maxVerticalSpeed   = 1000;
        jumpSpeed          = 500;

        maxHealth = 5;
        health    = maxHealth;

        totalJumpTime = (jumpSpeed / gravity) * 2.0f;

        String[] upFiles   = {"assets/Pablo/Salto/jump1.png","assets/Pablo/Salto/jump2.png","assets/Pablo/Salto/jump3.png"};
        jumpUp = loadAnimationFromFiles(upFiles, (totalJumpTime * 0.10f) / 3f, false);

        String[] landFiles = {"assets/Pablo/Salto/jump9.png","assets/Pablo/Salto/jump10.png"};
        jumpLand = loadAnimationFromFiles(landFiles, (totalJumpTime * 0.10f) / 2f, false);

        frameRise1      = loadTexture("assets/Pablo/Salto/jump4.png");
        frameRise2      = loadTexture("assets/Pablo/Salto/jump5.png");
        frameAltezzaMax = loadTexture("assets/Pablo/Salto/jump6.png");
        frameDrop1      = loadTexture("assets/Pablo/Salto/jump7.png");
        frameDrop2      = loadTexture("assets/Pablo/Salto/jump8.png");

        setBoundaryPolygon(6);
        belowSensor = new BaseActor(0, 0, s);
        belowSensor.loadTexture("assets/white.png");
        belowSensor.setSize(this.getWidth() - 8, 8);
        belowSensor.setBoundaryRectangle();
        belowSensor.setVisible(true);

        String[] attackFile = {
                "assets/Pablo/Attacco/attack3.png","assets/Pablo/Attacco/attack10.png","assets/Pablo/Attacco/attack8.png","assets/Pablo/Attacco/attack9.png"
        };
        attack = loadAnimationFromFiles(attackFile, 0.06f, false);
        attack.setPlayMode(Animation.PlayMode.NORMAL);

        String[] healFiles = {
                "assets/Pablo/Cura/heal1.png","assets/Pablo/Cura/heal2.png",
                "assets/Pablo/Cura/heal3.png","assets/Pablo/Cura/heal4.png",
                "assets/Pablo/Cura/heal5.png","assets/Pablo/Cura/heal6.png"
        };
        healAnim = loadAnimationFromFiles(healFiles, 0.10f, false);
        healAnim.setPlayMode(Animation.PlayMode.NORMAL);

        // Dash: 5 frame — durata calcolata per coprire DASH_DURATION
        String[] dashFiles = {
                "assets/Pablo/Dash/dash1.png","assets/Pablo/Dash/dash2.png",
                "assets/Pablo/Dash/dash3.png","assets/Pablo/Dash/dash4.png",
                "assets/Pablo/Dash/dash5.png"
        };
        boolean dashAssetsDisponibili = true;
        for (String fileName : dashFiles)
        {
            if (!Gdx.files.internal(fileName).exists())
            {
                dashAssetsDisponibili = false;
                break;
            }
        }
        if (dashAssetsDisponibili)
        {
            dashAnim = loadAnimationFromFiles(dashFiles, DASH_DURATION / 5f, false);
            dashAnim.setPlayMode(Animation.PlayMode.NORMAL);
        }
        else
        {
            // Fallback: evita il crash usando l'animazione di stand se il dash non ha asset.
            dashAnim = stand;
        }
    }

    // -----------------------------------------------------------------------
    // act()
    // -----------------------------------------------------------------------
    public void act(float dt)
    {
        super.act(dt);

        // --- Hitstop: congela tutto finché il timer è attivo ---
        if (hitstopActive)
        {
            hitstopTimer -= dt;
            if (hitstopTimer <= 0f)
                hitstopActive = false;
            else
                return; // salta physics e state machine
        }

        boolean onSolid = isOnSolid();

        // --- Invulnerabilità / flash ---
        if (invulnerable)
        {
            invulnTimer -= dt;
            flashTimer  -= dt;
            if (flashTimer <= 0f)
            {
                flashVisible = !flashVisible;
                setVisible(flashVisible);
                flashTimer = FLASH_INTERVAL;
            }
            if (invulnTimer <= 0f)
            {
                invulnerable = false;
                setVisible(true);
            }
        }

        // --- Dash cooldown ---
        if (dashCooldown > 0f) dashCooldown -= dt;

        // --- Atterraggio: resetta doppio salto ---
        if (onSolid && !wasOnSolid)
            doubleJumpUsed = false;

        // 1. FISICA (bloccata durante dash, attacco e cura)
        if (currentState != Playerstate.ATTACKING
                && currentState != Playerstate.HEALING
                && currentState != Playerstate.DASHING)
        {
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT)  || Gdx.input.isKeyPressed(Input.Keys.A))
                accelerationVec.add(-walkAcceleration, 0);
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D))
                accelerationVec.add(walkAcceleration, 0);
        }

        accelerationVec.add(0, -gravity);
        velocityVec.add(accelerationVec.x * dt, accelerationVec.y * dt);

        if (!Gdx.input.isKeyPressed(Input.Keys.RIGHT) && !Gdx.input.isKeyPressed(Input.Keys.LEFT) &&
                !Gdx.input.isKeyPressed(Input.Keys.D)     && !Gdx.input.isKeyPressed(Input.Keys.A))
        {
            float dec = walkDeceleration * dt;
            float dir = (velocityVec.x > 0) ? 1 : -1;
            float spd = Math.abs(velocityVec.x) - dec;
            if (spd < 0) spd = 0;
            velocityVec.x = spd * dir;
        }

        velocityVec.x = MathUtils.clamp(velocityVec.x, -maxHorizontalSpeed, maxHorizontalSpeed);
        velocityVec.y = MathUtils.clamp(velocityVec.y, -maxVerticalSpeed,    maxVerticalSpeed);

        moveBy(velocityVec.x * dt, velocityVec.y * dt);
        accelerationVec.set(0, 0);

        belowSensor.setPosition(getX() + 4, getY() - 8);

        // 2. STATE MACHINE
        switch (currentState)
        {
            case IDLE:
            case WALKING:

                // Dash — priorità massima a terra
                if (Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT) && dashCooldown <= 0f)
                {
                    startDash();
                    break;
                }

                // Cura
                if (Gdx.input.isKeyJustPressed(Input.Keys.F)
                        && soul >= SOUL_PER_HEAL && health < maxHealth)
                {
                    soul -= SOUL_PER_HEAL;
                    currentState = Playerstate.HEALING;
                    elapsedTime  = 0;
                    healApplied  = false;
                    break;
                }

                // Attacco
                if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.J))
                {
                    currentState = Playerstate.ATTACKING;
                    elapsedTime  = 0;
                    spawnAttackHitbox();
                    break;
                }

                // Salto
                if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.W))
                {
                    if (onSolid) { jump(false); break; }
                }

                // Cammina oltre un dirupo
                if (wasOnSolid && !onSolid)
                {
                    currentState   = Playerstate.FALLING;
                    jumpTimer      = totalJumpTime * 0.55f;
                    canDoubleJump  = true; // permetti doppio salto anche cadendo da un bordo
                    break;
                }

                currentState = (Math.abs(velocityVec.x) > 1f) ? Playerstate.WALKING : Playerstate.IDLE;
                setAnimation(currentState == Playerstate.WALKING ? walk : stand);
                break;

            case DASHING:

                dashTimer -= dt;
                setAnimation(dashAnim);
                velocityVec.x = DASH_SPEED * dashDir;
                velocityVec.y = 0; // niente gravità durante il dash

                if (dashTimer <= 0f)
                {
                    // Fine dash: velocità ridotta, torna allo stato appropriato
                    velocityVec.x = maxHorizontalSpeed * dashDir * 0.5f;
                    currentState  = onSolid ? Playerstate.IDLE : Playerstate.FALLING;
                    dashCooldown  = DASH_COOLDOWN;
                }
                break;

            case HEALING:
                setAnimation(healAnim);
                velocityVec.x = 0;
                if (!healApplied && healAnim.isAnimationFinished(elapsedTime))
                {
                    heal(1);
                    healApplied = true;
                }
                if (healAnim.isAnimationFinished(elapsedTime))
                    currentState = !onSolid ? Playerstate.FALLING
                            : Math.abs(velocityVec.x) > 1f ? Playerstate.WALKING : Playerstate.IDLE;
                break;

            case JUMPING:

                jumpTimer += dt;

                // Dash in aria
                if (Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT) && dashCooldown <= 0f)
                {
                    startDash();
                    break;
                }

                // Doppio salto
                if ((Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.W))
                        && canDoubleJump && !doubleJumpUsed)
                {
                    jump(true);
                    break;
                }

                if (velocityVec.y <= 0) { currentState = Playerstate.FALLING; break; }
                if (onSolid && !wasOnSolid)
                {
                    currentState = Playerstate.LANDING;
                    elapsedTime  = 0;
                    jumpTimer    = 0;
                    break;
                }
                updateJumpAnimation();
                break;

            case FALLING:

                jumpTimer += dt;

                // Dash in aria
                if (Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT) && dashCooldown <= 0f)
                {
                    startDash();
                    break;
                }

                // Doppio salto
                if ((Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.W))
                        && canDoubleJump && !doubleJumpUsed)
                {
                    jump(true);
                    break;
                }

                if (onSolid && !wasOnSolid)
                {
                    currentState = Playerstate.LANDING;
                    elapsedTime  = 0;
                    jumpTimer    = 0;
                    break;
                }
                updateJumpAnimation();
                break;

            case LANDING:
                setAnimation(jumpLand);
                if (jumpLand.isAnimationFinished(elapsedTime))
                    currentState = Math.abs(velocityVec.x) > 1f ? Playerstate.WALKING : Playerstate.IDLE;
                break;

            case ATTACKING:
                setAnimation(attack);
                if (attack.isAnimationFinished(elapsedTime))
                    currentState = !onSolid ? Playerstate.FALLING
                            : Math.abs(velocityVec.x) > 1f ? Playerstate.WALKING : Playerstate.IDLE;
                break;
        }

        // 3. DIREZIONE (bloccata durante dash, attacco e cura)
        if (currentState != Playerstate.ATTACKING
                && currentState != Playerstate.HEALING
                && currentState != Playerstate.DASHING)
        {
            if (velocityVec.x > 0) setScaleX(1);
            if (velocityVec.x < 0) setScaleX(-1);
        }

        // 4. SENSORE TERRA
        wasOnSolid = onSolid;
        belowSensor.setColor(onSolid ? Color.GREEN : Color.RED);

        alignCamera();
        boundToWorld();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Avvia il dash nella direzione corrente del personaggio. */
    private void startDash()
    {
        dashDir      = getScaleX() > 0 ? 1f : -1f;
        dashTimer    = DASH_DURATION;
        currentState = Playerstate.DASHING;
        elapsedTime  = 0;
        // Il dash concede invulnerabilità per tutta la sua durata (come HK)
        invulnerable = true;
        invulnTimer  = DASH_DURATION;
        flashTimer   = FLASH_INTERVAL;
        flashVisible = true;
    }

    /**
     * Salto normale (isDoubleJump=false) o doppio salto (isDoubleJump=true).
     * Il doppio salto usa la stessa animazione e la stessa velocità.
     */
    private void jump(boolean isDoubleJump)
    {
        velocityVec.y = jumpSpeed;
        currentState  = Playerstate.JUMPING;
        jumpTimer     = 0;
        elapsedTime   = 0;

        if (!isDoubleJump)
        {
            canDoubleJump  = true;
            doubleJumpUsed = false;
        }
        else
        {
            doubleJumpUsed = true;
        }
    }

    /** Mantiene compatibilità con le chiamate esterne (es. LevelScreen). */
    public void jump()
    {
        jump(false);
    }

    private void updateJumpAnimation()
    {
        float p = jumpTimer / totalJumpTime;
        if      (p < 0.10f) setAnimation(jumpUp);
        else if (p < 0.50f)
        {
            float r = (p - 0.10f) / 0.40f;
            setAnimation(r < 0.40f ? frameRise1 : r < 0.80f ? frameRise2 : frameAltezzaMax);
        }
        else if (p < 0.90f)
        {
            float d = (p - 0.50f) / 0.40f;
            setAnimation(d < 0.20f ? frameAltezzaMax : d < 0.60f ? frameDrop1 : frameDrop2);
        }
        else setAnimation(frameDrop2);
    }

    private void spawnAttackHitbox()
    {
        float hitboxWidth  = 40f;
        float hitboxHeight = 30f;
        float hitboxY = getY() + getHeight() / 2f - hitboxHeight / 2f;
        float hitboxX = (getScaleX() > 0) ? getX() + getWidth() : getX() - hitboxWidth;
        new Hitbox(hitboxX, hitboxY, hitboxWidth, hitboxHeight, 10, 0.05f, getStage(),
                () -> gainSoul(33));
    }

    public boolean belowOverlaps(BaseActor actor) { return belowSensor.overlaps(actor); }

    public boolean isOnSolid()
    {
        for (BaseActor actor : BaseActor.getList(getStage(), Object.class.getName()))
        {
            Object solid = (Object) actor;
            if (belowOverlaps(solid) && solid.isEnable()) return true;
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // HP e danno
    // -----------------------------------------------------------------------
    public void takeDamage(int amount)
    {
        if (isDead || invulnerable) return;

        if (currentState == Playerstate.HEALING)
        {
            currentState = Playerstate.IDLE;
            healApplied  = false;
        }

        health -= amount;

        if (health <= 0)
        {
            health = 0;
            isDead = true;
            setVisible(true);
            System.out.println("Pablo died!");
        }
        else
        {
            // Hitstop + invulnerabilità
            hitstopActive = true;
            hitstopTimer  = HITSTOP_DURATION;
            invulnerable  = true;
            invulnTimer   = INVULN_DURATION;
            flashTimer    = FLASH_INTERVAL;
            flashVisible  = true;
        }
    }

    public void heal(int amount)
    {
        health += amount;
        if (health > maxHealth) health = maxHealth;
    }

    // -----------------------------------------------------------------------
    // Anima
    // -----------------------------------------------------------------------
    public void gainSoul(int amount)
    {
        soul = Math.min(soul + amount, MAX_SOUL);
    }

    public void setSoul(int amount)
    {
        soul = MathUtils.clamp(amount, 0, MAX_SOUL);
    }

    public int     getSoul()          { return soul; }
    public int     getMaxSoul()       { return MAX_SOUL; }
    public boolean isInvulnerable()   { return invulnerable; }
    public boolean isDead()           { return isDead; }
    public int     getHealth()        { return health; }
    public int     getMaxHealth()     { return maxHealth; }
}