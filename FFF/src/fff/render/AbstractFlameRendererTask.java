package fff.render;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Jeremiah N. Hankins
 */
public abstract class AbstractFlameRendererTask implements FlameRendererTask {
    private FlameRendererSettings settings;
    private List<FlameRendererListener> listeners;
    private boolean isCancled = false;
    
    public AbstractFlameRendererTask(FlameRendererSettings settings, FlameRendererListener... listeners) {
        this(settings, Arrays.asList(listeners));
    }
    
    public AbstractFlameRendererTask(FlameRendererSettings settings, List<FlameRendererListener> listeners) {
        if (settings == null)
            throw new IllegalArgumentException("settings is null");
        if (listeners == null)
            throw new IllegalArgumentException("listeners is null");
        this.settings = settings;
        this.listeners = listeners;
    }

    @Override
    public FlameRendererSettings getSettings() {
        return settings;
    }

    @Override
    public FlameRendererListener[] getListeners() {
        return listeners.toArray(new FlameRendererListener[0]);
    }

    @Override
    public void cancle() {
        isCancled = true;
    }

    @Override
    public boolean isCancled() {
        return isCancled;
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FlameRendererTask {\n");
        builder.append(settings).append("\n");
        builder.append("listeners:    ").append(listeners.size()).append("\n");
        builder.append("hasNextFlame: ").append(hasNextFlame()).append("\n");
        builder.append("isCancled:    ").append(isCancled).append("\n");
        builder.append("}");
        return builder.toString();
    }
    
}
