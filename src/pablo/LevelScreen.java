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
import pablo.framework.BaseScreen;
import pablo.framework.TilemapActor;

public class LevelScreen extends BaseScreen
{
    private Pablo pablo;

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
        if (keyCode == Input.Keys.SPACE || keyCode == Input.Keys.W)
        {
            if (pablo.isOnSolid())
                pablo.jump();
        }
        return false;
    }
}