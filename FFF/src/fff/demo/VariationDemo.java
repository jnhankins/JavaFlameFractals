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
import fff.flame.Transform;
import fff.flame.VariationDefinition;
import fff.render.FlameRenderer;
import fff.render.FlameRendererCallback;
import fff.render.FlameRendererSettings;
import fff.render.FlameRendererTask;
import fff.render.FlameRendererTaskSingle;
import fff.render.ocl.FlameRendererOpenCL;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

/**
 * VariationDemo provides a main method which dynamically compiles a variation
 * from a source code string and renders a flame using newly compiled variation
 * and standard variations before saving the image to PNG file named 
 * "VariationDemo.png".
 * 
 * @author Jeremiah N. Hankins
 */
public class VariationDemo {
    
    public static void main(String[] args) {
        
        // The destination file name
        final String fileName = "VariationDemo.png";
        
        
        // Create the new variation definition
        VariationDefinition sincVariation = null;
        try {
            // Create a diagnostic listener
            DiagnosticListener<JavaFileObject> dl = new DiagnosticListener() {
                @Override
                public void report(Diagnostic d) {
                    System.out.println(d);
                }
            };
            // Compile the variation
            sincVariation = new VariationDefinition(
                "sinc",             // Name of the variation
                "X = sin(x)/x;\n" + 
                "Y = y;\n",  // Source code
                null,               // No parameters
                dl
            ); 
        } catch (Exception ex) {
            System.out.println("An error occured while attempting to compile the variation.");
            System.exit(0);
        }
        
        
        // Catch exceptions that occur so they can be displayed for debugging
        try {
            // Create the flame based on the sierpinski triangle
            Flame flame = Flame.newSierpinski();
            
            // Add a new transform using the sinc variation
            Transform xform = flame.addTransform();
            xform.addVariation(sincVariation);
            xform.removeVariation(VariationDefinition.LINEAR);

            // Change the final transform's variation from linear to swirl
            flame.getTransformFinal().addVariation(VariationDefinition.SWIRL);
            flame.getTransformFinal().removeVariation(VariationDefinition.LINEAR);
            
            // Display the flame's "genome"
            System.out.println(flame+"\n");
            
            // Create an OpenCL flame renderer
            FlameRenderer renderer = new FlameRendererOpenCL(FlameRendererOpenCL.DeviceType.ALL);
            renderer.setUpdatesPerSec(25);
            renderer.start();
            
            // Display the renderer's settings
            System.out.println(renderer+"\n");

            // Create the image settings
            FlameRendererSettings settings = new FlameRendererSettings();
            settings.setWidth(1920);
            settings.setHeight(1080);
            settings.setMaxQuality(200);
            
            // Display the image's settings
            System.out.println(settings+"\n");

            // Create the callback function
            FlameRendererCallback callback = new FlameRendererCallback() {
                // This method is called asynchonously by one of the flame 
                // renderer's interal threads after the enqueueTask() method
                // has been caled
                @Override
                public void flameRendererCallback(FlameRendererTask task, Flame flame, BufferedImage image, double quality, double points, double elapTime, boolean isFinished) {
                    
                    // Display progress updates
                    System.out.println(String.format("Drawn %.2fM dots in %.2f sec at %.2fM dots/sec for quality of %.2f.", points/1e7, elapTime, points/(1e7*elapTime), quality));
                    
                    // If the image is completed...
                    if (isFinished) {
                        
                        // Try to write the image as a PNG file
                        File file = new File(fileName);
                        try {
                            System.out.println("\nWriting PNG image file: "+file.getCanonicalPath());
                            ImageIO.write(image, "png", file);
                        } catch (IOException ex) {
                            ex.printStackTrace(System.err);
                            System.exit(0);
                        }
                        
                        // Rendering is complete, tell the program to exit
                        System.out.println("\nDone");
                    }
                };
            };

            // Create a render task to render the flame with the given settings and 
            // output listener
            FlameRendererTaskSingle task = new FlameRendererTaskSingle(callback, settings, flame);

            // Pass the rendering task to the flame renderer renderer
            // Work will begin immediatly
            renderer.getQueue().add(task);
            
            // Tell the renderer to shutdown when the task is complete
            renderer.shutdown();
        
        } catch (Exception ex) {
            // Print any exceptions that have been thrown to the err stream
            ex.printStackTrace(System.err);
            System.exit(0);
        }
    }
    
}
