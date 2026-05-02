// MenuScreen.java

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
import pablo.framework.BaseGame;
import pablo.framework.BaseScreen;

public class MenuScreen extends BaseScreen
{
    // -----------------------------------------------------------------------
    // TODO: sostituisci con i path reali
    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------
    private static final String BG_PATH   = "assets/MenùBG.png";
    private static final String FONT_PATH = "assets/TrajanPro-Regular.ttf";
    // Colori
    private static final Color COL_START_NORMAL = Color.WHITE;
    private static final Color COL_START_SEL    = new Color(0.75f, 1f, 0.75f, 1f);
    private static final Color COL_START_DOWN   = new Color(0.4f, 0.9f, 0.4f, 1f);
    private static final Color COL_OTHER_NORMAL = new Color(0.78f, 0.78f, 0.78f, 1f);
    private static final Color COL_OTHER_SEL    = Color.WHITE;
    private static final Color COL_SEP          = new Color(1f, 1f, 1f, 0.55f);

    private static final int MENU_COUNT = 3;
    private int selectedIndex = 0;
    private TextButton[] buttons;

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

    // -----------------------------------------------------------------------
    // initialize()
    // -----------------------------------------------------------------------
    public void initialize()
    {
        batch         = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        background    = new Texture(Gdx.files.internal(BG_PATH));

        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(
                Gdx.files.internal(FONT_PATH));

        FreeTypeFontParameter pStart = new FreeTypeFontParameter();
        pStart.size        = 42;
        pStart.color       = COL_START_NORMAL;
        pStart.borderWidth = 1.5f;
        pStart.borderColor = new Color(0f, 0f, 0f, 0.7f);
        fontStart = gen.generateFont(pStart);

        FreeTypeFontParameter pOther = new FreeTypeFontParameter();
        pOther.size        = 28;
        pOther.color       = COL_OTHER_NORMAL;
        pOther.borderWidth = 1.2f;
        pOther.borderColor = new Color(0f, 0f, 0f, 0.6f);
        fontOther = gen.generateFont(pOther);

        gen.dispose();

        styleStart    = makeStyle(fontStart, COL_START_NORMAL, COL_START_SEL,  COL_START_DOWN);
        styleOther    = makeStyle(fontOther, COL_OTHER_NORMAL, COL_OTHER_SEL,  COL_OTHER_SEL);
        styleStartSel = makeStyle(fontStart, COL_START_SEL,   COL_START_SEL,  COL_START_DOWN);
        styleOtherSel = makeStyle(fontOther, COL_OTHER_SEL,   COL_OTHER_SEL,  COL_OTHER_SEL);

        LabelStyle styleDecor = new LabelStyle();
        styleDecor.font = fontStart;

        TextButton startBtn = new TextButton("INIZIA", styleStart);
        startBtn.addListener(new ChangeListener() {
            public void changed(ChangeEvent e, Actor a) {
                BaseGame.setActiveScreen(new LevelScreen());
            }
        });

        TextButton optionsBtn = new TextButton("OPZIONI", styleOther);
        optionsBtn.addListener(new ChangeListener() {
            public void changed(ChangeEvent e, Actor a) {
                Gdx.app.log("MenuScreen", "Opzioni — non ancora implementato");
            }
        });

        TextButton exitBtn = new TextButton("ESCI", styleOther);
        exitBtn.addListener(new ChangeListener() {
            public void changed(ChangeEvent e, Actor a) { Gdx.app.exit(); }
        });

        buttons = new TextButton[]{ startBtn, optionsBtn, exitBtn };

        Label decorLeft  = new Label(" \u2767 ", styleDecor);
        Label decorRight = new Label(" \u2767 ", styleDecor);

        uiTable.bottom().right();
        uiTable.padBottom(80).padRight(90);

        Table startRow = new Table();
        startRow.add(decorLeft);
        startRow.add(startBtn).padLeft(6).padRight(6);
        startRow.add(decorRight);
        uiTable.add(startRow).right().padBottom(10).row();

        uiTable.add(new Label("", styleDecor)).height(2).padBottom(18).row();
        uiTable.add(optionsBtn).right().padBottom(12).row();
        uiTable.add(exitBtn).right().row();

        updateSelection();
    }

    // -----------------------------------------------------------------------
    // render()
    // -----------------------------------------------------------------------
    @Override
    public void render(float dt)
    {
        dt = Math.min(dt, 1 / 30f);

        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        batch.draw(background, 0, 0, w, h);
        batch.end();

        uiStage.act(dt);
        uiStage.draw();

        if (separatorY < 0) computeSeparator();
        if (separatorY >= 0)
        {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(COL_SEP);
            shapeRenderer.rectLine(separatorX1, separatorY,
                    separatorX2, separatorY, 1.5f);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
    }

    // -----------------------------------------------------------------------
    // update() — vuoto, la tastiera è gestita in keyDown()
    // -----------------------------------------------------------------------
    public void update(float dt) { }

    // -----------------------------------------------------------------------
    // keyDown() — navigazione tastiera (chiamato da BaseScreen via InputMultiplexer)
    // -----------------------------------------------------------------------
    @Override
    public boolean keyDown(int keycode)
    {
        if (keycode == Input.Keys.DOWN || keycode == Input.Keys.S)
        {
            selectedIndex = (selectedIndex + 1) % MENU_COUNT;
            updateSelection();
            return true; // evento consumato
        }
        if (keycode == Input.Keys.UP || keycode == Input.Keys.W)
        {
            selectedIndex = (selectedIndex - 1 + MENU_COUNT) % MENU_COUNT;
            updateSelection();
            return true;
        }
        if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE)
        {
            activateSelected();
            return true;
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // resize() — ricalcola separatore e aggiorna SpriteBatch projection
    // -----------------------------------------------------------------------
    @Override
    public void resize(int width, int height)
    {
        super.resize(width, height); // aggiorna i viewport di BaseScreen
        separatorY = -1;             // forza ricalcolo al prossimo frame
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------
    private void updateSelection()
    {
        for (int i = 0; i < buttons.length; i++)
        {
            if (i == 0)
                buttons[i].setStyle(i == selectedIndex ? styleStartSel : styleStart);
            else
                buttons[i].setStyle(i == selectedIndex ? styleOtherSel : styleOther);
        }
    }

    private void activateSelected()
    {
        switch (selectedIndex)
        {
            case 0: BaseGame.setActiveScreen(new LevelScreen()); break;
            case 1: Gdx.app.log("MenuScreen", "Opzioni — non ancora implementato"); break;
            case 2: Gdx.app.exit(); break;
        }
    }

    private TextButtonStyle makeStyle(BitmapFont font, Color normal, Color over, Color down)
    {
        TextButtonStyle s = new TextButtonStyle();
        s.font          = font;
        s.fontColor     = normal;
        s.overFontColor = over;
        s.downFontColor = down;
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
        fontStart.dispose();
        fontOther.dispose();
    }
}