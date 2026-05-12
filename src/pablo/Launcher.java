package pablo;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.DisplayMode;

public class Launcher
{
    public static void main(String[] args)
    {
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.title = "HollowKnight-Pablo";

        DisplayMode mode = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDisplayMode();

        config.width      = mode.getWidth();   // 1920
        config.height     = mode.getHeight();  // 1080
        config.fullscreen = true;
        config.useHDPI    = false;

        Game myGame = new PabloGame();
        new LwjglApplication(myGame, config);
    }
}