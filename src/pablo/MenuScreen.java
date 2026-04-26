// MenuScreen.java
package pablo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import pablo.framework.BaseGame;
import pablo.framework.BaseScreen;

public class MenuScreen extends BaseScreen
{
    public void initialize()
    {
        Label titleLabel = new Label("Pablo", BaseGame.labelStyle);

        TextButton startButton = new TextButton("Start Game", BaseGame.textButtonStyle);
        startButton.addListener(new ChangeListener()
        {
            public void changed(ChangeEvent event, Actor actor)
            {
                BaseGame.setActiveScreen(new LevelScreen());
            }
        });

        TextButton exitButton = new TextButton("Exit", BaseGame.textButtonStyle);
        exitButton.addListener(new ChangeListener()
        {
            public void changed(ChangeEvent event, Actor actor)
            {
                Gdx.app.exit();
            }
        });

        uiTable.add(titleLabel).padBottom(40).row();
        uiTable.add(startButton).width(200).padBottom(16).row();
        uiTable.add(exitButton).width(200).row();
    }

    public void update(float dt) { }
}