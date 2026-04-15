// Oggetto.java

package pablo;

import com.badlogic.gdx.scenes.scene2d.Stage;
import pablo.framework.BaseActor;

public class Object extends BaseActor {
    private boolean enable;

    public Object(float x, float y, float width, float height, Stage stage) {
        super(x,y,stage);
        setSize(width,height);
        setBoundaryRectangle();
        enable = true;

    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
    public boolean isEnable() {
        return enable;
    }
}

