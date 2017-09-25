package com.lcjian.lib.download;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class Download {

    private final Request request;
    private final Splitter splitter;
    private final DownloadAPI downloadAPI;
    private final RetryPolicy retryPolicy;
    private final PersistenceAdapter persistenceAdapter;
    private final ExecutorService chunkDownloadThreadPool;
    private final String defaultDestination;
    private final CopyOnWriteArrayList<DownloadListener> listeners;
    private final AtomicLong downloadedBytes;
    private final AtomicBoolean pauseFlag = new AtomicBoolean(true);
    private final AtomicBoolean retryFlag = new AtomicBoolean(false);
    private final AtomicBoolean shutdownFlag = new AtomicBoolean(false);
    private final AtomicBoolean deleteFlag = new AtomicBoolean(false);
    private final Semaphore semaphore;
    private ExecutorService actionThreadPool;
    private List<ChunkDownload> chunkDownloads;
    private DownloadStatus downloadStatus;
    private DownloadInfo downloadInfo;
    private boolean shutdown = false;
    private boolean needRelease = true;

    Download(Request request, DownloadStatus downloadStatus, DownloadInfo downloadInfo,
             List<ChunkDownload> chunkDownloads, String defaultDestination, Splitter splitter, DownloadAPI downloadAPI,
             RetryPolicy retryPolicy, PersistenceAdapter persistenceAdapter, ExecutorService chunkDownloadThreadPool, Semaphore semaphore) {
        this.request = request;
        this.downloadStatus = downloadStatus;
        this.downloadInfo = downloadInfo;
        this.chunkDownloads = chunkDownloads;
        this.splitter = splitter;
        this.downloadAPI = downloadAPI;
        this.retryPolicy = retryPolicy;
        this.persistenceAdapter = persistenceAdapter;
        this.chunkDownloadThreadPool = chunkDownloadThreadPool;
        this.defaultDestination = defaultDestination;
        this.semaphore = semaphore;
        this.downloadedBytes = new AtomicLong();
        this.listeners = new CopyOnWriteArrayList<>();

        if (this.chunkDownloads == null) {
            this.chunkDownloads = new ArrayList<>();
        } else {
            for (ChunkDownload chunkDownload : this.chunkDownloads) {
                chunkDownload.attach(this);
            }
        }
        int status = this.downloadStatus == null ? DownloadStatus.IDLE : this.downloadStatus.getStatus();
        if (status == DownloadStatus.IDLE
                || status == DownloadStatus.PENDING
                || status == DownloadStatus.INITIALIZING
                || status == DownloadStatus.CHUNK_PENDING
                || status == DownloadStatus.DOWNLOADING
                || status == DownloadStatus.MERGING) {
            this.downloadStatus = new DownloadStatus(DownloadStatus.IDLE);
        }
    }

    public Request getRequest() {
        return request;
    }

    public List<ChunkDownload> getChunkDownloads() {
        return Collections.unmodifiableList(chunkDownloads);
    }

    public File getDownloadFile() {
        return downloadInfo == null ? null : new File(
                Utils.isEmpty(request.destination()) ? defaultDestination : request.destination(),
                Utils.isEmpty(request.fileName()) ? downloadInfo.initInfo().fileName() : request.fileName());
    }

    public DownloadInfo getDownloadInfo() {
        return downloadInfo;
    }

    public DownloadStatus getDownloadStatus() {
        return downloadStatus;
    }

    public long getDownloadedBytes() {
        return downloadedBytes.get();
    }

    public void addDownloadListener(DownloadListener downloadListener) {
        listeners.add(downloadListener);
    }

    public void removeDownloadListener(DownloadListener downloadListener) {
        listeners.remove(downloadListener);
    }

    boolean getPauseFlag() {
        return pauseFlag.get();
    }

    void pauseAsync() {
        if (!pauseFlag.get()) {
            pauseFlag.getAndSet(true);
            List<Runnable> runnableList = actionThreadPool.shutdownNow();
            for (Runnable runnable : runnableList) {
                execute(runnable);
            }
        } else {
            if (!shutdownFlag.get()) {
                // This download is paused or pausing.
            }
        }
    }

    void resumeAsync() {
        if (pauseFlag.get()) {
            execute(new Runnable() {
                @Override
                public void run() {
                    pauseFlag.getAndSet(false);
                    int st = downloadStatus.getStatus();
                    if (retryFlag.get()
                            || st == DownloadStatus.IDLE
                            || st == DownloadStatus.ERROR
                            || st == DownloadStatus.MERGE_ERROR) {
                        if (retryFlag.get()) {
                            retryFlag.getAndSet(false);
                        } else {
                            try {
                                notifyDownloadStatus(new DownloadStatus(DownloadStatus.PENDING));
                                semaphore.acquire();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                needRelease = false;
                                notifyDownloadStatus(new DownloadStatus(DownloadStatus.IDLE));
                                return;
                            }
                        }
                        initialize();
                    } else {
                        // This download is running.
                    }
                }
            });
        } else {
            // This download is running;
        }
    }

    void shutdownAsync(boolean deleteFile) {
        if (!shutdownFlag.get()) {
            // use for winding up when this download is paused.
            shutdownFlag.getAndSet(true);
            deleteFlag.getAndSet(deleteFile);
            pauseAsync();

            // If this download is paused already, we wind up this download directly.
            execute(new Runnable() {
                @Override
                public void run() {
                    int st = downloadStatus.getStatus();
                    if (st == DownloadStatus.IDLE
                            || st == DownloadStatus.ERROR
                            || st == DownloadStatus.MERGE_ERROR
                            || st == DownloadStatus.COMPLETE) {
                        windUp();
                    }
                }
            });
        } else {
            // This download is stopped or stopping;
        }
    }

    void notifyDownloadProgress(long delta) {
        long bytes = this.downloadedBytes.addAndGet(delta);
        for (DownloadListener downloadListener : listeners) {
            downloadListener.onProgress(this, bytes);
        }
    }

    private void notifyDownloadStatus(DownloadStatus status) {
        int st = downloadStatus.getStatus();
        boolean changed = st != status.getStatus();
        if (changed) {
            if ((st == DownloadStatus.ERROR || st == DownloadStatus.MERGE_ERROR)
                    && retryPolicy.shouldRetry(Download.this, status.getThrowable())) {
                retryFlag.getAndSet(true);
                for (DownloadListener downloadListener : listeners) {
                    downloadListener.onRetry(Download.this, status.getThrowable());
                }
                resumeAsync();
                return;
            }

            downloadStatus = status;
            persistenceAdapter.saveDownloadStatus(request, downloadStatus);
            if (st == DownloadStatus.IDLE
                    || st == DownloadStatus.ERROR
                    || st == DownloadStatus.MERGE_ERROR
                    || st == DownloadStatus.COMPLETE) {
                if (needRelease) {
                    semaphore.release();
                }
                windUp();
            }
            for (DownloadListener downloadListener : listeners) {
                downloadListener.onDownloadStatusChanged(Download.this, downloadStatus);
            }
        }
    }

    void notifyDownloadStatus(ChunkDownloadStatus status) {
        DownloadStatus tempDownloadStatus = null;
        for (ChunkDownload chunkDownload : chunkDownloads) {
            if (chunkDownload.getChunkDownloadStatus().getStatus() == ChunkDownloadStatus.DOWNLOADING) {
                tempDownloadStatus = new DownloadStatus(DownloadStatus.DOWNLOADING);
                break;
            }
        }
        if (tempDownloadStatus == null) {
            for (ChunkDownload chunkDownload : chunkDownloads) {
                if (chunkDownload.getChunkDownloadStatus().getStatus() == ChunkDownloadStatus.PENDING) {
                    tempDownloadStatus = new DownloadStatus(DownloadStatus.CHUNK_PENDING);
                    break;
                }
            }
        }
        if (tempDownloadStatus == null) {
            for (ChunkDownload chunkDownload : chunkDownloads) {
                if (chunkDownload.getChunkDownloadStatus().getStatus() == ChunkDownloadStatus.IDLE) {
                    tempDownloadStatus = new DownloadStatus(DownloadStatus.IDLE);
                    break;
                }
            }
        }
        if (tempDownloadStatus == null) {
            for (ChunkDownload chunkDownload : chunkDownloads) {
                if (chunkDownload.getChunkDownloadStatus().getStatus() == ChunkDownloadStatus.ERROR) {
                    tempDownloadStatus = new DownloadStatus(chunkDownload.getChunkDownloadStatus().getThrowable());
                    break;
                }
            }
        }
        if (tempDownloadStatus == null) {
            merge();
        } else {
            notifyDownloadStatus(tempDownloadStatus);
        }
    }

    void execute(Runnable runnable) {
        if (actionThreadPool == null || actionThreadPool.isShutdown()) {
             /*
              * Use only a single thread to keep download status and data correct.
              */
            ThreadPoolExecutor temp = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
            temp.allowCoreThreadTimeOut(true);
            actionThreadPool = temp;
        }
        actionThreadPool.execute(runnable);
    }

    private void windUp() {
        if (shutdownFlag.get() && !shutdown) {
            actionThreadPool.shutdown();
            listeners.clear();
            if (deleteFlag.get()) {
                File downloadFile = getDownloadFile();
                if (downloadFile != null && downloadFile.exists()) {
                    if (!downloadFile.delete()) {

                    }
                }
                if (chunkDownloads != null && !chunkDownloads.isEmpty()) {
                    for (ChunkDownload chunkDownload : chunkDownloads) {
                        if (!new File(chunkDownload.getChunk().file()).delete()) {

                        }
                    }
                    chunkDownloads.clear();
                }
            }
            shutdown = true;
        }
    }

    private void initialize() {
        if (pauseFlag.get()) {
            notifyDownloadStatus(new DownloadStatus(DownloadStatus.IDLE));
            return;
        }
        notifyDownloadStatus(new DownloadStatus(DownloadStatus.INITIALIZING));

        boolean initialized = downloadInfo != null && downloadInfo.initInfo() != null;
        try {
            if (initialized) {
                boolean reInit = !downloadInfo.rangeInfo().rangeSupportable()
                        || downloadAPI.serverFileChanged(request.url(), request.headers(), downloadInfo.initInfo().lastModified());

                if (pauseFlag.get()) {
                    notifyDownloadStatus(new DownloadStatus(DownloadStatus.IDLE));
                    return;
                }

                if (reInit) {
                    downloadInfo = downloadInfo.newBuilder()
                            .initInfo(downloadAPI.getDownloadInitInfo(request.url(), request.headers()))
                            .rangeInfo(downloadAPI.getDownloadRangeInfo(request.url(), request.headers()))
                            .serverFileChanged(true)
                            .build();

                    // remove all chunks and re-split
                    if (chunkDownloads != null && !chunkDownloads.isEmpty()) {
                        for (ChunkDownload chunkDownload : chunkDownloads) {
                            if (!new File(chunkDownload.getChunk().file()).delete()) {

                            }
                        }
                        chunkDownloads.clear();
                    }
                    prepareDownload();
                }
            } else {
                downloadInfo = new DownloadInfo.Builder()
                        .initInfo(downloadAPI.getDownloadInitInfo(request.url(), request.headers()))
                        .rangeInfo(downloadAPI.getDownloadRangeInfo(request.url(), request.headers()))
                        .createTime(System.currentTimeMillis())
                        .serverFileChanged(false)
                        .build();
                // split
                prepareDownload();
            }

            if (pauseFlag.get()) {
                notifyDownloadStatus(new DownloadStatus(DownloadStatus.IDLE));
                return;
            }

            for (ChunkDownload chunkDownload : chunkDownloads) {
                ChunkDownload.ChunkDownloader chunkDownloader = chunkDownload.getChunkDownloader();
                chunkDownload.notifyDownloadStatus(new ChunkDownloadStatus(ChunkDownloadStatus.PENDING));
                chunkDownloadThreadPool.execute(chunkDownloader);
            }
        } catch (Exception e) {
            e.printStackTrace();
            notifyDownloadStatus(new DownloadStatus(e));
        }
    }

    private void prepareDownload() {
        File downloadFile = getDownloadFile();
        assert downloadFile != null;
        if (downloadFile.exists()) {
            throw new RuntimeException("File conflict");
        }
//        if (downloadFile.getUsableSpace() < downloadInfo.initInfo().contentLength()) {
//            throw new RuntimeException("Insufficient disk space");
//        }
        List<Chunk> chunks = splitter.split(
                downloadFile.getAbsolutePath(),
                downloadInfo.initInfo().contentLength(),
                downloadInfo.rangeInfo().rangeSupportable());
        persistenceAdapter.saveDownloadInfo(request, downloadInfo, chunks);
        for (Chunk chunk : chunks) {
            ChunkDownload chunkDownload = new ChunkDownload(request, chunk, null, downloadAPI, persistenceAdapter);
            chunkDownload.attach(Download.this);
            chunkDownloads.add(chunkDownload);
        }
    }

    private void merge() {
        notifyDownloadStatus(new DownloadStatus(DownloadStatus.MERGING));

        List<File> parts = new ArrayList<>();
        for (ChunkDownload chunkDownload : chunkDownloads) {
            parts.add(new File(chunkDownload.getChunk().file()));
        }
        File outFile = getDownloadFile();
        assert outFile != null;
        if (outFile.exists()) {
            if (!outFile.delete()) {
                notifyDownloadStatus(new DownloadStatus(DownloadStatus.MERGE_ERROR, new RuntimeException("can not delete file")));
                return;
            }
        }
        FileOutputStream os = null;
        FileChannel outFileChannel = null;
        try {
            os = new FileOutputStream(outFile);
            outFileChannel = os.getChannel();
            for (File part : parts) {
                FileInputStream is = null;
                FileChannel inPartFileChannel = null;
                try {
                    is = new FileInputStream(part);
                    inPartFileChannel = is.getChannel();
                    inPartFileChannel.transferTo(0, inPartFileChannel.size(), outFileChannel);
                } catch (Exception e) {
                    notifyDownloadStatus(new DownloadStatus(DownloadStatus.MERGE_ERROR, e));
                    return;
                } finally {
                    if (inPartFileChannel != null) {
                        try {
                            inPartFileChannel.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            for (File part : parts) {
                if (!part.delete()) {

                }
            }
            notifyDownloadStatus(new DownloadStatus(DownloadStatus.COMPLETE));
        } catch (FileNotFoundException e) {
            notifyDownloadStatus(new DownloadStatus(DownloadStatus.MERGE_ERROR, e));
        } finally {
            if (outFileChannel != null) {
                try {
                    outFileChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
