package com.ytdd9527.networksexpansion.utils.databases;

import com.balugaq.netex.utils.Debug;
import com.balugaq.netex.utils.Lang;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class QueryQueue {

    private final @NotNull BlockingQueue<QueuedTask> updateTasks;
    private final @NotNull BlockingQueue<QueuedTask> queryTasks;
    private final @NotNull ExecutorService executorService;
    private boolean threadStarted;

    public QueryQueue() {
        // Create database query processing thread
        updateTasks = new LinkedBlockingDeque<>();
        queryTasks = new LinkedBlockingDeque<>();
        executorService = Executors.newFixedThreadPool(2, task -> {
            Thread thread = new Thread(task, "NetworksExpansion-QueryQueue");
            thread.setDaemon(true);
            return thread;
        });

        threadStarted = false;
    }

    public synchronized void scheduleUpdate(@NotNull QueuedTask task) {
        if (!updateTasks.offer(task)) {
            throw new IllegalStateException(
                Lang.getString("messages.unsupported-operation.comprehensive.invalid_queue"));
        }
    }

    public synchronized void scheduleQuery(@NotNull QueuedTask task) {
        if (!queryTasks.offer(task)) {
            throw new IllegalStateException(
                Lang.getString("messages.unsupported-operation.comprehensive.invalid_queue"));
        }
    }

    public void startThread() {
        if (!threadStarted) {
            executorService.submit(getProcessor(queryTasks));
            executorService.submit(getProcessor(updateTasks));
            threadStarted = true;
        }
    }

    public int getTaskAmount() {
        return updateTasks.size() + queryTasks.size();
    }

    public boolean isAllDone() {
        return !threadStarted || getTaskAmount() == 0;
    }

    public void scheduleAbort() {
        QueuedTask abortTask = new QueuedTask() {
            @Override
            public boolean execute() {
                return true;
            }

            @Override
            public boolean callback() {
                return true;
            }
        };
        queryTasks.offer(abortTask);
        updateTasks.offer(abortTask);
        executorService.shutdown();
    }

    public boolean awaitShutdown(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        return executorService.awaitTermination(timeout, unit);
    }

    private @NotNull Runnable getProcessor(@NotNull BlockingQueue<QueuedTask> queue) {
        return () -> {
            while (true) {
                try {
                    QueuedTask task = queue.take();
                    if (task.execute() && task.callback()) {
                        break;
                    }
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    Debug.trace(e);
                }
            }
        };
    }
}
