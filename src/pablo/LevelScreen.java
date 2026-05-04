// LevelScreen.java

package pablo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import pablo.entities.enemies.crawlid.Crawlid;
import pablo.entities.enemies.Enemy;
import pablo.entities.enemies.falseKnight.FalseKnight;
import pablo.entities.enemies.tiktik.Tiktik;
import pablo.entities.enemies.vengefly.Vengefly;
import pablo.entities.player.Pablo;
import pablo.framework.BaseActor;
import pablo.framework.BaseGame;
import pablo.framework.BaseScreen;
import pablo.framework.TilemapActor;

import com.badlogic.gdx.graphics.Camera;

import pablo.entities.enemies.huskBully.HuskBully;
import pablo.entities.enemies.huskHornhead.HuskHornhead;
import pablo.entities.enemies.huskWarrior.HuskWarrior;

public class LevelScreen extends BaseScreen
{
    // -----------------------------------------------------------------------
    // Asset paths
    // -----------------------------------------------------------------------
    private static final String MASK_PATH   = "assets/mask.png";
    private static final String SOUL_DIR    = "assets/soul/";

    // Colori maschere
    private static final Color MASK_FULL  = Color.WHITE;
    private static final Color MASK_EMPTY = new Color(0.15f, 0.15f, 0.15f, 0.85f);

    // -----------------------------------------------------------------------
    // Ricettacolo anima — struttura dati
    //
    // Stato 0  → texture generata (vuoto, 1 frame)
    // Stato 1  → "1_1..1_6"   (6 frame)
    // ...
    // Stato 8  → "8_1..8_6"   (6 frame)
    //
    // Soglie anima (0-100): lo stato aumenta ogni ~12 punti
    // -----------------------------------------------------------------------
    private static final int   VESSEL_STATES      = 9;    // 0..8
    private static final int   FRAMES_PER_STATE   = 6;    // stati 1-8
    private static final float FRAME_DURATION     = 0.083f; // ~12 fps

    // [stato][frame] — stato 0 ha solo [0][0]
    private TextureRegionDrawable[][] vesselDrawables;
    private Texture[][]               vesselTextures;

    private Image vesselImage;
    private float vesselAnimTime = 0f;
    private int   lastVesselState = -1;

    // -----------------------------------------------------------------------
    // Resto dei campi
    // -----------------------------------------------------------------------
    private Pablo pablo;

    private Texture  maskTexture;
    private Image[]  maskImages;

    private FalseKnight falseKnight;
    private float shakeDuration  = 0f;
    private float shakeIntensity = 5f;

