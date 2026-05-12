// MenuScreen.java — modificato per integrare SoundManager
// DIFF:
//   initialize()  → avvia la musica del menu
//   keyDown()     → SFX selezione e conferma
//   updateSelection() → SFX select ad ogni cambio voce

package pablo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import pablo.entities.player.Pablo;
import pablo.framework.BaseGame;
import pablo.framework.BaseScreen;

public class MenuScreen extends BaseScreen
{
    private static final String BG_PATH   = "assets/MenùBG.png";
    private static final String FONT_PATH = "assets/TrajanPro-Regular.ttf";

    private static final Color COL_START_NORMAL = Color.WHITE;
    private static final Color COL_START_SEL    = new Color(0.75f, 1f, 0.75f, 1f);
    private static final Color COL_START_DOWN   = new Color(0.4f, 0.9f, 0.4f, 1f);
    private static final Color COL_OTHER_NORMAL = new Color(0.78f, 0.78f, 0.78f, 1f);
    private static final Color COL_OTHER_SEL    = Color.WHITE;
    private static final Color COL_SEP          = new Color(1f, 1f, 1f, 0.55f);
    private static final Color COL_ICON_NORMAL  = Color.WHITE;
    private static final Color COL_ICON_SEL     = new Color(0.4f, 1f, 0.4f, 1f);
    private static final String ICON_TEXT       = " \u2767 ";

    private static final int MENU_COUNT = 3;
    private int selectedIndex = 0;
    private TextButton[] buttons;
    private Label[] iconLeft;
    private Label[] iconRight;

    private static final int TITLE_FRAMES = 240;
    private static final float TITLE_FPS = 30f;
    private static final float TITLE_FRAME_DURATION = 1f / TITLE_FPS;

    private float titleElapsedTime = 0f;
    private int titleFrameIndex = -1;
    private Texture titleFrame;

    private SpriteBatch   batch;
    private ShapeRenderer shapeRenderer;
    private Texture       background;
    private BitmapFont    fontStart;
    private BitmapFont    fontOther;

    private TextButtonStyle styleStart;
    private TextButtonStyle styleOther;
    private TextButtonStyle styleStartSel;
    private TextButtonStyle styleOtherSel;

    private float separatorY  = -1;
    private float separatorX1, separatorX2;

