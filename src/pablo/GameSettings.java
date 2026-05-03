// GameSettings.java
// Singleton che contiene tutte le impostazioni di gioco.
// Salva e carica tramite LibGDX Preferences (file locale automatico).

package pablo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class GameSettings
{
    private static GameSettings instance;

    private static final String PREFS_NAME     = "pablo_settings";
    private static final String KEY_SFX        = "sfx_volume";
    private static final String KEY_MUSIC      = "music_volume";
    private static final String KEY_FULLSCREEN = "fullscreen";

    // Valori correnti
    private float   sfxVolume   = 1.0f;
    private float   musicVolume = 1.0f;
    private boolean fullscreen  = true;  // FORZATO A TRUE

    // -----------------------------------------------------------------------
    // Singleton
    // -----------------------------------------------------------------------
    private GameSettings() { load(); }

    public static GameSettings get()
    {
        if (instance == null) instance = new GameSettings();
        return instance;
    }

    // -----------------------------------------------------------------------
    // Persistenza
    // -----------------------------------------------------------------------
    public void save()
    {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.putFloat  (KEY_SFX,        sfxVolume);
        prefs.putFloat  (KEY_MUSIC,      musicVolume);
        prefs.putBoolean(KEY_FULLSCREEN, fullscreen);
        prefs.flush();
    }

    public void load()
    {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        sfxVolume   = prefs.getFloat  (KEY_SFX,        1.0f);
        musicVolume = prefs.getFloat  (KEY_MUSIC,       1.0f);
        fullscreen  = true;
    }

    // -----------------------------------------------------------------------
    // Applica le impostazioni al sistema
    // -----------------------------------------------------------------------

    /** Applica la risoluzione corrente (fullscreen o finestra). */
    public void applyResolution()
    {
        if (fullscreen) {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        } else {
            Gdx.graphics.setWindowedMode(800, 640);
        }

        // Forza il ridimensionamento della viewport
        // (non è garantito che resize() sia automaticamente chiamato)
        Gdx.graphics.requestRendering();
    }

    // -----------------------------------------------------------------------
    // Getter / Setter
    // -----------------------------------------------------------------------
    public float getSfxVolume()       { return sfxVolume; }
    public float getMusicVolume()     { return musicVolume; }
    public boolean isFullscreen()     { return fullscreen; }

    public void setSfxVolume(float v)
    {
        sfxVolume = v;
        // Applica immediatamente al SoundManager se esiste
    }

    public void setMusicVolume(float v)
    {
        musicVolume = v;
        // Applica immediatamente alla musica corrente se esiste
    }

    public void setFullscreen(boolean fs)
    {
        // Ignoriamo il parametro — fullscreen è sempre true
        fullscreen = true;
        applyResolution();
        save();
    }
}
