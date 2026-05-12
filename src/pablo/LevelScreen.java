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

import java.util.ArrayList;

public class LevelScreen extends BaseScreen
{
    // -----------------------------------------------------------------------
    // Path mappa — parametrico: passa il .tmx che vuoi nel costruttore
    // -----------------------------------------------------------------------
    private static final String DEFAULT_MAP = "assets/Maps/Mappa1.tmx";
    private final String mapPath;

    // -----------------------------------------------------------------------
    // Asset paths
    // -----------------------------------------------------------------------
    private static final String MASK_PATH = "assets/mask.png";
    private static final String SOUL_DIR  = "assets/soul/";

    // Colori maschere
    private static final Color MASK_FULL  = Color.WHITE;
    private static final Color MASK_EMPTY = new Color(0.15f, 0.15f, 0.15f, 0.85f);

    // -----------------------------------------------------------------------
    // Ricettacolo anima
    // -----------------------------------------------------------------------
    private static final int   VESSEL_STATES    = 9;
    private static final int   FRAMES_PER_STATE = 6;
    private static final float FRAME_DURATION   = 0.083f;

    private TextureRegionDrawable[][] vesselDrawables;
    private Texture[][]               vesselTextures;

    private Image vesselImage;
    private float vesselAnimTime  = 0f;
    private int   lastVesselState = -1;

    // -----------------------------------------------------------------------
    // Campi
    // -----------------------------------------------------------------------
    private Pablo pablo;

    private Texture  maskTexture;
    private Image[]  maskImages;

    // FalseKnight è opzionale: null se la mappa non lo contiene
    private FalseKnight falseKnight = null;

    private float shakeDuration  = 0f;
    private float shakeIntensity = 5f;
    private float enemyRuntimeLogTimer = 0f;
    private int   enemyRuntimeLogCount = 0;

    // -----------------------------------------------------------------------
    // UI scaling HUD
    // -----------------------------------------------------------------------
    private static final float HUD_SCALE       = 1.75f;
    private static final float MASK_W          = 40f * HUD_SCALE;
    private static final float MASK_H          = 32f * HUD_SCALE;
    private static final float MASK_PAD_RIGHT  = 6f  * HUD_SCALE;
    private static final float VESSEL_SIZE     = 48f * HUD_SCALE;
    private static final float HUD_PAD_TOP     = 14f * HUD_SCALE;
    private static final float VESSEL_PAD_TOP  = 10f * HUD_SCALE;
    private static final float VESSEL_PAD_LEFT = 20f * HUD_SCALE;

    private float voidTimer = 0f;
    private static final float VOID_RESPAWN_DELAY = 0.2f;

    // -----------------------------------------------------------------------
    // Costruttori
    // -----------------------------------------------------------------------

    /** Usa la mappa di default (mapPablo2.tmx). */
    public LevelScreen()
    {
        this(DEFAULT_MAP);
    }

    /**
     * Usa la mappa specificata.
     * @param mapPath path relativo al file .tmx, es. "assets/Maps/miaMap.tmx"
     */
    public LevelScreen(String mapPath)
    {
        this.mapPath = mapPath;
    }

    // -----------------------------------------------------------------------
    // initialize()
    // -----------------------------------------------------------------------
    public void initialize()
    {
        // Se mapPath è null, usa la mappa di default
        String effectiveMapPath = (mapPath != null) ? mapPath : DEFAULT_MAP;
        if (mapPath == null)
            Gdx.app.log("LevelScreen", "mapPath è null, usando default: " + DEFAULT_MAP);

        Gdx.app.log("LevelScreen", "Caricamento mappa: " + effectiveMapPath);

        TilemapActor tma = new TilemapActor(effectiveMapPath, mainStage);

        // Solidi
        for (MapObject obj : tma.getRectangleList("solido"))
        {
            MapProperties props = obj.getProperties();
            new Object(
                    (float) props.get("x"), (float) props.get("y"),
                    (float) props.get("width"), (float) props.get("height"),
                    mainStage
            );
        }

        // Punto di spawn — errore chiaro se mancante
        ArrayList<MapObject> startPoints = tma.getRectangleList("start");
        if (startPoints.isEmpty())
            throw new IllegalStateException(
                    "[LevelScreen] Nessun oggetto 'start' trovato in: " + mapPath
                            + "\nControlla che il layer Oggetti abbia un rettangolo con nome 'start'.");

        MapObject startPoint = startPoints.get(0);
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
            maskRow.add(maskImages[i]).size(MASK_W, MASK_H).padRight(MASK_PAD_RIGHT);
        }

