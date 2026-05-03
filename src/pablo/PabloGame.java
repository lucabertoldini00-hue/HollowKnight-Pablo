// PabloGame.java
package pablo;

import pablo.framework.BaseGame;

public class PabloGame extends BaseGame
{
    public void create()
    {
        super.create();
        setActiveScreen( new IntroScreen() ); // era: new MenuScreen()
    }
}