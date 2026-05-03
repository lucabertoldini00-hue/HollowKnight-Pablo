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
    private final Runnable onHitCallback; // chiamato una volta per nemico colpito

    private final Set<Enemy> hitEnemies = new HashSet<>();

    // Costruttore senza callback (retrocompatibile)
    public Hitbox(float x, float y, float width, float height,
                  int damage, float lifetime, Stage stage)
    {
        this(x, y, width, height, damage, lifetime, stage, null);
    }

    // Costruttore con callback — onHitCallback.run() chiamato ad ogni nemico colpito
    public Hitbox(float x, float y, float width, float height,
                  int damage, float lifetime, Stage stage, Runnable onHitCallback)
    {
        super(x, y, stage);
        setSize(width, height);
        setBoundaryRectangle();

        this.damage          = damage;
        this.lifetime        = lifetime;
        this.timer           = 0f;
        this.onHitCallback   = onHitCallback;

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

                // Notifica Pablo (o chiunque altro) del colpo andato a segno
                if (onHitCallback != null)
                    onHitCallback.run();
            }
        }

        timer += dt;
        if (timer >= lifetime)
            remove();
    }
}