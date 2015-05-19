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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author Jeremiah N. Hankins
 */
public abstract class FlameRenderer {
    // Running Flag
    private  boolean isRunning = false;
    // Running Lock
    private final Lock isRunningLock = new ReentrantLock();
    
    // Current task
    private FlameRendererTask currTask = null;
    // Task Queue
    private final BlockingQueue<FlameRendererTask> taskQueue = new LinkedBlockingQueue();
    
    // Update Rate (update call backs per second)
    protected double updateRate = 0;
    // Updates Generate Images
    protected boolean updateImages = false;
    // Dynamically adjust batch sizes to reduce overhead
    protected boolean isAccelerated = true;
    
    // Shutdown Hook
    private final Thread shutdownHook = new Thread() {
        @Override
        public void run() {
            stopRunning();
        }
    };
            
    // The main thread
    private MainThread mainThread;
    
    /**
     * If this {@code FlameRenderer} is not running, the 
     * {@link #isRunning() isRunning} flag will be set, required resources will
     * be allocated and initialized, and dependent threads will be started. If 
     * this {@code FlameRenderer} is already running, then invoking this method 
     * does nothing.
     */
    public void startRunning() {
        // Lock
        isRunningLock.lock();
        try {
            // If this FlameRenderer is not running...
            if (!isRunning) {
                // Set the running flag
                isRunning = true;
                // Add the shutdown hook
                Runtime.getRuntime().addShutdownHook(shutdownHook);
                // Initalize implementing classes resources
                initializeResources();
                // Start the main thread
                mainThread = new MainThread();
                mainThread.start();
            }
        } finally {
            // Unlock
            isRunningLock.unlock();
        }
    }
    
