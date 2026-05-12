// MapGraph.java
// Registro statico delle connessioni tra mappe.
// Per aggiungere una nuova mappa: crea un MapNode e collega i vicini.
//
// Convenzione:
//   rightNeighbor = mappa che si carica quando Pablo esce dal bordo destro
//   leftNeighbor  = mappa che si carica quando Pablo esce dal bordo sinistro

package pablo;

import java.util.HashMap;
import java.util.Map;

public class MapGraph
{
    // -----------------------------------------------------------------------
    // Nodo mappa
    // -----------------------------------------------------------------------
    public static class MapNode
    {
        public final String path;          // path del file .tmx
        public String leftNeighbor  = null; // path della mappa a sinistra (null = nessuna)
        public String rightNeighbor = null; // path della mappa a destra  (null = nessuna)

        public MapNode(String path) { this.path = path; }
    }

    // -----------------------------------------------------------------------
    // Registro
    // -----------------------------------------------------------------------
    private static final Map<String, MapNode> NODES = new HashMap<>();

    static
    {
        // ── Definizione nodi ──────────────────────────────────────────────
        MapNode map1 = new MapNode("assets/Maps/Mappa1.tmx");
        MapNode map2 = new MapNode("assets/Maps/Mappa2.tmx");
        MapNode map3 = new MapNode("assets/Maps/Mappa3.tmx");
        // Aggiungi altri nodi qui seguendo lo stesso schema

        // ── Connessioni ───────────────────────────────────────────────────
        //  map1  ──right──►  map2  ──right──►  map3
        //        ◄──left──         ◄──left──
        map1.rightNeighbor = map2.path;

        map2.leftNeighbor  = map1.path;
        map2.rightNeighbor = map3.path;

        map3.leftNeighbor  = map2.path;

        // ── Registrazione ─────────────────────────────────────────────────
        NODES.put(map1.path, map1);
        NODES.put(map2.path, map2);
        NODES.put(map3.path, map3);
    }

    // -----------------------------------------------------------------------
    // API pubblica
    // -----------------------------------------------------------------------

    /** Restituisce il nodo associato al path, o null se non registrato. */
    public static MapNode get(String mapPath)
    {
        return NODES.get(mapPath);
    }

    /** True se esiste un vicino a destra per questa mappa. */
    public static boolean hasRight(String mapPath)
    {
        MapNode n = NODES.get(mapPath);
        return n != null && n.rightNeighbor != null;
    }

    /** True se esiste un vicino a sinistra per questa mappa. */
    public static boolean hasLeft(String mapPath)
    {
        MapNode n = NODES.get(mapPath);
        return n != null && n.leftNeighbor != null;
    }
}