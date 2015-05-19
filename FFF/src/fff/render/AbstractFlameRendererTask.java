/**
 * FastFlameFractals (FFF)
 * A library for rendering flame fractals asynchronously using Java and OpenCL.
 * 
 * Copyright (c) 2015 Jeremiah N. Hankins
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
