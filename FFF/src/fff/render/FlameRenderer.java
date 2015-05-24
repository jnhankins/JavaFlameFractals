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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A flame image rendering engine.
 * <p>
 * Rendering {@link FlameRendererTask tasks} consist of an object containing 
 * {@link FlameRendererSettings settings}, a 
 * {@link FlameRendererCallback callback function}, and a sequence of 
 * {@link Flame} instances. Tasks are passed to a {@code FlameRenderer} for
 * rendering by adding them into the renderer's task queue which can be 
 * retrieved via {@link #getQueue()}.
 * <p>
 * Work on the first task will not begin until the {@link #start()} method has 
 * been invoked. Once the renderer has been started, an internal thread will
 * begin a loop which, on each iteration, takes the next task from the front of 
 * the queue and begins working on rendering it's images. If there are no tasks
 * in the queue, the internal thread will block until a task becomes available.
 * The task most recently dequeued for rendering can be retrieved via 
 * {@link #getTask()}. Any task can be cancelled at any time by invoking its
 * {@link FlameRendererTask#cancel(boolean) cancel()} method.
 * <p>
 * Use {@link #shutdown()} method to shutdown the renderer and its threads after
 * all tasks in the queue have been completed or cancelled and the queue is 
 * empty, or use {@link #shutdownNow()} to cancel the current task is one is 
 * currently underway and to ignore the remaining tasks in the queue so that
 * resources can be freed as quickly as possible. Use 
 * {@link #awaitTermination(long)} to cause a thread to block and wait until the
 * shutdown procedure has completed. To determine the current status of the
 * renderer query the {@link #isRunning()}, {@link #isShutdown()}, and
 * {@link #isTerminated()} methods.
 * <p>
 * Since work on individual flame images can be very time consuming, 
 * communication between the renderer and the rest of the program is done
 * asynchronously through the callback functions contained within the individual
 * render tasks. If the renderer's {@link #setUpdatesPerSec(double) update rate}
 * is set to a positive value, then the renderer will use the call back function
 * to deliver progress update information (e.g. time elapsed, percent complete,
 * etc..). If the {@link #setUpdateImages(boolean) update-images} flag is
 * set, then the progress updates will contain a preview of what the flame image
 * looks like so far, though this entails extra computation and will increase
 * the total amount of time needed to complete the final image. The final image
 * is also relayed through the callback function using the same method as the
 * progress updates.
 * <p>
 * {@code FlameRenderer} is designed for use across multiple threads and its
 * methods are synchronized where appropriate. All methods are non-blocking 
 * unless explicitly stated otherwise.
 * 
 * @author Jeremiah N. Hankins
 */
public abstract class FlameRenderer {
    private static final int READY        = -1;
    private static final int RUNNING      = 0;
    private static final int SHUTDOWN     = 1;
    private static final int SHUTDOWN_NOW = 2;
    private static final int TERMINATED   = 3;
    
    /**
     * Keeps track of the state (e.g. running, shutdown, terminated, etc...).
     */
    private volatile int state = READY;
    
    /** 
     * Synchronizes methods which query or alter the renderer's state.
     */
    private final ReentrantLock mainLock = new ReentrantLock();
    
    /**
     * Condition for entering the terminated state used by
     * {@link #awaitTermination()}.
     */
    private final Condition termination = mainLock.newCondition();
    
    /** 
     * Queue of tasks.
     */
    private final BlockingQueue<FlameRendererTask> queue = new LinkedBlockingQueue();
    
    /**
     * Task most recently dequeued for rendering.
     */
    private FlameRendererTask task;
    
    /**
     * Main thread for dequeuing tasks and passing them to the flame renderer.
     */
    private final Thread renderLoop = new Thread(new RenderLoop(), "Flame Engine Render Loop");
    
    /**
     * 
     */
    protected double updatesPerSec = 0;
    
    /**
     * If {@code true} images should be generated for progress update callbacks.
     */
    protected boolean updateImages = false;
    
    /**
     * Returns the task queue for the {@code FlameRenderEngine}.
     * 
     * @return the task queue for the {@code FlameRenderEngine}
     */
    public BlockingQueue<FlameRendererTask> getQueue() {
        return queue;
    }

    /**
     * Returns the task that was most recently dequeued from the front of the
     * task queue by the {@code FlameRenderEngine}. If no tasks have been
     * dequeued by the {@code FlameRenderEngine}, then this method returns
     * {@code null}.
     * 
     * @return the last task dequeued by the {@code FlameRenderEngine}
     */
    public FlameRendererTask getTask() {
        return task;
    }
    
    /**
     * Starts the flame renderer. Flames which are added to the queue will not
     * be rendered until this method has been called. If this method has already
     * been invoked, subsequent invocations will have no effect.
     */
    public void start() {
        mainLock.lock();
        try {
            if (state == READY) {
                renderLoop.start();
                state = RUNNING;
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
     * This method does not block. If needed, use {@link #awaitTermination()} 
     * to wait until the shutdown has completed.
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
     * This method does not block. If needed, use {@link #awaitTermination()} 
     * to wait until the shutdown has completed.
     */
    public void shutdownNow() {
        mainLock.lock();
        try {
            if (state <= RUNNING)  {
                // Cancel the current task (will not effect the task's state if
                // the task has alreay been completed or cancle
                if (task != null)
                    task.cancel(true);
                state = SHUTDOWN_NOW;
            }
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Blocks the current thread until {@link #isTerminated()} returns
     * {@code true}, or the timeout occurs, or the current thread is
     * interrupted, whichever happens first.
     *
     * @param nanos the maximum time to wait in nanoseconds
     * @return {@code false} if the timeout elapsed before termination
     * 
     * @throws java.lang.InterruptedException if interrupted while waiting
     */
    public boolean awaitTermination(long nanos) throws InterruptedException {
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
     * Main render loop thread. This thread takes tasks from the queue and
     * passes them to the {@link #renderNextFlame(fff.render3.FlameTask) rendernextFlame}
     * method. If the queue is empty, the thread will wait for either a task to
     * be enqueued or for one of the {@code shutdown} method's to be called.
     */
    private class RenderLoop implements Runnable {
        @Override
        public void run() {
            // Used to ensure the render loop thread is terminated as fast as 
            // possible when the program is shutting down
            final Thread shutdownHook = new Thread() {
                @Override
                public void run() {
                    shutdownNow();
                }
            };
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            
            // Initialize the renderer's resources
            initResources();
            
            while (!(state == SHUTDOWN_NOW || (state == SHUTDOWN && queue.isEmpty()))) {
                try {
                    // Block untill a task beccomes avaiable
                    task = queue.take();
                } catch (InterruptedException ex) {
                    // If interrupted, loop back arround to check the state
                    continue;
                }
                // If the task has not been canceled, start working on the task
                if (task.run()) {
                    // While the current task has not been canceled, and the task is
                    // not complete (contains more flames to be rendered)...
                    while (!task.isCancelled() && task.hasNextFlame()) {
                        // Render the next flame
                        renderNextFlame(task);
                    }
                    // Signal that the task is done (if it was not canceled)
                    task.done();
                }
            }
            
            // Free the renderer's resources
            freeResources();
            
            // Set the terminated state
            mainLock.lock();
            try {
                state = TERMINATED;
                termination.signalAll();
            } finally {
                mainLock.unlock();
            }
            
            try {
                // To prevent a memory leek, remove the shutdown hook
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (Exception ignore) {
                // If there was an exception, it was because we're already in
                // the shutdown phase, so just swollow the exception
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
     * {@link FlameTask#getNextFlame() getNextFlame} method of the given task
     * and block until the flame image is complete or canceled.
     * <p>
     * Implementations of this method should use the {@code FlameSettings} 
     * returned by {@link FlameTask#getSettings()} to render the flame image and
     * use the methods provided by the {@code FlameCallback} returned by 
     * {@link FlameTask#getCallback()} to asynchronously relay progress updates
     * and results to the rest of the application.
     * <p>
     * Implementations of this method are not required to call 
     * {@link FlameTask#hasNextFlame()} to ensure that calling
     * {@link FlameTask#getNextFlame()} will not result in an exception.
     * 
     * @param task the task
     */
    protected abstract void renderNextFlame(FlameRendererTask task);
}