    /**
     * If this {@code FlameRenderer} is running, the 
     * {@link #isRunning() isRunning} flag will be unset, any ongoing work will
     * cease immediatly, dependent threads will be joined, and all resouces will
     * be released. 
     *
     * Any {@link FlameRendererTask} objects in the queue or currently being
     * worked on will be {@link FlameRendererTask#cancle() cancled}, and the
     * task queue will be cleared.
     * 
     * If this {@code FlameRenderer} is not running, then this method does 
     * nothing.
     * 
     * If the thread that calls this method is interrupted while within this 
     * method, the thread will finish executing this method before setting it's
     * the interrupt flag will be set.
     *
     * If {@link System#exit(int)} is invoked while the render engine is still
     * running this method is invoked via shutdown hook to ensure that threads
     * are joined and resources are released when the program exists.
     */
    public void stopRunning() {
        // Lock
        isRunningLock.lock();
        try {
            // If this RenderEngine is running...
            if (isRunning) {
                // Unset the running flag
                isRunning = false;
                // Remove the shutdown hook (unless we're in the shutdown hook)
                if (Thread.currentThread() != shutdownHook)
                    Runtime.getRuntime().removeShutdownHook(shutdownHook);
                // Clear the queue and cancle all tasks
                clearTaskQueue();
                // Interupt in case we're stuck waiting for a task
                mainThread.interrupt();
                // Keep track of interrupts
                boolean isInterrupted = false;
                // Join the main thread, uninterruptably
                while (mainThread.isAlive()) {
                    try {
                        mainThread.join();
                    } catch (InterruptedException ex) {
                        isInterrupted = true;
                    }
                }
                // Release implementing classse resources uninterruptably
                if (releaseResources()) {
                    isInterrupted = true;
                }
                // If this thread was interupted while joining the main thread
                // or while releasing resources, propogate the interupt
                if (isInterrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        } finally {
            // Unlock
            isRunningLock.unlock();
        }
    }
    
    /**
     * Returns {@code true} if this {@code FlameRenderer} is running.
     * 
     * @return {@code true} if this {@code FlameRenderer} is running
     */
    public boolean isRunning() {
        // Lock
        isRunningLock.lock();
        try {
            // Return running flag
            return isRunning;
        } finally {
            // Unlock
            isRunningLock.unlock();
        }
    }

    /**
     * Enqueues the specified {@link FlameRendererTask} for rendering. If this 
     * {@code FlameRenderer} is not running, then {@link #startRunning()} will
     * be called, so that work on the task can begin as soon as possible.
     * 
     * @param task the {@link FlameRendererTask} to enqueue
     */
    public void enqueueTask(FlameRendererTask task) {
        if (task == null)
            throw new IllegalArgumentException("task is null");
        // Lock
        isRunningLock.lock();
        try {
            // If not running, start running
            if (!isRunning)
                startRunning(); 
            // Enqueue the task
            taskQueue.offer(task);
        } finally {
            // Unlock
            isRunningLock.unlock();
        }
    }

    /**
     * Enqueues the {@link FlameRendererTask} contained within the specified
     * {@code Collection} in the order that the 
     * {@link Collection#iterator() Iterator} returns them. See 
     * {@link #enqueueTask(fff.render.FlameRendererTask) enqueueTask} for more
     * information.
     * 
     * @param tasks the {@code Collection} of {@code FlameRendererTask}s to enqueue
     */
    public void enqueueTasks(Collection<FlameRendererTask> tasks) {
        // Lock
        isRunningLock.lock();
        try {
            for (FlameRendererTask task : tasks) {
                enqueueTask(task);
            }
        } finally {
            // Unlock
            isRunningLock.unlock();
        }
    }
    
    /**
     * {@link FlameRendererTask#cancle() Cancles} the current task and all tasks
     * in the queue, then clears the queue.
     */
    public void clearTaskQueue() {
        // Lock
        isRunningLock.lock();
        try {
            // Clear the task queue by draining it into a local list
            List<FlameRendererTask> tasks = new ArrayList();
            taskQueue.drainTo(tasks);
            // Cancle all of the tasks in the list
            for (FlameRendererTask t : tasks)
                t.cancle();
            // Cancle the current task
            try {
                if (currTask != null)
                    currTask.cancle();
            } catch (NullPointerException ignore) {
                // There is a small chance that MainThread could set task
                // to null after this thread has perforemd the null check
                // in the if statement. Catching and ignoring the possible
                // NullPointerException is prefered over adding locks 
                // because locks could impact MainThread's performance.
            }
        } finally {
            // Unlock
            isRunningLock.unlock();
        }
    }
    
    public List<FlameRendererTask> drainTaskQueue(List<FlameRendererTask> taskList) {
        // Create a list to store unfinished tasks if one was not provided
        if (taskList == null)
            taskList = new ArrayList();
        // Lock
        isRunningLock.lock();
        try {
            
        } finally {
            // Unlock
            isRunningLock.unlock();
        }
        return taskList;
    }
    
    /**
     * Sets the update rate in updates per second. If set to {@code 0}, updates 
     * will not be generated.
     * 
     * @param updateRate the update rate in updates per second or {@code 0}
     * @throws IllegalArgumentException if {@code updateRate} is not in range [0, inf)
     */
    public void setUpdateRate(double updateRate) {
        if (!(0<=updateRate && updateRate<Double.POSITIVE_INFINITY))
            throw new IllegalArgumentException("previewRate is not in range [0, inf): "+updateRate);
        this.updateRate = updateRate;
    }
    
    /**
     * Returns the update rate in updates per second or {@code 0}. If {@code 0},
     * then updates will not be generated.
     * 
     * @return the update rate in updates per second or {@code 0}
     */
    public double getUpdateRate() {
        return updateRate;
    }
    
    /**
     * Sets the update images flag. If set to {@code true} images will be
     * generated for every update.
     * 
     * @param updateImages the update images flag
     */
    public void setUpdateImages(boolean updateImages) {
        this.updateImages = updateImages;
    }
    
    /**
     * Return {@code true} if images will be generated for every update.
     * 
     * @return {@code true} if images will be generated for every update
     */
    public boolean getUpdateImages() {
        return updateImages;
    }
    
    /**
     * Sets a flag that, if set to {@code true} tells the renderer to to perform
     * plotting iterations in batches, and to attempt to dynamically adjust the
     * batch size so that the fewest batches are needed to complete the final
     * image. The purpose of this flag is to reduce the amount of overhead time
     * spent checking whether or not the current quality and elapsed time have
     * exceeded the maximum quality or time. This is done by predicting how 
     * many more iterations will be necessary before either the quality or 
     * time limit are reached and then performing those iterations without 
     * checks or updates. In practice a formula like the following is used so 
     * that the algorithm can converge on a solution with a minimum number of
     * limit checks and updates: 
     * {@code batchSize = predicitedBatchSize*9/10 + 5}.
     * 
     * Some subclasses of {@code FlameRenderer} may choose to ignore this flag.
     * 
     * @param isAccelerated the accelerated algorithm flag
     */
    public void setAccelerated(boolean isAccelerated) {
        this.isAccelerated = isAccelerated;
    }
    
    /**
     * Returns the accelerated flag.
     * 
     * @return the accelerated flag
     * @see #setAccelerated(boolean)
     */
    public boolean isAccelerated() {
        return isAccelerated;
    }
    
    /**
     * Allocates and initializes all resources and threads required by classes 
     * that extend {@code FlameRenderer}. Called by {@link #startRunning()}.
     */
    protected abstract void initializeResources();
    
    /**
     * Releases resources and joins threads required by classes that extend 
     * {@code FlameRenderer}. Returns {@code true} if the thread invoking
     * this method is interrupted while within this method. This method should
     * complete its tasks before returning even if it is interrupted. Called by
     * {@link #stopRunning()}.
     * 
     * @return {@code true} if the thread invoking this method is interrupted
     */
    protected abstract boolean releaseResources();

    /**
     * Renders the next image using the next {@link Flame}, 
     * {@link FlameRendererSettings}, and {@code List} of 
     * {@link FlameRendererListener} objects provided by the specified 
     * {@link FlameRendererTask}.
     * 
     * The {@code Flame} should be retrieved via {@link FlameRendererTask#getNextFlame() getNextFlame()},
     * the {@code FlameRendererSettings} via {@link FlameRendererTask#getSettings() getSettings()},
     * and the {@code List} of {@code FlameRendererListener} objects via {@link FlameRendererTask#getSettings() getListeners()}.
     * 
     * If the {@link FlameRendererTask#isCancled()} method return {@code true},
     * work on the image should cease.
     * 
     * Preview images should be produced and broadcast to the {@code FlameRendererListener} 
     * objects at the rate specified by {@link #getUpdateRate()}.
     * 
     * Finished images should also be broadcast to the {@code FlameRendererListener}
     * objects.
     * 
     * @param task the task containing the flame, settings, and listeners
     */
    protected abstract void renderNextFlame(FlameRendererTask task);
    
    private class MainThread extends Thread {
        MainThread() {
            super("FlameRenderer Thread");
        }
        
        @Override
        public void run() {
            // While this FlameRenderer is running...
            while (isRunning) {
                // Block until a task becomes avaialbe
                try {
                    currTask = taskQueue.take();
                } catch (InterruptedException ex) {
                    // If interrupted while waiting on take(), then continue the
                    // loop to test the isRunning flag and determine whether or
                    // not the thraid should return
                    continue;
                }
                // While this FlameRenderer is running, and the current task has
                // not been cancled, and the task is not complete (contains more
                // flames to be rendered)...
                while (isRunning && !currTask.isCancled() && currTask.hasNextFlame()) {
                    // Render the next flame
                    renderNextFlame(currTask);
                }
                // Set the current task to null
                currTask = null;
            }
        }
    }
}

