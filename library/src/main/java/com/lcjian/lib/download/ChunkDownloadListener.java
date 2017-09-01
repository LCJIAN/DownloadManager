package com.lcjian.lib.download;

public interface ChunkDownloadListener {

    /**
     * Pay attention to that this method will be called in every {@link Download}'s action thread pool.
     */
    void onDownloadStatusChanged(ChunkDownload chunkDownload, ChunkDownloadStatus chunkDownloadStatus);

    /**
     * Pay attention to that this method will be called in every {@link ChunkDownload} runnable.
     */
    void onProgress(ChunkDownload chunkDownload, long downloadedBytes);
}
