// Launcher.java

package pablo;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;

public class Launcher
{
    public static void main (String[] args)
    {
        Game myGame = new PabloGame();
        LwjglApplication launcher = new LwjglApplication( myGame, "HollowKnight-Pablo", 800, 640 );
    }
}
