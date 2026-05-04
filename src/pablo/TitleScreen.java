package pablo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pablo.framework.BaseGame;
import pablo.framework.BaseScreen;

public class TitleScreen extends BaseScreen
{
    private SpriteBatch batch;
    private Texture     currentFrame;

    private static final int   TOTAL_FRAMES   = 240;   // 7s × 30fps
    private static final float FPS            = 30f;
    private static final float FRAME_DURATION = 1f / FPS;

    private float elapsedTime      = 0f;
    private int   currentFrameIndex = -1;
    private boolean done            = false;

    @Override
    public void initialize()
    {
        batch = new SpriteBatch();
        // La musica è già in esecuzione — non la tocchiamo
    }

    @Override
    public void update(float dt)
    {
        elapsedTime += dt;
    }

    @Override
    public void render(float dt)
    {
        update(dt);

        // Skip su qualsiasi tasto o tocco
        if (Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY))
        {
            goToMenu();
            return;
        }

        int frameIndex = (int)(elapsedTime / FRAME_DURATION) + 1;

        if (frameIndex > TOTAL_FRAMES)
        {
            goToMenu();
            return;
        }

        // Carica il nuovo frame solo quando cambia (evita di ricaricare ogni render)
        if (frameIndex != currentFrameIndex)
        {
            // I frame reali sono in assets/Titolo/ con nome frame_0001.png, ecc.
            String path = String.format("assets/Titolo/frame_%04d.png", frameIndex);

            if (Gdx.files.internal(path).exists())
            {
                Texture next = new Texture(Gdx.files.internal(path));

                if (currentFrame != null)
                    currentFrame.dispose();

                currentFrame      = next;
                currentFrameIndex = frameIndex;
            }
        }

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (currentFrame != null)
        {
            mainStage.getViewport().apply();
            float vw = mainStage.getViewport().getWorldWidth();
            float vh = mainStage.getViewport().getWorldHeight();
            batch.setProjectionMatrix(mainStage.getCamera().combined);
            batch.begin();
            batch.draw(currentFrame, 0, 0, vw, vh);
            batch.end();
        }
    }

    private void goToMenu()
    {
        if (done) return;
        done = true;
        BaseGame.setActiveScreen(new MenuScreen());
    }

    @Override
    public void dispose()
    {
        if (batch        != null) batch.dispose();
        if (currentFrame != null) currentFrame.dispose();
    }
}