// PabloGame.java
package pablo;

import pablo.framework.BaseGame;

public class PabloGame extends BaseGame
{
    public void create()
    {
        super.create();
        // Ora il fullscreen è già impostato dal Launcher
        setActiveScreen( new IntroScreen() );
    }
}