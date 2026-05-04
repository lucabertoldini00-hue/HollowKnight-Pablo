// Hitbox.java

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
    // Set to true to see the hitbox rectangle during development
    private static final boolean DEBUG_DRAW = false;

    private final int      damage;
    private final float    lifetime;
    private float          timer;

    private final Runnable onHit;
    private final Set<Enemy> hitEnemies = new HashSet<>();

    public Hitbox(float x, float y, float width, float height,
                  int damage, float lifetime, Stage stage)
    {
        this(x, y, width, height, damage, lifetime, stage, null);
    }

    public Hitbox(float x, float y, float width, float height,
                  int damage, float lifetime, Stage stage, Runnable onHit)
    {
        super(x, y, stage);

        // Set the physical collision area first
        setSize(width, height);
        setBoundaryRectangle();

        this.damage   = damage;
        this.lifetime = lifetime;
        this.timer    = 0f;
        this.onHit    = onHit;

        // loadTexture calls setAnimation which resets actor size to the texture dimensions.
        // Re-apply width/height immediately after so the visual matches the collision area.
        loadTexture("assets/white.png");
        setSize(width, height);  // ← the actual fix: restore correct size after loadTexture

        if (DEBUG_DRAW)
            setColor(new Color(1f, 0f, 0f, 0.45f));
        else
            setVisible(false);
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
                SoundManager.get().playSfx(Sfx.ENEMY_HIT);
                if (onHit != null) onHit.run();
            }
        }

        timer += dt;
        if (timer >= lifetime)
            remove();
    }
}