    // -----------------------------------------------------------------------
    // initialize()
    // -----------------------------------------------------------------------
    public void initialize()
    {
        // Carica la mappa e popola i solidi dal layer oggetti "solido".
        TilemapActor tma = new TilemapActor("assets/Maps/mapPablo2.tmx", mainStage);

        for (MapObject obj : tma.getRectangleList("solido"))
        {
            MapProperties props = obj.getProperties();
            new Object(
                    (float) props.get("x"), (float) props.get("y"),
                    (float) props.get("width"), (float) props.get("height"),
                    mainStage
            );
        }

        // Punto di spawn del player dalla mappa (oggetto "start").
        MapObject startPoint = tma.getRectangleList("start").get(0);
        MapProperties startProps = startPoint.getProperties();
        pablo = new Pablo(
                (float) startProps.get("x"),
                (float) startProps.get("y"),
                mainStage
        );
        pablo.setSoul(0);

        // -----------------------------------------------------------------------
        // UI — maschere HP
        // -----------------------------------------------------------------------
        maskTexture = loadTextureSafe(MASK_PATH, Color.WHITE);
        maskImages  = new Image[pablo.getMaxHealth()];

        Table maskRow = new Table();
        for (int i = 0; i < pablo.getMaxHealth(); i++)
        {
            maskImages[i] = new Image(maskTexture);
            maskImages[i].setColor(MASK_FULL);
            maskRow.add(maskImages[i]).size(40, 32).padRight(6);
        }

        // -----------------------------------------------------------------------
        // UI — ricettacolo anima animato
        // -----------------------------------------------------------------------
        // Precarica i frame del ricettacolo anima (stati 0-8).
        vesselTextures  = new Texture[VESSEL_STATES][];
        vesselDrawables = new TextureRegionDrawable[VESSEL_STATES][];

        // Stato 0: texture generata (vuoto) — 1 frame
        vesselTextures[0]  = new Texture[1];
        vesselDrawables[0] = new TextureRegionDrawable[1];
        vesselTextures[0][0]  = createEmptyVesselTexture(48);
        vesselDrawables[0][0] = new TextureRegionDrawable(
                new TextureRegion(vesselTextures[0][0]));

        // Stati 1-8: "X_1.png" .. "X_6.png" — 6 frame ciascuno
        for (int state = 1; state < VESSEL_STATES; state++)
        {
            vesselTextures[state]  = new Texture[FRAMES_PER_STATE];
            vesselDrawables[state] = new TextureRegionDrawable[FRAMES_PER_STATE];

            for (int frame = 0; frame < FRAMES_PER_STATE; frame++)
            {
                String path = SOUL_DIR + state + "_" + (frame + 1) + ".png";
                Color fb = new Color(state / 8f, state / 8f, state / 8f, 1f);
                vesselTextures[state][frame]  = loadTextureSafe(path, fb);
                vesselDrawables[state][frame] = new TextureRegionDrawable(
                        new TextureRegion(vesselTextures[state][frame]));
            }
        }

        vesselImage = new Image(vesselDrawables[0][0]);

        // -----------------------------------------------------------------------
        // Layout UI — in alto a sinistra
        // -----------------------------------------------------------------------
        uiTable.top().left();
        uiTable.add(maskRow).left().padTop(14).padLeft(14).row();
        uiTable.add(vesselImage).size(48, 48).left().padTop(10).padLeft(20).row();

        // -----------------------------------------------------------------------
        // Nemici
        // -----------------------------------------------------------------------
        // Spawn nemici dai tile objects della mappa.
        for (MapObject obj : tma.getRectangleList("Crawlid"))
        {
            MapProperties props = obj.getProperties();
            new Crawlid((float) props.get("x"), (float) props.get("y"), mainStage, pablo);
        }
        for (MapObject obj : tma.getRectangleList("Tiktik"))
        {
            MapProperties props = obj.getProperties();
            new Tiktik((float) props.get("x"), (float) props.get("y"), mainStage, pablo);
        }
        for (MapObject obj : tma.getRectangleList("Vengefly"))
        {
            MapProperties props = obj.getProperties();
            new Vengefly((float) props.get("x"), (float) props.get("y"), mainStage, pablo);
        }
        for (MapObject obj : tma.getRectangleList("HuskBully"))
        {
            MapProperties props = obj.getProperties();
            new HuskBully((float) props.get("x"), (float) props.get("y"), mainStage, pablo);
        }
        for (MapObject obj : tma.getRectangleList("HuskHornhead"))
        {
            MapProperties props = obj.getProperties();
            new HuskHornhead((float) props.get("x"), (float) props.get("y"), mainStage, pablo);
        }
        for (MapObject obj : tma.getRectangleList("HuskWarrior"))
        {
            MapProperties props = obj.getProperties();
            new HuskWarrior((float) props.get("x"), (float) props.get("y"), mainStage, pablo);
        }

        // Spawn del boss e callback per screen shake.
        MapObject fkPoint = tma.getRectangleList("FalseKnight").get(0);
        MapProperties fkProps = fkPoint.getProperties();

        falseKnight = new FalseKnight(
                (float) fkProps.get("x"), (float) fkProps.get("y"),
                mainStage, pablo
        );
        falseKnight.setScreenShakeCallback(() -> shakeDuration = 0.15f);
    }

