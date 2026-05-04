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
    private boolean fullscreen  = true;

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
        sfxVolume   = prefs.getFloat  (KEY_SFX,   1.0f);
        musicVolume = prefs.getFloat  (KEY_MUSIC,  1.0f);
        fullscreen  = true; // forzato
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
        Gdx.app.getApplicationListener().resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.graphics.requestRendering();
    }

    // -----------------------------------------------------------------------
    // Getter / Setter
    // -----------------------------------------------------------------------
    public float getSfxVolume()   { return sfxVolume; }
    public float getMusicVolume() { return musicVolume; }
    public boolean isFullscreen() { return fullscreen; }

    public void setSfxVolume(float v)
    {
        sfxVolume = Math.max(0f, Math.min(1f, v));
        // Propaga immediatamente al SoundManager (se già inizializzato)
        try { SoundManager.get().setSfxVolume(sfxVolume); }
        catch (Exception ignored) { }
    }

    public void setMusicVolume(float v)
    {
        musicVolume = Math.max(0f, Math.min(1f, v));
        // Propaga immediatamente al SoundManager
        try { SoundManager.get().setMusicVolume(musicVolume); }
        catch (Exception ignored) { }
    }

    public void setFullscreen(boolean fs)
    {
        fullscreen = true; // forzato
        applyResolution();
        save();
    }
}