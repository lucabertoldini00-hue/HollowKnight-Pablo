// LevelScreen.java

package pablo;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.math.Vector2;
import pablo.entities.enemies.Crawlid;
import pablo.entities.enemies.Enemy;
import pablo.entities.enemies.Tiktik;
import pablo.entities.enemies.Vengefly;
import pablo.entities.player.Pablo;
import pablo.framework.BaseActor;
import pablo.framework.BaseGame;
import pablo.framework.BaseScreen;
import pablo.framework.TilemapActor;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import pablo.framework.BaseGame;

public class LevelScreen extends BaseScreen
{
    private Pablo pablo;

    private ProgressBar healthBar;
    private Label healthLabel;

    public void initialize()
    {
        TilemapActor tma = new TilemapActor("assets/mappa1.tmx", mainStage);

        // Oggetti solidi
        for (MapObject obj : tma.getRectangleList("solido"))
        {
            MapProperties props = obj.getProperties();
            new Object(
                    (float) props.get("x"),
                    (float) props.get("y"),
                    (float) props.get("width"),
                    (float) props.get("height"),
                    mainStage
            );
        }

        // Spawn Pablo (unchanged)
        MapObject startPoint  = tma.getRectangleList("start").get(0);
        MapProperties startProps = startPoint.getProperties();
        pablo = new Pablo((float) startProps.get("x"), (float) startProps.get("y"), mainStage);

        healthLabel = new Label("HP", BaseGame.labelStyle);

        healthBar = new ProgressBar(0, pablo.getMaxHealth(), 1, false,
                BaseGame.progressBarStyle);
        healthBar.setValue(pablo.getMaxHealth()); // Inizia piena

        uiTable.top().left();
        uiTable.add(healthLabel).pad(10).left().row();
        uiTable.add(healthBar).width(200).height(20).padLeft(10).left().row();

        // Spawn Crawlid (prova)
        for (MapObject obj : tma.getTileList("Crawlid"))
        {
            MapProperties props = obj.getProperties();
            new Crawlid((float) props.get("x"), (float) props.get("y"), mainStage);
        }

        // Spawn Tiktik (prova)
        for (MapObject obj : tma.getTileList("Tiktik"))
        {
            MapProperties props = obj.getProperties();
            new Tiktik((float) props.get("x"), (float) props.get("y"), mainStage);
        }

        // Spawn Vengefly (prova)
        for (MapObject obj : tma.getTileList("Vengefly"))
        {
            MapProperties props = obj.getProperties();
            new Vengefly((float) props.get("x"), (float) props.get("y"), mainStage, pablo);
        }
    }

    public void update(float dt)
    {
        healthBar.setValue(pablo.getHealth());

        // Collisioni Pablo
        for (BaseActor actor : BaseActor.getList(mainStage, Object.class.getName()))
        {
            Object oggetto = (Object) actor;
            if (pablo.overlaps(oggetto) && oggetto.isEnable())
            {
                Vector2 offset = pablo.preventOverlap(oggetto);
                if (offset != null)
                {
                    if (Math.abs(offset.x) > Math.abs(offset.y))
                        pablo.velocityVec.x = 0;
                    else
                        pablo.velocityVec.y = 0;
                }
            }
        }

        // Collisioni nemici
        for (BaseActor eActor : BaseActor.getList(mainStage, Enemy.class.getName()))
        {
            Enemy enemy = (Enemy) eActor;

            for (BaseActor sActor : BaseActor.getList(mainStage, Object.class.getName()))
            {
                Object solid = (Object) sActor;

                if (enemy.overlaps(solid) && solid.isEnable())
                {
                    Vector2 offset = enemy.preventOverlap(solid);
                    if (offset != null)
                    {
                        if (Math.abs(offset.x) > Math.abs(offset.y))
                            enemy.onWallHit(); // wall → flip direction
                        else
                            enemy.velocityVec.y = 0; // floor/ceiling → stop vertical movement
                    }
                }
            }
        }
    }

    public boolean keyDown(int keyCode)
    {
        if (keyCode == Input.Keys.ESCAPE || keyCode == Input.Keys.P)
        {
            BaseGame.setActiveScreen(new PauseScreen(this));
            return true;
        }

        if (keyCode == Input.Keys.K)
        {
            pablo.takeDamage(1);
            return true;
        }

        if (keyCode == Input.Keys.SPACE || keyCode == Input.Keys.W)
        {
            if (pablo.isOnSolid())
            {
                pablo.jump();
            }
        }
        return false;
    }
}