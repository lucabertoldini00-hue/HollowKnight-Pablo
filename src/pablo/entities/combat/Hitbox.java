// Hitbox.java — modificato per integrare SoundManager
// DIFF: aggiunta chiamata SFX ENEMY_HIT quando un nemico viene colpito.

package pablo.entities.combat;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import pablo.SoundManager;
import pablo.SoundManager.Sfx;
import pablo.entities.enemies.Enemy;
import pablo.framework.BaseActor;

import java.util.HashSet;
import java.util.Set;

public class Hitbox extends BaseActor
{
    private final int      damage;
    private final float    lifetime;
    private float          timer;

    private final Runnable onHit;
    private final Set<Enemy> hitEnemies = new HashSet<>();

    // -----------------------------------------------------------------------
    // Costruttore senza callback
    // -----------------------------------------------------------------------
    public Hitbox(float x, float y, float width, float height,
                  int damage, float lifetime, Stage stage)
    {
        this(x, y, width, height, damage, lifetime, stage, null);
    }

    // -----------------------------------------------------------------------
    // Costruttore con callback
    // -----------------------------------------------------------------------
    public Hitbox(float x, float y, float width, float height,
                  int damage, float lifetime, Stage stage, Runnable onHit)
    {
        super(x, y, stage);
        setSize(width, height);
        setBoundaryRectangle();

        this.damage   = damage;
        this.lifetime = lifetime;
        this.timer    = 0f;
        this.onHit    = onHit;

        loadTexture("assets/white.png");
        setColor(new Color(1f, 0f, 0f, 0.45f));
    }

    @Override
    public void act(float dt)
    {
        super.act(dt);

        for (BaseActor actor : BaseActor.getList(getStage(), Enemy.class.getName()))
        {
            Enemy enemy = (Enemy) actor;

            if (!hitEnemies.contains(enemy) && overlaps(enemy))
            {
                enemy.takeDamage(damage);
                hitEnemies.add(enemy);

                // SFX colpo — usa BOSS_HIT se il nemico ha vita > 10 (euristica semplice)
                SoundManager.get().playSfx(Sfx.ENEMY_HIT);

                if (onHit != null)
                    onHit.run();
            }
        }

        timer += dt;
        if (timer >= lifetime)
            remove();
    }
}