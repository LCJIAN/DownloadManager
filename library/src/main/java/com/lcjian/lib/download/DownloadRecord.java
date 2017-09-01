package com.lcjian.lib.download;

import java.io.Serializable;
import java.util.List;

public class DownloadRecord implements Serializable {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 1L;

    private Request request;

    private DownloadInfo downloadInfo;

    private DownloadStatus downloadStatus;

    private List<ChunkRecord> chunkRecords;

    public DownloadRecord(Request request,
                          DownloadInfo downloadInfo,
                          DownloadStatus downloadStatus,
                          List<ChunkRecord> chunkRecords) {
        this.request = request;
        this.downloadInfo = downloadInfo;
        this.downloadStatus = downloadStatus;
        this.chunkRecords = chunkRecords;
    }

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public DownloadInfo getDownloadInfo() {
        return downloadInfo;
    }

    public void setDownloadInfo(DownloadInfo downloadInfo) {
        this.downloadInfo = downloadInfo;
    }

    public DownloadStatus getDownloadStatus() {
        return downloadStatus;
    }

    public void setDownloadStatus(DownloadStatus downloadStatus) {
        this.downloadStatus = downloadStatus;
    }

    public List<ChunkRecord> getChunkRecords() {
        return chunkRecords;
    }

    public void setChunkRecords(List<ChunkRecord> chunkRecords) {
        this.chunkRecords = chunkRecords;
    }

    public static class ChunkRecord implements Serializable {

        /**
         * serialVersionUID
         */
        private static final long serialVersionUID = 1L;

        private Chunk chunk;
        private ChunkDownloadStatus chunkDownloadStatus;

        public ChunkRecord(Chunk chunk, ChunkDownloadStatus chunkDownloadStatus) {
            this.chunk = chunk;
            this.chunkDownloadStatus = chunkDownloadStatus;
        }

        public Chunk getChunk() {
            return chunk;
        }

        public void setChunk(Chunk chunk) {
            this.chunk = chunk;
        }

        public ChunkDownloadStatus getChunkDownloadStatus() {
            return chunkDownloadStatus;
        }

        public void setChunkDownloadStatus(ChunkDownloadStatus chunkDownloadStatus) {
            this.chunkDownloadStatus = chunkDownloadStatus;
        }
    }
}
