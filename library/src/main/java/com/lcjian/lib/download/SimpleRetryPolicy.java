package com.lcjian.lib.download;

import com.lcjian.lib.download.exception.FileExistsException;

public final class SimpleRetryPolicy implements RetryPolicy {

    private final int retryCount;

    private int count;

    private SimpleRetryPolicy(int retryCount) {
        super();
        this.retryCount = retryCount;
    }

    @Override
    public boolean shouldRetry(Download download, Throwable throwable) {
        if (throwable instanceof FileExistsException) {
            return false;
        }
        boolean result = count++ < retryCount;
        if (!result) {
            count = 0;
        }
        return result;
    }

    public static final class Factory implements RetryPolicy.Factory {

        private final int mRetryCount;

        private Factory(int retryCount) {
            super();
            this.mRetryCount = retryCount;
        }

        public static Factory create(int retryCount) {
            return new Factory(retryCount);
        }

        @Override
        public RetryPolicy createPolicy() {
            return new SimpleRetryPolicy(mRetryCount);
        }
    }
}
