package com.lcjian.lib.download;

public interface RetryPolicy {

    boolean shouldRetry(Download download, Throwable throwable);
    
    public interface Factory {
        
        RetryPolicy createPolicy();
    }
}
