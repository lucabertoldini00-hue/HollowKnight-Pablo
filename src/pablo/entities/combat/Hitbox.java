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
    private final int   damage;
    private final float lifetime;
    private float       timer;

    // prevents the same enemy from being hit more than once by this hitbox
    private final Set<Enemy> hitEnemies = new HashSet<>();

    public Hitbox(float x, float y, float width, float height,
                  int damage, float lifetime, Stage stage)
    {
        super(x, y, stage);
        setSize(width, height);
        setBoundaryRectangle();

        this.damage   = damage;
        this.lifetime = lifetime;
        this.timer    = 0f;

        // --- DEBUG VISUAL: semi-transparent red box ---
        // Comment these two lines out once you're happy with positioning
        loadTexture("assets/white.png");
        setColor(new Color(1f, 0f, 0f, 0.45f));
        // -----------------------------------------------
    }

    @Override
    public void act(float dt)
    {
        super.act(dt);

        // scan every enemy currently on the Stage
        for (BaseActor actor : BaseActor.getList(getStage(), Enemy.class.getName()))
        {
            Enemy enemy = (Enemy) actor;

            if (!hitEnemies.contains(enemy) && overlaps(enemy))
            {
                enemy.takeDamage(damage);
                hitEnemies.add(enemy);   // never hit this enemy again
            }
        }

        // self-destruct — removes this actor from the Stage cleanly
        timer += dt;
        if (timer >= lifetime)
            remove();
    }
}