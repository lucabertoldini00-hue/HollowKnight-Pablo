// Hitbox.java

package pablo.entities.combat;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import pablo.entities.enemies.Enemy;
import pablo.framework.BaseActor;

import java.util.HashSet;
import java.util.Set;

public class Hitbox extends BaseActor
{
    private final int      damage;
    private final float    lifetime;
    private float          timer;

    // Callback opzionale invocata una volta per ogni nemico colpito (es. gainSoul)
    private final Runnable onHit;

    // Impedisce di colpire lo stesso nemico più di una volta per hitbox
    private final Set<Enemy> hitEnemies = new HashSet<>();

    // -----------------------------------------------------------------------
    // Costruttore senza callback (compatibilità con il codice esistente)
    // -----------------------------------------------------------------------
    public Hitbox(float x, float y, float width, float height,
                  int damage, float lifetime, Stage stage)
    {
        this(x, y, width, height, damage, lifetime, stage, null);
    }

    // -----------------------------------------------------------------------
    // Costruttore con callback (usato da Pablo.spawnAttackHitbox)
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

        // DEBUG VISUAL: box rossa semitrasparente.
        // Commentare queste due righe una volta soddisfatti del posizionamento.
        loadTexture("assets/white.png");
        setColor(new Color(1f, 0f, 0f, 0.45f));
    }

    @Override
    public void act(float dt)
    {
        super.act(dt);

        // Scansiona tutti i nemici attualmente sulla Stage
        for (BaseActor actor : BaseActor.getList(getStage(), Enemy.class.getName()))
        {
            Enemy enemy = (Enemy) actor;

            if (!hitEnemies.contains(enemy) && overlaps(enemy))
            {
                enemy.takeDamage(damage);
                hitEnemies.add(enemy);

                // Notifica il chiamante (es. Pablo.gainSoul) — una volta per impatto
                if (onHit != null)
                    onHit.run();
            }
        }

        // Auto-distruzione alla scadenza del lifetime
        timer += dt;
        if (timer >= lifetime)
            remove();
    }
}