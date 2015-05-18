package fff.render;

import fff.flame.Flame;

/**
 *
 * @author Jeremiah N. Hankins
 */
public interface FlameRendererTask {
    public boolean hasNextFlame();
    public Flame getNextFlame();
    
    public FlameRendererSettings getSettings();
    public FlameRendererListener[] getListeners();
    
    public void cancle();
    public boolean isCancled();
}
