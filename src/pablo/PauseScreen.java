// PauseScreen.java
package pablo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import pablo.framework.BaseGame;
import pablo.framework.BaseScreen;

public class PauseScreen extends BaseScreen
{
    // -----------------------------------------------------------------------
    // Costanti di stile
    // -----------------------------------------------------------------------
    private static final String FONT_PATH      = "assets/TrajanPro-Regular.ttf";

    private static final Color  COL_ORN        = new Color(0.40f, 0.90f, 0.45f, 1.0f);  // verde
    private static final Color  COL_ITEM       = new Color(0.60f, 0.80f, 0.60f, 1.0f);  // verde tenue
    private static final Color  COL_SELECTED   = new Color(0.75f, 1.00f, 0.75f, 1.0f);  // verde chiaro selezionato

    private static final float  PANEL_W        = 360f;
    private static final float  PANEL_H        = 280f;
    private static final float  ITEM_PAD_TOP   = 14f;

    private static final int    FONT_SIZE_ITEM = 18;

    // -----------------------------------------------------------------------
    // Voci del menu
    // -----------------------------------------------------------------------
    private static final String[] LABELS = { "Continue", "Options", "Quit To Menu" };
    private int selectedIndex = 0;

    // -----------------------------------------------------------------------
    // Risorse grafiche
    // -----------------------------------------------------------------------
    private BaseScreen         gameplayScreen;

    private BitmapFont         fontItem;
    private boolean            fontOwned;

    private Texture            bgTexture;
    private Texture            lineTexture;
    private Texture            dotTexture;
    private Texture            decoTexture;

    private SpriteBatch        batch;
    private ShapeRenderer      shape;

    private GlyphLayout[]      layouts;

    // -----------------------------------------------------------------------
    // Costruttore
    // -----------------------------------------------------------------------
    public PauseScreen(BaseScreen gameplayScreen)
    {
        super();
        this.gameplayScreen = gameplayScreen;
        buildAssets();
        buildLayouts();
    }

    public void initialize() { }

    // -----------------------------------------------------------------------
    // Creazione risorse
    // -----------------------------------------------------------------------
    private void buildAssets()
    {
        fontOwned = false;
        try
        {
            FreeTypeFontGenerator gen = new FreeTypeFontGenerator(
                    Gdx.files.internal(FONT_PATH));
            FreeTypeFontParameter param = new FreeTypeFontParameter();
            param.size    = FONT_SIZE_ITEM;
            param.color   = Color.WHITE;
            param.mono    = false;
            param.hinting = FreeTypeFontGenerator.Hinting.Full;
            fontItem  = gen.generateFont(param);
            gen.dispose();
            fontOwned = true;
        }
        catch (Exception e)
        {
            Gdx.app.log("PauseScreen", "Font non trovato, uso default: " + e.getMessage());
            fontItem = new BitmapFont();
        }

        bgTexture   = makeColorTex(new Color(0.05f, 0.05f, 0.07f, 0.92f));
        lineTexture = makeHLineTex(256, 2, COL_ORN);
        dotTexture  = makeDotTex(6, COL_ORN);
        decoTexture = makeDiamondTex(10, COL_ORN);

        batch = new SpriteBatch();
        shape = new ShapeRenderer();
    }

    private void buildLayouts()
    {
        layouts = new GlyphLayout[LABELS.length];
        for (int i = 0; i < LABELS.length; i++)
            layouts[i] = new GlyphLayout(fontItem, LABELS[i].toUpperCase());
    }

    // -----------------------------------------------------------------------
    // Rendering
    // -----------------------------------------------------------------------
    @Override
    public void render(float dt)
    {
        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();

        // 1. Clear + gameplay congelato come sfondo
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        gameplayScreen.drawFrozen();

        float panelX = (sw - PANEL_W) / 2f;
        float panelY = (sh - PANEL_H) / 2f;

        batch.begin();

        // 2. Overlay scuro semitrasparente — nessun pannello colorato
        batch.setColor(0.05f, 0.05f, 0.07f, 0.88f);
        batch.draw(bgTexture, 0, 0, sw, sh);
        batch.setColor(Color.WHITE);

        // 3. Ornamento superiore
        float ornY = panelY + PANEL_H - 48f;
        drawOrnament(panelX, ornY, PANEL_W);

        // 4. Ornamento inferiore
        float ornBotY = panelY + 28f;
        batch.draw(lineTexture, panelX + 10,  ornBotY + 8, PANEL_W * 0.35f, 1);
        batch.draw(lineTexture, panelX + PANEL_W * 0.65f - 10, ornBotY + 8, PANEL_W * 0.35f, 1);
        drawCenterEmblem(panelX + PANEL_W / 2f, ornBotY + 8);

        // 5. Voci del menu
        float totalH = LABELS.length * (FONT_SIZE_ITEM + ITEM_PAD_TOP * 2);
        float startY = panelY + (PANEL_H / 2f) + (totalH / 2f) - 10;

        for (int i = 0; i < LABELS.length; i++)
        {
            float itemY = startY - i * (FONT_SIZE_ITEM + ITEM_PAD_TOP * 2);
            drawMenuItem(panelX, itemY, PANEL_W, i);
        }

        batch.end();
    }