        // -----------------------------------------------------------------------
        // UI — ricettacolo anima
        // -----------------------------------------------------------------------
        vesselTextures  = new Texture[VESSEL_STATES][];
        vesselDrawables = new TextureRegionDrawable[VESSEL_STATES][];

        vesselTextures[0]  = new Texture[1];
        vesselDrawables[0] = new TextureRegionDrawable[1];
        vesselTextures[0][0]  = createEmptyVesselTexture((int) VESSEL_SIZE);
        vesselDrawables[0][0] = new TextureRegionDrawable(
                new TextureRegion(vesselTextures[0][0]));

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

        // Layout UI
        uiTable.top().left();
        uiTable.add(maskRow).left().padTop(HUD_PAD_TOP).padLeft(14).row();
        uiTable.add(vesselImage).size(VESSEL_SIZE, VESSEL_SIZE)
                .left().padTop(VESSEL_PAD_TOP).padLeft(VESSEL_PAD_LEFT).row();

        // -----------------------------------------------------------------------
        // Nemici comuni
        // -----------------------------------------------------------------------
        spawnEnemiesFromMap(tma);

        // -----------------------------------------------------------------------
        // FalseKnight — opzionale
        // -----------------------------------------------------------------------
        ArrayList<MapObject> falseKnightPoints = tma.getRectangleList("FalseKnight");

        if (!falseKnightPoints.isEmpty())
        {
            MapObject fkPoint = falseKnightPoints.get(0);
            MapProperties fkProps = fkPoint.getProperties();
            float fkX = (float) fkProps.get("x");
            float fkY = (float) fkProps.get("y");

            falseKnight = new FalseKnight(fkX, fkY, mainStage, pablo);
            falseKnight.setScreenShakeCallback(() -> shakeDuration = 0.15f);

            Gdx.app.log("LevelScreen", "FalseKnight spawned at " + fkX + "," + fkY);
        }
        else
        {
            Gdx.app.log("LevelScreen", "FalseKnight non presente in questa mappa — skip.");
        }

