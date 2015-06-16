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

package fff.render;

import fff.flame.Flame;
import java.awt.image.BufferedImage;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A flame image rendering engine.
 * <p>
 * Rendering {@link RendererTask tasks} consist of an object containing
 * {@link RendererSettings settings}, a
 * {@link RendererCallback callback function}, and a sequence of
 * {@link fff.flame.Flame} instances. Tasks are passed to a
 * {@code FlameRenderer} for rendering by adding them into the renderer's task
 * queue which can be retrieved via {@link #getQueue()}.
 * <p>
 * Work on the first task will not begin until the {@link #start()} method has
 * been invoked. Once the renderer has been started, an internal thread will
 * begin a loop which, on each iteration, takes the next task from the front of
 * the queue and begins working on rendering it's images. If there are no tasks
 * in the queue, the internal thread will block until a task becomes available.
 * The task most recently dequeued for rendering can be retrieved via
 * {@link #getTask()}. Any task can be cancelled at any time by invoking its
 * {@link RendererTask#cancel(boolean) cancel()} method.
 * <p>
 * Use {@link #shutdown()} method to shutdown the renderer and its threads after
 * all tasks in the queue have been completed or cancelled and the queue is
 * empty, or use {@link #shutdownNow()} to cancel the current task is one is
 * currently underway and to ignore the remaining tasks in the queue so that
 * resources can be freed as quickly as possible. Use
 * {@link #awaitTermination()} to cause a thread to block and wait until the
 * shutdown procedure has completed. To determine the current status of the
 * renderer query the {@link #isRunning()}, {@link #isShutdown()}, and
 * {@link #isTerminated()} methods.
 * <p>
 * Since work on individual flame images can be very time consuming,
 * communication between the renderer and the rest of the program is performed
 * asynchronously through the callback functions contained within the individual
 * renderer tasks. If the renderer's
 * {@link #setUpdatesPerSec(double) update rate} is set to a positive value,
 * then the renderer will use the call back function to deliver progress update
 * information (e.g. time elapsed, percent complete, etc..). If the
 * {@link #setUpdateImages(boolean) update-images} flag is set, then the
 * progress updates will contain a preview of what the flame image looks like so
 * far, though this entails extra computation and will increase the total amount
 * of time needed to complete the final image. The final image is also relayed
 * through the callback function using the same method as the progress updates.
 * <p>
 * {@code FlameRenderer} is designed for use across multiple threads and its
 * methods are synchronized where appropriate. All methods are non-blocking
 * unless explicitly stated otherwise.
 *
 * @author Jeremiah N. Hankins
 */
public abstract class FlameRenderer {
    /**
     * Timeout time used for {@link BlockingQueue#poll(long, TimeUnit)} method
     * calls. The timed version of the {@code BlockingQueue} remove method is
     * used so that the render and update threads can periodically check the
     * renderer's state and react to shutdown requests. A smaller time period
     * would allow the renderer to shut down more quickly in some circumstances,
     * but also would wake the threads more frequently without reason when the
     * state hasn't changed and there are no tasks in the queue.
     * <p>
     * Currently set to 100 milliseconds.
     */
    private static final long timeoutMilli = 100;
    
    /**
     * A state indicating that the renderer has been initialize but has not
     * started yet .
     */
    private static final int READY        = 0;
    /**
     * A state indicating that the renderer is currently running.
     */
    private static final int RUNNING      = 1;
    /**
     * A state indicating that the renderer has received a leisurely shutdown 
     * request.
     */
    private static final int SHUTDOWN     = 2;
    /**
     * A state indicating that the renderer has received an immediate shutdown
     * request.
     */
    private static final int SHUTDOWN_NOW = 3;
    /**
     * A state indicating that the renderer has completed its shutdown
     * procedure.
     */
    private static final int TERMINATED   = 4;
    
    /**
     * Keeps track of the renderer's state.
     */
    private volatile int state = READY;
    
    /** 
     * Synchronizes methods which query or alter the renderer's state.
     */
    private final ReentrantLock mainLock = new ReentrantLock();
    
    /**
     * Condition indicating the transition to the terminated state.
     * This condition is used used by the awaitTermination methods.
     */
    private final Condition termination = mainLock.newCondition();
    
    /** 
     * Queue of tasks.
     */
    private final BlockingQueue<RendererTask> queue = new LinkedBlockingQueue();
    
    /**
     * Objects used to store update information.
     */
    private RendererUpdate updateA = new RendererUpdate();
    private RendererUpdate updateB = new RendererUpdate();
    
    /**
     * Queue used to synchronously pass updates from the renderer thread to the
     * update processing thread.
     */
    private final TransferQueue<RendererUpdate> updateQueue = new LinkedTransferQueue();
    
    /**
     * Task most recently dequeued for rendering.
     */
    private RendererTask task;
    
    /**
     * Thread for dequeuing tasks and rendering flames.
     */
    private Thread renderThread;
    
    /**
     * Thread for processing updates.
     */
    private Thread updateThread;
    
    /**
     * RendererUpdate rate. If set to {@code 0} updates will not be generated.
     */
    protected double updatesPerSec = 0;
    
    /**
     * If {@code true} images should be generated for progress update callbacks.
     */
    protected boolean updateImages = false;
    
    /** 
     * If true, use the accelerated batching algorithm.
     * @see #setBatchAccerlated(boolean) 
     */
    protected boolean isBatchAccelerated = true;
    
    /**
     * Maximum batch time. 
     * Used by the accelerated batching algorithm to limit time spent working
     * any given batch.
     * @see #setMaxBatchTimeSec(double)
     * @see #setBatchAccerlated(boolean) 
     */
    protected double maxBatchTimeSec = 1;
    
    /**
     * Returns the task queue for the {@code FlameRenderer}.
     * 
     * @return the task queue for the {@code FlameRenderer}
     */
    public BlockingQueue<RendererTask> getQueue() {
        return queue;
    }
    
    /**
     * Enqueues the specified task at the end of the queue task queue.
     * <br>
     * Equivalent to <pre>{@code getQeueue().add(task)}</pre>
     * 
     * @param task the task to enqueue
     */
    public void enqueueTask(RendererTask task) {
        queue.add(task);
    }

    /**
     * Returns the task that was most recently dequeued from the front of the
     * task queue by the {@code FlameRenderer}. If no tasks have been
     * dequeued by the {@code FlameRenderer}, then this method returns
     * {@code null}.
     * 
     * @return the last task dequeued by the {@code FlameRenderer}
     */
    public RendererTask getTask() {
        return task;
    }
    
    /**
     * Starts the flame renderer. Flames which are added to the queue will not
     * be rendered until this method has been called. If this method or one of
     * the shutdown methods has already been invoked, subsequent invocations of
     * this method will have no effect.
     */
    public void start() {
        mainLock.lock();
        try {
            if (state == READY) {
                state = RUNNING;
                updateThread = new Thread(new UpdateLoop(), "FlameRenderer Update Loop");
                updateThread.start();
                renderThread = new Thread(new RenderLoop(), "FlameRenderer Render Loop");
                renderThread.start();
            }
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Frees threads and resources after all tasks in the queue have been
     * completed or cancelled. Invocation has no additional effect if already
     * shut down.
     * <p>
     * This method does not wait for the shutdown to complete. If needed, use
     * {@link #awaitTermination()} to block until the shutdown has completed.
     */
    public void shutdown() {
        mainLock.lock();
        try {
            if (state <= RUNNING)  {
                state = SHUTDOWN;
            }
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Frees the threads and resources as quickly as possible by canceling the
     * current task and ignoring remaining tasks the queue. Invocation has no
     * additional effect if already shut down.
     * <p>
     * This method does not wait for the shutdown to complete. If needed, use
     * {@link #awaitTermination()} to block until the shutdown has completed.
     */
    public void shutdownNow() {
        mainLock.lock();
        try {
            if (state <= SHUTDOWN)  {
                // Set the new state flag
                state = SHUTDOWN_NOW;
                // Cancel the current task (will not effect the task's state if
                // the task has alreay been completed or cancled)
                if (task != null)
                    task.cancel(true);
            }
        } finally {
            mainLock.unlock();
        }
    }
    
    /**
     * Blocks the thread invoking this method until either
     * {@link #isTerminated()} returns {@code true} or the invoking thread is
     * interrupted, whichever happens first.
     *
     * @throws InterruptedException if the invoking thread was interrupted while waiting
     */
    public void awaitTermination() throws InterruptedException {
        mainLock.lock();
        try {
            while (!isTerminated())
                termination.await();
        } finally {
            mainLock.unlock();
        }
    }
    
    /**
     * Blocks the thread invoking this method until either
     * {@link #isTerminated()} returns {@code true}, the timeout occurs, or the
     * invoking thread is interrupted, whichever happens first.
     * 
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return {@code true} if this renderer terminated and {@code false} if the timeout elapsed before termination
     * @throws InterruptedException if the invoking thread is interrupted while waiting
     */
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        mainLock.lock();
        try {
            while (!isTerminated()) {
                if (nanos <= 0)
                    return false;
                nanos = termination.awaitNanos(nanos);
            }
            return true;
        } finally {
            mainLock.unlock();
        }
    }
    
    /**
     * Returns {@code true} if the the renderer is running. The renderer is in
     * the running state after {@link #start()} has been invoked and neither
     * {@link #shutdown()} or {@link #shutdownNow()} have been invoked.
     * 
     * @return {@code true} if the the renderer is running
     */
    public boolean isRunning() {
        mainLock.lock();
        try {
            return state == RUNNING;
        } finally {
            mainLock.unlock();
        }
    }
    
    /**
     * Returns {@code true} if either {@link #shutdown()} or 
     * {@link #shutdownNow()} has been called.
     *
     * @return {@code true} if either {@code shutdown} method has been called
     */
    public boolean isShutdown() {
        mainLock.lock();
        try {
            return state > RUNNING;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns {@code true} if the shutdown procedure has been completed and all
     * threads and resources have been freed.
     * 
     * @return {@code true} if the shutdown procedure has been completed
     */
    public boolean isTerminated() {
        return state == TERMINATED;
    }
    
    /**
     * Sets the number of progress update callbacks per second. If set to
     * {@code 0}, updates will not be generated.
     * 
     * @param updatesPerSecond the number progress update callbacks per second {@code 0}
     * @throws IllegalArgumentException if {@code updateRate} is not in range [0, inf)
     */
    public void setUpdatesPerSec(double updatesPerSecond) {
        if (!(0<=updatesPerSecond && updatesPerSecond<Double.POSITIVE_INFINITY))
            throw new IllegalArgumentException("updatesPerSecond is not in range [0, inf): "+updatesPerSecond);
        this.updatesPerSec = updatesPerSecond;
    }
    
    /**
     * Returns the number of progress update callbacks per second or {@code 0}. 
     * If {@code 0}, then updates will not be generated.
     * 
     * @return the update rate in updates per second or {@code 0}
     */
    public double getUpdatesPerSec() {
        return updatesPerSec;
    }
    
    /**
     * Sets the update images flag. If set to {@code true}, images will be
     * generated for every progress update callback.
     * 
     * @param updateImages the update images flag
     */
    public void setUpdateImages(boolean updateImages) {
        this.updateImages = updateImages;
    }
    
    /**
     * Return {@code true} if images will be generated for every progress 
     * update callback.
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
     * exceeded the maximum quality or time. This is accomplished by predicting
     * how many more iterations will be necessary before either the quality or
     * time limit are reached and then performing those iterations without
     * checks or updates.
     * <p>
     * The maximum time that can pass between checks is limited by the
     * {@link #setUpdatesPerSec(double) update rate}, the
     * {@link #setMaxBatchTimeSec(double) maximum batch time}, the 
     * {@link RendererSettings#setMaxQuality(double) maximum quality}, and
     * the {@link RendererSettings#setMaxTime(double) maximum render time},
     * whichever comes first.
     * <p>When this flag is set, the {@code FlameRenderer} may reduce the number
     * of updates per second by as much as half of what the 
     * {@code updatesPerSec} parameter would normally imply.
     * <p>
     * Implementations of {@code FlameRenderer} may choose to ignore this flag.
     * 
     * @param isAccelerated the accelerated algorithm flag
     */
    public void setBatchAccerlated(boolean isAccelerated) {
        this.isBatchAccelerated = isAccelerated;
    }
    
    /**
     * Returns the accelerated flag.
     * 
     * @return the accelerated flag
     * @see #setBatchAccerlated(boolean)
     */
    public boolean isBatchAccerlated() {
        return this.isBatchAccelerated;
    }
    
    /**
     * Sets the maximum time the renderer will spend working on a single batch
     * when using the batch-accelerated algorithm. If set to {@code 0}, there
     * will be no limit to the time spent working on a single batch.
     * <p>
     * <b>Warning:</b>The purpose of this flag is to get the program to spend a
     * majority of its time inside of an OpenCL kernel. This means that the
     * kernel can run for relatively long periods of time (potentially several
     * seconds). If this flag is used and the program is executed on a GPU, the
     * video driver may temporarily stop responding to the operating system
     * causing the OS to cancel the operation by resetting the driver. Use a
     * lower batch time or see
     * <a href="http://stackoverflow.com/a/25116354">http://stackoverflow.com/a/25116354</a>
     * for potential ways to fix this problem though OS and driver settings.
     *
     * @param maxBatchTimeSec the maximum time to spend on a single batch
     * @see #setBatchAccerlated(boolean) 
     */
    public void setMaxBatchTimeSec(double maxBatchTimeSec) {
        if (!(0<=maxBatchTimeSec && maxBatchTimeSec < Double.POSITIVE_INFINITY))
            throw new IllegalArgumentException("maxBatchTimeSec is not in range [0,inf): "+maxBatchTimeSec);
        this.maxBatchTimeSec = maxBatchTimeSec;
    }
    
    /**
     * Returns the maximum time the renderer will spend working on a single
     * batch while using the batch-accelerated algorithm.
     * 
     * @return the maximum time to spend on a single batch
     * @see #setMaxBatchTimeSec(double) 
     * @see #setBatchAccerlated(boolean) 
     */
    public double getMaxBatchTimeSec() {
        return maxBatchTimeSec;
    }
    
    /**
     * Returns the maximum time the renderer should spend working on a single
     * batch accounting for the update rate.
     * <p>
     * If either {@link #updatesPerSec} or {@link #maxBatchTimeSec} is non-zero,
     * then of the two values, {@code 1/updatesPerSec} and
     * {@code maxBatchTimeSec}, the smaller value which is both positive and
     * normal is returned. If both {@code #updatesPerSec} and
     * {@code #maxBatchTimeSec} is zero, then zero is returned.
     * 
     * @return the maximum batch time in seconds
     */
    protected double calcMaxBatchTimeSec() {
        double maxBatchTime = Double.MAX_VALUE;
        if (updatesPerSec > 0)
            maxBatchTime = 1/updatesPerSec;
        if (maxBatchTimeSec < maxBatchTime)
            maxBatchTime = maxBatchTimeSec;
        if (maxBatchTime == Double.MAX_VALUE)
            maxBatchTime = 0;
        return maxBatchTime;
    }
    
    /**
     * Called asynchronously by a {@link FlameRenderer} to deliver progress
     * updates and final results while rendering a flame image.
     * 
     * @param task the task which generated the flame
     * @param flame the flame being rendered
     * @param image the current image of the flame
     * @param quality the quality of the image
     * @param points the number of points plotted to render the image
     * @param elapsedTime the amount of time in seconds spent rendering the image
     * @param isFinished {@code true} if the image is finished, {@code false} if work on the image is ongoing
     */
    protected void update(
            RendererTask task, 
            Flame flame, 
            BufferedImage image, 
            double quality, 
            double points, 
            double elapsedTime, 
            boolean isFinished) {
        // If for some crazy reason the update thread isn't alive, we would get
        // blocked trying to transfer the update, so don't even try.
        if (!updateThread.isAlive())
            return;
        // Swap the two update objects
        RendererUpdate updateT;
        updateT = updateA;
        updateA = updateB;
        updateB = updateT;
        // Store the data in an update object
        updateA.renderer = this;
        updateA.task = task;
        updateA.flame = flame;
        updateA.image = image;
        updateA.quality = quality;
        updateA.points = points;
        updateA.elapsedTime = elapsedTime;
        updateA.isFinished = isFinished;
        try {
            // Put the update into the update queue, blocking if necessary
            updateQueue.transfer(updateA);
        } catch (InterruptedException ex) {
            // If interrupted while trying to enqueue the update, propogate the
            // interupt
            Thread.currentThread().interrupt();    
        }
    }
    
    /**
     * Main render loop thread. This thread dequeues takes from the queue and
     * passes their flames one at a time to the {@link #renderNextFlame(RendererTask)}
     * method. If the queue is empty, the thread will wait for either a task to
     * be enqueued or for one of the {@code shutdown()} method's to be called.
     */
    private class RenderLoop implements Runnable {
        @Override
        public void run() {
            // Initialize the renderer's resources
            initResources();
            
            while (state != SHUTDOWN_NOW && !(state == SHUTDOWN && queue.isEmpty())) {
                try {
                    // Block untill a task beccomes avaiable
                    task = queue.poll(timeoutMilli, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ex) {
                    // If interrupted, loop back arround to check the state
                    continue;
                }
                // If the poll did not time out and the task has not been 
                // canceled, start working on the task
                if (task != null && task.setRunningState()) {
                    // While the current task has not been canceled, and the task is
                    // not complete (contains more flames to be rendered)...
                    while (!task.isCancelled() && task.hasNextFlame()) {
                        // Render the flame
                        renderNextFlame(task);
                    }
                    // Signal that the task is completed (if it was not canceled)
                    task.setCompletedState();
                }
            }
            
            // Free the renderer's resources
            freeResources();
            
            // Set the SHUTDOWN_NOW state so the update thread knows to shutdown
            // after processing any remaining updates
            mainLock.lock();
            try {
                state = SHUTDOWN_NOW;
            } finally {
                mainLock.unlock();
            }
        }
    }
    
    /**
     * Update processing loop thread. This thread accepts update objects from
     * the transfer queue and passes them to the callback function so that the
     * render thread can concurrently continue working while updates are being
     * processed (i.e. the callback function is invoked).
     */
    private class UpdateLoop implements Runnable {
        @Override
        public void run() {
            RendererUpdate update = null;
            // Loop until (SHUTDOWN_NOW or TERMINATED) or SHUTDOWN
            while (state != SHUTDOWN_NOW) {
                try {
                    // Block until an update becomes available
                    update = updateQueue.poll(timeoutMilli, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ex) {
                    // If interrupted, loop back arround to check the state
                }
                // If the poll did not time out and the task has not been 
                // canceled...
                if (update != null && !update.getTask().isCancelled()) {
                    // Get the callback function for the update
                    RendererCallback callback = update.task.getCallback();
                    // Invoke the callback function for the update
                    callback.rendererCallback(update);
                }
            }
            
            // Set the terminated state
            mainLock.lock();
            try {
                state = TERMINATED;
                termination.signalAll();
            } finally {
                mainLock.unlock();
            }
        }
    }
    
    /**
     * Allocates and initializes threads and resources used by the 
     * {@code FlameRenderer}.
     */
    protected abstract void initResources();
    
    /**
     * Frees threads and resources used by implementations of 
     * {@code FlameRenderer}.
     */
    protected abstract void freeResources();
    
    /**
     * Renders the next flame returned by the given the
     * {@link RendererTask#getNextFlame() getNextFlame} method of the given task
     * and block until the flame image is complete or canceled.
     * <p>
     * Implementations of this method should use the {@code FlameSettings}
     * returned by {@link RendererTask#getSettings()} to render the flame image
     * and use the methods provided by the {@code FlameCallback} returned by
     * {@link RendererTask#getCallback()} to asynchronously relay progress
     * updates and results to the rest of the application.
     * <p>
     * Implementations of this method are not required to call 
     * {@link RendererTask#hasNextFlame()} to ensure that calling
     * {@link RendererTask#getNextFlame()} will not result in an exception.
     * 
     * @param task the task
     */
    protected abstract void renderNextFlame(RendererTask task);
}
