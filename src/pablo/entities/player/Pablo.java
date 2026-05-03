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

    /*
       Queste sono le variabili relative alla fisica:
       - valori di accelerazione e decelerazione per camminare,
       - la massima velocità orizzontale (di camminata),
       - l'entità della forza di gravità che tira il personaggio verso il basso
       - la massima velocità verticale possibile (di salto/caduta).
     */
    private float walkAcceleration;
    private float walkDeceleration;
    private float maxHorizontalSpeed;
    private float gravity;
    private float maxVerticalSpeed;

    // Health — maxHealth valore massimo per regolare la vita, dovrebbe diventare +20 (uso 5 per il test)
    private int maxHealth;
    private int health;
    private boolean isDead = false;

    /*
    Variabili utili al salto.
    Bisogna determinare quando il personaggio è a terra e quindi in grado di saltare.
    Controllare se la velocità nella direzione y è uguale a 0 non è sufficiente, poiché ciò è vero anche nell'istante in cui
    il personaggio è all'altezza massima di un salto.
    L'approccio che verrà utilizzato qui è quello di creare un piccolo oggetto ausiliario (chiamato belowSensor)
posizionato direttamente sotto il personaggio in ogni momento. Se questo oggetto si sovrappone a un oggetto solido,
allora il koala sarà in grado di saltare. Nella versione finale del gioco, questo sensore sarà invisibile, ma per
fini di test, la casella sarà colorata di verde o rosso, a seconda che il koala sia su un solido o meno.
     */
    private Animation jumpUp;
    private Animation jumpLand;
    private Animation frameRise1, frameRise2, frameAltezzaMax, frameDrop1, frameDrop2;

    private float jumpSpeed;
    private BaseActor belowSensor;

    private PabloState currentState;
    private boolean wasOnSolid = true;

    private float jumpTimer = 0;
    // private float totalJumpTime = 0.8f; // vecchio valore, era fisso
    private float totalJumpTime; // Valore assegnato nel costruttore

    private Animation attack;

    //Nel costruttore si devono personalizzare le diverse immagini per i diversi movimenti del personaggio
    //Per poter implementare la fisica nel salto per prima cosa, nella classe BaseActor, cambia il modificatore di accesso
    // delle variabili accelerationVec e velocityVec da private a protected in modo che la classe Sceriffo possa accedere direttamente a queste variabili

    private static final String PATH = "assets/Pablo/";

    public Pablo(float x, float y, Stage s)
    {
        super(x, y, s);

        // Inizializzo lo stato di pablo
        currentState = PabloState.IDLE;

        stand = loadTexture(PATH+"idle.png");

        String[] walkFile={PATH+"walk1.png",PATH+"walk2.png",PATH+"walk3.png",PATH+"walk4.png", PATH+"walk5.png", PATH+"walk6.png", PATH+"walk7.png", PATH+"walk8.png", PATH+"walk9.png" };
        walk = loadAnimationFromFiles(walkFile,0.10f,true);

        // Definisco la "fisica" del personaggio
        //per effetti fisici diversi modificare questi valori
        maxHorizontalSpeed = 200;
        walkAcceleration = 200;
        walkDeceleration = 200;
        gravity = 700;
        maxVerticalSpeed = 1000;
        jumpSpeed = 500;

        maxHealth = 5;
        health    = maxHealth;

        // Calcolo il tempo "fisico" del salto.
        // Tempo fino all'apice (altezza max salto) = jumpSpeed/gravity. Il tempo totale è il doppio di questo.
        totalJumpTime = (jumpSpeed / gravity) * 2.0f;

        // Inizializzazione delle animazioni per il salto usando totalJumpTime
        String[] upFiles = {PATH+"jump1.png", PATH+"jump2.png", PATH+"jump3.png"};
        jumpUp = loadAnimationFromFiles(upFiles, (totalJumpTime * 0.10f) / 3f, false);

        String[] landFiles = {PATH+"jump9.png", PATH+"jump10.png"};
        jumpLand = loadAnimationFromFiles(landFiles, (totalJumpTime * 0.10f) / 2f, false);

        frameRise1 = loadTexture(PATH+"jump4.png");
        frameRise2 = loadTexture(PATH+"jump5.png");
        frameAltezzaMax = loadTexture(PATH+"jump6.png");
        frameDrop1 = loadTexture(PATH+"jump7.png");
        frameDrop2 = loadTexture(PATH+"jump8.png");

        setBoundaryPolygon(6);
        belowSensor = new BaseActor(0,0, s);
        belowSensor.loadTexture("assets/white.png");
        belowSensor.setSize( this.getWidth() - 8, 8 );
        belowSensor.setBoundaryRectangle();
        belowSensor.setVisible(true);

        //inizializzazione degli oggetti per l'attacco
        String[] attackFile = {PATH+"attack3.png", PATH+"attack10.png", PATH+"attack8.png", PATH+"attack9.png"};
        attack = loadAnimationFromFiles(attackFile, 0.06f, false);
        attack.setPlayMode(Animation.PlayMode.NORMAL);
    }

    public void act(float dt)
    {
        super.act(dt);

        boolean onSolid = isOnSolid();   // calcolato una sola volta, utilizzato ovunque di seguito

        // 1. PHYSICS

        // Input orizzontale (bloccato durante l'attacco)
        if (currentState != PabloState.ATTACKING)
        {
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A))
                accelerationVec.add(-walkAcceleration, 0);
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D))
                accelerationVec.add(walkAcceleration, 0);
        }

        // La gravità fa cadere pablo sempre verso il basso
        accelerationVec.add(0, -gravity);
        velocityVec.add(accelerationVec.x * dt, accelerationVec.y * dt);

        // Decelera quando non viene premuto alcun tasto orizzontale
        if (!Gdx.input.isKeyPressed(Input.Keys.RIGHT) && !Gdx.input.isKeyPressed(Input.Keys.LEFT) &&
                !Gdx.input.isKeyPressed(Input.Keys.D)     && !Gdx.input.isKeyPressed(Input.Keys.A))
        {
            float dec = walkDeceleration * dt;
            float dir = (velocityVec.x > 0) ? 1 : -1;
            float spd = Math.abs(velocityVec.x) - dec;
            if (spd < 0) spd = 0;
            velocityVec.x = spd * dir;
        }

        // Blocca entrambi gli assi
        velocityVec.x = MathUtils.clamp(velocityVec.x, -maxHorizontalSpeed, maxHorizontalSpeed);
        velocityVec.y = MathUtils.clamp(velocityVec.y, -maxVerticalSpeed,    maxVerticalSpeed);

        // Applica il movimento e reimposta l'accelerazione
        moveBy(velocityVec.x * dt, velocityVec.y * dt);
        accelerationVec.set(0, 0);

        // Mantiene il sensore di terra sotto il giocatore
        belowSensor.setPosition(getX() + 4, getY() - 8);

        // 2. STATE MACHINE — transizioni, animazioni e input per stato

        switch (currentState)
        {
            case IDLE:
            case WALKING:

                // Attack maggiore priorità
                if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.J))
                {
                    currentState = PabloState.ATTACKING;
                    elapsedTime  = 0;
                    spawnAttackHitbox();
                    break;
                }

                // Jump
                if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.W))
                {
                    if (onSolid) { jump(); break; }
                }

                // Cammina oltre un dirupo (senza jump premuto)
                if (wasOnSolid && !onSolid)
                {
                    currentState = PabloState.FALLING;
                    jumpTimer    = totalJumpTime * 0.55f; // Inizia la cadutà a "metà" della animazione
                    break;
                }

                // Resta a terra — IDLE (fermo) o WALKING
                if (Math.abs(velocityVec.x) > 1f)
                {
                    currentState = PabloState.WALKING;
                }
                else
                {
                    currentState = PabloState.IDLE;
                }

                if (currentState == PabloState.WALKING)
                {
                    setAnimation(walk);
                }
                else
                {
                    setAnimation(stand);
                }
                break;

            case JUMPING:

                jumpTimer += dt;

                // Raggiunge picco (apex), inizia a cadere
                if (velocityVec.y <= 0)
                {
                    currentState = PabloState.FALLING;
                    break;
                }

                // Colpisce qualcosa dal basso (rimbalzo sul soffitto)
                if (onSolid && !wasOnSolid)
                {
                    currentState = PabloState.LANDING;
                    elapsedTime = 0;
                    jumpTimer = 0;
                    break;
                }

                updateJumpAnimation();
                break;

            case FALLING:

                jumpTimer += dt; // continua ad incrementare in modo che i frame caduta fanno animazione

                // Atteratto
                if (onSolid && !wasOnSolid)
                {
                    currentState = PabloState.LANDING;
                    elapsedTime  = 0;
                    jumpTimer    = 0;
                    break;
                }

                updateJumpAnimation();
                break;

            case LANDING:

                setAnimation(jumpLand);

                if (jumpLand.isAnimationFinished(elapsedTime))
                {
                    if (Math.abs(velocityVec.x) > 1f)
                    {
                        currentState = PabloState.WALKING;
                    }
                    else
                    {
                        currentState = PabloState.IDLE;
                    }
                }
                break;

            case ATTACKING:

                setAnimation(attack);

                if (attack.isAnimationFinished(elapsedTime))
                {
                    if (!onSolid)
                        currentState = PabloState.FALLING;
                    else if (Math.abs(velocityVec.x) > 1f)
                        currentState = PabloState.WALKING;
                    else
                        currentState = PabloState.IDLE;
                }
                break;
        }

        // 3. DIREZIONE DOVE GUARDA, usa currentState

        if (currentState != PabloState.ATTACKING)
        {
            if (velocityVec.x > 0) setScaleX(1);
            if (velocityVec.x < 0) setScaleX(-1);
        }

        // 4. CONTROLLO TERRA

        wasOnSolid = onSolid;
        if (onSolid)
        {
            belowSensor.setColor(Color.GREEN);
        }
        else
        {
            belowSensor.setColor(Color.RED);
        }

        alignCamera();
        boundToWorld();
    }

    private void updateJumpAnimation()
    {
        float percentuale = jumpTimer / totalJumpTime;

        if (percentuale < 0.10f) // Carica salto - 10%
        {
            setAnimation(jumpUp);
        }
        else if (percentuale < 0.50f) // Stacco - 40%
        {
            float riseProgress = (percentuale - 0.10f) / 0.40f;

            if (riseProgress < 0.40f) setAnimation(frameRise1);
            else if (riseProgress < 0.80f) setAnimation(frameRise2);
            else setAnimation(frameAltezzaMax);
        }
        else if (percentuale < 0.90f) // Caduta - 40%
        {
            float dropProgress = (percentuale - 0.50f) / 0.40f;

            if (dropProgress < 0.20f) setAnimation(frameAltezzaMax);
            else if (dropProgress < 0.60f) setAnimation(frameDrop1);
            else setAnimation(frameDrop2);
        }
        else // Atterraggio - 10%
        {
            setAnimation(frameDrop2);
        }
    }

    //metodi per verificare che il sensore sia posizionato sopra un oggetto solido
    public boolean belowOverlaps(BaseActor actor)
    {
        return belowSensor.overlaps(actor);
    }

    public boolean isOnSolid()
    {
        for (BaseActor actor : BaseActor.getList( getStage(), Object.class.getName() ))
        {
            Object solid = (Object)actor;
            if ( belowOverlaps(solid) && solid.isEnable() )
                return true;
        }
        return false;
    }

    public void jump()
    {
        velocityVec.y = jumpSpeed;
        currentState  = PabloState.JUMPING;
        jumpTimer     = 0;
        elapsedTime   = 0;
    }

    private void spawnAttackHitbox()
    {
        float hitboxWidth  = 40f;
        float hitboxHeight = 30f;

        // vertically centered on Pablo's body
        float hitboxY = getY() + getHeight() / 2f - hitboxHeight / 2f;

        // getScaleX() is 1 (right) or -1 (left) — set during IDLE/WALKING, frozen during ATTACKING
        float hitboxX;

        if (getScaleX() > 0) {
            // facing right → place to the right
            hitboxX = getX() + getWidth();
        } else {
            // facing left → place to the left
            hitboxX = getX() - hitboxWidth;
        }

        // damage=10, lifetime=0.05s ≈ 3 frames at 60 fps
        new Hitbox(hitboxX, hitboxY, hitboxWidth, hitboxHeight, 4, 0.05f, getStage());
    }

    // KEEP and update this one
    public void takeDamage(int amount)
    {
        if (isDead) return;          // add this line
        health -= amount;
        if (health <= 0)             // change < to <= to be safe
        {
            health = 0;
            isDead = true;           // add this line
            System.out.println("Pablo died!");
        }
    }

    public void heal(int amount)
    {
        health += amount;
        if (health > maxHealth)
            health = maxHealth;
    }

    public boolean isDead() { return isDead; }

    public int getHealth()    { return health; }
    public int getMaxHealth() { return maxHealth; }
}
