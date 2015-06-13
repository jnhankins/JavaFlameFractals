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
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package fff.demo;

import fff.flame.Flame;
import fff.flame.FlameFactory;
import fff.render.BasicCallback;
import fff.render.FlameRenderer;
import fff.render.RendererCallback;
import fff.render.RendererSettings;
import fff.render.RendererTaskSingle;
import fff.render.ocl.FlameRendererOpenCL;
import fff.render.ocl.FlameRendererOpenCL.DeviceType;

/**
 * SierpinskiDemo provides a main method which renders a
 * <a href="https://en.wikipedia.org/wiki/Sierpinski_triangle">Sierpinski
 * triangle (wiki)</a>and writes the image as a PNG file named
 * "SierpinskiDemo.png".
 * 
 * @author Jeremiah N. Hankins
 */
public class SierpinskiDemo {
    
    public static void main(String[] args) {
        // Catch exceptions that occur so they can be displayed for debugging
        try {
            
            // Create an OpenCL flame renderer
            FlameRenderer renderer = new FlameRendererOpenCL(DeviceType.ALL);
            renderer.setUpdatesPerSec(25);
            renderer.start();
            
            // Display the renderer's settings
            System.out.println(renderer+"\n");

            // Create the image settings
            RendererSettings settings = new RendererSettings();
            settings.setWidth(1920);
            settings.setHeight(1080);
            settings.setMaxQuality(200);
            
            // Display the image's settings
            System.out.println(settings+"\n");

            // Create the flame
            Flame flame = FlameFactory.newSierpinskiTriangle();
            
            // Display the flame's "genome"
            System.out.println(flame+"\n");

            // Create the callback function object
            RendererCallback callback = new BasicCallback("SierpinskiDemo", 0);

            // Create a task to render the flame with the given settings and
            // callback function
            RendererTaskSingle task = new RendererTaskSingle(callback, settings, flame);

            // Pass the rendering task to the flame renderer renderer
            // Work will begin immediatly
            renderer.getQueue().add(task);
            
            // Tell the renderer to shutdown when the task is complete
            renderer.shutdown();
            
            // Wait for the task to comple and for the renderer to shutdown
            renderer.awaitTermination(Long.MAX_VALUE);
            
            // Rendering is complete
            System.out.println("\nDone");
            
        } catch (Exception ex) {
            
            // Print any exceptions that have been thrown to the err stream
            ex.printStackTrace(System.err);
            
            // Invoke the renderer's shutdown hook
            System.exit(0);
        }
    }
}