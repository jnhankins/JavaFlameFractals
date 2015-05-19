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

package fff;

import fff.flame.Flame;
import fff.render.FlameRenderer;
import fff.render.FlameRendererListener;
import fff.render.FlameRendererSettings;
import fff.render.FlameRendererTask;
import fff.render.FlameRendererTaskSingle;
import fff.render.ocl.FlameRendererOpenCL;
import fff.render.ocl.FlameRendererOpenCL.DeviceType;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * SierpinskiDemo provides a main method which renders a Sierpinski triangle 
 * (https://en.wikipedia.org/wiki/Sierpinski_triangle) and writes the image as
 * a PNG file named "SierpinskiDemo.png".
 * 
 * @author Jeremiah N. Hankins
 */
public class SierpinskiDemo {
    
    public static void main(String[] args) {
        
        // The destination file name
        final String fileName = "SierpinskiDemo.png";
        
        // Catch exceptions that occur so they can be displayed for debugging
        try {
            
            // Create an OpenCL flame renderer
            FlameRenderer renderer = new FlameRendererOpenCL(DeviceType.ALL);
            renderer.setUpdateImages(true);
            System.out.println(renderer);

            // Create the image settings
            FlameRendererSettings settings = new FlameRendererSettings();
            settings.setWidth(1920);
            settings.setHeight(1080);
            settings.setMaxQuality(200);
            System.out.println(settings);

            // Create the flame
            Flame flame = Flame.newSierpinski();
            System.out.println(flame);

            // Create an output listener
            FlameRendererListener listener = new FlameRendererListener() {
                
                // This method is called asynchonously by one of the flame 
                // renderer's interal threads after the enqueueTask() method
                // has been caled
                @Override
                public void flameImageEvent(FlameRendererTask task, Flame flame, BufferedImage image, double quality, double points, double elapTime, boolean isFinished) {
                    
                    // Display progress updates
                    System.out.println(String.format("Drawn %.2fM dots at %.2fM dots/sec for quality of %.2f.", points/1e7, points/(1e7*elapTime), quality));
                    
                    // If the image is completed...
                    if (isFinished) {
                        
                        // Try to write the image as a PNG file
                        System.out.println("Writing PNG image file: "+fileName);
                        try {
                            ImageIO.write(image, "png", new File(fileName));
                        } catch (IOException ex) {
                            ex.printStackTrace(System.err);
                            System.exit(0);
                        }
                        
                        // Rendering is complete, tell the program to exit
                        System.out.println("Done");
                        System.exit(0);
                    }
                };
            };

            // Create a render task to render the flame with the given settings and 
            // output listener
            FlameRendererTaskSingle task = new FlameRendererTaskSingle(flame, settings, listener);

            // Pass the rendering task to the flame renderer renderer
            renderer.enqueueTask(task);
        
        } catch (Exception ex) {
            // Print any exceptions that have been thrown to the err stream
            ex.printStackTrace(System.err);
            System.exit(0);
        }
    }
}