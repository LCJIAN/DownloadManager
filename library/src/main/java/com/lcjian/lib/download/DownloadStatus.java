package com.lcjian.lib.download;

import java.io.Serializable;

public class DownloadStatus implements Serializable {

    public static final int IDLE = 0;
    public static final int PENDING = 1;
    public static final int INITIALIZING = 2;
    public static final int CHUNK_PENDING = 3;
    public static final int DOWNLOADING = 4;
    public static final int ERROR = 5;
    public static final int MERGING = 6;
    public static final int MERGE_ERROR = 7;
    public static final int COMPLETE = 8;
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 1L;
    private int status;

    private Throwable throwable;

    DownloadStatus(int status) {
        if (status == ERROR || status == MERGE_ERROR) {
            throw new IllegalArgumentException("DownloadStatus ERROR");
        }
        this.status = status;
    }

    DownloadStatus(Throwable throwable) {
        this.status = ERROR;
        this.throwable = throwable;
    }

    DownloadStatus(int status, Throwable throwable) {
        if (status != MERGE_ERROR) {
            throw new IllegalArgumentException("DownloadStatus ERROR");
        }
        this.status = status;
        this.throwable = throwable;
    }

    public int getStatus() {
        return status;
    }

    public Throwable getThrowable() {
        return throwable;
    }
}
