// SoundManager.java
// Singleton che gestisce tutta l'audio del gioco: OST (Music) e SFX (Sound).
//
// STRUTTURA ASSET ATTESA:
//   assets/audio/music/menu.mp3
//   assets/audio/music/level.mp3
//   assets/audio/music/boss.mp3
//   assets/audio/sfx/player/jump.wav
//   assets/audio/sfx/player/land.wav
//   assets/audio/sfx/player/attack.wav
//   assets/audio/sfx/player/dash.wav
//   assets/audio/sfx/player/hurt.wav
//   assets/audio/sfx/player/death.wav
//   assets/audio/sfx/player/heal_start.wav
//   assets/audio/sfx/player/heal_end.wav
//   assets/audio/sfx/enemy/hit.wav
//   assets/audio/sfx/enemy/death.wav
//   assets/audio/sfx/ui/select.wav
//   assets/audio/sfx/ui/confirm.wav
//   assets/audio/sfx/ui/back.wav
//
// Tutti i file mancanti vengono ignorati silenziosamente (nessun crash).

package pablo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

import java.util.EnumMap;
import java.util.Map;

public class SoundManager
{
    // =========================================================================
    // Enum tracce musicali
    // =========================================================================
    public enum Track
    {
        MENU,
        LEVEL,
        BOSS
    }

    // =========================================================================
    // Enum effetti sonori
    // =========================================================================
    public enum Sfx
    {
        // Player
        PLAYER_JUMP,
        PLAYER_LAND,
        PLAYER_ATTACK,
        PLAYER_DASH,
        PLAYER_HURT,
        PLAYER_DEATH,
        PLAYER_HEAL_START,
        PLAYER_HEAL_END,
        PLAYER_SOUL_GAIN,

        // Nemici
        ENEMY_HIT,
        ENEMY_DEATH,
        BOSS_HIT,
        BOSS_DEATH,
        BOSS_ATTACK,
        BOSS_LAND,

        // UI
        UI_SELECT,
        UI_CONFIRM,
        UI_BACK
    }

    // =========================================================================
    // Singleton
    // =========================================================================
    private static SoundManager instance;

    public static SoundManager get()
    {
        if (instance == null)
            instance = new SoundManager();
        return instance;
    }

    // =========================================================================
    // Campi
    // =========================================================================
    private final Map<Track, Music>  musicMap = new EnumMap<>(Track.class);
    private final Map<Sfx,   Sound>  sfxMap   = new EnumMap<>(Sfx.class);

    private Track   currentTrack  = null;
    private float   musicVolume   = 1.0f;
    private float   sfxVolume     = 1.0f;
    private boolean musicEnabled  = true;
    private boolean sfxEnabled    = true;

    // Cooldown minimo tra due riproduzione dello stesso SFX (evita stacking)
    private final Map<Sfx, Float> sfxCooldown   = new EnumMap<>(Sfx.class);
    private final Map<Sfx, Float> sfxCooldownMax = new EnumMap<>(Sfx.class);

    // =========================================================================
    // Costruttore privato
    // =========================================================================
    private SoundManager()
    {
        loadMusic();
        loadSfx();
        initCooldowns();

        // Sincronizza con le impostazioni salvate
        GameSettings s = GameSettings.get();
        musicVolume = s.getMusicVolume();
        sfxVolume   = s.getSfxVolume();
    }

    // =========================================================================
    // Caricamento assets
    // =========================================================================

    private void loadMusic()
    {
        tryLoadMusic(Track.MENU,  "assets/audio/music/menu.ogg");
        tryLoadMusic(Track.LEVEL, "assets/audio/music/level.ogg");
        tryLoadMusic(Track.BOSS,  "assets/audio/music/boss.ogg");
    }

