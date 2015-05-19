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
