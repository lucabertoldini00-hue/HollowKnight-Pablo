// LevelScreen.java

package pablo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ShortArray;
import com.badlogic.gdx.utils.Align;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
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

import com.badlogic.gdx.scenes.scene2d.actions.Actions;

public class LevelScreen extends BaseScreen
{
    private static final String PROMPT_FONT_PATH = "assets/TrajanPro-Regular.ttf";

    private static final Color PROMPT_COL_ORN  = new Color(0.40f, 0.90f, 0.45f, 1.0f);
    private static final Color PROMPT_COL_TEXT = new Color(0.75f, 1.00f, 0.75f, 1.0f);
    private static final Color PROMPT_BG_COL   = new Color(0.02f, 0.02f, 0.03f, 0.94f);

    private static final float PROMPT_PANEL_W = 240f;
    private static final float PROMPT_PANEL_H = 80f;
    private static final float PROMPT_LINE_W  = 80f;
    private static final float PROMPT_LINE_H  = 2f;
    // -----------------------------------------------------------------------
    // Path mappa — parametrico: passa il .tmx che vuoi nel costruttore
    // -----------------------------------------------------------------------
    private static final String DEFAULT_MAP = "assets/Maps/Mappa1.tmx";
    private final String mapPath;
    private final String spawnSide; // "start", "left", "right"

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

    private BaseActor bossTrigger = null;
    private Vector2 bossTeleportPoint = null;
    private Vector2 bossSpawnPoint = null;
    private boolean bossDialogOpen = false;
    private boolean bossFightStarted = false;
    private boolean bossDeathSequenceActive = false;

    private Table bossDialog;
    private Texture dialogTexture;
    private BitmapFont dialogFont;
    private Texture dialogBtnUpTexture;
    private Texture dialogBtnDownTexture;
    private Table bossDeathTable;
    private Texture bossDeathBgTexture;
    private Texture bossDeathTexture;
    private Image bossDeathImage;
    private Label bossDeathLine1;
    private Label bossDeathLine2;

    private Table bossPromptTable;
    private Label bossPromptLabel;
    private BitmapFont promptFont;
    private boolean promptFontOwned;
    private Texture promptBgTexture;
    private Texture promptLineTexture;
    private Texture promptDotTexture;
    private Texture promptDecoTexture;

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

    /** Usa la mappa di default. */
    public LevelScreen()
    {
        this(DEFAULT_MAP, "start");
    }

    /** Usa la mappa specificata. */
    public LevelScreen(String mapPath)
    {
        this(mapPath, "start");
    }

    /** Usa la mappa e lato di spawn specificati. */
    public LevelScreen(String mapPath, String spawnSide)
    {
        super();
        this.mapPath = mapPath;
        this.spawnSide = (spawnSide != null) ? spawnSide : "start";
        initializeLevel();
    }

    // -----------------------------------------------------------------------
    // initialize()
    // -----------------------------------------------------------------------
    public void initialize()
    {
        // BaseScreen chiama initialize() nel suo costruttore: qui evitiamo
        // di usare campi non ancora assegnati e inizializziamo dopo.
    }

