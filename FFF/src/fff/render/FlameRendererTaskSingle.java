package fff.render;

import fff.flame.Flame;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Jeremiah N. Hankins
 */
public class FlameRendererTaskSingle extends AbstractFlameRendererTask {
    private Flame flame;
    private boolean hasNextFlame = true;
    
    public FlameRendererTaskSingle(Flame flame, FlameRendererSettings settings, FlameRendererListener... listeners) {
        this(flame, settings, Arrays.asList(listeners));
    }
    
    public FlameRendererTaskSingle(Flame flame, FlameRendererSettings settings, List<FlameRendererListener> listeners) {
        super(settings, listeners);
        if (flame == null)
            throw new IllegalArgumentException("flame is null");
        this.flame = flame;
    }
    
    @Override
    public boolean hasNextFlame() {
        return hasNextFlame;
    }

    @Override
    public Flame getNextFlame() {
        hasNextFlame = false;
        return flame;
    }
}