    private void loadSfx()
    {
        tryLoadSfx(Sfx.PLAYER_JUMP,       "assets/audio/sfx/player/jump.wav");
        tryLoadSfx(Sfx.PLAYER_LAND,       "assets/audio/sfx/player/land.wav");
        tryLoadSfx(Sfx.PLAYER_ATTACK,     "assets/audio/sfx/player/attack.wav");
        tryLoadSfx(Sfx.PLAYER_DASH,       "assets/audio/sfx/player/dash.wav");
        tryLoadSfx(Sfx.PLAYER_HURT,       "assets/audio/sfx/player/hurt.wav");
        tryLoadSfx(Sfx.PLAYER_DEATH,      "assets/audio/sfx/player/death.wav");
        tryLoadSfx(Sfx.PLAYER_HEAL_START, "assets/audio/sfx/player/heal_start.wav");
        tryLoadSfx(Sfx.PLAYER_HEAL_END,   "assets/audio/sfx/player/heal_end.wav");
        tryLoadSfx(Sfx.PLAYER_SOUL_GAIN,  "assets/audio/sfx/player/soul_gain.wav");

        tryLoadSfx(Sfx.ENEMY_HIT,   "assets/audio/sfx/enemy/hit.wav");
        tryLoadSfx(Sfx.ENEMY_DEATH, "assets/audio/sfx/enemy/death.wav");
        tryLoadSfx(Sfx.BOSS_HIT,    "assets/audio/sfx/enemy/boss_hit.wav");
        tryLoadSfx(Sfx.BOSS_DEATH,  "assets/audio/sfx/enemy/boss_death.wav");
        tryLoadSfx(Sfx.BOSS_ATTACK, "assets/audio/sfx/enemy/boss_attack.wav");
        tryLoadSfx(Sfx.BOSS_LAND,   "assets/audio/sfx/enemy/boss_land.wav");

        tryLoadSfx(Sfx.UI_SELECT,  "assets/audio/sfx/ui/select.wav");
        tryLoadSfx(Sfx.UI_CONFIRM, "assets/audio/sfx/ui/confirm.wav");
        tryLoadSfx(Sfx.UI_BACK,    "assets/audio/sfx/ui/back.wav");
    }

    private void initCooldowns()
    {
        // Definisci i cooldown minimi (secondi) per ogni SFX
        // Impedisce che lo stesso suono si sovrapponga troppe volte in rapida successione
        sfxCooldownMax.put(Sfx.PLAYER_JUMP,       0.10f);
        sfxCooldownMax.put(Sfx.PLAYER_LAND,       0.05f);
        sfxCooldownMax.put(Sfx.PLAYER_ATTACK,     0.05f);
        sfxCooldownMax.put(Sfx.PLAYER_DASH,       0.20f);
        sfxCooldownMax.put(Sfx.PLAYER_HURT,       0.30f);
        sfxCooldownMax.put(Sfx.PLAYER_DEATH,      2.00f);
        sfxCooldownMax.put(Sfx.PLAYER_HEAL_START, 0.10f);
        sfxCooldownMax.put(Sfx.PLAYER_HEAL_END,   0.10f);
        sfxCooldownMax.put(Sfx.PLAYER_SOUL_GAIN,  0.08f);
        sfxCooldownMax.put(Sfx.ENEMY_HIT,         0.05f);
        sfxCooldownMax.put(Sfx.ENEMY_DEATH,       0.05f);
        sfxCooldownMax.put(Sfx.BOSS_HIT,          0.10f);
        sfxCooldownMax.put(Sfx.BOSS_DEATH,        2.00f);
        sfxCooldownMax.put(Sfx.BOSS_ATTACK,       0.10f);
        sfxCooldownMax.put(Sfx.BOSS_LAND,         0.20f);
        sfxCooldownMax.put(Sfx.UI_SELECT,         0.05f);
        sfxCooldownMax.put(Sfx.UI_CONFIRM,        0.10f);
        sfxCooldownMax.put(Sfx.UI_BACK,           0.10f);

        // Inizializza tutti i timer a 0
        for (Sfx s : Sfx.values())
            sfxCooldown.put(s, 0f);
    }

    // =========================================================================
    // API pubblica — Musica
    // =========================================================================

    /**
     * Avvia una traccia musicale con fade-out dalla traccia precedente.
     * Se la traccia richiesta è già in riproduzione, non fa nulla.
     */
    public void playMusic(Track track)
    {
        if (!musicEnabled) return;
        if (track == currentTrack) return;

        // Ferma la musica corrente immediatamente
        // (un sistema con fade richiederebbe un update() separato)
        stopMusic();

        currentTrack = track;
        Music m = musicMap.get(track);
        if (m == null) return;

        m.setLooping(true);
        m.setVolume(musicVolume);
        m.play();
    }

    /** Ferma la musica corrente. */
    public void stopMusic()
    {
        if (currentTrack != null)
        {
            Music m = musicMap.get(currentTrack);
            if (m != null && m.isPlaying())
                m.stop();
            currentTrack = null;
        }
    }