    private void initializeLevel()
    {
        String effectiveMapPath = (mapPath != null) ? mapPath : DEFAULT_MAP;

        Gdx.app.log("LevelScreen", "Caricamento mappa: " + effectiveMapPath);

        TilemapActor tma = new TilemapActor(effectiveMapPath, mainStage);

        // Solidi
        for (MapObject obj : tma.getRectangleList("solido"))
        {
            if (obj instanceof RectangleMapObject)
            {
                Rectangle rect = ((RectangleMapObject) obj).getRectangle();
                new Object(rect.x, rect.y, rect.width, rect.height, mainStage);
                continue;
            }

            if (obj instanceof PolygonMapObject)
            {
                addSolidTriangles(((PolygonMapObject) obj).getPolygon());
            }
        }

        // Punto di spawn
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
        PlayerState.applyTo(pablo);
        if (spawnSide.equals("right"))
            pablo.setX(BaseActor.getWorldBounds().width - pablo.getWidth() - 10f);
        else if (spawnSide.equals("left"))
            pablo.setX(10f);

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

        buildBossPrompt();

        // -----------------------------------------------------------------------
        // Nemici comuni
        // -----------------------------------------------------------------------
        spawnEnemiesFromMap(tma);

        // -----------------------------------------------------------------------
        // Trigger boss e spawn point
        // -----------------------------------------------------------------------
        setupBossTrigger(tma);

        // -----------------------------------------------------------------------
        // FalseKnight — opzionale
        // -----------------------------------------------------------------------
        if (bossTrigger == null)
            spawnFalseKnightIfPresent();

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

        // Respawn immediato alla morte
        if (pablo.isDead())
        {
            if (bossFightStarted && !bossDeathSequenceActive)
            {
                startBossDeathSequence();
                return;
            }
             pablo.respawn();
             voidTimer = 0f;
             return;
        }
        if (bossDeathSequenceActive)
            return;

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
        // Tutti i nemici ricevono preventOverlap — sia in settle che attivi.
        // La collision orizzontale (onWallHit) viene applicata solo ai nemici
        // che hanno terminato il settle e sono vicini alla camera.
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
                            // onWallHit solo se l'AI è attiva
                            if (!enemy.isSettling() && enemy.isActiveNearCamera())
                                enemy.onWallHit();
                        }
                        else
                        {
                            enemy.velocityVec.y = 0;
                        }
                    }
                }
            }
            // Snap verticale: evita attraversamenti quando la caduta è veloce
            enemy.snapToGroundIfOverlapping();
        }

        for (BaseActor eActor : BaseActor.getList(mainStage, Enemy.class.getName()))
        {
            Enemy enemy = (Enemy) eActor;
            if (!enemy.isDead() && !enemy.isSettling() && pablo.overlaps(enemy))
                pablo.takeDamage(1, enemy.getX() + enemy.getWidth() / 2f);
        }

        // Screen shake
        if (shakeDuration > 0)
        {
            shakeDuration -= dt;
            Camera cam = mainStage.getCamera();
            cam.position.y += (Math.random() > 0.5 ? 1 : -1) * shakeIntensity;
            cam.update();
        }

        // Trigger dialogo boss
        if (bossTrigger != null && !bossFightStarted && !bossDialogOpen)
        {
            if (pablo.overlaps(bossTrigger))
            {
                bossPromptLabel.setVisible(true);
                bossPromptTable.setVisible(true);
                if (Gdx.input.isKeyJustPressed(Input.Keys.E))
                    openBossDialog();
            }
            else
            {
                bossPromptLabel.setVisible(false);
                bossPromptTable.setVisible(false);
            }
        }
        else if (bossPromptLabel != null)
        {
            bossPromptLabel.setVisible(false);
            bossPromptTable.setVisible(false);
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
                pablo.takeDamage(5, falseKnight.getX() + falseKnight.getWidth() / 2f);
        }

        // Transizione mappa destra
        MapGraph.MapNode node = MapGraph.get(mapPath);
        if (node != null && node.rightNeighbor != null)
        {
            if (pablo.getX() + pablo.getWidth() >= BaseActor.getWorldBounds().width - 2f)
            {
                PlayerState.saveFrom(pablo);
                BaseGame.setActiveScreen(new LevelScreen(node.rightNeighbor, "left"));
            }
        }
        // Transizione mappa sinistra
        if (node != null && node.leftNeighbor != null)
        {
            if (pablo.getX() <= 2f)
            {
                PlayerState.saveFrom(pablo);
                BaseGame.setActiveScreen(new LevelScreen(node.leftNeighbor, "right"));
            }
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
        int   frameCount    = vesselDrawables[vesselState].length;
        float totalDuration = FRAME_DURATION * frameCount;
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
        if (bossDialogOpen)
        {
            if (keyCode == Input.Keys.ENTER)
            {
                closeBossDialog();
                if (bossTeleportPoint != null)
                {
                    pablo.setPosition(bossTeleportPoint.x, bossTeleportPoint.y);
                    pablo.velocityVec.set(0, 0);
                }
                spawnFalseKnightIfPresent();
                return true;
            }
            if (keyCode == Input.Keys.ESCAPE)
            {
                closeBossDialog();
                return true;
            }
        }

        if (keyCode == Input.Keys.ESCAPE || keyCode == Input.Keys.P)
        {
            BaseGame.setActiveScreen(new PauseScreen(this));
            return true;
        }
        if (keyCode == Input.Keys.K) { pablo.takeDamage(1); return true; }
        if (keyCode == Input.Keys.NUM_9 || keyCode == Input.Keys.NUMPAD_9)
        {
            if (MapGraph.getFirstPath().equals(mapPath))
            {
                PlayerState.saveFrom(pablo);
                BaseGame.setActiveScreen(new LevelScreen(MapGraph.getLastPath(), "start"));
            }
            return true;
        }
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
    // Boss dialog / trigger
    // -----------------------------------------------------------------------
    private void setupBossTrigger(TilemapActor tma)
    {
        MapObject tpObj = getFirstMapObject(tma, "tp");
        if (tpObj != null)
        {
            if (tpObj instanceof RectangleMapObject)
            {
                Rectangle rect = ((RectangleMapObject) tpObj).getRectangle();
                bossTrigger = new BaseActor(rect.x, rect.y, mainStage);
                bossTrigger.setSize(rect.width, rect.height);
            }
            else
            {
                MapProperties props = tpObj.getProperties();
                float x = (float) props.get("x");
                float y = (float) props.get("y");
                float w = props.containsKey("width")  ? (float) props.get("width")  : 32f;
                float h = props.containsKey("height") ? (float) props.get("height") : 32f;
                bossTrigger = new BaseActor(x, y, mainStage);
                bossTrigger.setSize(w, h);
            }
            bossTrigger.setBoundaryRectangle();
            bossTrigger.setVisible(false);
        }

        MapObject sp = getFirstMapObject(tma, "spawntp");
        if (sp != null)
        {
            MapProperties props = sp.getProperties();
            bossTeleportPoint = new Vector2((float) props.get("x"), (float) props.get("y"));
        }

        MapObject fkPoint = getFirstMapObject(tma, "FalseKnight");
        if (fkPoint != null)
        {
            MapProperties fkProps = fkPoint.getProperties();
            bossSpawnPoint = new Vector2((float) fkProps.get("x"), (float) fkProps.get("y"));
        }
    }

    private MapObject getFirstMapObject(TilemapActor tma, String name)
    {
        ArrayList<MapObject> rects = tma.getRectangleList(name);
        if (!rects.isEmpty()) return rects.get(0);

        ArrayList<MapObject> tiles = tma.getTileList(name);
        if (!tiles.isEmpty()) return tiles.get(0);

        return null;
    }

    private void spawnFalseKnightIfPresent()
    {
        if (bossSpawnPoint == null)
        {
            return;
        }

        if (falseKnight != null) return;
        falseKnight = new FalseKnight(bossSpawnPoint.x, bossSpawnPoint.y, mainStage, pablo);
        falseKnight.setScreenShakeCallback(() -> shakeDuration = 0.15f);
        falseKnight.toFront();
        bossFightStarted = true;
    }

    private void openBossDialog()
    {
        if (bossDialog == null) buildBossDialog();
        bossDialog.setVisible(true);
        bossDialogOpen = true;
    }

    private void closeBossDialog()
    {
        if (bossDialog != null) bossDialog.setVisible(false);
        bossDialogOpen = false;
    }

    private void buildBossDialog()
    {
        dialogTexture = loadTextureSafe("assets/dialog.png", new Color(0f, 0f, 0f, 0.75f));
        try
        {
            FreeTypeFontGenerator gen = new FreeTypeFontGenerator(
                    Gdx.files.internal(PROMPT_FONT_PATH));
            FreeTypeFontParameter param = new FreeTypeFontParameter();
            param.size = 20;
            param.color = PROMPT_COL_TEXT;
            param.hinting = FreeTypeFontGenerator.Hinting.Full;
            dialogFont = gen.generateFont(param);
            gen.dispose();
        }
        catch (Exception e)
        {
            Gdx.app.log("LevelScreen", "Font dialogo non trovato, uso default: " + e.getMessage());
            dialogFont = new BitmapFont();
            dialogFont.setColor(PROMPT_COL_TEXT);
        }

        Pixmap upPix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        upPix.setColor(new Color(0.25f, 0.25f, 0.25f, 0.9f));
        upPix.fill();
        dialogBtnUpTexture = new Texture(upPix);
        upPix.dispose();

        Pixmap downPix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        downPix.setColor(new Color(0.4f, 0.4f, 0.4f, 0.95f));
        downPix.fill();
        dialogBtnDownTexture = new Texture(downPix);
        downPix.dispose();

        TextButtonStyle btnStyle = new TextButtonStyle();
        btnStyle.font = dialogFont;
        btnStyle.up   = new TextureRegionDrawable(new TextureRegion(dialogBtnUpTexture));
        btnStyle.down = new TextureRegionDrawable(new TextureRegion(dialogBtnDownTexture));
        btnStyle.over = btnStyle.down;

        LabelStyle labelStyle = new LabelStyle(dialogFont, PROMPT_COL_TEXT);

        Label message = new Label(
                "Questa è la tua prova finale, vuoi affrontare il boss?",
                labelStyle);
        message.setWrap(true);
        message.setAlignment(Align.center);

        TextButton yesBtn = new TextButton("SI", btnStyle);
        TextButton noBtn  = new TextButton("NO", btnStyle);

        yesBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor)
            {
                closeBossDialog();
                if (bossTeleportPoint != null)
                {
                    pablo.setPosition(bossTeleportPoint.x, bossTeleportPoint.y);
                    pablo.velocityVec.set(0, 0);
                }
                spawnFalseKnightIfPresent();
            }
        });

        noBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor)
            {
                closeBossDialog();
            }
        });

        Table box = new Table();
        box.setBackground(new TextureRegionDrawable(new TextureRegion(promptBgTexture)));
        box.pad(24f);
        box.add(message).width(520).center().row();

        Table buttons = new Table();
        buttons.add(yesBtn).width(120).height(36).padRight(20);
        buttons.add(noBtn).width(120).height(36);
        box.add(buttons).padTop(16).center();

        bossDialog = new Table();
        bossDialog.setFillParent(true);
        bossDialog.center();
        bossDialog.add(box).center();
        bossDialog.setVisible(false);
        uiStage.addActor(bossDialog);
    }

    private void buildBossPrompt()
    {
        buildPromptAssets();

        LabelStyle style = new LabelStyle(promptFont, PROMPT_COL_TEXT);
        bossPromptLabel = new Label("Premi E", style);
        bossPromptLabel.setVisible(false);

        Table panel = new Table();
        panel.setBackground(new TextureRegionDrawable(new TextureRegion(promptBgTexture)));
        panel.setSize(PROMPT_PANEL_W, PROMPT_PANEL_H);
        panel.pad(8f);

        Table topOrn = new Table();
        Image lineLeft = new Image(promptLineTexture);
        Image lineRight = new Image(promptLineTexture);
        Image deco = new Image(promptDecoTexture);
        Image dot = new Image(promptDotTexture);
        topOrn.add(lineLeft).size(PROMPT_LINE_W, PROMPT_LINE_H).padRight(6f);
        topOrn.add(deco).size(10f, 10f).padRight(2f);
        topOrn.add(dot).size(6f, 6f).padRight(2f);
        topOrn.add(deco).size(10f, 10f).padLeft(2f).padRight(6f);
        topOrn.add(lineRight).size(PROMPT_LINE_W, PROMPT_LINE_H);

        Table bottomOrn = new Table();
        Image lineLeftB = new Image(promptLineTexture);
        Image lineRightB = new Image(promptLineTexture);
        Image decoB = new Image(promptDecoTexture);
        Image dotB = new Image(promptDotTexture);
        bottomOrn.add(lineLeftB).size(PROMPT_LINE_W, PROMPT_LINE_H).padRight(6f);
        bottomOrn.add(decoB).size(10f, 10f).padRight(2f);
        bottomOrn.add(dotB).size(6f, 6f).padRight(2f);
        bottomOrn.add(decoB).size(10f, 10f).padLeft(2f).padRight(6f);
        bottomOrn.add(lineRightB).size(PROMPT_LINE_W, PROMPT_LINE_H);

        panel.add(topOrn).expandX().center().row();
        panel.add(bossPromptLabel).padTop(4f).padBottom(4f).center().row();
        panel.add(bottomOrn).expandX().center();

        bossPromptTable = new Table();
        bossPromptTable.setFillParent(true);
        bossPromptTable.bottom();
        bossPromptTable.add(panel).padBottom(24f);
        bossPromptTable.setVisible(false);
        uiStage.addActor(bossPromptTable);
    }

    private void buildPromptAssets()
    {
        if (promptFont != null && promptBgTexture != null)
            return;

        promptFontOwned = false;
        try
        {
            FreeTypeFontGenerator gen = new FreeTypeFontGenerator(
                    Gdx.files.internal(PROMPT_FONT_PATH));
            FreeTypeFontParameter param = new FreeTypeFontParameter();
            param.size = 18;
            param.color = PROMPT_COL_TEXT;
            param.hinting = FreeTypeFontGenerator.Hinting.Full;
            promptFont = gen.generateFont(param);
            gen.dispose();
            promptFontOwned = true;
        }
        catch (Exception e)
        {
            Gdx.app.log("LevelScreen", "Font prompt non trovato, uso default: " + e.getMessage());
            promptFont = new BitmapFont();
        }

        promptBgTexture   = makeColorTex(PROMPT_BG_COL);
        promptLineTexture = makeHLineTex(256, 2, PROMPT_COL_ORN);
        promptDotTexture  = makeDotTex(6, PROMPT_COL_ORN);
        promptDecoTexture = makeDiamondTex(10, PROMPT_COL_ORN);
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
        if (dialogTexture        != null) dialogTexture.dispose();
        if (dialogFont           != null) dialogFont.dispose();
        if (dialogBtnUpTexture   != null) dialogBtnUpTexture.dispose();
        if (dialogBtnDownTexture != null) dialogBtnDownTexture.dispose();
        if (promptFontOwned && promptFont != null) promptFont.dispose();
        if (promptBgTexture   != null) promptBgTexture.dispose();
        if (promptLineTexture != null) promptLineTexture.dispose();
        if (promptDotTexture  != null) promptDotTexture.dispose();
        if (promptDecoTexture != null) promptDecoTexture.dispose();
        if (bossDeathBgTexture != null) bossDeathBgTexture.dispose();
        if (bossDeathTexture != null) bossDeathTexture.dispose();
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

    private Texture makeColorTex(Color c)
    {
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(c);
        p.fill();
        Texture t = new Texture(p);
        p.dispose();
        return t;
    }

    private Texture makeHLineTex(int w, int h, Color c)
    {
        Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        p.setColor(0, 0, 0, 0);
        p.fill();
        for (int x = 0; x < w; x++)
        {
            float alpha = 0.8f * (float) Math.sin(Math.PI * x / w);
            p.setColor(c.r, c.g, c.b, alpha);
            for (int y = 0; y < h; y++)
                p.drawPixel(x, y);
        }
        Texture t = new Texture(p);
        p.dispose();
        return t;
    }

    private Texture makeDotTex(int size, Color c)
    {
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setColor(0, 0, 0, 0);
        p.fill();
        p.setColor(c);
        p.fillCircle(size / 2, size / 2, size / 2 - 1);
        Texture t = new Texture(p);
        p.dispose();
        return t;
    }

    private Texture makeDiamondTex(int size, Color c)
    {
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setColor(0, 0, 0, 0);
        p.fill();
        p.setColor(c);
        int half = size / 2;
        for (int y = 0; y < size; y++)
            for (int x = 0; x < size; x++)
                if (Math.abs(x - half) + Math.abs(y - half) <= half)
                    p.drawPixel(x, y);
        Texture t = new Texture(p);
        p.dispose();
        return t;
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

    private void addSolidTriangles(Polygon polygon)
    {
        float[] worldVertices = polygon.getTransformedVertices();
        if (worldVertices == null || worldVertices.length < 6) return;

        float[] localVertices = new float[worldVertices.length];
        for (int i = 0; i < worldVertices.length; i += 2)
        {
            localVertices[i]     = worldVertices[i]     - polygon.getX();
            localVertices[i + 1] = worldVertices[i + 1] - polygon.getY();
        }

        EarClippingTriangulator triangulator = new EarClippingTriangulator();
        ShortArray indices = triangulator.computeTriangles(localVertices);
        Rectangle bounds = polygon.getBoundingRectangle();

        for (int i = 0; i < indices.size; i += 3)
        {
            int i1 = indices.get(i) * 2;
            int i2 = indices.get(i + 1) * 2;
            int i3 = indices.get(i + 2) * 2;

            float[] tri = new float[] {
                    localVertices[i1], localVertices[i1 + 1],
                    localVertices[i2], localVertices[i2 + 1],
                    localVertices[i3], localVertices[i3 + 1]
            };

            Object solid = new Object(polygon.getX(), polygon.getY(), bounds.width, bounds.height, mainStage);
            solid.setBoundaryPolygon(tri);
        }
    }

    private void startBossDeathSequence()
    {
        bossDeathSequenceActive = true;

        if (bossDeathTable == null)
            buildBossDeathOverlay();

        bossDeathTable.setVisible(true);
        bossDeathImage.setVisible(false);
        bossDeathLine1.setVisible(false);
        bossDeathLine2.setVisible(false);

        bossDeathTable.clearActions();
        bossDeathTable.addAction(Actions.sequence(
                Actions.delay(3f),
                Actions.run(() -> {
                    bossDeathLine1.setVisible(true);
                    bossDeathLine2.setVisible(true);
                }),
                Actions.delay(4f),
                Actions.run(() -> bossDeathImage.setVisible(true)),
                Actions.delay(0.6f),
                Actions.run(() -> Gdx.app.exit())
        ));
    }

    private void buildBossDeathOverlay()
    {
        buildPromptAssets();

        bossDeathBgTexture = makeColorTex(new Color(0f, 0f, 0f, 1f));
        bossDeathTexture = loadTextureSafe("assets/se.jpg", new Color(0f, 0f, 0f, 1f));
        bossDeathImage = new Image(bossDeathTexture);
        bossDeathImage.setVisible(false);
        LabelStyle style = new LabelStyle(promptFont, PROMPT_COL_TEXT);

        bossDeathLine1 = new Label("Beh, sei morto... puoi sempre riprovarci...", style);
        bossDeathLine2 = new Label("Oppure no", style);
        bossDeathLine1.setFontScale(4f);
        bossDeathLine2.setFontScale(4f);

        Table textBox = new Table();
        textBox.add(bossDeathImage).padBottom(18f).row();
        textBox.add(bossDeathLine1).padBottom(10f).row();
        textBox.add(bossDeathLine2);

        bossDeathTable = new Table();
        bossDeathTable.setFillParent(true);
        bossDeathTable.setBackground(new TextureRegionDrawable(new TextureRegion(bossDeathBgTexture)));
        bossDeathTable.add(textBox).center();
        bossDeathTable.setVisible(false);
        uiStage.addActor(bossDeathTable);
    }
}
