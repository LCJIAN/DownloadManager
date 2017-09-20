package com.lcjian.lib.download;

import java.util.List;

public interface PersistenceAdapter {

    List<DownloadRecord> getDownloadRecords();

    /**
     * delete the request and it's whole info when delete a download task.
     */
    void deleteRequest(Request request);

    /**
     * Save the request when add a download task.
     */
    void saveRequest(Request request);

    /**
     * Save the download info and chunks when this download has been initialized and split in chunks.
     * Pay attention to that this method will be called in every {@link Download}'s action thread pool.
     */
    void saveDownloadInfo(Request request, DownloadInfo downloadInfo, List<Chunk> chunks);

    /**
     * Update the download's status. Pay attention to that this method will be
     * called in every {@link Download}'s action thread pool.
     */
    void saveDownloadStatus(Request request, DownloadStatus downloadStatus);

    /**
     * Update the download's chunk's status. Pay attention to that this method will be
     * called in every {@link Download}'s action thread pool.
     */
    void saveChunkDownloadStatus(Request request, Chunk chunk, ChunkDownloadStatus chunkDownloadStatus);

}
