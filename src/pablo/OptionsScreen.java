// OptionsScreen.java
package pablo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Slider.SliderStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import pablo.framework.BaseGame;
import pablo.framework.BaseScreen;

/**
 * Schermata opzioni — overlay semitrasparente SUL GAMEPLAY/HOME congelato.
 *
 * Costruttori:
 *   - OptionsScreen(BaseScreen previousScreen) : dalla home, mostra previousScreen come sfondo
 *   - OptionsScreen(BaseScreen previousScreen, BaseScreen backgroundScreen) : dal menu pausa, mostra gameplay frozen
 *
 * Ordine di render:
 *   1. backgroundScreen.drawFrozen() (frozen)
 *   2. overlay scuro semitrasparente
 *   3. uiStage (slider, label, pulsanti)
 */
public class OptionsScreen extends BaseScreen
{
    private static final String FONT_PATH = "assets/TrajanPro-Regular.ttf";

    // Colori HK
    private static final Color COL_TITLE   = Color.WHITE;
    private static final Color COL_LABEL   = new Color(0.80f, 0.80f, 0.80f, 1f);
    private static final Color COL_BTN     = new Color(0.78f, 0.78f, 0.78f, 1f);
    private static final Color COL_BTN_HOV = Color.WHITE;
    private static final Color COL_BTN_SEL = new Color(0.75f, 1f, 0.75f, 1f);
    private static final Color COL_OVERLAY = new Color(0f, 0f, 0f, 0.72f);

    private SpriteBatch   batch;
    private ShapeRenderer shapes;

    private BitmapFont fontTitle;
    private BitmapFont fontLabel;
    private BitmapFont fontBtn;

    private Slider    sfxSlider;
    private Slider    musicSlider;
    private TextButton backBtn;

    private static final int SECTION_COUNT = 3;
    private int selectedSection = 0;

    private final BaseScreen previousScreen;     // a cui tornare (PauseScreen o MenuScreen)
    private final BaseScreen backgroundScreen;    // sfondo da renderizzare

    // -----------------------------------------------------------------------
    // Costruttore 1: da home
    // -----------------------------------------------------------------------
    public OptionsScreen(BaseScreen previousScreen)
    {
        this.previousScreen = previousScreen;
        this.backgroundScreen = previousScreen;
    }

    // -----------------------------------------------------------------------
    // Costruttore 2: dal menu pausa
    // -----------------------------------------------------------------------
    public OptionsScreen(BaseScreen previousScreen, BaseScreen backgroundScreen)
    {
        this.previousScreen = previousScreen;
        this.backgroundScreen = backgroundScreen;
    }

    // -----------------------------------------------------------------------
    // initialize()
    // -----------------------------------------------------------------------
    @Override
    public void initialize()
    {
        batch  = new SpriteBatch();
        shapes = new ShapeRenderer();

        // Font
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(
                Gdx.files.internal(FONT_PATH));

        FreeTypeFontParameter pTitle = new FreeTypeFontParameter();
        pTitle.size = 38; pTitle.color = COL_TITLE;
        pTitle.borderWidth = 1.5f; pTitle.borderColor = new Color(0,0,0,0.7f);
        fontTitle = gen.generateFont(pTitle);

        FreeTypeFontParameter pLabel = new FreeTypeFontParameter();
        pLabel.size = 24; pLabel.color = COL_LABEL;
        pLabel.borderWidth = 1f; pLabel.borderColor = new Color(0,0,0,0.6f);
        fontLabel = gen.generateFont(pLabel);

        FreeTypeFontParameter pBtn = new FreeTypeFontParameter();
        pBtn.size = 26; pBtn.color = COL_BTN;
        pBtn.borderWidth = 1.2f; pBtn.borderColor = new Color(0,0,0,0.6f);
        fontBtn = gen.generateFont(pBtn);

        gen.dispose();

        // Stili label / button
        LabelStyle styleTitle = new LabelStyle(); styleTitle.font = fontTitle;
        LabelStyle styleLabel = new LabelStyle(); styleLabel.font = fontLabel;

        TextButtonStyle styleBtn = new TextButtonStyle();
        styleBtn.font          = fontBtn;
        styleBtn.fontColor     = COL_BTN;
        styleBtn.overFontColor = COL_BTN_HOV;
        styleBtn.downFontColor = COL_BTN_SEL;

        SliderStyle sliderStyle = buildSliderStyle();

        // Widget
        GameSettings s = GameSettings.get();

        sfxSlider = new Slider(0f, 1f, 0.01f, false, sliderStyle);
        sfxSlider.setValue(s.getSfxVolume());
        sfxSlider.addListener(new ChangeListener() {
            public void changed(ChangeEvent e, Actor a) {
                GameSettings.get().setSfxVolume(sfxSlider.getValue());
            }
        });

        musicSlider = new Slider(0f, 1f, 0.01f, false, sliderStyle);
        musicSlider.setValue(s.getMusicVolume());
        musicSlider.addListener(new ChangeListener() {
            public void changed(ChangeEvent e, Actor a) {
                GameSettings.get().setMusicVolume(musicSlider.getValue());
            }
        });

        backBtn = new TextButton("← Indietro", styleBtn);
        backBtn.addListener(new ChangeListener() {
            public void changed(ChangeEvent e, Actor a) { goBack(); }
        });

        // Layout uiTable
        uiTable.center();
        uiTable.add(new Label("OPZIONI", styleTitle)).padBottom(30).row();
        addSeparator(styleLabel);

        uiTable.add(new Label("Volume SFX", styleLabel)).left().padBottom(6).row();
        uiTable.add(sfxSlider).width(340).padBottom(22).row();

        uiTable.add(new Label("Volume Musica", styleLabel)).left().padBottom(6).row();
        uiTable.add(musicSlider).width(340).padBottom(22).row();

        addSeparator(styleLabel);
        uiTable.add(new Label("COMANDI", styleTitle)).padTop(18).padBottom(14).row();
        addControlRow("Movimento",     "WASD / Frecce",       styleLabel);
        addControlRow("Salto",         "SPAZIO / W",          styleLabel);
        addControlRow("Doppio salto",  "SPAZIO / W (in aria)",styleLabel);
        addControlRow("Dash",          "SHIFT",               styleLabel);
        addControlRow("Attacco",       "J / Click sinistro",  styleLabel);
        addControlRow("Cura",          "F",                   styleLabel);

        addSeparator(styleLabel);
        uiTable.add(backBtn).padTop(20).row();
    }

