package pablo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import pablo.framework.BaseGame;
import pablo.framework.BaseScreen;

public class IntroScreen extends BaseScreen
{
    private SpriteBatch batch;
    private Texture currentFrame; // Solo 1 texture attiva alla volta
    private Music music;

    private boolean done = false;

    // Assicurati che totaleFrames corrisponda al numero esatto di file nella tua cartella (es. avevi 424 frame in precedenza)
    private int totalFrames = 424;
    private float fps = 30f;
    private float frameDuration = 1f / fps;
    private float elapsedTime = 0f;
    private int currentFrameIndex = -1;

    public void initialize()
    {
        batch = new SpriteBatch();

        music = Gdx.audio.newMusic(Gdx.files.internal("assets/Video/intro.mp3"));
        music.play();
    }

    @Override
    public void update(float dt) {
        SoundManager.get().update(dt);
        // Accumuliamo il tempo passato usando il deltaTime di LibGDX (più affidabile di music.getPosition())
        elapsedTime += dt;
    }

    @Override
    public void render(float dt)
    {
        update(dt);
        if (music == null) return;

        if (Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY)) {
            goToMenu();
            return;
        }

        // Calcoliamo a quale file / fotogramma corrispondiamo (partendo da 1 come i tuoi nomi file)
        int frameIndex = (int)(elapsedTime / frameDuration) + 1;

        if (frameIndex > totalFrames) {
            goToMenu();
            return;
        }

        // Se è avvenuto il momento di cambiare fotogramma...
        if (frameIndex != currentFrameIndex) {
            String path = String.format("assets/Video/frame_%04d.png", frameIndex);
            if (Gdx.files.internal(path).exists()) {
                Texture nextFrame = new Texture(Gdx.files.internal(path));
                if (currentFrame != null) {
                    currentFrame.dispose();
                }
                currentFrame = nextFrame;
                currentFrameIndex = frameIndex;
            }
        }

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (currentFrame != null) {
            mainStage.getViewport().apply();
            float viewportWidth = mainStage.getViewport().getWorldWidth();
            float viewportHeight = mainStage.getViewport().getWorldHeight();
            batch.setProjectionMatrix(mainStage.getCamera().combined);
            batch.begin();
            batch.draw(currentFrame, 0, 0, viewportWidth, viewportHeight);
            batch.end();
        }
    }

    private void goToMenu()
    {
        if (done) return;
        done = true;

        if (music != null) {
            music.stop();
        }

        BaseGame.setActiveScreen(new MenuScreen());
    }

    @Override
    public void dispose()
    {
        if (batch != null) batch.dispose();

        // Liberiamo la singola immagine attuale dallo heap
        if (currentFrame != null) {
            currentFrame.dispose();
        }

        if (music != null) music.dispose();
    }
}