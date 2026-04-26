// BaseGame.java

package pablo.framework;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar.ProgressBarStyle;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

/**
 *  Created when program is launched;
 *  manages the screens that appear during the game.
 */
public abstract class BaseGame extends Game
{
    /**
     *  Stores reference to game; used when calling setActiveScreen method.
     */
    private static BaseGame game;

    public static LabelStyle labelStyle;
    public static TextButtonStyle textButtonStyle;
    public static ProgressBarStyle progressBarStyle;

    /**
     *  Called when game is initialized; stores global reference to game object.
     */
    public BaseGame()
    {
        game = this;
    }

    /**
     *  Called when game is initialized,
     *  after Gdx.input and other objects have been initialized.
     */
    public void create()
    {
        // prepare for multiple classes/stages/actors to receive discrete input
        InputMultiplexer im = new InputMultiplexer();
        Gdx.input.setInputProcessor( im );

        // parameters for generating a custom bitmap font
        FreeTypeFontGenerator fontGenerator =
                new FreeTypeFontGenerator(Gdx.files.internal("assets/OpenSans.ttf"));
        FreeTypeFontParameter fontParameters = new FreeTypeFontParameter();
        fontParameters.size = 36;
        fontParameters.color = Color.WHITE;
        fontParameters.borderWidth = 2;
        fontParameters.borderColor = Color.BLACK;
        fontParameters.borderStraight = true;
        fontParameters.minFilter = TextureFilter.Linear;
        fontParameters.magFilter = TextureFilter.Linear;

        BitmapFont customFont = fontGenerator.generateFont(fontParameters);

        labelStyle = new LabelStyle();
        labelStyle.font = customFont;

        textButtonStyle = new TextButtonStyle();

        // dark gray background track
        Pixmap bgPixmap = new Pixmap(1, 1, Format.RGBA8888);
        bgPixmap.setColor(0x555555FF);
        bgPixmap.fill();
        TextureRegionDrawable bgDrawable = new TextureRegionDrawable(
                new TextureRegion(new Texture(bgPixmap)));
        bgPixmap.dispose();

        // white filled portion
        Pixmap fillPixmap = new Pixmap(1, 1, Format.RGBA8888);
        fillPixmap.setColor(0xFFFFFFFF);
        fillPixmap.fill();
        TextureRegionDrawable fillDrawable = new TextureRegionDrawable(
                new TextureRegion(new Texture(fillPixmap)));
        fillPixmap.dispose();

        // empty drawable for the knob handle (no visible handle needed)
        Pixmap knobPixmap = new Pixmap(1, 1, Format.RGBA8888);
        knobPixmap.setColor(0x00000000);
        knobPixmap.fill();
        TextureRegionDrawable knobDrawable = new TextureRegionDrawable(
                new TextureRegion(new Texture(knobPixmap)));
        knobPixmap.dispose();

        progressBarStyle = new ProgressBarStyle();
        progressBarStyle.background = bgDrawable;
        progressBarStyle.knobBefore  = fillDrawable;
        progressBarStyle.knob        = knobDrawable;

        Texture   buttonTex   = new Texture( Gdx.files.internal("assets/button.png") );
        NinePatch buttonPatch = new NinePatch(buttonTex, 24,24,24,24);
        textButtonStyle.up    = new NinePatchDrawable( buttonPatch );
        textButtonStyle.font      = customFont;
        textButtonStyle.fontColor = Color.GRAY;
    }

    /**
     *  Used to switch screens while game is running.
     *  Method is static to simplify usage.
     */
    public static void setActiveScreen(BaseScreen s)
    {
        game.setScreen(s);
    }
}