    // -----------------------------------------------------------------------
    // render() — gameplay frozen → overlay → UI
    // -----------------------------------------------------------------------
    @Override
    public void render(float dt)
    {
        dt = Math.min(dt, 1 / 30f);

        // 1. Clear
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 2. Schermata di sfondo FROZEN (solo draw, niente act)
        backgroundScreen.drawFrozen();

        // 3. Overlay scuro semitrasparente
        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();
        Gdx.gl.glViewport(0, 0, sw, sh);
        shapes.getProjectionMatrix().setToOrtho2D(0, 0, sw, sh);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(COL_OVERLAY);
        shapes.rect(0, 0, sw, sh);
        shapes.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // 4. UI (slider, label, pulsanti)
        uiStage.getViewport().apply();
        uiStage.act(dt);
        uiStage.draw();
    }

    // -----------------------------------------------------------------------
    // update()
    // -----------------------------------------------------------------------
    @Override
    public void update(float dt)
    {
        SoundManager.get().update(dt);
    }

    // -----------------------------------------------------------------------
    // keyDown()
    // -----------------------------------------------------------------------
    @Override
    public boolean keyDown(int keycode)
    {
        if (keycode == Input.Keys.ESCAPE) { goBack(); return true; }

        if (keycode == Input.Keys.DOWN || keycode == Input.Keys.S)
        { selectedSection = (selectedSection + 1) % SECTION_COUNT; return true; }

        if (keycode == Input.Keys.UP || keycode == Input.Keys.W)
        { selectedSection = (selectedSection - 1 + SECTION_COUNT) % SECTION_COUNT; return true; }

        if (keycode == Input.Keys.LEFT || keycode == Input.Keys.A)
        {
            if      (selectedSection == 0) sfxSlider.setValue(sfxSlider.getValue() - 0.05f);
            else if (selectedSection == 1) musicSlider.setValue(musicSlider.getValue() - 0.05f);
            return true;
        }
        if (keycode == Input.Keys.RIGHT || keycode == Input.Keys.D)
        {
            if      (selectedSection == 0) sfxSlider.setValue(sfxSlider.getValue() + 0.05f);
            else if (selectedSection == 1) musicSlider.setValue(musicSlider.getValue() + 0.05f);
            return true;
        }

        if ((keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE)
                && selectedSection == 2)
        { goBack(); return true; }

        return false;
    }

    // -----------------------------------------------------------------------
    // resize()
    // -----------------------------------------------------------------------
    @Override
    public void resize(int width, int height)
    {
        super.resize(width, height);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------
    private void goBack()
    {
        GameSettings.get().save();
        BaseGame.setActiveScreen(previousScreen);
    }

    private void addSeparator(LabelStyle style)
    {
        uiTable.add(new Label("──────────────────────────────", style))
                .padBottom(10).padTop(4).row();
    }

    private void addControlRow(String action, String keys, LabelStyle style)
    {
        Table row = new Table();
        row.add(new Label(action, style)).width(200).left();
        row.add(new Label(keys,   style)).left().padLeft(20);
        uiTable.add(row).left().padBottom(8).row();
    }

    private SliderStyle buildSliderStyle()
    {
        Pixmap bgPix = new Pixmap(1, 4, Pixmap.Format.RGBA8888);
        bgPix.setColor(new Color(0.4f, 0.4f, 0.4f, 1f)); bgPix.fill();
        TextureRegionDrawable bgDrw = new TextureRegionDrawable(
                new TextureRegion(new Texture(bgPix)));
        bgPix.dispose();

        Pixmap fillPix = new Pixmap(1, 4, Pixmap.Format.RGBA8888);
        fillPix.setColor(Color.WHITE); fillPix.fill();
        TextureRegionDrawable fillDrw = new TextureRegionDrawable(
                new TextureRegion(new Texture(fillPix)));
        fillPix.dispose();

        Pixmap knobPix = new Pixmap(14, 14, Pixmap.Format.RGBA8888);
        knobPix.setColor(Color.WHITE); knobPix.fillCircle(7, 7, 6);
        TextureRegionDrawable knobDrw = new TextureRegionDrawable(
                new TextureRegion(new Texture(knobPix)));
        knobPix.dispose();

        SliderStyle style = new SliderStyle();
        style.background = bgDrw;
        style.knobBefore = fillDrw;
        style.knob       = knobDrw;
        return style;
    }

    // -----------------------------------------------------------------------
    // dispose()
    // -----------------------------------------------------------------------
    @Override
    public void dispose()
    {
        super.dispose();
        if (batch      != null) batch.dispose();
        if (shapes     != null) shapes.dispose();
        if (fontTitle  != null) fontTitle.dispose();
        if (fontLabel  != null) fontLabel.dispose();
        if (fontBtn    != null) fontBtn.dispose();
    }
}