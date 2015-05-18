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
 *
 * @author Jeremiah N. Hankins
 */
public class SierpinskiDemo {
    
    
    public static void main(String[] args) {
        // Set the destination file name
        final String fileName = "SierpinskiDemo.png";
        
        // Catch any exceptions that occur so they can be displayed
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
                
                // This method is called asynchonously by the flame renderer
                // while
                @Override
                public void flameImageEvent(FlameRendererTask task, Flame flame, BufferedImage image, double quality, double points, double elapTime, boolean isFinished) {
                    
                    // Display progress updates
                    System.out.println(String.format("Drawn %.2fM dots at %.2fM dots/sec for quality of %.2f.", points/1e7, points/(1e7*elapTime), quality));
                    
                    if (isFinished) {
                        System.out.println("Writing PNG image file: "+fileName);
                        try {
                            ImageIO.write(image, "png", new File(fileName));
                        } catch (IOException ex) {
                            ex.printStackTrace(System.err);
                        }
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
        }
    }
}