    /** Mette in pausa la musica corrente. */
    public void pauseMusic()
    {
        if (currentTrack == null) return;
        Music m = musicMap.get(currentTrack);
        if (m != null && m.isPlaying()) m.pause();
    }

    /** Riprende la musica precedentemente messa in pausa. */
    public void resumeMusic()
    {
        if (!musicEnabled || currentTrack == null) return;
        Music m = musicMap.get(currentTrack);
        if (m != null && !m.isPlaying()) m.play();
    }

    // =========================================================================
    // API pubblica — SFX
    // =========================================================================

    /**
     * Riproduce un effetto sonoro.
     * Se il cooldown non è scaduto, la chiamata viene ignorata.
     */
    public void playSfx(Sfx sfx)
    {
        playSfx(sfx, 1.0f);
    }

    /**
     * Riproduce un SFX con un pitch personalizzato (1.0 = normale, >1 = acuto, <1 = grave).
     */
    public void playSfx(Sfx sfx, float pitchMultiplier)
    {
        if (!sfxEnabled) return;

        // Controlla cooldown
        Float remaining = sfxCooldown.get(sfx);
        if (remaining != null && remaining > 0f) return;

        Sound s = sfxMap.get(sfx);
        if (s == null) return;

        // Pitch leggera randomizzazione per varietà naturale (±3%)
        float finalPitch = pitchMultiplier * (0.97f + (float)Math.random() * 0.06f);

        s.play(sfxVolume, finalPitch, 0f);

        // Reimposta il cooldown
        Float cd = sfxCooldownMax.get(sfx);
        sfxCooldown.put(sfx, cd != null ? cd : 0f);
    }

    /**
     * Deve essere chiamato ogni frame (da LevelScreen.update o simile)
     * per decrementare i cooldown SFX.
     */
    public void update(float dt)
    {
        for (Sfx sfx : Sfx.values())
        {
            Float remaining = sfxCooldown.get(sfx);
            if (remaining != null && remaining > 0f)
                sfxCooldown.put(sfx, Math.max(0f, remaining - dt));
        }
    }

    // =========================================================================
    // Volume
    // =========================================================================

    public void setMusicVolume(float volume)
    {
        musicVolume = Math.max(0f, Math.min(1f, volume));
        if (currentTrack != null)
        {
            Music m = musicMap.get(currentTrack);
            if (m != null) m.setVolume(musicVolume);
        }
    }

    public void setSfxVolume(float volume)
    {
        sfxVolume = Math.max(0f, Math.min(1f, volume));
    }

    public float getMusicVolume() { return musicVolume; }
    public float getSfxVolume()   { return sfxVolume; }

    public void setMusicEnabled(boolean enabled)
    {
        musicEnabled = enabled;
        if (!enabled) stopMusic();
    }

    public void setSfxEnabled(boolean enabled)
    {
        sfxEnabled = enabled;
    }

    // =========================================================================
    // Dispose
    // =========================================================================

    public void dispose()
    {
        stopMusic();
        for (Music m : musicMap.values())
            if (m != null) m.dispose();
        for (Sound s : sfxMap.values())
            if (s != null) s.dispose();
        musicMap.clear();
        sfxMap.clear();
        instance = null;
    }

    // =========================================================================
    // Helpers privati
    // =========================================================================

    private void tryLoadMusic(Track track, String path)
    {
        try
        {
            if (Gdx.files.internal(path).exists())
            {
                musicMap.put(track, Gdx.audio.newMusic(Gdx.files.internal(path)));
                Gdx.app.log("SoundManager", "OST caricata: " + path);
            }
            else
            {
                Gdx.app.log("SoundManager", "OST non trovata (skip): " + path);
            }
        }
        catch (Exception e)
        {
            Gdx.app.error("SoundManager", "Errore caricamento OST: " + path, e);
        }
    }

    private void tryLoadSfx(Sfx sfx, String path)
    {
        try
        {
            if (Gdx.files.internal(path).exists())
            {
                sfxMap.put(sfx, Gdx.audio.newSound(Gdx.files.internal(path)));
                Gdx.app.log("SoundManager", "SFX caricato: " + path);
            }
            else
            {
                Gdx.app.log("SoundManager", "SFX non trovato (skip): " + path);
            }
        }
        catch (Exception e)
        {
            Gdx.app.error("SoundManager", "Errore caricamento SFX: " + path, e);
        }
    }
}
