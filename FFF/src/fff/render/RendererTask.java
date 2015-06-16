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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * A {@link FlameRenderer} task. A task consists of a sequence of {@code Flame}
 * instances, flame image settings, and a call back function which can be
 * retrieved via {@link #getNextFlame()}, {@link #getSettings()}, and
 * {@link #getCallback()} respectively.
 * <p>
 * If a task as not already been completed, then it can be canceled using
 * {@link #cancel(boolean)}, and the status of a task can be queried using the
 * {@link #isCancelled()} and {@link #isTerminated()}. A thread can be blocked
 * until the task has been completed or cancelled using the
 * {@link #awaitTermination()} methods.
 *
 * @author Jeremiah N. Hankins
 */
public abstract class RendererTask {
    /**
     * A nested inner class that implements atomic state transitions.
     */
    private final Sync sync = new Sync();
    
    /**
     * The callback function that should be used by the {@code FlameRenderer}
     * that works on this task.
     */
    private final RendererCallback callback;
    
    /**
     * The renderer settings that should be used by the {@code FlameRenderer}
     * that works on this task.
     */
    private final RendererSettings settings;
    
    /**
     * Initializes a {@code RendererTask} using the specified renderer callback
     * function and settings.
     * 
     * @param callback the renderer callback function
     * @param settings the renderer settings
     */
    public RendererTask( 
            RendererCallback callback,
            RendererSettings settings) {
        if (callback == null)
            throw new IllegalArgumentException("callback = null");
        if (settings == null)
            throw new IllegalArgumentException("settings = null");
        this.callback = callback;
        this.settings = settings;
    }
    
    /**
     * Returns {@code true} if the task has more {@link Flame} instances that
     * can be retrieved via {@link #getNextFlame()}.
     * 
     * @return {@code true} if this task has more flames
     */
    public abstract boolean hasNextFlame();
    
    /**
     * Returns the next {@link Flame} instance for this task. The effect of
     * invoking this method when calling {@link #hasNextFlame()} would return
     * {@code false} is undefined.
     * 
     * @return the next {@link Flame} instance for this task
     */
    public abstract Flame getNextFlame();
    
    /**
     * Returns the {@link RendererCallback} object which allows the 
     * {@link FlameRenderer} that is working on this task to communicate
     * progress and results asynchronously to the rest of the program.
     * 
     * @return the {@code RendererCallback} for this task
     */
    public RendererCallback getCallback() {
        return callback;
    }
    
    /**
     * Returns the {@link RendererSettings} that should be used to render
     * all {@link Flame} instances returned by {@link #getNextFlame()}.
     * 
     * @return the {@code RendererSettings} for this task
     */
    public RendererSettings getSettings() {
        return settings;
    }
    
    /**
     * Returns {@code true} if the task has been cancelled.
     * 
     * @return {@code true} if the task has been cancelled
     */
    public boolean isCancelled() {
        return sync.innerIsCancelled();
    }
    
    /**
     * Returns {@code true} if the task has been completed.
     * 
     * @return {@code true} if the task has been completed
     */
    public boolean isCompleted() {
        return sync.innerIsCompleted();
    }

    /**
     * Returns {@code true} if the task has been completed or cancelled
     * 
     * @return {@code true} if the task has been completed or cancelled
     */
    public boolean isTerminated() {
        return sync.innerIsTerminated();
    }

    /**
     * Attempts to cancel the task. This attempt will fail if the task has
     * already been completed or cancelled. If the task has already started,
     * then the {@code mayCancleIfStarted} parameter determines whether the
     * {@link FlameRenderer} working on this task should attempt to stop the
     * task. If the task is successfully cancelled, then threads blocked by this
     * task's awaitTermination methods will become unblocked and resume.
     *
     * @param mayCancleIfStarted if {@code false} and work on the task has
     * already started, then the task will be completed
     * @return {@code false} if the task could not be cancelled
     */
    public boolean cancel(boolean mayCancleIfStarted) {
        return sync.innerCancel(mayCancleIfStarted);
    }

    /**
     * Blocks the thread invoking this method until either
     * {@link #isTerminated()} returns {@code true} or the invoking thread is
     * interrupted, whichever happens first.
     *
     * @throws InterruptedException if the invoking thread was interrupted while waiting
     */
    public void awaitTermination() throws InterruptedException {
        sync.innerAwaitTermination();
    }

    /**
     * Blocks the thread invoking this method until either
     * {@link #isTerminated()} returns {@code true}, the timeout occurs, or the
     * invoking thread is interrupted, whichever happens first.
     * 
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return {@code true} if this task terminated and {@code false} if the timeout elapsed before termination
     * @throws InterruptedException if the invoking thread is interrupted while waiting
     */
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return sync.innerAwaitTermination(timeout, unit);
    }
    
    /**
     * Invoked by the {@link FlameRenderer} when the the task is started. This
     * method will fail and return {@code false} if the task has already been
     * started at some point or if it has been cancelled.
     *
     * @return {@code true} if the task was in the ready state when this method was invoked
     */
    protected boolean setRunningState() {
        return sync.innerSetRunningState();
    }

    /**
     * Invoked by the {@link FlameRenderer} if and when the task is completed
     * successfully. Invocation of this method causes threads blocked by this
     * task's awaitTermination methods to become unblocked and resume. This
     * method will fail and return {@code false} if the task has not been
     * started yet or has already been cancelled.
     *
     * @return {@code true} if the task was in the running state when this method was invoked
     */
    protected boolean setCompletedState() {
        return sync.innerSetCompletedState();
    }
    
    /**
     * {@code Sync} extends {@link AbstractQueuedSynchronizer} and implements
     * {@code RendererTask} state transitions atomically.
     */
    private final class Sync extends AbstractQueuedSynchronizer {
        /**
         * A state indicating that the task has not started yet.
         */
        public static final int READY     = 0;
        /**
         * A state indicating that the task is underway.
         */
        public static final int RUNNING   = 1;
        /**
         * A state indicating that the task has been completed.
         */
        public static final int COMPLETED = 2;
        /**
         * A state indicating that the task has been cancelled.
         */
        public static final int CANCELLED = 3;
        
        /**
         * Implementation of {@link #isCancelled()}.
         * 
         * @return {@code true} if the task is cancelled
         */
        boolean innerIsCancelled() {
            return getState() == CANCELLED;
        }
        
        /**
         * Implementation of {@link #isCompleted()}.
         * 
         * @return {@code true} if the task is completed
         */
        boolean innerIsCompleted() {
            return getState() == COMPLETED;
        }
        
        /**
         * Implementation of {@link #isTerminated()}.
         * 
         * @return {@code true} if the task is completed or cancelled
         */
        boolean innerIsTerminated() {
            return (getState() & (COMPLETED | CANCELLED)) != 0;
        }
        
        /**
         * Implementation of {@link #setRunningState()}.
         * 
         * @return {@code true} if the previous state was {@code READY}
         */
        boolean innerSetRunningState() {
            // Only transiton into the running state if the task is currently
            // in the ready state.
            return compareAndSetState(READY, RUNNING);
        }
        
        /**
         * Implementation of {@link #setCompletedState()}.
         * 
         * @return {@code true} if the pervious state was {@code RUNNING}
         */
        boolean innerSetCompletedState() {
            // Only transition into the completed state if the task is currently
            // in the running state (i.e. not cancelled).
            if (!compareAndSetState(RUNNING, COMPLETED))
                return false;
            // Unblock any threads blocked in the awaitTermination() methods
            releaseShared(0);
            // Return true
            return true;
        }
        
        /**
         * Implementation of {@link #cancel(boolean)}.
         * 
         * @param mayCancleIfStarted if {@code true}, the state may transition from {@code RUNNING} to {@code CANCELLED}, otherwise it cannot
         * @return {@code true} if the state successfully transitions to {@code CANCELLED}
         */
        boolean innerCancel(boolean mayCancleIfStarted) {
            // We need to atomically set the task state to CANCLED if and only 
            // if (1) the task is not already completed or cancled and (2) if 
            // the task is running the mayCancleIfStarted flag is set.
            // However, AbstractQueuedSynchronizer does not provide atomic 
            // methods this complex, but we can get it done with a short spin 
            // loop: We aquire the state at the beginning of the loop, check the
            // conditions that would cause the cancelation to fail, and only set
            // the cancelled state if the state has not changed since we started
            // the loop and preforemed the checks. If the state has changed, 
            // then the new state may fail the checks. The state transitions and
            // checks prevent this loop from iterating more than twice.
            for (;;) {
                int s = getState();
                // If the mayCancleIfStarted flat is set, and the task was 
                // started at any point in the past, return false
                if (mayCancleIfStarted && s != READY)
                    return false;
                // If the task has already completed or already been cancled,
                // then there is no reason to cancle again, so return false
                if ((s & (COMPLETED | CANCELLED)) != 0)
                    return false;
                // If the state has not been changed during this iteration set
                // the state to cancled, otherwise loop arround and try again
                if (compareAndSetState(s, CANCELLED))
                    break;
            }
            // Unblock any threads blocked in the awaitTermination() methods
            releaseShared(0);
            // Return true
            return true;
        }
        
        /**
         * Implementation of {@link #awaitTermination()}.
         * 
         * @throws InterruptedException if the invoking thread is interrupted
         */
        void innerAwaitTermination() throws InterruptedException {
            // Block until innerIsTerminated returns true (via tryAcquireShared)
            acquireSharedInterruptibly(0);
        }
        
        /**
         * Implementation of {@link #awaitTermination(long)}.
         * 
         * @return {@code} true if terminated and {@code false} if timed out
         * @throws InterruptedException if the invoking thread is interrupted
         */
        boolean innerAwaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            // Block until innerIsTerminated returns true (via tryAcquireShared)
            long nanos = unit.toNanos(timeout);
            return tryAcquireSharedNanos(0, nanos);
        }
        
        @Override
        protected int tryAcquireShared(int ignore) {
            return innerIsTerminated() ? 1 : -1;
        }
        
        @Override
        protected boolean tryReleaseShared(int ignore) {
            return true;
        }
    }
}
