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
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import javax.imageio.ImageIO;

/**
 * {@code BasicCallback} is a basic implementation  of {@link RendererCallback}
 * which prints updates to a print stream ({@code System.out} by default) and
 * writes out completed images as PNG files.
 * 
 * @author Jeremiah N. Hankins
 */
public class BasicCallback implements RendererCallback {
    /**
     * The root of the file name.
     */
    private final String fileNameRoot;
    
    /**
     * The number of digits in {@code flameCount}.
     * <p>
     * If {@code digits} is less than 1, then the first flame's file name will
     * not contain appended digits. If the number of digits in 
     * {@code flameCount} is greater than {@code digits}, the file name will
     * 
     * 
     * {@code digits} 
     */
    private final int zeroPadWidth;
    
    /**
     * The number of completed flames.
     */
    private int flameCount;
    
    /**
     * The output stream used to display updates.
     */
    private PrintStream printStream;
    
    /**
     * Constructs a new {@code BasicCallback}.
     * <p>
     * Equivalent to {@code BasicCallback(fileNameRoot, 0, System.out)}.
     *
     * @param fileNameRoot the root of the file name
     * @see #BasicCallback(java.lang.String, int, java.io.PrintStream) 
     */
    public BasicCallback(String fileNameRoot) {
        this(fileNameRoot, 0);
    }
    
    /**
     * Constructs a new {@code BasicCallback}.
     * <p>
     * Equivalent to
     * {@code BasicCallback(fileNameRoot, zeroPadWidth, System.out)}.
     *
     * @param fileNameRoot the root of the file name
     * @param zeroPadWidth width to which the number appended to the file name root will be zero-padded
     * @see #BasicCallback(java.lang.String, int, java.io.PrintStream) 
     */
    public BasicCallback(String fileNameRoot, int zeroPadWidth) {
        this(fileNameRoot, zeroPadWidth, System.out);
    }
    
    /**
     * Constructs a new {@code BasicCallback} using the specified file name
     * root, the specified width to which the number appended to the file name
     * root will be zero-padded, and specified print stream to which updates
     * will be printed.
     * <p>
     * If {@code fileNameRoot} is {@code null}, then completed images will not
     * be written as PNG files. If {@code zeroPadWidth} is less than {@code 1}
     * then first flame image will not have a number appended to its file name.
     * If {@code printStream} is {@code null}, then updates will not be
     * generated.
     * 
     * @param fileNameRoot the root of the file name
     * @param zeroPadWidth width to which the number appended to the file name root will be zero-padded
     * @param printStream the stream to which updates will be printed, or {@code null}
     */
    public BasicCallback(String fileNameRoot, int zeroPadWidth, PrintStream printStream) {
        // Store the file name root, digits, and print stream
        this.fileNameRoot = fileNameRoot;
        this.zeroPadWidth = zeroPadWidth;
        this.printStream = printStream;
        // Store the current flame count
        flameCount = 0;
    }
    
    @Override
    public void flameRendererCallback(RendererTask task, Flame flame, BufferedImage image, double quality, double points, double elapTime, boolean isFinished) {
        // Display progress updates
        if (printStream != null)
            printStream.println(String.format("Drawn %.2fM dots in %.2f sec at %.2fM dots/sec for quality of %.2f.", points/1e7, elapTime, points/(1e7*elapTime), quality));
        // If the image is completed...
        if (isFinished && fileNameRoot != null) {
            try {
                // Increment the flameCount
                flameCount++;
                // Construct the file name
                String fileName = fileNameRoot;
                if (!(zeroPadWidth < 1 && flameCount == 1))
                    fileName += String.format("%0"+Math.max(zeroPadWidth,1)+"d", flameCount);
                fileName += ".png";
                // Create the file
                File file = new File(fileName);
                // Display the file name
                if (printStream != null)
                    printStream.println("Writing PNG image file: "+file.getCanonicalPath());
                // Write the file
                ImageIO.write(image, "png", file);
                // If there was an error...
            } catch (IOException ex) {
                // Print the stacktrace
                ex.printStackTrace(System.err);
                // Cancle the task
                task.cancel(true);
            }
        }
    }
}
