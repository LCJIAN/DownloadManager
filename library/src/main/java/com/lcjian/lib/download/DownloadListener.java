package com.lcjian.lib.download;

import java.util.List;

public interface DownloadListener {

    /**
     * Pay attention to that this method will be called in every {@link Download}'s action thread pool.
     */
    void onDownloadStatusChanged(Download download, DownloadStatus downloadStatus);

    /**
     * Pay attention to that this method will be called in multi {@link ChunkDownload.ChunkDownloader} runnable.
     */
    void onProgress(Download download, long downloadedBytes);

    /**
     * Pay attention to that this method will be called in every {@link Download}'s action thread pool.
     */
    void onRetry(Download download, Throwable throwable);

    void onChunkDownloadsCreate(Download download, List<ChunkDownload> chunkDownloads);

    void onChunkDownloadsDestroy(Download download, List<ChunkDownload> chunkDownloads);

}
