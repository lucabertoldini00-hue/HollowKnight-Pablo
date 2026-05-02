// IntroScreen.java
//
// Usa VideoPlayerDesktop direttamente (cast esplicito) perché render()
// non è esposto nell'interfaccia VideoPlayer di questa versione anonl.
// render() gestisce internamente decodifica + disegno a schermo tramite shader.

package pablo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.video.VideoPlayer.CompletionListener;
import com.badlogic.gdx.video.VideoPlayerCreator;
import com.badlogic.gdx.video.VideoPlayerDesktop;
import pablo.framework.BaseGame;
import pablo.framework.BaseScreen;

public class IntroScreen extends BaseScreen
{
    // -----------------------------------------------------------------------
    // TODO: imposta il path del tuo file video
    // -----------------------------------------------------------------------
    private static final String VIDEO_PATH = "assets/YOUR_INTRO_VIDEO.mp4";

    private VideoPlayerDesktop videoPlayer;
    private boolean            done = false;

    // -----------------------------------------------------------------------
    // initialize()
    // -----------------------------------------------------------------------
    public void initialize()
    {
        try
        {
            videoPlayer = (VideoPlayerDesktop) VideoPlayerCreator.createVideoPlayer();

            videoPlayer.setOnCompletionListener(new CompletionListener()
            {
                public void onCompletionListener(FileHandle file)
                {
                    done = true;
                }
            });

            boolean loaded = videoPlayer.play(Gdx.files.internal(VIDEO_PATH));
            if (!loaded)
            {
                Gdx.app.error("IntroScreen", "Impossibile caricare: " + VIDEO_PATH);
                done = true;
            }
        }
        catch (Exception e)
        {
            Gdx.app.error("IntroScreen", "Errore video: " + e.getMessage());
            done = true;
        }
    }

    // -----------------------------------------------------------------------
    // render() — override completo, nessuno stage da disegnare
    // -----------------------------------------------------------------------
    @Override
    public void render(float dt)
    {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY) || Gdx.input.justTouched())
            done = true;

        if (done)
        {
            goToMenu();
            return;
        }

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (videoPlayer != null)
        {
            // render() decodifica e disegna il frame corrente a schermo
            videoPlayer.render();
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------
    private void goToMenu()
    {
        if (videoPlayer != null)
        {
            videoPlayer.dispose();
            videoPlayer = null;
        }
        BaseGame.setActiveScreen(new MenuScreen());
    }

    public void update(float dt) { /* gestito in render() */ }

    @Override
    public void dispose()
    {
        super.dispose();
        if (videoPlayer != null) videoPlayer.dispose();
    }
}