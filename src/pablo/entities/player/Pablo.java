// Pablo.java — modificato per integrare SoundManager
// DIFF rispetto all'originale: aggiunte chiamate SoundManager.get().playSfx(...)
// nei punti chiave della state machine e dei metodi helper.

package pablo.entities.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import pablo.Object;
import pablo.SoundManager;
import pablo.SoundManager.Sfx;
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
    // Hitstop
    // -----------------------------------------------------------------------
    private static final float HITSTOP_DURATION = 0.12f;
    private float   hitstopTimer = 0f;
    private boolean hitstopActive = false;

    // -----------------------------------------------------------------------
    // Dash
    // -----------------------------------------------------------------------
    private static final float DASH_SPEED     = 520f;
    private static final float DASH_DURATION  = 0.18f;
    private static final float DASH_COOLDOWN  = 0.6f;
    private float dashTimer    = 0f;
    private float dashCooldown = 0f;
    private float dashDir      = 1f;
    private Animation dashAnim;

    // -----------------------------------------------------------------------
    // Doppio salto
    // -----------------------------------------------------------------------
    private boolean canDoubleJump  = false;
    private boolean doubleJumpUsed = false;

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
    // SFX: flag per evitare ripetizioni
    // -----------------------------------------------------------------------
    private boolean landSoundPlayed   = false; // suono atterraggio già emesso
    private boolean attackSoundPlayed = false; // suono attacco già emesso

    // Spawn / respawn position
    private float spawnX;
    private float spawnY;
    // Y threshold below which Pablo is considered in the void
    private static final float VOID_Y = -300f;

    // -----------------------------------------------------------------------
    // Costruttore
    // -----------------------------------------------------------------------
    public Pablo(float x, float y, Stage s)
    {
        super(x, y, s);

        this.spawnX = x;
        this.spawnY = y;

        currentState = Playerstate.IDLE;

        stand = loadTexture("assets/Pablo/stand.png");

        String[] walkFile = {
                "assets/Pablo/walk1.png","assets/Pablo/walk2.png","assets/Pablo/walk3.png","assets/Pablo/walk4.png",
                "assets/Pablo/walk5.png","assets/Pablo/walk6.png","assets/Pablo/walk7.png","assets/Pablo/walk8.png","assets/Pablo/walk9.png"
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

        String[] upFiles   = {"assets/Pablo/jump1.png","assets/Pablo/jump2.png","assets/Pablo/jump3.png"};
        jumpUp = loadAnimationFromFiles(upFiles, (totalJumpTime * 0.10f) / 3f, false);

        String[] landFiles = {"assets/Pablo/jump9.png","assets/Pablo/jump10.png"};
        jumpLand = loadAnimationFromFiles(landFiles, (totalJumpTime * 0.10f) / 2f, false);

        frameRise1      = loadTexture("assets/Pablo/jump4.png");
        frameRise2      = loadTexture("assets/Pablo/jump5.png");
        frameAltezzaMax = loadTexture("assets/Pablo/jump6.png");
        frameDrop1      = loadTexture("assets/Pablo/jump7.png");
        frameDrop2      = loadTexture("assets/Pablo/jump8.png");

        setBoundaryPolygon(6);
        belowSensor = new BaseActor(0, 0, s);
        belowSensor.loadTexture("assets/white.png");
        belowSensor.setSize(this.getWidth() - 8, 8);
        belowSensor.setBoundaryRectangle();
        belowSensor.setVisible(true);

        String[] attackFile = {
                "assets/Pablo/attack3.png","assets/Pablo/attack10.png","assets/Pablo/attack8.png","assets/Pablo/attack9.png"
        };
        attack = loadAnimationFromFiles(attackFile, 0.06f, false);
        attack.setPlayMode(Animation.PlayMode.NORMAL);

        String[] healFiles = {
                "assets/Pablo/heal1.png","assets/Pablo/heal2.png",
                "assets/Pablo/heal3.png","assets/Pablo/heal4.png",
                "assets/Pablo/heal5.png","assets/Pablo/heal6.png"
        };
        healAnim = loadAnimationFromFiles(healFiles, 0.10f, false);
        healAnim.setPlayMode(Animation.PlayMode.NORMAL);

        String[] dashFiles = {
                "assets/Pablo/dash1.png","assets/Pablo/dash2.png",
                "assets/Pablo/dash3.png","assets/Pablo/dash4.png",
                "assets/Pablo/dash5.png"
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
            dashAnim = stand;
        }
    }

    // -----------------------------------------------------------------------
    // act()
    // -----------------------------------------------------------------------
    public void act(float dt)
    {
        super.act(dt);

        // Aggiorna i cooldown SFX
        SoundManager.get().update(dt);

        // --- Hitstop ---
        if (hitstopActive)
        {
            hitstopTimer -= dt;
            if (hitstopTimer <= 0f)
                hitstopActive = false;
            else
                return;
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
        {
            doubleJumpUsed = false;
            // SFX atterraggio (emesso solo al momento del contatto)
            if (!landSoundPlayed)
            {
                SoundManager.get().playSfx(Sfx.PLAYER_LAND);
                landSoundPlayed = true;
            }
        }
        if (!onSolid) landSoundPlayed = false;

        // 1. FISICA
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

                attackSoundPlayed = false;

                if (Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT) && dashCooldown <= 0f)
                {
                    startDash();
                    break;
                }

                if (Gdx.input.isKeyJustPressed(Input.Keys.F)
                        && soul >= SOUL_PER_HEAL && health < maxHealth)
                {
                    soul -= SOUL_PER_HEAL;
                    currentState = Playerstate.HEALING;
                    elapsedTime  = 0;
                    healApplied  = false;
                    // SFX: inizio cura
                    SoundManager.get().playSfx(Sfx.PLAYER_HEAL_START);
                    break;
                }

                if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.J))
                {
                    currentState = Playerstate.ATTACKING;
                    elapsedTime  = 0;
                    spawnAttackHitbox();
                    // SFX: attacco (whoosh)
                    SoundManager.get().playSfx(Sfx.PLAYER_ATTACK);
                    break;
                }

                if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.W))
                {
                    if (onSolid) { jump(false); break; }
                }

                if (wasOnSolid && !onSolid)
                {
                    currentState   = Playerstate.FALLING;
                    jumpTimer      = totalJumpTime * 0.55f;
                    canDoubleJump  = true;
                    break;
                }

                currentState = (Math.abs(velocityVec.x) > 1f) ? Playerstate.WALKING : Playerstate.IDLE;
                setAnimation(currentState == Playerstate.WALKING ? walk : stand);
                break;

            case DASHING:

                dashTimer -= dt;
                setAnimation(dashAnim);
                velocityVec.x = DASH_SPEED * dashDir;
                velocityVec.y = 0;

                if (dashTimer <= 0f)
                {
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
                    // SFX: cura completata
                    SoundManager.get().playSfx(Sfx.PLAYER_HEAL_END);
                }
                if (healAnim.isAnimationFinished(elapsedTime))
                    currentState = !onSolid ? Playerstate.FALLING
                            : Math.abs(velocityVec.x) > 1f ? Playerstate.WALKING : Playerstate.IDLE;
                break;

            case JUMPING:

                jumpTimer += dt;

                if (Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT) && dashCooldown <= 0f)
                {
                    startDash();
                    break;
                }

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

                if (Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT) && dashCooldown <= 0f)
                {
                    startDash();
                    break;
                }

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

        // 3. DIREZIONE
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

    private void startDash()
    {
        dashDir      = getScaleX() > 0 ? 1f : -1f;
        dashTimer    = DASH_DURATION;
        currentState = Playerstate.DASHING;
        elapsedTime  = 0;
        invulnerable = true;
        invulnTimer  = DASH_DURATION;
        flashTimer   = FLASH_INTERVAL;
        flashVisible = true;
        // SFX: dash
        SoundManager.get().playSfx(Sfx.PLAYER_DASH);
    }

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

        // SFX: salto (pitch leggermente più acuto per il doppio salto)
        SoundManager.get().playSfx(Sfx.PLAYER_JUMP, isDoubleJump ? 1.15f : 1.0f);
    }

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
            // SFX: morte
            SoundManager.get().playSfx(Sfx.PLAYER_DEATH);
            System.out.println("Pablo died!");
        }
        else
        {
            // SFX: danno ricevuto
            SoundManager.get().playSfx(Sfx.PLAYER_HURT);

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

    /**
     * Teleports Pablo back to his spawn position and resets all state.
     * Call this when he falls into the void or dies (future death screen excluded).
     */
    public void respawn()
    {
        setPosition(spawnX, spawnY);
        velocityVec.set(0, 0);
        accelerationVec.set(0, 0);

        currentState   = Playerstate.FALLING;
        jumpTimer      = 0;
        elapsedTime    = 0;
        wasOnSolid     = false;

        // Reset combat and defensive state
        invulnerable   = false;
        hitstopActive  = false;
        invulnTimer    = 0;
        flashTimer     = 0;
        flashVisible   = true;
        setVisible(true);

        // Reset health on void fall (remove this line if you want void = no HP loss)
        health         = maxHealth;
        isDead         = false;
    }

    /** Returns true when Pablo has fallen below the void threshold. */
    public boolean isInVoid()
    {
        return getY() < VOID_Y || (getY() <= 0f && velocityVec.y < 0f && !isOnSolid());
    }

    // -----------------------------------------------------------------------
    // Anima
    // -----------------------------------------------------------------------
    public void gainSoul(int amount)
    {
        int prev = soul;
        soul = Math.min(soul + amount, MAX_SOUL);
        // SFX: guadagno anima (solo se effettivamente aumentata)
        if (soul > prev)
            SoundManager.get().playSfx(Sfx.PLAYER_SOUL_GAIN);
    }

    public void setSoul(int amount)
    {
        soul = MathUtils.clamp(amount, 0, MAX_SOUL);
    }

    public int     getSoul()        { return soul; }
    public int     getMaxSoul()     { return MAX_SOUL; }
    public boolean isInvulnerable() { return invulnerable; }
    public boolean isDead()         { return isDead; }
    public int     getHealth()      { return health; }
    public int     getMaxHealth()   { return maxHealth; }
}