    public void initialize()
    {
        batch         = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        background    = new Texture(Gdx.files.internal(BG_PATH));

        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(
                Gdx.files.internal(FONT_PATH));

        FreeTypeFontParameter pStart = new FreeTypeFontParameter();
        pStart.size = 46; pStart.color = COL_START_NORMAL;
        pStart.borderWidth = 1.5f; pStart.borderColor = new Color(0,0,0,0.7f);
        fontStart = gen.generateFont(pStart);

        FreeTypeFontParameter pOther = new FreeTypeFontParameter();
        pOther.size = 28; pOther.color = COL_OTHER_NORMAL;
        pOther.borderWidth = 1.2f; pOther.borderColor = new Color(0,0,0,0.6f);
        fontOther = gen.generateFont(pOther);

        gen.dispose();

        styleStart    = makeStyle(fontStart, COL_START_NORMAL, COL_START_SEL,  COL_START_DOWN);
        styleOther    = makeStyle(fontOther, COL_OTHER_NORMAL, COL_OTHER_SEL,  COL_OTHER_SEL);
        styleStartSel = makeStyle(fontStart, COL_START_SEL,   COL_START_SEL,  COL_START_DOWN);
        styleOtherSel = makeStyle(fontOther, COL_OTHER_SEL,   COL_OTHER_SEL,  COL_OTHER_SEL);

        LabelStyle styleDecorStart = new LabelStyle(); styleDecorStart.font = fontStart;
        LabelStyle styleDecorOther = new LabelStyle(); styleDecorOther.font = fontOther;

        TextButton startBtn = new TextButton("INIZIA", styleStart);
        startBtn.addListener(new ChangeListener() {
            public void changed(ChangeEvent e, Actor a) {
                SoundManager.get().playSfx(SoundManager.Sfx.UI_CONFIRM);
                SoundManager.get().playMusic(SoundManager.Track.LEVEL);
                BaseGame.setActiveScreen(new LevelScreen());
            }
        });

        TextButton optionsBtn = new TextButton("OPZIONI", styleOther);
        optionsBtn.addListener(new ChangeListener() {
            public void changed(ChangeEvent e, Actor a) {
                SoundManager.get().playSfx(SoundManager.Sfx.UI_CONFIRM);
                BaseGame.setActiveScreen(new OptionsScreen(MenuScreen.this, MenuScreen.this));
            }
        });

        TextButton exitBtn = new TextButton("ESCI", styleOther);
        exitBtn.addListener(new ChangeListener() {
            public void changed(ChangeEvent e, Actor a) {
                SoundManager.get().playSfx(SoundManager.Sfx.UI_BACK);
                Gdx.app.exit();
            }
        });

        buttons = new TextButton[]{ startBtn, optionsBtn, exitBtn };
        iconLeft  = new Label[MENU_COUNT];
        iconRight = new Label[MENU_COUNT];

        iconLeft[0]  = new Label(ICON_TEXT, styleDecorStart);
        iconRight[0] = new Label(ICON_TEXT, styleDecorStart);
        iconLeft[1]  = new Label(ICON_TEXT, styleDecorOther);
        iconRight[1] = new Label(ICON_TEXT, styleDecorOther);
        iconLeft[2]  = new Label(ICON_TEXT, styleDecorOther);
        iconRight[2] = new Label(ICON_TEXT, styleDecorOther);

        uiTable.bottom().right();
        uiTable.padBottom(80).padRight(90);

        Table startRow = new Table();
        startRow.add(iconLeft[0]);
        startRow.add(startBtn).padLeft(6).padRight(6);
        startRow.add(iconRight[0]);
        uiTable.add(startRow).right().padBottom(10).row();

        uiTable.add(new Label("", styleDecorOther)).height(2).padBottom(18).row();

        Table optionsRow = new Table();
        optionsRow.add(iconLeft[1]);
        optionsRow.add(optionsBtn).padLeft(6).padRight(6);
        optionsRow.add(iconRight[1]);
        uiTable.add(optionsRow).right().padBottom(12).row();

        Table exitRow = new Table();
        exitRow.add(iconLeft[2]);
        exitRow.add(exitBtn).padLeft(6).padRight(6);
        exitRow.add(iconRight[2]);
        uiTable.add(exitRow).right().row();

        updateSelection();

        // ── OST: avvia la musica del menu ──────────────────────────────────
        SoundManager.get().playMusic(SoundManager.Track.MENU);
    }

    @Override
    public void render(float dt)
    {
        dt = Math.min(dt, 1 / 30f);
        titleElapsedTime += dt;

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        drawBackground();

        uiStage.act(dt);
        uiStage.draw();

        if (separatorY < 0) computeSeparator();
        if (separatorY >= 0)
        {
            uiStage.getViewport().apply();
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(COL_SEP);
            shapeRenderer.rectLine(separatorX1, separatorY, separatorX2, separatorY, 1.5f);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
    }

    @Override
    public void drawFrozen()
    {
        drawBackground();
        uiStage.getViewport().apply();
        uiStage.draw();
    }

    private void drawBackground()
    {
        mainStage.getViewport().apply();
        float viewportWidth  = mainStage.getViewport().getWorldWidth();
        float viewportHeight = mainStage.getViewport().getWorldHeight();

        int frameIndex = ((int)(titleElapsedTime / TITLE_FRAME_DURATION) % TITLE_FRAMES) + 1;
        if (frameIndex != titleFrameIndex)
        {
            String path = String.format("assets/Titolo/frame_%04d.png", frameIndex);
            if (Gdx.files.internal(path).exists())
            {
                Texture next = new Texture(Gdx.files.internal(path));
                if (titleFrame != null) titleFrame.dispose();
                titleFrame = next;
                titleFrameIndex = frameIndex;
            }
        }

        Texture toDraw = (titleFrame != null) ? titleFrame : background;

        batch.setProjectionMatrix(mainStage.getCamera().combined);
        batch.begin();
        batch.draw(toDraw, 0, 0, viewportWidth, viewportHeight);
        batch.end();
    }

    public void update(float dt)
    {
        SoundManager.get().update(dt);
    }

    @Override
    public boolean keyDown(int keycode)
    {
        if (keycode == Input.Keys.DOWN || keycode == Input.Keys.S)
        {
            selectedIndex = (selectedIndex + 1) % MENU_COUNT;
            updateSelection();
            return true;
        }
        if (keycode == Input.Keys.UP || keycode == Input.Keys.W)
        {
            selectedIndex = (selectedIndex - 1 + MENU_COUNT) % MENU_COUNT;
            updateSelection();
            return true;
        }
        if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE)
        {
            SoundManager.get().playSfx(SoundManager.Sfx.UI_CONFIRM);
            activateSelected();
            return true;
        }
        return false;
    }

