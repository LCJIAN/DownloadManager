package com.lcjian.lib.download;

import com.lcjian.lib.download.exception.ConnectException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public final class ChunkDownload {

    private final Request request;
    private final Chunk chunk;
    private final DownloadAPI downloadAPI;
    private final PersistenceAdapter persistenceAdapter;
    private final CopyOnWriteArrayList<ChunkDownloadListener> listeners;
    private final ChunkDownloader chunkDownloader;
    private final Logger logger;
    private Download download;
    private ChunkDownloadStatus chunkDownloadStatus;
    private long downloadedBytes;

    ChunkDownload(Request request, Chunk chunk, ChunkDownloadStatus chunkDownloadStatus, DownloadAPI downloadAPI,
                  PersistenceAdapter persistenceAdapter, Logger logger) {
        this.request = request;
        this.chunk = chunk;
        this.downloadAPI = downloadAPI;
        this.persistenceAdapter = persistenceAdapter;
        this.logger = logger;
        this.chunkDownloadStatus = chunkDownloadStatus;
        this.listeners = new CopyOnWriteArrayList<>();
        this.chunkDownloader = new ChunkDownloader();

        int status = this.chunkDownloadStatus == null ? ChunkDownloadStatus.IDLE : this.chunkDownloadStatus.getStatus();
        if (status == ChunkDownloadStatus.IDLE
                || status == ChunkDownloadStatus.PENDING
                || status == ChunkDownloadStatus.DOWNLOADING) {
            this.chunkDownloadStatus = new ChunkDownloadStatus(ChunkDownloadStatus.IDLE);
        }
    }

    void attach(Download download) {
        this.download = download;
        File file = new File(chunk.file());
        if (file.exists()) {
            notifyDownloadProgress(file.length());
        }
    }

    ChunkDownloader getChunkDownloader() {
        return chunkDownloader;
    }

    public Request getRequest() {
        return request;
    }

    public Chunk getChunk() {
        return chunk;
    }

    public ChunkDownloadStatus getChunkDownloadStatus() {
        return chunkDownloadStatus;
    }

    public long getDownloadedBytes() {
        return downloadedBytes;
    }

    public void addChunkDownloadListener(ChunkDownloadListener chunkDownloadListener) {
        listeners.add(chunkDownloadListener);
    }

    public void removeChunkDownloadListener(ChunkDownloadListener chunkDownloadListener) {
        listeners.remove(chunkDownloadListener);
    }

    private void notifyDownloadProgress(long delta) {
        this.downloadedBytes += delta;
        for (ChunkDownloadListener chunkDownloadListener : listeners) {
            chunkDownloadListener.onProgress(this, downloadedBytes);
        }
        download.notifyDownloadProgress(delta);
    }

    void notifyChunkDownloadStatus(ChunkDownloadStatus status) {
        chunkDownloadStatus = status;
        persistenceAdapter.saveChunkDownloadStatus(request, chunk, chunkDownloadStatus);
        for (ChunkDownloadListener chunkDownloadListener : listeners) {
            chunkDownloadListener.onDownloadStatusChanged(ChunkDownload.this, chunkDownloadStatus);
        }
        download.updateDownloadStatusAsync();
        logger.finest(Utils.formatString("Download(%s)'s chunk download(%s)'s status is changed, status:%d",
                request.simplifiedId(), chunk.file(), chunkDownloadStatus.getStatus()));
    }

    class ChunkDownloader implements Runnable {

        @SuppressWarnings("all")
        @Override
        public void run() {
            boolean rangeSupportable = download.getDownloadInfo().rangeInfo().rangeSupportable();
            boolean serverFileChanged = download.getDownloadInfo().serverFileChanged();

            File file = new File(chunk.file());
            long start = file.exists() ? chunk.start() + file.length() : chunk.start();
            long end = chunk.end();
            if (file.exists()) {
                if (start - 1 == end) {
                    notifyChunkDownloadStatus(new ChunkDownloadStatus(ChunkDownloadStatus.COMPLETE));
                    return;
                }
            } else {
                File folder = file.getParentFile();
                if (!folder.exists()) {
                    folder.mkdirs();
                }
            }
            if (download.getPauseFlag()) {
                notifyChunkDownloadStatus(new ChunkDownloadStatus(ChunkDownloadStatus.IDLE));
                return;
            }
            InputStream inputStream;
            try {
                if (rangeSupportable) {
                    inputStream = downloadAPI.getInputStream(
                            request.url(),
                            request.headers(),
                            start,
                            end);
                } else {
                    inputStream = downloadAPI.getInputStream(request.url(), request.headers());
                }
            } catch (ConnectException e) {
                notifyChunkDownloadStatus(new ChunkDownloadStatus(e));
                return;
            }
            inputStream = new BufferedInputStream(inputStream);
            OutputStream outputStream = null;
            try {
                notifyChunkDownloadStatus(new ChunkDownloadStatus(ChunkDownloadStatus.DOWNLOADING));
                outputStream = new FileOutputStream(file, rangeSupportable && !serverFileChanged);
                byte data[] = new byte[8192];
                int length;
                while (!download.getPauseFlag()
                        && (length = inputStream.read(data)) != -1) {
                    outputStream.write(data, 0, length);
                    notifyDownloadProgress(length);
                }
                outputStream.flush();
                if (download.getPauseFlag()) {
                    notifyChunkDownloadStatus(new ChunkDownloadStatus(ChunkDownloadStatus.IDLE));
                } else {
                    notifyChunkDownloadStatus(new ChunkDownloadStatus(ChunkDownloadStatus.COMPLETE));
                }
            } catch (IOException e) {
                notifyChunkDownloadStatus(new ChunkDownloadStatus(e));
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