        // Z-order
        tma.toBack();
        for (BaseActor eActor : BaseActor.getList(mainStage, Enemy.class.getName()))
            eActor.toFront();
        if (falseKnight != null) falseKnight.toFront();
        pablo.toFront();
    }

    // -----------------------------------------------------------------------
    // spawnEnemiesFromMap()
    // -----------------------------------------------------------------------
    private void spawnEnemiesFromMap(TilemapActor tma)
    {
        spawnEnemyType(tma, "Crawlid");
        spawnEnemyType(tma, "Tiktik");
        spawnEnemyType(tma, "Vengefly");
        spawnEnemyType(tma, "HuskBully");
        spawnEnemyType(tma, "HuskHornhead");
        spawnEnemyType(tma, "HuskWarrior");
    }

    private void spawnEnemyType(TilemapActor tma, String type)
    {
        ArrayList<MapObject> objects = tma.getRectangleList(type);

        for (MapObject obj : objects)
        {
            MapProperties props = obj.getProperties();
            float x = (float) props.get("x");
            float y = (float) props.get("y");

            Enemy enemy = createEnemy(type, x, y);
            enemy.toFront();
        }
    }

    private Enemy createEnemy(String type, float x, float y)
    {
        switch (type)
        {
            case "Crawlid":      return new Crawlid(x, y, mainStage, pablo);
            case "Tiktik":       return new Tiktik(x, y, mainStage, pablo);
            case "Vengefly":     return new Vengefly(x, y, mainStage, pablo);
            case "HuskBully":    return new HuskBully(x, y, mainStage, pablo);
            case "HuskHornhead": return new HuskHornhead(x, y, mainStage, pablo);
            case "HuskWarrior":  return new HuskWarrior(x, y, mainStage, pablo);
            default: throw new IllegalArgumentException("Tipo nemico sconosciuto: " + type);
        }
    }

    // -----------------------------------------------------------------------
    // update()
    // -----------------------------------------------------------------------
    public void update(float dt)
    {
        SoundManager.get().update(dt);

        // Maschere HP
        int health = pablo.getHealth();
        for (int i = 0; i < maskImages.length; i++)
            maskImages[i].setColor(i < health ? MASK_FULL : MASK_EMPTY);

        // Ricettacolo anima
        updateVessel(dt);

        // Void fall
        if (pablo.getY() <= 2f && !pablo.isOnSolid())
        {
            voidTimer += dt;
            if (voidTimer >= VOID_RESPAWN_DELAY)
            {
                pablo.respawn();
                voidTimer = 0f;
            }
            return;
        }
        else
        {
            voidTimer = 0f;
        }

        // Collisioni player <-> solidi
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

        // Collisioni nemici <-> solidi
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
                        {
                            enemy.velocityVec.x = 0;
                            enemy.onWallHit();
                        }
                        else
                        {
                            enemy.velocityVec.y = 0;
                        }
                    }
                }
            }
        }

        // Screen shake
        if (shakeDuration > 0)
        {
            shakeDuration -= dt;
            Camera cam = mainStage.getCamera();
            cam.position.y += (Math.random() > 0.5 ? 1 : -1) * shakeIntensity;
            cam.update();
        }

        // FalseKnight — solo se presente nella mappa
        if (falseKnight != null && !falseKnight.isDead())
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

            if (falseKnight.maceOverlapsPablo())
                pablo.takeDamage(1);
        }

        logEnemyRuntimeState(dt);
    }

    private void logEnemyRuntimeState(float dt)
    {
        if (enemyRuntimeLogCount >= 5) return;
        enemyRuntimeLogTimer += dt;
        if (enemyRuntimeLogTimer < 1f) return;

        enemyRuntimeLogTimer = 0f;
        enemyRuntimeLogCount++;

        Camera cam = mainStage.getCamera();
        Gdx.app.log("EnemyRuntime", "sample=" + enemyRuntimeLogCount
                + " camera=" + cam.position.x + "," + cam.position.y
                + " enemies=" + BaseActor.count(mainStage, Enemy.class.getName()));
    }

    // -----------------------------------------------------------------------
    // updateVessel()
    // -----------------------------------------------------------------------
    private void updateVessel(float dt)
    {
        int soul = pablo.getSoul();
        int vesselState = getVesselStateForSoul(soul);

        if (vesselState != lastVesselState)
        {
            vesselAnimTime  = 0f;
            lastVesselState = vesselState;
        }

        if (vesselState == 0)
        {
            vesselImage.setDrawable(vesselDrawables[0][0]);
            return;
        }

        vesselAnimTime += dt;
        int   frameCount     = vesselDrawables[vesselState].length;
        float totalDuration  = FRAME_DURATION * frameCount;
        vesselAnimTime %= totalDuration;

        int frameIndex = (int)(vesselAnimTime / FRAME_DURATION);
        frameIndex = Math.min(frameIndex, frameCount - 1);

        vesselImage.setDrawable(vesselDrawables[vesselState][frameIndex]);
    }

    // -----------------------------------------------------------------------
    // keyDown()
    // -----------------------------------------------------------------------
    public boolean keyDown(int keyCode)
    {
        if (keyCode == Input.Keys.ESCAPE || keyCode == Input.Keys.P)
        {
            BaseGame.setActiveScreen(new PauseScreen(this));
            return true;
        }
        if (keyCode == Input.Keys.K) { pablo.takeDamage(1); return true; }
        if (keyCode == Input.Keys.O)
        {
            if (falseKnight != null) falseKnight.takeDamage(1);
            return true;
        }
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
        Pixmap pix = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pix.setColor(0f, 0f, 0f, 0f);
        pix.fill();

        int cx     = size / 2;
        int cy     = size / 2;
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
            if (getVesselStateForSoul(soul) == state) return soul;
        return Pablo.MAX_SOUL;
    }

    private void bumpSoulToNextVesselState()
    {
        int currentState = getVesselStateForSoul(pablo.getSoul());
        int nextState    = Math.min(currentState + 1, 8);
        pablo.setSoul(getSoulForVesselState(nextState));
    }

    // -----------------------------------------------------------------------
    // dispose()
    // -----------------------------------------------------------------------
    @Override
    public void dispose()
    {
        super.dispose();
        if (maskTexture != null) maskTexture.dispose();
        if (vesselTextures != null)
            for (Texture[] row : vesselTextures)
                if (row != null)
                    for (Texture t : row)
                        if (t != null) t.dispose();
    }
}

