// PauseScreen.java
package pablo;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import pablo.framework.BaseGame;
import pablo.framework.BaseScreen;

public class PauseScreen extends BaseScreen
{
    private BaseScreen gameplayScreen;

    public PauseScreen(BaseScreen gameplayScreen)
    {
        super();                              // calls initialize() — no-op at this point
        this.gameplayScreen = gameplayScreen;
        buildUI();                           // build the UI now that the ref is set
    }

    // Called by super() before gameplayScreen is assigned — must stay empty.
    public void initialize() { }

    private void buildUI()
    {
        Label pausedLabel = new Label("Paused", BaseGame.labelStyle);

        TextButton resumeButton = new TextButton("Resume", BaseGame.textButtonStyle);
        resumeButton.addListener(new ChangeListener()
        {
            public void changed(ChangeEvent event, Actor actor)
            {
                BaseGame.setActiveScreen(gameplayScreen);
            }
        });

        TextButton menuButton = new TextButton("Return to Menu", BaseGame.textButtonStyle);
        menuButton.addListener(new ChangeListener()
        {
            public void changed(ChangeEvent event, Actor actor)
            {
                BaseGame.setActiveScreen(new MenuScreen());
            }
        });

        uiTable.add(pausedLabel).padBottom(40).row();
        uiTable.add(resumeButton).width(200).padBottom(16).row();
        uiTable.add(menuButton).width(200).row();
    }

    public void update(float dt) { }
}