    // -----------------------------------------------------------------------
    // Ornamenti
    // -----------------------------------------------------------------------
    private void drawOrnament(float x, float y, float w)
    {
        float cx = x + w / 2f;
        float lineW = w * 0.32f;

        batch.setColor(COL_ORN.r, COL_ORN.g, COL_ORN.b, 0.6f);
        batch.draw(lineTexture, x + 10, y, lineW, 1);
        batch.draw(lineTexture, cx + 18, y, lineW, 1);
        batch.setColor(Color.WHITE);

        drawCenterEmblem(cx, y);
    }

    private void drawCenterEmblem(float cx, float y)
    {
        float d = 8f;
        batch.setColor(COL_ORN);
        batch.draw(decoTexture, cx - d * 3 - 5, y - 5, 10, 10);
        batch.draw(dotTexture,  cx - 3, y - 3, 6, 6);
        batch.draw(decoTexture, cx + d * 2 - 5, y - 5, 10, 10);
        batch.setColor(Color.WHITE);
    }

    // -----------------------------------------------------------------------
    // Voce del menu
    // -----------------------------------------------------------------------
    private void drawMenuItem(float panelX, float itemY, float panelW, int index)
    {
        boolean selected = (index == selectedIndex);
        Color col = selected ? COL_SELECTED : COL_ITEM;

        fontItem.setColor(col);

        GlyphLayout gl = layouts[index];
        float textX = panelX + (panelW - gl.width) / 2f;

        fontItem.draw(batch, gl, textX, itemY);

        if (selected)
        {
            float decoY = itemY - FONT_SIZE_ITEM / 2f - 5;
            float gap = 18f;
            batch.setColor(COL_ORN);
            batch.draw(decoTexture, textX - gap - 10, decoY, 10, 10);
            batch.draw(decoTexture, textX + gl.width + gap, decoY, 10, 10);
            batch.setColor(Color.WHITE);
        }
    }

    // -----------------------------------------------------------------------
    // Input
    // -----------------------------------------------------------------------
    @Override
    public boolean keyDown(int keyCode)
    {
        switch (keyCode)
        {
            case Input.Keys.UP:
            case Input.Keys.W:
                selectedIndex = (selectedIndex - 1 + LABELS.length) % LABELS.length;
                return true;

            case Input.Keys.DOWN:
            case Input.Keys.S:
                selectedIndex = (selectedIndex + 1) % LABELS.length;
                return true;

            case Input.Keys.ENTER:
            case Input.Keys.SPACE:
                activateSelected();
                return true;

            case Input.Keys.ESCAPE:
            case Input.Keys.P:
                BaseGame.setActiveScreen(gameplayScreen);
                return true;
        }
        return false;
    }

    private void activateSelected()
    {
        switch (selectedIndex)
        {
            case 0: // Continue
                BaseGame.setActiveScreen(gameplayScreen);
                break;
            case 1: // Options — passa il gameplay come sfondo, non PauseScreen
                BaseGame.setActiveScreen(new OptionsScreen(this, gameplayScreen));
                break;
            case 2: // Quit To Menu
                BaseGame.setActiveScreen(new MenuScreen());
                break;
        }
    }

    public void update(float dt)
    {
        SoundManager.get().update(dt);
    }

    // -----------------------------------------------------------------------
    // Factory texture
    // -----------------------------------------------------------------------
    private Texture makeColorTex(Color c)
    {
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(c); p.fill();
        Texture t = new Texture(p); p.dispose();
        return t;
    }

    private Texture makeHLineTex(int w, int h, Color c)
    {
        Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        p.setColor(0, 0, 0, 0); p.fill();
        for (int x = 0; x < w; x++)
        {
            float alpha = 0.8f * (float) Math.sin(Math.PI * x / w);
            p.setColor(c.r, c.g, c.b, alpha);
            for (int y = 0; y < h; y++) p.drawPixel(x, y);
        }
        Texture t = new Texture(p); p.dispose();
        return t;
    }

    private Texture makeDotTex(int size, Color c)
    {
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setColor(0, 0, 0, 0); p.fill();
        p.setColor(c); p.fillCircle(size / 2, size / 2, size / 2 - 1);
        Texture t = new Texture(p); p.dispose();
        return t;
    }

    private Texture makeDiamondTex(int size, Color c)
    {
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setColor(0, 0, 0, 0); p.fill();
        p.setColor(c);
        int half = size / 2;
        for (int y = 0; y < size; y++)
            for (int x = 0; x < size; x++)
                if (Math.abs(x - half) + Math.abs(y - half) <= half)
                    p.drawPixel(x, y);
        Texture t = new Texture(p); p.dispose();
        return t;
    }

    // -----------------------------------------------------------------------
    // dispose()
    // -----------------------------------------------------------------------
    @Override
    public void dispose()
    {
        super.dispose();
        if (fontOwned && fontItem != null) fontItem.dispose();
        if (bgTexture   != null) bgTexture.dispose();
        if (lineTexture != null) lineTexture.dispose();
        if (dotTexture  != null) dotTexture.dispose();
        if (decoTexture != null) decoTexture.dispose();
        if (batch       != null) batch.dispose();
        if (shape       != null) shape.dispose();
    }
}