    // -----------------------------------------------------------------------
    // update()
    // -----------------------------------------------------------------------
    public void update(float dt)
    {
        // Aggiorna le maschere in base agli HP attuali.
        int health = pablo.getHealth();
        for (int i = 0; i < maskImages.length; i++)
            maskImages[i].setColor(i < health ? MASK_FULL : MASK_EMPTY);

        // Aggiorna l'animazione del ricettacolo in base alla soul.
        updateVessel(dt);

        // Void fall: respawn Pablo if he falls below the map
        if (pablo.isInVoid())
        {
            pablo.respawn();
            return; // skip collision resolution this frame
        }

        // Collisioni player <-> solidi con risoluzione tramite MTV.
        for (BaseActor actor : BaseActor.getList(mainStage, Object.class.getName()))
        {
            Object oggetto = (Object) actor;
            if (pablo.overlaps(oggetto) && oggetto.isEnable())
            {
                Vector2 offset = pablo.preventOverlap(oggetto);
                if (offset != null)
                {
                    if (Math.abs(offset.x) > Math.abs(offset.y))
                        pablo.velocityVec.x = 0;
                    else
                        pablo.velocityVec.y = 0;
                }
            }
        }

        // Collisioni nemici <-> solidi (wall hit o stop verticale).
        for (BaseActor eActor : BaseActor.getList(mainStage, Enemy.class.getName()))
        {
            Enemy enemy = (Enemy) eActor;
            for (BaseActor sActor : BaseActor.getList(mainStage, Object.class.getName()))
            {
                Object solid = (Object) sActor;
                if (enemy.overlaps(solid) && solid.isEnable())
                {
                    Vector2 offset = enemy.preventOverlap(solid);
                    if (offset != null)
                    {
                        if (Math.abs(offset.x) > Math.abs(offset.y))
                            enemy.onWallHit();
                        else
                            enemy.velocityVec.y = 0;
                    }
                }
            }
        }

        // Screen shake leggero dopo gli attacchi del boss.
        if (shakeDuration > 0)
        {
            shakeDuration -= dt;
            Camera cam = mainStage.getCamera();
            cam.position.y += (Math.random() > 0.5 ? 1 : -1) * shakeIntensity;
            cam.update();
        }

        // Collisioni del boss con solidi e gestione atterraggio/parete.
        if (!falseKnight.isDead())
        {
            for (BaseActor sActor : BaseActor.getList(mainStage, Object.class.getName()))
            {
                Object solid = (Object) sActor;
                if (falseKnight.overlaps(solid) && solid.isEnable())
                {
                    Vector2 offset = falseKnight.preventOverlap(solid);
                    if (offset != null)
                    {
                        if (Math.abs(offset.x) > Math.abs(offset.y))
                        {
                            falseKnight.velocityVec.x = 0;
                            falseKnight.onWallHit();
                        }
                        else
                        {
                            falseKnight.velocityVec.y = 0;
                            falseKnight.onGroundLanded();
                        }
                    }
                }
            }
        }

        // Danno diretto se il mace hitbox colpisce Pablo.
        if (!falseKnight.isDead() && falseKnight.maceOverlapsPablo())
            pablo.takeDamage(1);
    }

    // -----------------------------------------------------------------------
    // Logica animazione ricettacolo
    // -----------------------------------------------------------------------
    private void updateVessel(float dt)
    {
        int soul = pablo.getSoul();

        // Mappa soul (0-99) → stato (0-8)
        // stato 0 = vuoto, stati 1-8 proporzionali
        int vesselState = getVesselStateForSoul(soul);

        // Se cambio stato, resetta il timer di animazione
        if (vesselState != lastVesselState)
        {
            vesselAnimTime  = 0f;
            lastVesselState = vesselState;
        }

        // Stato 0: unico frame, nessuna animazione
        if (vesselState == 0)
        {
            vesselImage.setDrawable(vesselDrawables[0][0]);
            return;
        }

        // Stati 1-8: avanza il timer e scegli il frame corrente (looping)
        vesselAnimTime += dt;
        int frameCount = vesselDrawables[vesselState].length; // sempre 6
        float totalDuration = FRAME_DURATION * frameCount;

        // Looping: riparti dall'inizio quando finisce il ciclo
        vesselAnimTime %= totalDuration;

        int frameIndex = (int)(vesselAnimTime / FRAME_DURATION);
        frameIndex = Math.min(frameIndex, frameCount - 1); // sicurezza bounds

        vesselImage.setDrawable(vesselDrawables[vesselState][frameIndex]);
    }

