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

package fff.demo;

import fff.flame.Flame;
import fff.render.*;
import fff.render.ocl.FlameRendererOpenCL;
import fff.render.ocl.FlameRendererOpenCL.DeviceType;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * HelloWorldDemo provides a main method which renders a
 * <a href="https://en.wikipedia.org/wiki/Sierpinski_triangle">Sierpinski
 * triangle (wiki)</a>and writes the image as a PNG file named
 * "HelloWorldDemo.png" using very few lines of code.
 * 
 * @author Jeremiah N. Hankins
 */
public class HelloWorldDemo implements FlameRendererCallback {
    public static void main(String[] args) {
        // Create the renderer
        FlameRenderer renderer = new FlameRendererOpenCL(DeviceType.ALL);
        // Add a new flame rendering task to the queue
        renderer.getQueue().add(
            new FlameRendererTaskSingle( // That renders a single flame
                new HelloWorldDemo(),        // Using HelloWorldDemo's callback function
                new FlameRendererSettings(), // Using default image settings
                Flame.newSierpinski()        // Rendering a Sierpinski Triangle
            ));
        // Begin rendering
        renderer.start();
        // Shutdown when finished
        renderer.shutdown();
    }
    
    @Override
    public void flameRendererCallback(FlameRendererTask task, Flame flame, BufferedImage image, double quality, double points, double elapTime, boolean isFinished) {
        // When the image is finished, save it to file HelloWorldDemo.png
        if (isFinished) {
            try {
                ImageIO.write(image, "png", new File("HelloWorldDemo.png"));
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
                System.exit(0);
            }
        }
    };
}