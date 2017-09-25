package com.lcjian.lib.download;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
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
    private long length;

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
        this.length = chunk.end() - chunk.start();
    }

    void attach(Download download) {
        this.download = download;
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
        if (length > 0) {
            logger.finest(Utils.formatString("%s %s download of download(%s)'s chunk download(%s)",
                    Utils.formatBytes(downloadedBytes, 2), Utils.formatPercent(downloadedBytes / (double) length), request.simplifiedId(), chunk.file()));
        } else {
            logger.finest(Utils.formatString("%s download of download(%s)'s chunk download(%s)",
                    Utils.formatBytes(downloadedBytes, 2), request.simplifiedId(), chunk.file()));
        }
    }

    void notifyDownloadStatus(final ChunkDownloadStatus status) {
        download.execute(new Runnable() {
            @Override
            public void run() {
                chunkDownloadStatus = status;
                persistenceAdapter.saveChunkDownloadStatus(request, chunk, chunkDownloadStatus);

                for (ChunkDownloadListener chunkDownloadListener : listeners) {
                    chunkDownloadListener.onDownloadStatusChanged(ChunkDownload.this, chunkDownloadStatus);
                }
                download.notifyDownloadStatus(chunkDownloadStatus);
                logger.finer(Utils.formatString("Download(%s)'s chunk download(%s)'s status:%d",
                        request.simplifiedId(), chunk.file(), chunkDownloadStatus.getStatus()));
            }
        });
    }

    private static class ProgressOutputStream extends FilterOutputStream {

        private static final int NOTIFICATION_THRESHOLD = 24 * 1024;

        private final ProgressListener listener;

        private int delta;

        /**
         * Creates an output stream filter built on top of the specified
         * underlying output stream.
         *
         * @param out the underlying output stream to be assigned to
         *            the field <tt>this.out</tt> for later use, or
         *            <code>null</code> if this instance is to be
         *            created without an underlying stream.
         */
        ProgressOutputStream(OutputStream out, ProgressListener listener) {
            super(out);
            this.listener = listener;
        }

        public void write(int b) throws IOException {
            super.write(b);
            notifyProgress();
        }

        @Override
        public void close() throws IOException {
            if (delta > 0) {
                listener.progressChanged(delta);
                delta = 0;
            }
            super.close();
        }

        private void notifyProgress() {
            delta++;
            if (delta >= NOTIFICATION_THRESHOLD) {
                listener.progressChanged(delta);
                delta = 0;
            }
        }

        interface ProgressListener {
            void progressChanged(int delta);
        }
    }

    class ChunkDownloader implements Runnable {

        @Override
        public void run() {
            boolean rangeSupportable = download.getDownloadInfo().rangeInfo().rangeSupportable();
            boolean serverFileChanged = download.getDownloadInfo().serverFileChanged();

            File file = new File(chunk.file());
            long start = file.exists() ? chunk.start() + file.length() : chunk.start();
            long end = chunk.end();
            if (file.exists()) {
                if (rangeSupportable) {
                    if (serverFileChanged) {
                        if (file.delete()) {
                            notifyDownloadStatus(new ChunkDownloadStatus(new RuntimeException("Can not delete old chunk file")));
                            return;
                        }
                    } else {
                        notifyDownloadProgress(file.length());
                        if (start - 1 == end) {
                            notifyDownloadStatus(new ChunkDownloadStatus(ChunkDownloadStatus.COMPLETE));
                            return;
                        }
                    }
                }
            }
            if (!file.exists()) {
                try {
                    File folder = new File(file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf(File.separator)));
                    if (!folder.exists()) {
                        if (folder.mkdirs()) {
                            notifyDownloadStatus(new ChunkDownloadStatus(new RuntimeException("Can not mkdirs")));
                            return;
                        }
                    }
                    if (!file.createNewFile()) {
                        notifyDownloadStatus(new ChunkDownloadStatus(new RuntimeException("Can not create file")));
                        return;
                    }
                } catch (IOException e) {
                    notifyDownloadStatus(new ChunkDownloadStatus(e));
                    return;
                }
            }
            if (download.getPauseFlag()) {
                notifyDownloadStatus(new ChunkDownloadStatus(ChunkDownloadStatus.IDLE));
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
            } catch (Exception e) {
                notifyDownloadStatus(new ChunkDownloadStatus(e));
                return;
            }
            if (inputStream == null) {
                notifyDownloadStatus(new ChunkDownloadStatus(new RuntimeException("null inputStream")));
                return;
            }
            inputStream = new BufferedInputStream(inputStream);
            OutputStream outputStream = null;
            try {
                notifyDownloadStatus(new ChunkDownloadStatus(ChunkDownloadStatus.DOWNLOADING));
                outputStream = new ProgressOutputStream(new BufferedOutputStream(new FileOutputStream(file, rangeSupportable), 204800),
                        new ProgressOutputStream.ProgressListener() {
                            @Override
                            public void progressChanged(int delta) {
                                notifyDownloadProgress(delta);
                            }
                        });
                byte data[] = new byte[1024];
                int length;
                while (!download.getPauseFlag()
                        && (length = inputStream.read(data)) != -1) {
                    outputStream.write(data, 0, length);
                }
                outputStream.flush();
                if (download.getPauseFlag()) {
                    notifyDownloadStatus(new ChunkDownloadStatus(ChunkDownloadStatus.IDLE));
                } else {
                    notifyDownloadStatus(new ChunkDownloadStatus(ChunkDownloadStatus.COMPLETE));
                }
            } catch (Exception e) {
                notifyDownloadStatus(new ChunkDownloadStatus(e));
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
