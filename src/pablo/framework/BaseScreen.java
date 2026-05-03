// BaseScreen.java

package pablo.framework;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.*;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;

public abstract class BaseScreen implements Screen, InputProcessor
{
    // Dimensioni di riferimento del mondo di gioco
    private static final int WORLD_WIDTH  = 800;
    private static final int WORLD_HEIGHT = 640;

    protected Stage mainStage;
    protected Stage uiStage;
    protected Table uiTable;

    public BaseScreen()
    {
        // StretchViewport: riempie lo schermo senza bande nere (deforma l'aspect ratio).
        mainStage = new Stage( new StretchViewport(WORLD_WIDTH, WORLD_HEIGHT) );

        // ScreenViewport: 1 unità = 1 pixel reale.
        // Corretto per la UI — i bottoni e i font si posizionano in coordinate schermo reali.
        uiStage = new Stage( new ScreenViewport() );

        uiTable = new Table();
        uiTable.setFillParent(true);
        uiStage.addActor(uiTable);

        initialize();
    }

    public abstract void initialize();
    public abstract void update(float dt);

    public void render(float dt)
    {
        dt = Math.min(dt, 1 / 30f);

        mainStage.getViewport().apply();
        uiStage.getViewport().apply();

        uiStage.act(dt);
        mainStage.act(dt);

        update(dt);

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        mainStage.draw();
        uiStage.draw();
    }

    // Propaga le nuove dimensioni ad entrambi i viewport
    public void resize(int width, int height)
    {
        // true = centra la camera (importante per FitViewport)
        mainStage.getViewport().update(width, height, true);
        uiStage.getViewport().update(width, height, true);
    }

    public void pause()   { }
    public void resume()  { }
    public void dispose() { }

    public void show()
    {
        InputMultiplexer im = (InputMultiplexer) Gdx.input.getInputProcessor();
        // BaseScreen prima: keyDown() viene intercettato prima di uiStage
        im.addProcessor(this);
        im.addProcessor(uiStage);
        im.addProcessor(mainStage);
    }

    public void hide()
    {
        InputMultiplexer im = (InputMultiplexer) Gdx.input.getInputProcessor();
        im.removeProcessor(this);
        im.removeProcessor(uiStage);
        im.removeProcessor(mainStage);
    }

    public boolean isTouchDownEvent(Event e)
    {
        return (e instanceof InputEvent) && ((InputEvent) e).getType().equals(Type.touchDown);
    }

    public boolean keyDown(int keycode)                              { return false; }
    public boolean keyUp(int keycode)                                { return false; }
    public boolean keyTyped(char c)                                  { return false; }
    public boolean mouseMoved(int x, int y)                         { return false; }
    public boolean scrolled(int amount)                              { return false; }
    public boolean touchDown(int x, int y, int pointer, int button) { return false; }
    public boolean touchDragged(int x, int y, int pointer)          { return false; }
    public boolean touchUp(int x, int y, int pointer, int button)   { return false; }
}