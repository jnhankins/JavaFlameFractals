package fff.render;

import fff.flame.Flame;
import java.awt.image.BufferedImage;

/**
 *
 * @author Jeremiah N. Hankins
 */
public interface FlameRendererListener {
    
    /**
     * This method is called by classes {@link fff.render.FlameRenderer FlameRenderer}. 
     * 
     * @param task
     * @param flame
     * @param image
     * @param quality
     * @param points
     * @param elapsedTime
     * @param isFinished 
     */
    public void flameImageEvent(FlameRendererTask task, Flame flame, BufferedImage image, double quality, double points, double elapsedTime, boolean isFinished);
}
