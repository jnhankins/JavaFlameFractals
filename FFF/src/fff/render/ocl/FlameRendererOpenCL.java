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

package fff.render.ocl;

import fff.flame.Flame;
import fff.flame.VariationDefinition;
import fff.render.FlameRenderer;
import fff.render.FlameRendererCallback;
import fff.render.FlameRendererSettings;
import fff.render.FlameRendererTask;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import org.jocl.*;
import static org.jocl.CL.*;

/**
 *
 * @author Jeremiah N. Hankins
 */
public class FlameRendererOpenCL extends FlameRenderer {
    // Progam Code Templates
    private static final String oclVariationCodeTemplatePath = "OCLVariationTemplate.cl";
    private static final String oclProgramCodeTemplatePath = "OCLProgramTemplate.cl";
    private static String oclVariationCodeTemplate;
    private static String oclProgramCodeTemplate;
    
    // OpenCL Platform and Device Info
    private final int platformIndex;
    private final int deviceIndex;
    private final cl_platform_id platform;
    private final cl_device_id device;
    
    // OpenCL Context and Command Queue
    private cl_context context;
    private cl_command_queue queue;
    
    // OpenCL Program
    private final Set<VariationDefinition> variationSet = new TreeSet();
    private cl_program program;
    private cl_kernel initKernel;
    private cl_kernel warmKernel;
    private cl_kernel plotKernel;
    private cl_kernel previewKernel;
    private cl_kernel finishKernel1;
    private cl_kernel finishKernel2;
    
    // Worksize
    private int workSize = -1;
    private long[] workOffset = new long[1];
    private long[] workSizePlot = new long[1];
    private long[] workSizeColor = new long[1];
    
    // Iteration Buffers
    private int workCapacity = -1;
    private cl_mem iterRStateMem;
    private cl_mem iterPointsMem;
    private cl_mem iterColorsMem;
    
    // Xform Buffers
    private int xformCapacity = -1;
    private int xformVariationsCapacity = -1;
    private int xformParametersCapacity = -1;
    private cl_mem xformWeightMem;
    private cl_mem xformCmixesMem;
    private cl_mem xformColorsMem;
    private cl_mem xformAffineMem;
    private cl_mem xformVariationsMem;
    private cl_mem xformParametersMem;
    
    // Flame Buffers
    private cl_mem flameViewAffineMem;
    private cl_mem flameColorationMem;
    private cl_mem flameBackgroundMem;
    
    // Blur Param Buffer
    private cl_mem blurParamMem;
    
    // Image Buffers
    private int imageCapacity = -1;
    private cl_mem histogramMem;
    private cl_mem preRasterMem;
    private cl_mem imgRasterMem;
    
    // Hit-Counts Buffer
    private cl_mem hitcountsMem;
    
    // Bufferd Images
    private BufferedImage frontImage;
    private BufferedImage backImage;
    
    // The preview time
    private long updateInterval;
    private long preUpdateTime;
    
    // Current flame, settings, and listeners
    private FlameRendererTask task;
    private Flame flame;
    private FlameRendererSettings settings;
    private FlameRendererCallback callback;
    
    static {
        // Enable exceptions
        setExceptionsEnabled(true);
    }
    
    /**
     * Constructs a new {@code FlameRendererOpenCL} that attempts to aquire
     * an OpenCL device of the given type.
     * 
     * @param type the OpenCL device type (e.g. CPU, GPU, ...)
     * @throws IllegalArgumentException if no suitable device can be found
     */
    public FlameRendererOpenCL(DeviceType type) {
        // Get the platforms
        int numPlatforms[] = new int[1];
        clGetPlatformIDs(0, null, numPlatforms);
        cl_platform_id platforms[] = new cl_platform_id[numPlatforms[0]];
        clGetPlatformIDs(platforms.length, platforms, null);
        // Keep track of which device & platform is the fastest
        int bestPlatformIndex = -1;
        int bestDeviceIndex = -1;
        cl_platform_id bestPlatform = null;
        cl_device_id bestDevice = null;
        int bestSpeed = -1;
        // For each platform
        for (int platformI=0; platformI<platforms.length; platformI++) {
            cl_platform_id currPlatform = platforms[platformI];
            // Get all devices
            cl_device_id[] devices = getAllDevices(currPlatform);
            // For each device
            for (int deviceI=0; deviceI<devices.length; deviceI++) {
                cl_device_id currDevice = devices[deviceI];
                // Skip this device if it is not the correct type
                if ((getDeviceLong(currDevice, CL_DEVICE_TYPE) & type.type) == 0)
                    continue;
                // Determine the speed of the device
                int freq = getDeviceInt(currDevice, CL_DEVICE_MAX_CLOCK_FREQUENCY);
                int unit = getDeviceInt(currDevice, CL_DEVICE_MAX_COMPUTE_UNITS);
                int speed = freq*unit;
                // Keep track of the fastest device and platform
                if (bestSpeed < speed) {
                    bestPlatformIndex = platformI;
                    bestDeviceIndex = deviceI;
                    bestPlatform = currPlatform;
                    bestDevice = currDevice;
                    bestSpeed = speed;
                }
            }
        }
        // If no suitable platform and device was found, throw an exception
        if (bestPlatform == null || bestDevice == null)
            throw new RuntimeException("Could not find a suitable platform and device of type: "+type.name());
        // Store the platform and device info
        platformIndex = bestPlatformIndex;
        deviceIndex = bestDeviceIndex;
        platform = bestPlatform;
        device = bestDevice;
    }
    
    /**
     * Constructs a new {@code FlameRendererOpenCL} that attempts to aquire
     * the OpenCL platform and device with the given indexes.
     * 
     * @param platformIndex the index of the platform
     * @param deviceIndex the index of the device
     * @throws IllegalArgumentException if the platform and device index pair is invalid
     */
    public FlameRendererOpenCL(int platformIndex, int deviceIndex) {
        // Store the platform and device indexes
        this.platformIndex = platformIndex;
        this.deviceIndex = deviceIndex;
        // Obtain the platform
        cl_platform_id platforms[] = getAllPlatforms();
        if (!(0<=platformIndex && platformIndex<platforms.length))
            throw new IllegalArgumentException("Invalid platform and device index pair: "+platformIndex+" "+deviceIndex);
        platform = platforms[platformIndex];
        // Obtain the device
        cl_device_id devices[] = getAllDevices(platform);
        if (!(0<=deviceIndex && deviceIndex<devices.length))
            throw new IllegalArgumentException("Invalid platform and device index pair: "+platformIndex+" "+deviceIndex);
        device = devices[deviceIndex];
    }
    
    /** 
     * Returns the index of the OpenCL platform being used by this 
     * {@code FlameRendererOpenCL} object.
     * 
     * @return the index of the OpenCL platform being used by this 
     * {@code FlameRendererOpenCL} object.
     */
    public int getPlatformIndex() {
        return platformIndex;
    }
    
