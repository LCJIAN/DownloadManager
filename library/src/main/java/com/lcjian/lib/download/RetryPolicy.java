package com.lcjian.lib.download;

public interface RetryPolicy {

    boolean shouldRetry(Download download, Throwable throwable);

    interface Factory {

        RetryPolicy createPolicy();
    }
}
