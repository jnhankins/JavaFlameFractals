/**
 * JavaFlameFractals (JFF)
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

package com.jnhankins.jff.demo;

import com.jnhankins.jff.flame.Flame;
import com.jnhankins.jff.flame.FlameFactory;
import com.jnhankins.jff.flame.Transform;
import com.jnhankins.jff.flame.VariationDefinition;
import com.jnhankins.jff.render.BasicCallback;
import com.jnhankins.jff.render.FlameRenderer;
import com.jnhankins.jff.render.RendererCallback;
import com.jnhankins.jff.render.RendererSettings;
import com.jnhankins.jff.render.RendererTaskSingle;
import com.jnhankins.jff.render.ocl.FlameRendererOpenCL;
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
            Flame flame = FlameFactory.newSierpinskiTriangle();
            
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
            // Set the update rate to 25 updates per second
            renderer.setUpdatesPerSec(25);
            // Start the renderer
            renderer.start();
            
            // Display the renderer's settings
            System.out.println(renderer+"\n");

            // Create the image settings
            RendererSettings settings = new RendererSettings();
            // Set the image size to 1920x1080
            settings.setWidth(1920).setHeight(1080);
            // Set the image "quality" to 200
            settings.setMaxQuality(200);
            
            // Display the image's settings
            System.out.println(settings+"\n");

            // Create the callback function, ouputs to VariationDemo.png
            RendererCallback callback = new BasicCallback("VariationDemo");

            // Create a task to render the flame with the given settings and
            // callback function
            RendererTaskSingle task = new RendererTaskSingle(callback, settings, flame);

            // Pass the rendering task to the flame renderer renderer
            // Work will begin immediatly
            renderer.enqueueTask(task);
            
            // Tell the renderer to shutdown when the task is complete
            renderer.shutdown();
            
            // Wait for the task to comple and for the renderer to shutdown
            renderer.awaitTermination();
            
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