    /** 
     * Returns the index of the OpenCL device being used by this 
     * {@code FlameRendererOpenCL} object.
     * 
     * @return the index of the OpenCL device being used by this 
     * {@code FlameRendererOpenCL} object
     */
    public int getDeviceIndex() {
        return deviceIndex;
    }
    
    /** 
     * Returns the {@code String} name of the OpenCL platform being used by this 
     * {@code FlameRendererOpenCL} object.
     * 
     * @return the {@code String} name of the OpenCL platform being used by this 
     * {@code FlameRendererOpenCL} object
     */
    public String getPlatformName() {
        return getPlatformString(platform, CL_PLATFORM_NAME);
    }
    
    /** 
     * Returns the {@code String} name of the OpenCL device being used by this 
     * {@code FlameRendererOpenCL} object.
     * 
     * @return the {@code String} name of the OpenCL device being used by this 
     * {@code FlameRendererOpenCL} object
     */
    public String getDeviceName() {
        return getDeviceString(device, CL_DEVICE_NAME);
    }
    
    /**
     * Returns the maximum clock frequency for the OpenCL device being used by
     * this {@code FlameRendererOpenCL} object.
     *
     * @return the maximum clock frequency for the OpenCL device being used by
     * this {@code FlameRendererOpenCL} object.
     */
    public int getDeviceMaxClockFrequency() {
        return getDeviceInt(device, CL_DEVICE_MAX_CLOCK_FREQUENCY);
    }
    
    /** 
     * Returns the maximum number of avaiable compute units for the OpenCL
     * device being used by this {@code FlameRendererOpenCL} object.
     * 
     * @return the maximum number of avaiable compute units for the OpenCL
     * device being used by this {@code FlameRendererOpenCL} object.
     */
    public int getDeviceMaxComputeUnits() {
        return getDeviceInt(device, CL_DEVICE_MAX_COMPUTE_UNITS);
    }

