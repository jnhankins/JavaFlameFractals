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
import java.util.concurrent.CancellationException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * A {@link FlameRenderer} task. A task consists of a sequence of {@code Flame}
 * instances, flame image settings, and a call back function which can be
 * retrieved via {@link #getNextFlame()}, {@link #getSettings()}, and
 * {@link #getCallback()} respectively.
 * <p>
 * If a task as not already been completed, then it can be canceled using 
 * {@link #cancel(boolean)}, and the status of a task can be queried using the 
 * {@link #isCancelled()} and {@link #isDone()}. A thread can be blocked until
 * the task has been completed or cancelled using the {@link #block()} methods.
 * 
 * @author Jeremiah N. Hankins
 */
public abstract class RendererTask {
    private final Sync sync = new Sync();
    
    private final RendererCallback callback;
    private final RendererSettings settings;
    
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
     * Returns {@code true} if the task has been cancelled or completed 
     * successfully.
     * 
     * @return {@code true} if the task has been cancelled or completed 
     * successfully
     */
    public boolean isDone() {
        return sync.innerIsDone();
    }

    /**
     * Attempts to cancel the task. This attempt will fail if the task has
     * already completed or has already been cancelled. If the task has already
     * started, then the {@code mayCancleIfStarted} parameter determines
     * whether the {@link FlameRenderer} working on this task should attempt
     * to stop the task. If the task is successfully cancelled, then threads
     * blocked by this task's block methods will become unblocked and resume.
     * 
     * @param mayCancleIfStarted if {@code false} and work on the task has
     * already started, then the task will be completed
     * @return {@code false} if the task could not be cancelled
     */
    public boolean cancel(boolean mayCancleIfStarted) {
        return sync.innerCancel(mayCancleIfStarted);
    }

    /**
     * Blocks the thread invoking this method until the task 
     * {@link #isDone() is done}.
     * 
     * @throws InterruptedException if the invoking thread was interrupted while waiting
     */
    public void block() throws InterruptedException {
        sync.innerBlock();
    }

    /**
     * Blocks the thread invoking this method until the task 
     * {@link #isDone() is done} or given amount of time in nanoseconds has 
     * elapsed.
     * 
     * @param nanosTimeout the maximum amount of time in nanoseconds to wait
     * @throws InterruptedException if the invoking thread was interrupted while waiting
     */
    public void block(long nanosTimeout) throws InterruptedException {
        sync.innerBlock(nanosTimeout);
    }
    
    /**
     * Invoked by the {@link FlameRenderer} when the the task is started. This
     * method will fail and return {@code false} if the task has already been
     * started at some point or if it has been cancelled.
     *
     * @return {@code true} if the task was in the ready state when this method was invoked
     */
    protected boolean run() {
        return sync.innerRun();
    }

    /**
     * Invoked by the {@link FlameRenderer} if and when the task is completed
     * successfully. Invocation of this method causes threads blocked by this
     * task's block methods to become unblocked and resume. This method will
     * fail and return {@code false} if the task has not been started yet or has
     * already been cancelled.
     *
     * @return {@code true} if the task was in the running state when this method was invoked
     */
    protected boolean done() {
        return sync.innerDone();
    }
    
    private final class Sync extends AbstractQueuedSynchronizer {
        public static final int READY     = 0;
        public static final int RUNNING   = 1;
        public static final int COMPLETED = 2;
        public static final int CANCELLED = 4;
        
        boolean innerIsCancelled() {
            // Return true if the task is cancelled
            return getState() == CANCELLED;
        }
        
        boolean innerIsDone() {
            // Return true if the task is completed or cancelled
            return (getState() & (COMPLETED | CANCELLED)) != 0;
        }
        
        boolean innerRun() {
            // Only transiton into the running state if the task is currently
            // in the ready state.
            return compareAndSetState(READY, RUNNING);
        }
        
        boolean innerDone() {
            // Only transition into the done state if the task is currently
            // in the running state (i.e. not cancelled).
            if (!compareAndSetState(RUNNING, COMPLETED))
                return false;
            // Unblock any threads blocked in the get methods
            releaseShared(0);
            // Return true
            return true;
        }
        
        boolean innerCancel(boolean mayCancleIfStarted) {
            // We need to atomically set the task state to CANCLED if and 
            // only if the task is not already done or cancled and, if the
            // mayCancleIfStarted flag is set not running.
            // However, AbstractQueuedSynchronizer does not provide atomic 
            // methods this complex, so we'll use a short spin loop. We aquire
            // the state at the beginning of the loop, check the conditions that
            // would cause the cancelation to fail, and only set the canclled
            // state if the state has not changed since we preforemed the
            // checks. If the state has changed, then the new state may fail
            // the checks. The state transitions and checks prevent this loop 
            // from iterating more than twice.
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
            // Unblock any threads blocked in the get methods
            releaseShared(0);
            // Return true
            return true;
        }
        
        void innerBlock() throws InterruptedException {
            // Block until innerIsDone return true (via tryAcquireShared)
            acquireSharedInterruptibly(0);
        }
        
        void innerBlock(long nanosTimeout) throws InterruptedException {
            // Block until innerIsDone return true (via tryAcquireShared)
            tryAcquireSharedNanos(0, nanosTimeout);
        }
        
        @Override
        protected int tryAcquireShared(int ignore) {
            return innerIsDone() ? 1 : -1;
        }
        
        @Override
        protected boolean tryReleaseShared(int ignore) {
            return true;
        }
    }
}
