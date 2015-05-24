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
import java.awt.image.BufferedImage;

/**
 * An interface providing a method for asynchronous communication between a
 * {@link FlameRenderer} and the rest of the program.
 * 
 * @author Jeremiah N. Hankins
 */
public interface FlameRendererCallback {
    /**
     * Called asynchronously by a {@link FlameRenderer} to deliver progress
     * updates and final results while rendering a flame image.
     * 
     * @param task the task which generated the flame
     * @param flame the flame being rendered
     * @param image the current image of the flame
     * @param quality the quality of the image
     * @param points the number of points plotted to render the image
     * @param elapsedTime the amount of time in seconds spent rendering the image
     * @param isFinished {@code true} if the image is finished, {@code false} if work on the image is ongoing
     */
    public void flameRendererCallback(
            FlameRendererTask task, 
            Flame flame, 
            BufferedImage image, 
            double quality, 
            double points, 
            double elapsedTime, 
            boolean isFinished);
}
