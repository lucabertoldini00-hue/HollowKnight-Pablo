// TilemapActor.java

package pablo.framework;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Loads a Tiled map file (*.tmx), extends Actor to automatically render.
 */
public class TilemapActor extends Actor
{
    public static int windowWidth  = 800;
    public static int windowHeight = 640;

    private TiledMap tiledMap;
    private OrthographicCamera tiledCamera;
    private OrthogonalTiledMapRenderer tiledMapRenderer;

    // Path del file caricato — usato nei messaggi di errore
    private String loadedPath;

    public TilemapActor(String filename, Stage theStage)
    {
        this.loadedPath = filename;

        tiledMap = new TmxMapLoader().load(filename);

        int tileWidth          = (int) tiledMap.getProperties().get("tilewidth");
        int tileHeight         = (int) tiledMap.getProperties().get("tileheight");
        int numTilesHorizontal = (int) tiledMap.getProperties().get("width");
        int numTilesVertical   = (int) tiledMap.getProperties().get("height");
        int mapWidth  = tileWidth  * numTilesHorizontal;
        int mapHeight = tileHeight * numTilesVertical;

        Gdx.app.log("TilemapActor", "Mappa caricata: " + filename
                + " (" + mapWidth + "x" + mapHeight + " px)");

        tiledMapRenderer = new OrthogonalTiledMapRenderer(tiledMap);
        tiledCamera = new OrthographicCamera();
        tiledCamera.setToOrtho(false, windowWidth, windowHeight);
        tiledCamera.update();

        theStage.addActor(this);
        BaseActor.setWorldBounds(mapWidth, mapHeight);

        // Log di tutti gli oggetti trovati nei layer per facilitare il debug
        logAllObjects();
    }

    // -----------------------------------------------------------------------
    // getRectangleList — cerca oggetti rettangolari per tipo/nome
    // -----------------------------------------------------------------------

    public ArrayList<MapObject> getRectangleList(String propertyName)
    {
        ArrayList<MapObject> list = new ArrayList<MapObject>();

        for (MapLayer layer : tiledMap.getLayers())
        {
            for (MapObject obj : layer.getObjects())
            {
                if (!(obj instanceof RectangleMapObject))
                    continue;

                MapProperties props = obj.getProperties();

                if (matchesObjectType(obj, props, propertyName))
                    list.add(obj);
            }
        }

        if (list.isEmpty())
            Gdx.app.log("TilemapActor", "ATTENZIONE: nessun oggetto '" + propertyName
                    + "' trovato in " + loadedPath);

        return list;
    }

    // -----------------------------------------------------------------------
    // getTileList — cerca tile-objects per tipo/nome
    // -----------------------------------------------------------------------

    public ArrayList<MapObject> getTileList(String propertyName)
    {
        ArrayList<MapObject> list = new ArrayList<MapObject>();

        for (MapLayer layer : tiledMap.getLayers())
        {
            for (MapObject obj : layer.getObjects())
            {
                if (!(obj instanceof TiledMapTileMapObject))
                    continue;

                MapProperties props = obj.getProperties();

                TiledMapTileMapObject tmtmo = (TiledMapTileMapObject) obj;
                TiledMapTile t = tmtmo.getTile();
                MapProperties defaultProps = t.getProperties();

                Iterator<String> propertyKeys = defaultProps.getKeys();
                while (propertyKeys.hasNext())
                {
                    String key = propertyKeys.next();
                    if (!props.containsKey(key))
                        props.put(key, defaultProps.get(key));
                }

                if (matchesObjectType(obj, props, propertyName))
                    list.add(obj);
            }
        }

        return list;
    }

    // -----------------------------------------------------------------------
    // matchesObjectType — controlla nome oggetto, property "name" o "type"
    // -----------------------------------------------------------------------

    /**
     * Confronta il tipo atteso con:
     *   1. Il nome XML dell'oggetto (attributo "name" del tag <object>)
     *   2. La custom property "name"
     *   3. La custom property "type"
     *
     * Case-insensitive per robustezza.
     */
    private boolean matchesObjectType(MapObject obj, MapProperties props, String expectedType)
    {
        // 1. Nome XML dell'oggetto
        String objName = obj.getName();
        if (objName != null && objName.equalsIgnoreCase(expectedType))
            return true;

        // 2. Custom property "name"
        if (matchesProperty(props, "name", expectedType))
            return true;

        // 3. Custom property "type"
        if (matchesProperty(props, "type", expectedType))
            return true;

        return false;
    }

    private boolean matchesProperty(MapProperties props, String key, String expectedValue)
    {
        if (!props.containsKey(key))
            return false;
        Object val = props.get(key);
        return val != null && expectedValue.equalsIgnoreCase(String.valueOf(val));
    }

    // -----------------------------------------------------------------------
    // logAllObjects — stampa tutti gli oggetti in console al caricamento
    // -----------------------------------------------------------------------

    private void logAllObjects()
    {
        Gdx.app.log("TilemapActor", "--- Oggetti nella mappa " + loadedPath + " ---");
        for (MapLayer layer : tiledMap.getLayers())
        {
            int count = 0;
            for (MapObject obj : layer.getObjects()) count++;

            Gdx.app.log("TilemapActor", "Layer '" + layer.getName()
                    + "' (" + count + " oggetti):");

            for (MapObject obj : layer.getObjects())
            {
                String tipo   = obj.getClass().getSimpleName();
                String nome   = obj.getName();
                Object nameProp = obj.getProperties().containsKey("name")
                        ? obj.getProperties().get("name") : "(assente)";

                Gdx.app.log("TilemapActor", "  [" + tipo + "] nome='"
                        + nome + "' prop.name='" + nameProp + "'");
            }
        }
        Gdx.app.log("TilemapActor", "-------------------------------------------");
    }

    // -----------------------------------------------------------------------
    // act / draw
    // -----------------------------------------------------------------------

    public void act(float dt)
    {
        super.act(dt);
    }

    public void draw(Batch batch, float parentAlpha)
    {
        Camera mainCamera = getStage().getCamera();

        // Sincronizza le dimensioni della camera tile con il viewport reale.
        // Con ExtendViewport le dimensioni del mondo cambiano in base alla
        // risoluzione dello schermo — se usiamo valori fissi (800×640) i tile
        // risultano sfasati rispetto agli oggetti fisici (solidi, spawn, ecc.).
        tiledCamera.setToOrtho(false,
                getStage().getViewport().getWorldWidth(),
                getStage().getViewport().getWorldHeight());

        tiledCamera.position.x = mainCamera.position.x;
        tiledCamera.position.y = mainCamera.position.y;
        tiledCamera.update();
        tiledMapRenderer.setView(tiledCamera);

        batch.end();
        tiledMapRenderer.render();
        batch.begin();
    }
}