    @Override
    public void resize(int width, int height)
    {
        super.resize(width, height);
        separatorY = -1;
    }

    private void updateSelection()
    {
        for (int i = 0; i < buttons.length; i++)
        {
            if (i == 0)
                buttons[i].setStyle(i == selectedIndex ? styleStartSel : styleStart);
            else
                buttons[i].setStyle(i == selectedIndex ? styleOtherSel : styleOther);

            Color iconColor = (i == selectedIndex) ? COL_ICON_SEL : COL_ICON_NORMAL;
            iconLeft[i].setColor(iconColor);
            iconRight[i].setColor(iconColor);
        }
        // SFX: cambio selezione
        SoundManager.get().playSfx(SoundManager.Sfx.UI_SELECT);
    }

    private void activateSelected()
    {
        switch (selectedIndex)
        {
            case 0:
                SoundManager.get().playMusic(SoundManager.Track.LEVEL);
                BaseGame.setActiveScreen(new LevelScreen());
                break;
            case 1: BaseGame.setActiveScreen(new OptionsScreen(this, this)); break;
            case 2: Gdx.app.exit();                                           break;
        }
    }

    private TextButtonStyle makeStyle(BitmapFont font, Color normal, Color over, Color down)
    {
        TextButtonStyle s = new TextButtonStyle();
        s.font = font; s.fontColor = normal; s.overFontColor = over; s.downFontColor = down;
        return s;
    }

    private void computeSeparator()
    {
        uiStage.getViewport().apply();
        float tableW = uiTable.getPrefWidth();
        float tableX = uiTable.getX();
        separatorX1  = tableX + 16f;
        separatorX2  = tableX + tableW - 16f;
        separatorY   = uiTable.getY() + uiTable.getPrefHeight() - 80f;
    }

    @Override
    public void dispose()
    {
        super.dispose();
        batch.dispose();
        shapeRenderer.dispose();
        background.dispose();
        if (titleFrame != null) titleFrame.dispose();
        fontStart.dispose();
        fontOther.dispose();
    }

    public static class PlayerState
    {
        private static PlayerState instance;

        private int     health = -1;  // -1 = non inizializzato → usa il default di Pablo
        private int     soul   = 0;

        // -----------------------------------------------------------------------
        // Singleton
        // -----------------------------------------------------------------------
        private PlayerState() {}

        public static PlayerState get()
        {
            if (instance == null)
                instance = new PlayerState();
            return instance;
        }

        // -----------------------------------------------------------------------
        // Salva / Ripristina
        // -----------------------------------------------------------------------

        /** Copia salute e anima da un Pablo esistente prima della transizione. */
        public void saveFrom(Pablo pablo)
        {
            health = pablo.getHealth();
            soul   = pablo.getSoul();
        }

        /**
         * Applica salute e anima al nuovo Pablo spawanto nella mappa successiva.
         * Se non è mai stato chiamato saveFrom(), non fa nulla (Pablo usa i suoi default).
         */
        public void restoreTo(Pablo pablo)
        {
            if (health >= 0)
                // pablo.setHealth(health);
            pablo.setSoul(soul);
        }

        /** True se contiene uno stato salvato da una transizione precedente. */
        public boolean hasSavedState() { return health >= 0; }

        /** Azzera lo stato (usato per "nuova partita" o game-over). */
        public void reset()
        {
            health = -1;
            soul   = 0;
        }
    }
}