    @Override
    public void setUpdatesPerSec(double updatesPerSecond) {
        super.setUpdatesPerSec(updatesPerSecond);
        if (updatesPerSecond <= 0) {
            updateInterval = 0;
        } else {
            updateInterval = Math.max((long)(1e9/updatesPerSecond), 1);
        }
    }
    
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("Flame Renderer {\n");
        str.append("   Status: ");
        if (isRunning())
            str.append("RUNNING\n");
        else if (isShutdown())
            str.append("SHUTDOWN\n");
        else if (isTerminated())
            str.append("TERMINATED\n");
        else
            str.append("READY\n");
        str.append("   Type: OpenCL\n");
        str.append("   CL_PLATFORM_INDEX:   ").append(platformIndex).append('\n');
        str.append("   CL_PLATFORM_NAME:    ").append(getPlatformName()).append('\n');
        str.append("   CL_PLATFORM_VERSION: ").append(getPlatformString(platform, CL_PLATFORM_VERSION)).append('\n');
        str.append("   CL_DEVICE_INDEX:     ").append(platformIndex).append('\n');
        str.append("   CL_DEVICE_NAME:      ").append(getDeviceName()).append('\n');
        str.append("   CL_DEVICE_MAX_CLOCK_FREQUENCY: ").append(getDeviceMaxClockFrequency()).append('\n');
        str.append("   CL_DEVICE_MAX_COMPUTE_UNITS:   ").append(getDeviceMaxComputeUnits()).append('\n');
        str.append("   updatesPerSec: ").append(getUpdatesPerSec()).append('\n');
        str.append("   updateImages:  ").append(getUpdateImages()).append('\n');
        str.append("   isBatchAccelerated: ").append(this.isBatchAccerlated()).append('\n');
        str.append("   maxBatchTime:       ").append(this.getMaxBatchTimeSec()).append('\n');
        str.append("}");
        return str.toString();
    }
    
    @Override
    protected void initResources() {
        // Initialize code templates
        loadCodeTempaltes();
        // Create the context
        cl_context_properties contextProps = new cl_context_properties();
        contextProps.addProperty(CL_CONTEXT_PLATFORM, platform);
        context = clCreateContext(contextProps, 1, new cl_device_id[]{device}, null, null, null);
        // Create the command-queue
        queue = clCreateCommandQueue(context, device, 0, null);
        // Create fixed-size mem objets
        flameViewAffineMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR, 6*Sizeof.cl_float, null, null);
        flameColorationMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR, 3*Sizeof.cl_float, null, null);
        flameBackgroundMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR, 4*Sizeof.cl_float, null, null);
        blurParamMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR, 3*Sizeof.cl_float, null, null);
        hitcountsMem = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_ALLOC_HOST_PTR, 2*Sizeof.cl_int, null, null);
    }
    
    private void loadCodeTempaltes() {
        // Load the OpenCL source code templates
        try {
            URL url;
            String temp;
            
            // Load the variation tempalte code
            if (oclVariationCodeTemplate == null) {
                url = getClass().getResource(oclVariationCodeTemplatePath);
                if (url == null)
                    throw new FileNotFoundException("Could not find file: "+oclVariationCodeTemplatePath);
                temp = new String(Files.readAllBytes(Paths.get(url.toURI())));
                temp = temp.replaceAll("\r\n","\n");       // Remove redundent carriage returns
                temp = temp.replaceAll("\r","\n");         // Convert the remaining carriage returns to newlines
                temp = temp.substring(temp.indexOf("//")); // Remove header
                oclVariationCodeTemplate = temp;
            }
            
            // Load the kenel template code
            if (oclProgramCodeTemplate ==  null) {
                url = getClass().getResource(oclProgramCodeTemplatePath);
                if (url == null)
                    throw new FileNotFoundException("Could not find file: "+oclVariationCodeTemplatePath);
                temp = new String(Files.readAllBytes(Paths.get(url.toURI())));
                temp = temp.replaceAll("\r\n","\n");  // Remove redundent carriage returns
                temp = temp.replaceAll("\r","\n");    // Convert the remaining carriage returns to newlines
                oclProgramCodeTemplate = temp;
            }
        } catch (URISyntaxException | IOException ex) {
            // Should never happen unless the user deletes or renames the 
            // OpenCL template files.
            throw new RuntimeException(ex);
        }
    }

    @Override
    protected void freeResources() {
        // Work Size
        workSize = -1;

        // Iteration Buffers
        workCapacity = -1;
        if (iterRStateMem != null) { clReleaseMemObject(iterRStateMem); iterRStateMem = null; }
        if (iterPointsMem != null) { clReleaseMemObject(iterPointsMem); iterPointsMem = null; }
        if (iterColorsMem != null) { clReleaseMemObject(iterColorsMem); iterColorsMem = null; }

        // Xform Buffers
        xformCapacity = -1;
        xformVariationsCapacity = -1;
        xformParametersCapacity = -1;
        if (xformWeightMem != null) { clReleaseMemObject(xformWeightMem); xformWeightMem = null; }
        if (xformCmixesMem != null) { clReleaseMemObject(xformCmixesMem); xformCmixesMem = null; }
        if (xformColorsMem != null) { clReleaseMemObject(xformColorsMem); xformColorsMem = null; }
        if (xformAffineMem != null) { clReleaseMemObject(xformAffineMem); xformAffineMem = null; }
        if (xformVariationsMem != null) { clReleaseMemObject(xformVariationsMem); xformVariationsMem = null; }
        if (xformParametersMem != null) { clReleaseMemObject(xformParametersMem); xformParametersMem = null; }

        // Flame Buffers
        if (flameViewAffineMem != null) { clReleaseMemObject(flameViewAffineMem); flameViewAffineMem = null; }
        if (flameColorationMem != null) { clReleaseMemObject(flameColorationMem); flameColorationMem = null; }
        if (flameBackgroundMem != null) { clReleaseMemObject(flameBackgroundMem); flameBackgroundMem = null; }
        
        // Blur Param Buffer
        if (blurParamMem != null) { clReleaseMemObject(blurParamMem); blurParamMem = null; }
        
        // Image Buffers
        imageCapacity = -1;
        if (histogramMem != null) { clReleaseMemObject(histogramMem); histogramMem = null; }
        if (preRasterMem != null) { clReleaseMemObject(preRasterMem); preRasterMem = null; }
        if (imgRasterMem != null) { clReleaseMemObject(imgRasterMem); imgRasterMem = null; }

        // Hit-Counts Buffer
        if (hitcountsMem != null) { clReleaseMemObject(hitcountsMem); hitcountsMem = null; }

        // Bufferd Images
        frontImage = null;
        backImage = null;
        
        // OpenCL Program
        variationSet.clear();
        if (initKernel != null) { clReleaseKernel(initKernel); initKernel = null; }
        if (warmKernel != null) { clReleaseKernel(warmKernel); warmKernel = null; }
        if (plotKernel != null) { clReleaseKernel(plotKernel); plotKernel = null; }
        if (previewKernel != null) { clReleaseKernel(previewKernel); previewKernel = null; }
        if (finishKernel1 != null) { clReleaseKernel(finishKernel1); finishKernel1 = null; }
        if (finishKernel2 != null) { clReleaseKernel(finishKernel2); finishKernel2 = null; }
        if (program != null) { clReleaseProgram(program); program = null; }
        
        // OpenCL Data
        if (queue != null) { clReleaseCommandQueue(queue); queue = null; }
        if (context != null) { clReleaseContext(context); context = null; }
    }
    
    @Override
    protected void renderNextFlame(FlameRendererTask task) {
        // Store the task
        this.task = task;
        callback = task.getCallback();
        settings = task.getSettings();
        flame = task.getNextFlame();
        
        workSizeColor[0] = settings.getWidth()*settings.getHeight();
        
        // Prepare the OpenCL program and kernels
        prepCLProgram();
        
        // Prep flame buffers
        prepFlameBuffers();
        // Prep iter buffers
        prepIterBuffers();
        // Prep image buffers
        prepImageBuffers();
        // Prep hit coutners buffer
        prepHitCountsBuffer();
        
        // Run the init kernel
        clEnqueueNDRangeKernel(queue, initKernel, 1, workOffset, workSizePlot, null, 0, null, null);
        
        // Run the warmup kernel
        int[] numTransforms = new int[] { flame.getNumTransforms() };
        clSetKernelArg(warmKernel, 0, Sizeof.cl_int, Pointer.to(numTransforms));
        clEnqueueNDRangeKernel(queue, warmKernel, 1, workOffset, workSizePlot, null, 0, null, null);
        
        // Set the remaining plot kernel args
        clSetKernelArg(plotKernel, 0, Sizeof.cl_float, Pointer.to(new float[]{settings.getWidth()}));
        clSetKernelArg(plotKernel, 1, Sizeof.cl_float, Pointer.to(new float[]{settings.getHeight()}));
        clSetKernelArg(plotKernel, 2, Sizeof.cl_int, Pointer.to(numTransforms));
        
        // Set the remaining blur kernel args if needed
        if (settings.getUseBlur()) {
            clSetKernelArg(finishKernel2, 0, Sizeof.cl_float, Pointer.to(new int[] { settings.getWidth() }));
            clSetKernelArg(finishKernel2, 1, Sizeof.cl_float, Pointer.to(new int[] { settings.getHeight() }));
            ByteBuffer blurParamBuffer = clEnqueueMapBuffer(queue, blurParamMem, CL_BLOCKING, CL_MAP_WRITE, 0, 3*Sizeof.cl_float, 0, null, null, null).order(ByteOrder.LITTLE_ENDIAN);
            blurParamBuffer.putFloat(settings.getBlurAlpha());
            blurParamBuffer.putFloat(settings.getBlurMinRadius());
            blurParamBuffer.putFloat(settings.getBlurMaxRadius()); // (2n+1)^2
            blurParamBuffer.rewind();
            clEnqueueUnmapMemObject(queue, blurParamMem, blurParamBuffer, 0, null, null);
        }
        
        // Keep track of the image quality
        long longTotalHits = 0, longPixelHits = 0;
        int totalHits, pixelHits;
        double currQuality = 0;
        // Start the timer
        long currTime = System.nanoTime();
        long startTime = currTime;
        // Keep track of the total number of points plotted
        long numIter = 0;
        // Keep track of the total elapsed time
        double elapTime = 0;
        // Store frequenly used settings on the stack
        double maxTime = settings.getMaxTime();
        double maxQuality = settings.getMaxQuality();
        // If the renderer is overdue for a preview image, force one to happen
        boolean forcePreview = updateImages && updateInterval > 0 && currTime >= preUpdateTime+updateInterval;
        
        // Set the batch size. Always starts at 1. May change each iteration if 
        // the isBatchAccerlated flag is set to true.
        int[] batchSize = new int[] { 1 };
        Pointer batchPointer = Pointer.to(batchSize);
        clSetKernelArg(plotKernel, 15, Sizeof.cl_int, batchPointer);
        
        // Store a copy of the isAccerlated flag on the stack, so that if the
        // flag changes mid render, an error does not occur.
        boolean isAccel = isBatchAccelerated;
        //
        double maxBatchTime = 0;
        if (updatesPerSec > 0)
            maxBatchTime = 1/updatesPerSec;
        if (maxBatchTimeSec > 0 && maxBatchTimeSec < maxBatchTime)
            maxBatchTime = maxBatchTimeSec;
        
        // Perform the main ploting cycle
        while ((!task.isCancelled()|| forcePreview) && currQuality < maxQuality && elapTime < maxTime) {
            // If it is time to update
            if (updateInterval > 0 && currTime >= preUpdateTime+updateInterval) {
                // If not generating update image...
                if (!updateImages) {
                    // Send a progress update callback without an image
                    callback.flameRendererCallback(task, flame, null, currQuality, numIter*workSize, elapTime, false);
                    // Calculate the 'previous' update time
                    preUpdateTime = Math.max(preUpdateTime+updateInterval, currTime-updateInterval);
                } else if (numIter >= 20) {
                    // Update the image
                    updateImage(currQuality, numIter*workSize, startTime, false);
                    // Calculate the 'previous' preview time
                    preUpdateTime = Math.max(preUpdateTime+updateInterval, currTime-updateInterval);
                    // Unset the force preview flag
                    forcePreview = false;
                    // If we just previewed and the task is cancled, return
                    if (task.isCancelled()) {
                        return;
                    }
                }
            }
            // Flush and finish the queue to prevent hanging
            clFinish(queue); 
            // Keep track of how long it takes to run the batch
            long batchTime = System.nanoTime();
            // Run the plotting kernel
            clEnqueueNDRangeKernel(queue, plotKernel, 1, workOffset, workSizePlot, null, 0, null, null);
            clFinish(queue);
            batchTime = System.nanoTime() - batchTime;
            
            // Map the hit counters buffer for reading
            ByteBuffer hitCounters = clEnqueueMapBuffer(queue, hitcountsMem, CL_BLOCKING, CL.CL_MAP_READ, 0, 2*Sizeof.cl_int, 0, null, null, null).order(ByteOrder.LITTLE_ENDIAN);
            // Get the total number pixel hits
            totalHits = hitCounters.getInt();
            // Get the number of individual pixel's hit
            pixelHits = hitCounters.getInt();
            // Rewind the hit counters buffer
            hitCounters.rewind();
            // Unmap the hit counters buffer
            clEnqueueUnmapMemObject(queue, hitcountsMem, hitCounters, 0, null, null);
            
            // If the total-hits counter overflows...
            if (totalHits < 0) {
                // Map the hit counters buffer for writing
                hitCounters = clEnqueueMapBuffer(queue, hitcountsMem, CL_BLOCKING, CL.CL_MAP_WRITE, 0, 2*Sizeof.cl_int, 0, null, null, null);
                // Zero the buffer
                hitCounters.putLong(0);
                // Unmap the hit counters buffer
                clEnqueueUnmapMemObject(queue, hitcountsMem, hitCounters, 0, null, null);
                // Accumulate the total hits and pixel hits into the long totals
                longTotalHits += totalHits & 0xFFFFFFFFL;
                longPixelHits += pixelHits & 0xFFFFFFFFL;
                // Zero the counters
                totalHits = 0;
                pixelHits = 0;
            }
            
            // Rember the old quality
            double oldQuality = currQuality;
            // Calculate the current quality
            currQuality = pixelHits==0?0:(longTotalHits+totalHits)/(double)(longPixelHits+pixelHits);
                        
            // Get the curren time
            currTime = System.nanoTime();
            // Calculate the elapsed time
            elapTime = (currTime-startTime)/1e9;
            
            // Update the total number of iterations
            numIter += batchSize[0];
            
            // If using the accelerated batching algorithm...
            if (isAccel) {
                // Caclulate the improvement in quality per second
                double qrate = (currQuality - oldQuality)/(batchTime*1e-9);
                
                // Caclualte the seconds remaining based on quality
                double dtime = (maxQuality - currQuality)/qrate;
                
                // Reduce the time for the next batch based on max-time
                dtime = Math.min(dtime, maxTime - elapTime);
                    
                // If using a max batch-time, limit by the max batch-time
                if (maxBatchTime > 0)
                    dtime = Math.min(dtime, maxBatchTime);
                
                // Reduce the time for the next batch based on the next update
                // Calc the batch next batch-size based on the batch-time
                int iBatchSize = (int)(batchSize[0]*dtime/batchTime*1e9);
                
                // Ensure the batch size is atleast one
                batchSize[0] = iBatchSize <= 0? 1 : iBatchSize;
                
                // Set the batch size argument
                clSetKernelArg(plotKernel, 15, Sizeof.cl_int, batchPointer);
            }
        }
        
        // If the task has not been cancled, update the final image
        if (!task.isCancelled()) {
            // Update the image
            updateImage(currQuality, numIter*workSize, startTime, true);
        }
    }
    
    private void prepCLProgram() {
        // Get all of the variation definitions from the flame
        Set<VariationDefinition> variations = flame.getVariationSet();
        // If this variation set is not equal to the flame's variation set
        if (!variationSet.equals(variations)) {
            // Make this variation set equal to the flame's variation set
            variationSet.clear();
            variationSet.addAll(variations);
            // Recompile the OpenCL program kernels
            makeCLProgram();
        }
    }
    private void makeCLProgram() {
        // Release the existing program and  kernels
        if (initKernel != null) { clReleaseKernel(initKernel); initKernel = null; }
        if (warmKernel != null) { clReleaseKernel(warmKernel); warmKernel = null; }
        if (plotKernel != null) { clReleaseKernel(plotKernel); plotKernel = null; }
        if (previewKernel != null) { clReleaseKernel(previewKernel); previewKernel = null; }
        if (finishKernel1 != null) { clReleaseKernel(finishKernel1); finishKernel1 = null; }
        if (finishKernel2 != null) { clReleaseKernel(finishKernel2); finishKernel2 = null; }
        if (program != null) { clReleaseProgram(program); program = null; }
        // Create the clProgram code for this set of variations
        String programSource = makeCLProgramCode();
        try {
            // Print the source code
//            printSourceCode(programSource);
            // Create the program
            program = clCreateProgramWithSource(context, 1, new String[]{ programSource }, null, null);
            // Create the device list
            cl_device_id[] devices = new cl_device_id[] { device };
            // Compile the program on the device
            clBuildProgram(program, 1, devices, "-cl-fast-relaxed-math", null, null);
            // Create the kernels
            initKernel = clCreateKernel(program, "initKernel", null);
            warmKernel = clCreateKernel(program, "warmKernel", null);
            plotKernel = clCreateKernel(program, "plotKernel", null);
            previewKernel = clCreateKernel(program, "quckFinishKernel", null);
            finishKernel1 = clCreateKernel(program, "blurFinishKernel1", null);
            finishKernel2 = clCreateKernel(program, "blurFinishKernel2", null);
            // Determine the work group size
            clGetKernelWorkGroupInfo(plotKernel, device, CL_KERNEL_WORK_GROUP_SIZE, Sizeof.size_t, Pointer.to(workSizePlot), null);
            workSize = (int)workSizePlot[0];
//            System.out.println("workSize: "+workSize);
            // Set kernel arguments
            setKernelArgs();
        } catch (Exception ex) {
            // Print the source code
            printSourceCode(programSource);
            // Print the error stack trage
            ex.printStackTrace(System.err);
        }
    }   
    private String makeCLProgramCode() {
        // Create the kernel #define flags source code block
        StringBuilder flags = new StringBuilder();
        // If linear variations are the only variations, then disable variations
        boolean useVariations = settings.getUseVariations();
        if (variationSet.size() == 1 && variationSet.contains(VariationDefinition.LINEAR))
            useVariations = false;
        if (useVariations)
            flags.append("#define USE_VARIATIONS 1\n");
        if (settings.getUseFinalTransform())
            flags.append("#define USE_FINAL_TRANSFORM 1\n");
        if (settings.getUsePostAffines())
            flags.append("#define USE_POST_AFFINES 1\n");
        if (settings.getUseJitter())
            flags.append("#define USE_JITTER 1\n");
        // Create the variation code block
        StringBuilder variationCodeBlock = new StringBuilder();
        // Keep track of the variation and parameter indexes
        int variationIndex = 0;
        int parameterIndex = 0;
        // For each variation
        for (VariationDefinition variation: variationSet) {
            // Get the varation's name
            String variationName = variation.getName();
            // Get the variation's source code
            String variationCode = variation.getCode();
            // For each paramter in theis variation definition
            for (String parameterName: variation.getParameters().keySet()) {
                // Replace direct references to parameters with a references to the parameter array
                variationCode = variationCode.replaceAll(parameterName, "(PARAMETERS["+parameterIndex+"])");
                // Increment the parameter index
                parameterIndex++;
            }
            // Format the variation's source code
            variationCode = variationCode.replaceAll("\n","\n        ").trim(); 
            // Insert the variation's source code intot he variation block template
            variationCode = oclVariationCodeTemplate.replaceAll("___VARIATION_SOURCE_CODE___", variationCode);
            // Insert the variation's name
            variationCode = variationCode.replaceAll("___VARIATION_NAME___", variationName);
            // Insert the variation's index
            variationCode = variationCode.replaceAll("___VARIATION_INDEX___", Integer.toString(variationIndex));
            // Insert the variation's coefficient refeence
            variationCode = variationCode.replaceAll("COEF", "(VARIATIONS["+variationIndex+"])");
            // Append this variation's code to the rest of the variation code block
            variationCodeBlock.append(variationCode);
            // Increment the variation index
            variationIndex++;
        }
        // Initialize the clProgram's source code from the template
        String newProgramCode = oclProgramCodeTemplate;
        // Insert the kernel flags
        newProgramCode = newProgramCode.replaceFirst("___FLAGS___", flags.toString());
        // Insert the variation source block
        newProgramCode = newProgramCode.replaceFirst("___VARIATIONS___", variationCodeBlock.toString());
        // Insert the total number of variations
        newProgramCode = newProgramCode.replaceAll("___NUM_VARIATIONS___", Integer.toString(flame.getNumVariations()));
        // Insert the total number of parameters
        newProgramCode = newProgramCode.replaceAll("___NUM_PARAMETERS___", Integer.toString(flame.getNumParameters()));
        // Return the clProgram code
        return newProgramCode;
    }
    private void printSourceCode(String programCode) {
        // Split the source code by line
        String[] lines = programCode.split("\n");
        // Figure out how many digits are needed to display the largest line number
        int numDigits = (int)Math.log10(lines.length)+1;
        // Use a string builder to build the output string
        StringBuilder builder = new StringBuilder();
        // For each line
        for (int i=0; i<lines.length; i++) {
            // Prefix the line with the line number using left 0 paded numbers
            builder.append(String.format("%"+numDigits+"d", i).replace(' ', '0'));
            // Append the line
            builder.append(" ");
            builder.append(lines[i]);
            builder.append("\n");
        }
        // Print the source code to the error stream
        System.err.println(builder.toString());
    }
    private void setKernelArgs() {
        if (iterRStateMem != null) {
            /// InitKernel
            clSetKernelArg(initKernel, 0, Sizeof.cl_mem, Pointer.to(iterRStateMem));
            clSetKernelArg(initKernel, 1, Sizeof.cl_mem, Pointer.to(iterPointsMem));
            clSetKernelArg(initKernel, 2, Sizeof.cl_mem, Pointer.to(iterColorsMem));
            
            /// WarmKernel
            // clSetKernelArg(warmKernel, 0, Sizeof.cl_int, numTransforms);
            clSetKernelArg(warmKernel, 1, Sizeof.cl_mem, Pointer.to(xformWeightMem));
            clSetKernelArg(warmKernel, 2, Sizeof.cl_mem, Pointer.to(xformCmixesMem));
            clSetKernelArg(warmKernel, 3, Sizeof.cl_mem, Pointer.to(xformColorsMem));
            clSetKernelArg(warmKernel, 4, Sizeof.cl_mem, Pointer.to(xformAffineMem));
            clSetKernelArg(warmKernel, 5, Sizeof.cl_mem, Pointer.to(xformVariationsMem));
            clSetKernelArg(warmKernel, 6, Sizeof.cl_mem, Pointer.to(xformParametersMem));
            clSetKernelArg(warmKernel, 7, Sizeof.cl_mem, Pointer.to(iterRStateMem));
            clSetKernelArg(warmKernel, 8, Sizeof.cl_mem, Pointer.to(iterPointsMem));
            clSetKernelArg(warmKernel, 9, Sizeof.cl_mem, Pointer.to(iterColorsMem));
            
            /// PlotKernel
            // clSetKernelArg(plotKernel, 0, Sizeof.cl_float, Pointer.to{new float[]{width,});
            // clSetKernelArg(plotKernel, 1, Sizeof.cl_float, Pointer.to{new float[]{height});
            // clSetKernelArg(plotKernel, 2, Sizeof.cl_int, numTransforms);
            clSetKernelArg(plotKernel, 3, Sizeof.cl_mem, Pointer.to(xformWeightMem));
            clSetKernelArg(plotKernel, 4, Sizeof.cl_mem, Pointer.to(xformCmixesMem));
            clSetKernelArg(plotKernel, 5, Sizeof.cl_mem, Pointer.to(xformColorsMem));
            clSetKernelArg(plotKernel, 6, Sizeof.cl_mem, Pointer.to(xformAffineMem));
            clSetKernelArg(plotKernel, 7, Sizeof.cl_mem, Pointer.to(xformVariationsMem));
            clSetKernelArg(plotKernel, 8, Sizeof.cl_mem, Pointer.to(xformParametersMem));
            // clSetKernelArg(plotKernel, 9, Sizeof.cl_mem, Pointer.to(flameViewAffineMem));
            clSetKernelArg(plotKernel, 10, Sizeof.cl_mem, Pointer.to(iterRStateMem));
            clSetKernelArg(plotKernel, 11, Sizeof.cl_mem, Pointer.to(iterPointsMem));
            clSetKernelArg(plotKernel, 12, Sizeof.cl_mem, Pointer.to(iterColorsMem));
            clSetKernelArg(plotKernel, 13, Sizeof.cl_mem, Pointer.to(histogramMem));
            // clSetKernelArg(plotKernel, 14, Sizeof.cl_mem, Pointer.to(hitcountsMem));
            
            /// PreviewKernel
            // clSetKernelArg(previewKernel, 0, Sizeof.cl_float, 1.0f/quality);
            // clSetKernelArg(previewKernel, 1, Sizeof.cl_mem, Pointer.to(flameColorationMem));
            // clSetKernelArg(previewKernel, 2, Sizeof.cl_mem, Pointer.to(flameBackgroundMem));
            clSetKernelArg(previewKernel, 3, Sizeof.cl_mem, Pointer.to(histogramMem));
            clSetKernelArg(previewKernel, 4, Sizeof.cl_mem, Pointer.to(imgRasterMem));
            
            /// FinishKernel1
            // clSetKernelArg(finishKernel1, 0, Sizeof.cl_float, 1.0f/quality);
            // clSetKernelArg(finishKernel1, 1, Sizeof.cl_mem, Pointer.to(flameColorationMem));
            clSetKernelArg(finishKernel1, 2, Sizeof.cl_mem, Pointer.to(histogramMem));
            clSetKernelArg(finishKernel1, 3, Sizeof.cl_mem, Pointer.to(preRasterMem));
            
            /// FinishKernel2
            // clSetKernelArg(finishKernel2, 0, Sizeof.cl_int, width);
            // clSetKernelArg(finishKernel2, 1, Sizeof.cl_int, height);
            // clSetKernelArg(finishKernel2, 2, Sizeof.cl_mem, Pointer.to(flameBlurParamsMem));
            // clSetKernelArg(finishKernel2, 3, Sizeof.cl_mem, Pointer.to(flameBackgroundMem));
            clSetKernelArg(finishKernel2, 4, Sizeof.cl_mem, Pointer.to(histogramMem));
            clSetKernelArg(finishKernel2, 5, Sizeof.cl_mem, Pointer.to(preRasterMem));
            clSetKernelArg(finishKernel2, 6, Sizeof.cl_mem, Pointer.to(imgRasterMem));
        }
        clSetKernelArg(plotKernel, 9, Sizeof.cl_mem, Pointer.to(flameViewAffineMem));
        clSetKernelArg(plotKernel, 14, Sizeof.cl_mem, Pointer.to(hitcountsMem));
        clSetKernelArg(previewKernel, 1, Sizeof.cl_mem, Pointer.to(flameColorationMem));
        clSetKernelArg(previewKernel, 2, Sizeof.cl_mem, Pointer.to(flameBackgroundMem));
        clSetKernelArg(finishKernel1, 1, Sizeof.cl_mem, Pointer.to(flameColorationMem));
        clSetKernelArg(finishKernel2, 2, Sizeof.cl_mem, Pointer.to(blurParamMem));
        clSetKernelArg(finishKernel2, 3, Sizeof.cl_mem, Pointer.to(flameBackgroundMem));
    }
    
    private void prepFlameBuffers() {
        // Calc the new capacity requirement
        int transformSize = flame.getNumTransforms();
        int variationSize = Math.max(transformSize*flame.getNumVariations(), 1);
        int parameterSize = Math.max(transformSize*flame.getNumParameters(), 1);
        // Flame Data
        if (xformCapacity < transformSize) {
            // Store the new capacity
            xformCapacity = transformSize;
            // Release the mem objects
            if (xformWeightMem != null) {
                clReleaseMemObject(xformWeightMem);
                clReleaseMemObject(xformCmixesMem);
                clReleaseMemObject(xformColorsMem);
                clReleaseMemObject(xformAffineMem);
            }
            // Create new mem objects
            xformWeightMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR, transformSize*Sizeof.cl_float, null, null);
            xformCmixesMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR, transformSize*Sizeof.cl_float, null, null);
            xformColorsMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR, transformSize*4*Sizeof.cl_float, null, null);
            xformAffineMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR, transformSize*12*Sizeof.cl_float, null, null);
            // Set the kernel arguments
            clSetKernelArg(warmKernel, 1, Sizeof.cl_mem, Pointer.to(xformWeightMem));
            clSetKernelArg(warmKernel, 2, Sizeof.cl_mem, Pointer.to(xformCmixesMem));
            clSetKernelArg(warmKernel, 3, Sizeof.cl_mem, Pointer.to(xformColorsMem));
            clSetKernelArg(warmKernel, 4, Sizeof.cl_mem, Pointer.to(xformAffineMem));
            clSetKernelArg(plotKernel, 3, Sizeof.cl_mem, Pointer.to(xformWeightMem));
            clSetKernelArg(plotKernel, 4, Sizeof.cl_mem, Pointer.to(xformCmixesMem));
            clSetKernelArg(plotKernel, 5, Sizeof.cl_mem, Pointer.to(xformColorsMem));
            clSetKernelArg(plotKernel, 6, Sizeof.cl_mem, Pointer.to(xformAffineMem));
        }
        if (xformVariationsCapacity < variationSize) {
            // Store the new capacity
            xformVariationsCapacity = variationSize;
            // Release the mem objects
            if (xformVariationsMem != null) clReleaseMemObject(xformVariationsMem);
            // Create new mem objects
            xformVariationsMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR, variationSize*Sizeof.cl_float, null, null);
            // Set the kernel arguments
            clSetKernelArg(warmKernel, 5, Sizeof.cl_mem, Pointer.to(xformVariationsMem));
            clSetKernelArg(plotKernel, 7, Sizeof.cl_mem, Pointer.to(xformVariationsMem));
        }
        if (xformParametersCapacity < parameterSize) {
            // Store the new capacity
            xformParametersCapacity = parameterSize;
            // Release the mem objects
            if (xformParametersMem != null) clReleaseMemObject(xformParametersMem);
            // Create new mem objects
            xformParametersMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_ALLOC_HOST_PTR, parameterSize*Sizeof.cl_float, null, null);
            // Set the kernel arguments
            clSetKernelArg(warmKernel, 6, Sizeof.cl_mem, Pointer.to(xformParametersMem));
            clSetKernelArg(plotKernel, 8, Sizeof.cl_mem, Pointer.to(xformParametersMem));
        }
        // Map the buffers
        ByteBuffer xformWeightBuffer = clEnqueueMapBuffer(queue, xformWeightMem, CL_NON_BLOCKING, CL_MAP_WRITE, 0, transformSize*Sizeof.cl_float, 0, null, null, null).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer xformCmixesBuffer = clEnqueueMapBuffer(queue, xformCmixesMem, CL_NON_BLOCKING, CL_MAP_WRITE, 0, transformSize*Sizeof.cl_float, 0, null, null, null).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer xformColorsBuffer = clEnqueueMapBuffer(queue, xformColorsMem, CL_NON_BLOCKING, CL_MAP_WRITE, 0, transformSize*4*Sizeof.cl_float, 0, null, null, null).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer xformAffineBuffer = clEnqueueMapBuffer(queue, xformAffineMem, CL_NON_BLOCKING, CL_MAP_WRITE, 0, transformSize*12*Sizeof.cl_float, 0, null, null, null).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer xformVariationsBuffer = clEnqueueMapBuffer(queue, xformVariationsMem, CL_NON_BLOCKING, CL_MAP_WRITE, 0, variationSize*Sizeof.cl_float, 0, null, null, null).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer xformParametersBuffer = clEnqueueMapBuffer(queue, xformParametersMem, CL_NON_BLOCKING, CL_MAP_WRITE, 0, parameterSize*Sizeof.cl_float, 0, null, null, null).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer flameViewAffineBuffer = clEnqueueMapBuffer(queue, flameViewAffineMem, CL_NON_BLOCKING, CL_MAP_WRITE, 0, 6*Sizeof.cl_float, 0, null, null, null).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer flameColorationBuffer = clEnqueueMapBuffer(queue, flameColorationMem, CL_NON_BLOCKING, CL_MAP_WRITE, 0, 3*Sizeof.cl_float, 0, null, null, null).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer flameBackgroundBuffer = clEnqueueMapBuffer(queue, flameBackgroundMem, CL_NON_BLOCKING, CL_MAP_WRITE, 0, 4*Sizeof.cl_float, 0, null, null, null).order(ByteOrder.LITTLE_ENDIAN);
        clFlush(queue);
        // Write to the buffers
        flame.fillBuffers(settings.getWidth(), settings.getHeight(),
            xformWeightBuffer.asFloatBuffer(),
            xformCmixesBuffer.asFloatBuffer(),
            xformColorsBuffer.asFloatBuffer(),
            xformAffineBuffer.asFloatBuffer(),
            xformVariationsBuffer.asFloatBuffer(),
            xformParametersBuffer.asFloatBuffer(),
            flameViewAffineBuffer.asFloatBuffer(),
            flameColorationBuffer.asFloatBuffer(),
            flameBackgroundBuffer.asFloatBuffer());
        xformWeightBuffer.rewind();
