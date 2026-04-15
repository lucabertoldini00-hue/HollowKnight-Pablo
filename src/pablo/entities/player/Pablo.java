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

    private boolean isAttacking = false;
    private boolean isJumping = false;
    private boolean isLanding = false;
    private boolean wasOnSolid = true;

    private float jumpTimer = 0;
    // private float totalJumpTime = 0.8f; // vecchio valore, era fisso
    private float totalJumpTime; // Valore assegnato nel costruttore

    private Animation attack;

    //Nel costruttore si devono personalizzare le diverse immagini per i diversi movimenti del personaggio
    //Per poter implementare la fisica nel salto per prima cosa, nella classe BaseActor, cambia il modificatore di accesso
    // delle variabili accelerationVec e velocityVec da private a protected in modo che la classe Sceriffo possa accedere direttamente a queste variabili

    public Pablo(float x, float y, Stage s)
    {
        super(x, y, s);
        stand = loadTexture("assets/stand.png");

        String[] walkFile={"assets/walk1.png","assets/walk2.png","assets/walk3.png","assets/walk4.png", "assets/walk5.png", "assets/walk6.png", "assets/walk7.png", "assets/walk8.png", "assets/walk9.png" };
        walk = loadAnimationFromFiles(walkFile,0.10f,true);

        // Definisco la "fisica" del personaggio
        //per effetti fisici diversi modificare questi valori
        maxHorizontalSpeed = 200;
        walkAcceleration = 200;
        walkDeceleration = 200;
        gravity = 700;
        maxVerticalSpeed = 1000;
        jumpSpeed = 500;

        // Calcolo il tempo "fisico" del salto.
        // Tempo fino all'apice (altezza max salto) = jumpSpeed/gravity. Il tempo totale è il doppio di questo.
        totalJumpTime = (jumpSpeed / gravity) * 2.0f;

        // Inizializzazione delle animazioni per il salto usando totalJumpTime
        String[] upFiles = {"assets/jump1.png", "assets/jump2.png", "assets/jump3.png"};
        jumpUp = loadAnimationFromFiles(upFiles, (totalJumpTime * 0.10f) / 3f, false);

        String[] landFiles = {"assets/jump9.png", "assets/jump10.png"};
        jumpLand = loadAnimationFromFiles(landFiles, (totalJumpTime * 0.10f) / 2f, false);

        frameRise1 = loadTexture("assets/jump4.png");
        frameRise2 = loadTexture("assets/jump5.png");
        frameAltezzaMax = loadTexture("assets/jump6.png");
        frameDrop1 = loadTexture("assets/jump7.png");
        frameDrop2 = loadTexture("assets/jump8.png");

        setBoundaryPolygon(6);
        belowSensor = new BaseActor(0,0, s);
        belowSensor.loadTexture("assets/white.png");
        belowSensor.setSize( this.getWidth() - 8, 8 );
        belowSensor.setBoundaryRectangle();
        belowSensor.setVisible(true);

        //inizializzazione degli oggetti per l'attacco
        String[] attackFile = {"assets/attack3.png", "assets/attack10.png", "assets/attack8.png", "assets/attack9.png"};
        attack = loadAnimationFromFiles(attackFile, 0.06f, false);
        attack.setPlayMode(Animation.PlayMode.NORMAL);
    }

    public void act(float dt)
    {
        super.act( dt );

        boolean currentlyOnSolid = isOnSolid();

        // Logica per rilevare l'inizio di una caduta (Coyote time o salto)
        if (wasOnSolid && !currentlyOnSolid && !isJumping)
        {
            isJumping = true;
            jumpTimer = totalJumpTime * 0.11f;
        }

        // Logica per rilevare l'atterraggio
        if (!wasOnSolid && currentlyOnSolid)
        {
            isJumping = false;
            isLanding = true;
            jumpTimer = 0;
            elapsedTime = 0;
        }

        wasOnSolid = currentlyOnSolid;

        if (!isAttacking)
        {
            if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.J))
            {
                isAttacking = true;
                elapsedTime = 0;
                setAnimation(attack);
            }
        }

        if (!isAttacking)
        {
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A))
                accelerationVec.add( -walkAcceleration, 0 );
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D))
                accelerationVec.add( walkAcceleration, 0 );
        }

        //l'effetto di gravità agisce anche sulla velocità del personaggio
        accelerationVec.add(0, -gravity);
        velocityVec.add( accelerationVec.x * dt, accelerationVec.y * dt );

        /*
        Successivamente, se il personaggio non sta accelerando (il che accade quando il giocatore non preme sinistra o destra),
        allora avviene una decelerazione. Innanzitutto, viene calcolata la quantità di decelerazione (che dipende da dt, la
        quantità di tempo trascorso).
        La direzione di camminata (positiva indica destra, negativa indica sinistra) e la velocità di camminata (il valore assoluto della velocità) vengono memorizzate nelle variabili. La velocità di camminata
        viene diminuita della quantità di decelerazione e, se il valore diventa negativo, viene impostato su 0.
        Dopo queste regolazioni, il valore della velocità x viene ricalcolato dalla velocità e dalla direzione di camminata
         */
        if ( !Gdx.input.isKeyPressed(Input.Keys.RIGHT) && !Gdx.input.isKeyPressed(Input.Keys.LEFT) &&
                !Gdx.input.isKeyPressed(Input.Keys.D) && !Gdx.input.isKeyPressed(Input.Keys.A) )
        {
            float decelerationAmount = walkDeceleration * dt;
            float walkDirection = (velocityVec.x > 0) ? 1 : -1;
            float walkSpeed = Math.abs( velocityVec.x );
            walkSpeed -= decelerationAmount;
            if (walkSpeed < 0)
                walkSpeed = 0;
            velocityVec.x = walkSpeed * walkDirection;
        }

        /*
        Oltre agli effetti della decelerazione, la velocità nelle direzioni x e y deve rimanere entro
        i limiti stabiliti dalle variabili che memorizzano la velocità massima in queste direzioni. Ciò può essere
        ottenuto utilizzando il metodo clamp della classe MathUtils come segue:
         */
        velocityVec.x = MathUtils.clamp( velocityVec.x, -maxHorizontalSpeed, maxHorizontalSpeed );
        velocityVec.y = MathUtils.clamp( velocityVec.y, -maxVerticalSpeed, maxVerticalSpeed );

        //si aggiorna la posizione del personaggio
        moveBy( velocityVec.x * dt, velocityVec.y * dt );
        accelerationVec.set(0,0);

        //allinea il sensore nella corretta posizione, IMPORTANTE inserirlo dopo il reset dell'accelerazione
        belowSensor.setPosition( getX() + 4, getY() - 8 );

        /*
        Infine, devi passare da un'animazione all'altra in base allo stato (attacco, salto, movimento);
        una velocità pari a 0 indica che il personaggio è fermo e dovrebbe usare l'animazione corrispondente.
        Inoltre, se il personaggio si muove verso sinistra, si utilizza l'immagine speculare delle texture in modo che
        sembri rivolto verso sinistra.
        L'inversione dell'immagine può essere facilmente eseguita impostando la scala nella direzione x su -1 e l'immagine può essere ripristinata impostando di nuovo la scala su 1.
         */
        if (isAttacking)
        {
            if (attack.isAnimationFinished(elapsedTime))
            {
                isAttacking = false;
            }
        }
        else if (isLanding)
        {
            setAnimation(jumpLand);
            if (jumpLand.isAnimationFinished(elapsedTime))
            {
                isLanding = false;
            }
        }
        else if (isJumping)
        {
            jumpTimer += dt;
            updateJumpAnimation();
        }
        else
        {
            if ( currentlyOnSolid )
            {
                belowSensor.setColor( Color.GREEN );
                if ( velocityVec.x == 0 )
                    setAnimation(stand);
                else
                    setAnimation(walk);

                if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.W))
                {
                    jump();
                }
            }
        }

        if (!isAttacking)
        {
            if ( velocityVec.x > 0 ) setScaleX(1);
            if ( velocityVec.x < 0 ) setScaleX(-1);
        }

        //allinea la telecamera sul personaggio e verifica che non si superino i confini della mappa
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
        isJumping = true;
        jumpTimer = 0;
        elapsedTime = 0;
    }
}
