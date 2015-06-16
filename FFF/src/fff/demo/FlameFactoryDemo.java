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
import fff.render.BasicCallback;
import fff.render.FlameRenderer;
import fff.render.RendererCallback;
import fff.render.RendererSettings;
import fff.render.RendererTask;
import fff.render.RendererTaskSingle;
import fff.render.ocl.FlameRendererOpenCL;
import fff.render.ocl.FlameRendererOpenCL.DeviceType;
import java.util.Map;
import java.util.TreeMap;
import static fff.flame.FlameFactory.*;

/**
 * FlameFactoryDemo provides a main method which demonstrates the functionality
 * of {@code FlameFactory}.
 * 
 * @author Jeremiah N. Hankins
 */
public class FlameFactoryDemo {
    public static void main(String[] args) throws InterruptedException {
        // Create a map from using the names of flames as keys and the flame
        // instances as values
        Map<String, Flame> map = new TreeMap();
        map.put("SierpinskiTriangle",      newSierpinskiTriangle());
        map.put("SierpinskiCarpet",        newSierpinskiCarpet());
        map.put("SierpinskiPedalTriangle", newSierpinskiPedalTriangle(Math.toRadians(65), Math.toRadians(50)));
        map.put("SierpinksiNgon",          newSierpinksiNgon(5));
        map.put("DragonCurve",             newDragonCurve(0.5));
        map.put("GoldenDragonCurve",       newGoldenDragonCurve());
        map.put("TwindragonCurve",         newTwindragonCurve());
        map.put("TerdragonCurve",          newTerdragonCurve());
        map.put("KochCurve",               newKochCurve());
        map.put("KochSnowflake",           newKochSnowflake());
        map.put("Pentadentrite",           newPentadentrite());
        map.put("BinaryTree",              newBinaryTree(Math.toRadians(60), 0.6));
        map.put("LevyDragon",              newLevyDragon());
        map.put("PythagoreanTree",         newPythagoreanTree(Math.toRadians(60)));
        map.put("BarnsleyFern",            newBarnsleyFern());
        // Create the renderer
        FlameRenderer renderer = new FlameRendererOpenCL(DeviceType.ALL);
        // Set the update rate to 10 updates per second
        renderer.setUpdatesPerSec(10);
        // Start the renderer
        renderer.start();
        // Create the renderer settings
        RendererSettings settings = new RendererSettings();
        // For every flame...
        for (Map.Entry<String, Flame> entry : map.entrySet()) {
            // Get the name
            String name = entry.getKey();
            // Get the flame
            Flame flame = entry.getValue();
            // Create a basic callback using the name
            RendererCallback callback = new BasicCallback(name);
            // Create the task
            RendererTask task = new RendererTaskSingle(callback, settings, flame);
            // Enqueue the task
            renderer.enqueueTask(task);
        }
        // Tell the renderer to shutdown when all tasks are completed
        renderer.shutdown();
        // Wait for the tasks to comple and for the renderer to shutdown
        renderer.awaitTermination();
        // Rendering is complete
        System.out.println("\nDone");
    }
}
