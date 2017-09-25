package com.lcjian.lib.download;

import java.io.Serializable;

public class ChunkDownloadStatus implements Serializable {

    public static final int IDLE = 0;
    public static final int PENDING = 1;
    public static final int DOWNLOADING = 2;
    public static final int ERROR = 3;
    public static final int COMPLETE = 4;
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 1L;
    private int status;

    private Throwable throwable;

    ChunkDownloadStatus(int status) {
        if (status == ERROR) {
            throw new IllegalArgumentException("You should call ChunkDownloadStatus(Throwable throwable).");
        }
        this.status = status;
    }

    ChunkDownloadStatus(Throwable throwable) {
        this.status = ERROR;
        this.throwable = throwable;
    }

    public int getStatus() {
        return status;
    }

    public Throwable getThrowable() {
        return throwable;
    }
}