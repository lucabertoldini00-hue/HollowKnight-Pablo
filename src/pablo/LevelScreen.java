// LevelScreen.java

package pablo;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.math.Vector2;
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

        for (MapObject obj : tma.getRectangleList("solido") )
        {
            MapProperties props = obj.getProperties();
            new Object( (float)props.get("x"), (float)props.get("y"),
                    (float)props.get("width"), (float)props.get("height"),
                    mainStage );
        }
        MapObject startPoint = tma.getRectangleList("start").get(0);
        MapProperties startProps = startPoint.getProperties();
        pablo = new Pablo( (float)startProps.get("x"), (float)startProps.get("y"), mainStage);
    }

    public void update(float dt)
    {
        //questo ciclo serve per evitare comportamenti anomali quando il personaggio tocca i bordi di un oggetto
        //per esempio in caso di salto o caduta il personaggio non deve attraversare gli ostacoli, nè la sua gravità deve arrestarsi
        for (BaseActor actor : BaseActor.getList(mainStage, Object.class.getName()))
        {
            Object oggetto = (Object) actor;
            if ( pablo.overlaps(oggetto) && oggetto.isEnable() )
            {
                Vector2 offset = pablo.preventOverlap(oggetto);
                if (offset != null)
                {
                    // collisione nella direzione x
                    if ( Math.abs(offset.x) > Math.abs(offset.y) )
                        pablo.velocityVec.x = 0;
                    else // collided in Y direction
                        pablo.velocityVec.y = 0;
                }
            }
        }
    }

    // Metodo che gestisce il salto del personaggio
    public boolean keyDown(int keyCode)
    {
        // Ora il jump è gestito nella "Pablo state machine"
        // Qua si possono aggiungere funzionalità future
        // Questo è quello che dovrebbe succedere --^

        // Questa è la versione vecchia e funzionanete, toglientdo il codice sotto tranne
        // return false dovrebbe funzionare ma non va :(

        if (keyCode == Input.Keys.SPACE || keyCode == Input.Keys.W)
        {
            if ( pablo.isOnSolid() )
            {
                pablo.jump();
            }
        }
        return false;
    }
}