//        print("xformWeightBuffer", 1, xformWeightBuffer.asFloatBuffer());
//        print("xformCmixesBuffer", 1, xformCmixesBuffer.asFloatBuffer());
//        print("xformColorsBuffer", 4, xformColorsBuffer.asFloatBuffer());
//        print("xformAffineBuffer", 2, xformAffineBuffer.asFloatBuffer());
//        print("xformVariationsBuffer", 1, xformVariationsBuffer.asFloatBuffer());
//        print("xformParametersBuffer", 1, xformParametersBuffer.asFloatBuffer());
//        print("flameViewAffineBuffer", 2, flameViewAffineBuffer.asFloatBuffer());
        // Unmap the buffers
        clEnqueueUnmapMemObject(queue, xformWeightMem, xformWeightBuffer, 0, null, null);
        clEnqueueUnmapMemObject(queue, xformCmixesMem, xformCmixesBuffer, 0, null, null);
        clEnqueueUnmapMemObject(queue, xformColorsMem, xformColorsBuffer, 0, null, null);
        clEnqueueUnmapMemObject(queue, xformAffineMem, xformAffineBuffer, 0, null, null);
        clEnqueueUnmapMemObject(queue, xformVariationsMem, xformVariationsBuffer, 0, null, null);
        clEnqueueUnmapMemObject(queue, xformParametersMem, xformParametersBuffer, 0, null, null);
        clEnqueueUnmapMemObject(queue, flameViewAffineMem, flameViewAffineBuffer, 0, null, null);
        clEnqueueUnmapMemObject(queue, flameColorationMem, flameColorationBuffer, 0, null, null);
        clEnqueueUnmapMemObject(queue, flameBackgroundMem, flameBackgroundBuffer, 0, null, null);
    }
    /* FOR DEBUGGING
    private void print(String name, int size, FloatBuffer buffer) {
        try (FileWriter out = new FileWriter(new File(name+".csv"))) {
            buffer.rewind();
            for (int i=0; i<buffer.limit(); ) {
                for (int j=0; j<size; j++, i++) {
                    out.write(Float.toString(buffer.get()));
                    out.write(",");
                }
                out.write("\n");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    */
    private void prepIterBuffers() {
        // If the mem objects are not large enough
        if (workCapacity < workSize) {
            // Store the new capacity
            workCapacity = workSize;
            // Release the mem objects
            if (iterRStateMem != null) {
                clReleaseMemObject(iterRStateMem);
                clReleaseMemObject(iterPointsMem);
                clReleaseMemObject(iterColorsMem);
            }
            // Create new mem objects
            iterRStateMem = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_ALLOC_HOST_PTR, workCapacity*2*Sizeof.cl_uint, null, null);
            iterPointsMem = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_ALLOC_HOST_PTR, workCapacity*2*Sizeof.cl_float, null, null);
            iterColorsMem = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_ALLOC_HOST_PTR, workCapacity*4*Sizeof.cl_float, null, null);
            // Set the kernel arguments
            clSetKernelArg(initKernel, 0, Sizeof.cl_mem, Pointer.to(iterRStateMem));
            clSetKernelArg(initKernel, 1, Sizeof.cl_mem, Pointer.to(iterPointsMem));
            clSetKernelArg(initKernel, 2, Sizeof.cl_mem, Pointer.to(iterColorsMem));
            clSetKernelArg(warmKernel, 7, Sizeof.cl_mem, Pointer.to(iterRStateMem));
            clSetKernelArg(warmKernel, 8, Sizeof.cl_mem, Pointer.to(iterPointsMem));
            clSetKernelArg(warmKernel, 9, Sizeof.cl_mem, Pointer.to(iterColorsMem));
            clSetKernelArg(plotKernel, 10, Sizeof.cl_mem, Pointer.to(iterRStateMem));
            clSetKernelArg(plotKernel, 11, Sizeof.cl_mem, Pointer.to(iterPointsMem));
            clSetKernelArg(plotKernel, 12, Sizeof.cl_mem, Pointer.to(iterColorsMem));
            // Fill the RNG state buffer with random numbers
            Random random = new Random();
            ByteBuffer iterRStateBuffer = clEnqueueMapBuffer(queue, iterRStateMem, CL_BLOCKING, CL_MAP_WRITE, 0, workCapacity*2*Sizeof.cl_uint, 0, null, null, null).order(ByteOrder.LITTLE_ENDIAN);
            for (int i=0; i<workCapacity*2; i++)
                iterRStateBuffer.putInt(random.nextInt());
            clEnqueueUnmapMemObject(queue, iterRStateMem, iterRStateBuffer, 0, null, null);
        }
    }
    private void prepImageBuffers() {
        int width = settings.getWidth();
        int height = settings.getHeight();
        // Calc the new capacity requirement
        int imageSize = width*height;
        // If the mem objects are not large enough
        if (imageCapacity < imageSize) {
            // Store the new capacity
            imageCapacity = imageSize;
            // Release the mem objects
            if (histogramMem != null) {
                clReleaseMemObject(histogramMem);
                clReleaseMemObject(imgRasterMem);
            }
            // Create new mem objects
            histogramMem = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_ALLOC_HOST_PTR, imageCapacity*4*Sizeof.cl_float, null, null);
            preRasterMem = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_ALLOC_HOST_PTR, imageCapacity*4*Sizeof.cl_float, null, null);
            imgRasterMem = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_ALLOC_HOST_PTR, imageCapacity*Sizeof.cl_int, null, null);
            // Set the kernel arguments
            clSetKernelArg(plotKernel, 13, Sizeof.cl_mem, Pointer.to(histogramMem));
            clSetKernelArg(previewKernel, 3, Sizeof.cl_mem, Pointer.to(histogramMem));
            clSetKernelArg(previewKernel, 4, Sizeof.cl_mem, Pointer.to(imgRasterMem));
            clSetKernelArg(finishKernel1, 2, Sizeof.cl_mem, Pointer.to(histogramMem));
            clSetKernelArg(finishKernel1, 3, Sizeof.cl_mem, Pointer.to(preRasterMem));
            clSetKernelArg(finishKernel2, 4, Sizeof.cl_mem, Pointer.to(histogramMem));
            clSetKernelArg(finishKernel2, 5, Sizeof.cl_mem, Pointer.to(preRasterMem));
            clSetKernelArg(finishKernel2, 6, Sizeof.cl_mem, Pointer.to(imgRasterMem));
            
        } 
        // Zero the histogram
        ByteBuffer histogramBuffer = clEnqueueMapBuffer(queue, histogramMem, CL_BLOCKING, CL_MAP_WRITE, 0, imageSize*4*Sizeof.cl_float, 0, null, null, null).order(ByteOrder.LITTLE_ENDIAN);
        for (int i=0; i<imageSize*4; i++)
            histogramBuffer.putInt(0);
        clEnqueueUnmapMemObject(queue, histogramMem, histogramBuffer, 0, null, null);
    }
    private void prepHitCountsBuffer() {
        // Zero the hit counts buffer
        ByteBuffer buffer = clEnqueueMapBuffer(queue, hitcountsMem, CL_BLOCKING, CL_MAP_WRITE, 0, 2*Sizeof.cl_int, 0, null, null, null).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(0);
        buffer.putInt(0);
        buffer.rewind();
        clEnqueueUnmapMemObject(queue, hitcountsMem, buffer, 0, null, null);
    }
    
    private void updateImage(double quality, double points, long startTime, boolean isFinished) {
        int width = settings.getWidth();
        int height = settings.getHeight();
        // Set the scaleConstant arg
        float[] scaleConstant = new float[] { (float)(1/quality) };
        if (!isFinished || !settings.getUseBlur()) {
            // Set the argument
            clSetKernelArg(previewKernel, 0, Sizeof.cl_float, Pointer.to(scaleConstant));
            // Run the coloration kernel
            clEnqueueNDRangeKernel(queue, previewKernel, 1, workOffset, workSizeColor, null, 0, null, null);
        } else {
            // Set the argument for the first kernel
            clSetKernelArg(finishKernel1, 0, Sizeof.cl_float, Pointer.to(scaleConstant));
            // Run the first kernel
            clEnqueueNDRangeKernel(queue, finishKernel1, 1, workOffset, workSizeColor, null, 0, null, null);
            // Run the second kernel
            clEnqueueNDRangeKernel(queue, finishKernel2, 1, workOffset, workSizeColor, null, 0, null, null);
        }
        
        // Ensure the back image is the right size
        if (backImage == null || backImage.getWidth()!=width || backImage.getHeight()!=height)
            backImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        // Get the int array backing the bufferd image
        int[] data = ((DataBufferInt)backImage.getRaster().getDataBuffer()).getData();
        // Read the data directly into the buffered image's array
        clEnqueueReadBuffer(queue, imgRasterMem, CL_BLOCKING, 0, width*height*Sizeof.cl_int, Pointer.to(data), 0, null, null);
        // Swap the front and back images
        BufferedImage swapImage = frontImage;
        frontImage = backImage;
        backImage = swapImage;
        // Determine the elapsed time (since work began on this image)
        double elapTime = (System.nanoTime()-startTime)*1e-9;
        // Alert the image listeners
        callback.flameRendererCallback(task, flame, frontImage, quality, points, elapTime, isFinished);
    }
    
    private static cl_platform_id[] getAllPlatforms() {
        int numPlatforms[] = new int[1];
        clGetPlatformIDs(0, null, numPlatforms);
        cl_platform_id platforms[] = new cl_platform_id[numPlatforms[0]];
        clGetPlatformIDs(platforms.length, platforms, null);
        return platforms;
    }
    private static cl_device_id[] getAllDevices(cl_platform_id platform) {
        int numDevices[] = new int[1];
        clGetDeviceIDs(platform, CL_DEVICE_TYPE_ALL, 0, null, numDevices);
        cl_device_id devices[] = new cl_device_id[numDevices[0]];
        clGetDeviceIDs(platform, CL_DEVICE_TYPE_ALL, numDevices[0], devices, null);
        return devices;
    }
    private static String getPlatformString(cl_platform_id platform, int paramName) {
        long size[] = new long[1];
        clGetPlatformInfo(platform, paramName, 0, null, size);
        byte buffer[] = new byte[(int)size[0]];
        clGetPlatformInfo(platform, paramName, buffer.length, Pointer.to(buffer), null);
        return new String(buffer, 0, buffer.length-1);
    }
    private static String getDeviceString(cl_device_id device, int paramName) {
        long size[] = new long[1];
        clGetDeviceInfo(device, paramName, 0, null, size);
        byte buffer[] = new byte[(int)size[0]];
        clGetDeviceInfo(device, paramName, buffer.length, Pointer.to(buffer), null);
        return new String(buffer, 0, buffer.length-1);
    }
    private static int getDeviceInt(cl_device_id device, int paramName) {
        int buffer[] = new int[1];
        clGetDeviceInfo(device, paramName, Sizeof.cl_int, Pointer.to(buffer), null);
        return buffer[0];
    }
    private static long getDeviceLong(cl_device_id device, int paramName) {
        long buffer[] = new long[1];
        clGetDeviceInfo(device, paramName, Sizeof.cl_long, Pointer.to(buffer), null);
        return buffer[0];
    }
    
    public static enum DeviceType {
        ALL(CL_DEVICE_TYPE_ALL),
        CPU(CL_DEVICE_TYPE_CPU),
        GPU(CL_DEVICE_TYPE_GPU),
        DEFAULT(CL_DEVICE_TYPE_DEFAULT),
        ACCELERATOR(CL_DEVICE_TYPE_ACCELERATOR);
        
        final long type;
        DeviceType(long type) {
            this.type = type;
        }
    }
}
