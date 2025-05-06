package org.yakdanol.nstrafficsecurityservice.users.request;

import java.util.concurrent.CompletableFuture;

public class TaskCanceller {
    private final CompletableFuture<Void> cancelled = new CompletableFuture<>();
    boolean isCancelled() {
        return cancelled.isDone();
    }

    public void cancel() {
        cancelled.complete(null);
    }
}