    // -----------------------------------------------------------------------
    // keyDown()
    // -----------------------------------------------------------------------
    public boolean keyDown(int keyCode)
    {
        // Pausa e comandi debug rapidi per test.
        if (keyCode == Input.Keys.ESCAPE || keyCode == Input.Keys.P)
        {
            BaseGame.setActiveScreen(new PauseScreen(this));
            return true;
        }
        if (keyCode == Input.Keys.K) { pablo.takeDamage(1);       return true; }
        if (keyCode == Input.Keys.O) { falseKnight.takeDamage(1); return true; }
        if (keyCode == Input.Keys.L) { bumpSoulToNextVesselState(); return true; }

        if (keyCode == Input.Keys.SPACE || keyCode == Input.Keys.W)
            if (pablo.isOnSolid()) pablo.jump();

        return false;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------
    private Texture loadTextureSafe(String path, Color fallback)
    {
        // Carica la texture; se manca, genera un placeholder colorato per evitare crash.
        try
        {
            return new Texture(Gdx.files.internal(path));
        }
        catch (Exception e)
        {
            Gdx.app.log("LevelScreen", "Asset non trovato: " + path);
            Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pix.setColor(fallback);
            pix.fill();
            Texture t = new Texture(pix);
            pix.dispose();
            return t;
        }
    }

    private Texture createEmptyVesselTexture(int size)
    {
        // Cerchio scuro su fondo trasparente per indicare il vuoto.
        Pixmap pix = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pix.setColor(0f, 0f, 0f, 0f);
        pix.fill();

        int cx = size / 2;
        int cy = size / 2;
        int radius = Math.max(1, (size / 2) - 2);

        pix.setColor(0.12f, 0.12f, 0.12f, 1f);
        pix.fillCircle(cx, cy, radius - 2);

        pix.setColor(0.35f, 0.35f, 0.35f, 1f);
        pix.drawCircle(cx, cy, radius - 1);

        Texture t = new Texture(pix);
        pix.dispose();
        return t;
    }

    private int getVesselStateForSoul(int soul)
    {
        if (soul <= 0) return 0;
        return Math.min(1 + (soul - 1) * 8 / (Pablo.MAX_SOUL - 1), 8);
    }

    private int getSoulForVesselState(int state)
    {
        if (state <= 0) return 0;
        if (state >= 8) return Pablo.MAX_SOUL;

        for (int soul = 1; soul <= Pablo.MAX_SOUL; soul++)
        {
            if (getVesselStateForSoul(soul) == state)
                return soul;
        }
        return Pablo.MAX_SOUL;
    }

    private void bumpSoulToNextVesselState()
    {
        int currentState = getVesselStateForSoul(pablo.getSoul());
        int nextState = Math.min(currentState + 1, 8);
        pablo.setSoul(getSoulForVesselState(nextState));
    }

    // -----------------------------------------------------------------------
    // dispose()
    // -----------------------------------------------------------------------
    @Override
    public void dispose()
    {
        super.dispose();
        // Libera le texture UI caricate manualmente.
        if (maskTexture != null) maskTexture.dispose();
        if (vesselTextures != null)
            for (Texture[] row : vesselTextures)
                if (row != null)
                    for (Texture t : row)
                        if (t != null) t.dispose();
    }
}