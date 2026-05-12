// PlayerState.java
// Salva lo stato di Pablo tra una transizione di mappa e l'altra.
// Essendo statico, sopravvive alla creazione di un nuovo LevelScreen.
//
// Uso:
//   Prima di caricare la nuova mappa → PlayerState.saveFrom(pablo)
//   Dopo aver creato il nuovo Pablo  → PlayerState.applyTo(pablo)

package pablo;

import pablo.entities.player.Pablo;

public class PlayerState
{
    private static int     health    = -1;  // -1 = stato non salvato
    private static int     soul      = 0;
    private static boolean hasSave   = false;

    /** Salva lo stato corrente di Pablo prima della transizione. */
    public static void saveFrom(Pablo pablo)
    {
        health  = pablo.getHealth();
        soul    = pablo.getSoul();
        hasSave = true;
    }

    /**
     * Applica lo stato salvato al nuovo Pablo.
     * Se non è mai stato salvato nulla, non fa niente (Pablo usa i valori di default).
     */
    public static void applyTo(Pablo pablo)
    {
        if (!hasSave) return;
        pablo.setHealth(health);
        pablo.setSoul(soul);
    }

    /** True se c'è uno stato salvato da applicare. */
    public static boolean hasSave()
    {
        return hasSave;
    }

    /** Resetta lo stato salvato (es. alla morte o al ritorno al menu). */
    public static void reset()
    {
        health  = -1;
        soul    = 0;
        hasSave = false;
    }
}