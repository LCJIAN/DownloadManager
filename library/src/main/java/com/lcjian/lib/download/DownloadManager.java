package com.lcjian.lib.download;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

// TODO 1.pause pending ...task 2.okhttp downloader 3.sqlite persistence 4. rxjava support
public class DownloadManager {

    private final String defaultDestination;
    private final ExecutorService actionThreadPool;
    private final ExecutorService chunkDownloadThreadPool;
    private final PersistenceAdapter persistenceAdapter;
    private final Splitter splitter;
    private final DownloadAPI downloadAPI;
    private final RetryPolicy.Factory retryPolicyFactory;
    private final CopyOnWriteArrayList<Listener> listeners;
    private final int maxDownloadCount;
    private final Semaphore semaphore;
    private final Logger logger;
    private final DownloadMonitor downloadMonitor;

    private final List<Request> requests;
    private final List<Download> downloads;
    private final Map<Request, Download> requestDownloadMap;

    private DownloadManager(Builder builder) {
        downloads = new ArrayList<>();
        requests = new ArrayList<>();
        requestDownloadMap = new HashMap<>();
        listeners = new CopyOnWriteArrayList<>();

        ThreadPoolExecutor temp = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        temp.allowCoreThreadTimeOut(true);
        actionThreadPool = temp;

        defaultDestination = builder.defaultDestination;
        chunkDownloadThreadPool = builder.chunkDownloadThreadPool;
        persistenceAdapter = builder.persistenceAdapter;
        splitter = builder.splitter;
        downloadAPI = builder.downloadAPI;
        retryPolicyFactory = builder.retryPolicyFactory;
        maxDownloadCount = builder.maxDownloadCount;
        semaphore = new Semaphore(maxDownloadCount, true);
        logger = builder.logger;
        downloadMonitor = new DownloadMonitor(this);
        init();
    }

    private void init() {
        actionThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                List<DownloadRecord> downloadRecords = persistenceAdapter.getDownloadRecords();
                if (downloadRecords != null && !downloadRecords.isEmpty()) {
                    for (DownloadRecord downloadRecord : downloadRecords) {
                        Request request = downloadRecord.getRequest();
                        DownloadStatus downloadStatus = downloadRecord.getDownloadStatus();
                        DownloadInfo downloadInfo = downloadRecord.getDownloadInfo();
                        List<DownloadRecord.ChunkRecord> chunkRecords = downloadRecord.getChunkRecords();

                        List<ChunkDownload> chunkDownloads = new ArrayList<>();
                        if (chunkRecords != null) {
                            for (DownloadRecord.ChunkRecord chunkRecord : chunkRecords) {
                                chunkDownloads.add(new ChunkDownload(request, chunkRecord.getChunk(),
                                        chunkRecord.getChunkDownloadStatus(), downloadAPI, persistenceAdapter, logger));
                            }
                        }

                        Download download = new Download(
                                request,
                                downloadStatus,
                                downloadInfo,
                                chunkDownloads,
                                defaultDestination,
                                splitter,
                                downloadAPI,
                                retryPolicyFactory.createPolicy(),
                                persistenceAdapter,
                                chunkDownloadThreadPool,
                                semaphore,
                                logger);
                        requests.add(request);
                        downloads.add(download);
                        requestDownloadMap.put(request, download);
                        if (listeners != null && !listeners.isEmpty()) {
                            for (Listener listener : listeners) {
                                listener.onDownloadCreate(download);
                            }
                        }
                    }
                }
            }
        });
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public Request getRequest(String id) {
        for (Request request : requests) {
            if (Utils.equals(id, request.id())) {
                return request;
            }
        }
        return null;
    }

    public Download getDownload(Request request) {
        return requestDownloadMap.get(request);
    }

    public List<Request> getRequests() {
        return Collections.unmodifiableList(requests);
    }

    public List<Download> getDownloads() {
        return Collections.unmodifiableList(downloads);
    }

    public void enqueue(final Request request) {
        actionThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                if (!requests.contains(request)) {
                    Download download = new Download(
                            request,
                            null,
                            null,
                            null,
                            defaultDestination,
                            splitter,
                            downloadAPI,
                            retryPolicyFactory.createPolicy(),
                            persistenceAdapter,
                            chunkDownloadThreadPool,
                            semaphore,
                            logger);
                    requests.add(request);
                    downloads.add(download);
                    requestDownloadMap.put(request, download);
                    persistenceAdapter.saveRequest(request);
                    if (listeners != null && !listeners.isEmpty()) {
                        for (Listener listener : listeners) {
                            listener.onDownloadCreate(download);
                        }
                    }
                    download.resumeAsync();
                }
            }
        });
    }

    public void resume(final Request request) {
        actionThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                getDownload(request).resumeAsync();
            }
        });
    }

    public void pause(final Request request) {
        actionThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                getDownload(request).pauseAsync();
            }
        });
    }

    public void resumeAll() {
        actionThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                for (Download download : downloads) {
                    download.resumeAsync();
                }
            }
        });
    }

    public void pauseAll() {
        actionThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                for (Download download : downloads) {
                    download.pauseAsync();
                }
            }
        });
    }

    public void priorityResumeAll() {
        actionThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                List<Download> downloadsTemp = new ArrayList<>(downloads);
                Collections.sort(downloadsTemp, new Comparator<Download>() {
                    @Override
                    public int compare(Download o1, Download o2) {
                        return o1.getRequest().priority() - o2.getRequest().priority();
                    }
                });
                for (Download download : downloadsTemp) {
                    download.resumeAsync();
                }
            }
        });
    }

    public void delete(final Download download, final boolean deleteFile) {
        actionThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                Request request = download.getRequest();
                requests.remove(request);
                downloads.remove(download);
                requestDownloadMap.remove(request);
                persistenceAdapter.deleteRequest(request);
                if (listeners != null && !listeners.isEmpty()) {
                    for (Listener listener : listeners) {
                        listener.onDownloadDestroy(download);
                    }
                }
                download.shutdownAsync(deleteFile);
            }
        });
    }

    public void shutdown() {
        actionThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                for (Download download : downloads) {
                    download.shutdownAsync(false);
                }
                actionThreadPool.shutdown();
                chunkDownloadThreadPool.shutdown();
                listeners.clear();
            }
        });
    }

    public DownloadMonitor getDownloadMonitor() {
        return downloadMonitor;
    }

    public interface Listener {

        void onDownloadCreate(Download download);

        void onDownloadDestroy(Download download);
    }

    public static class Builder {

        private String defaultDestination;
        private ExecutorService chunkDownloadThreadPool;
        private PersistenceAdapter persistenceAdapter;
        private Splitter splitter;
        private DownloadAPI downloadAPI;
        private RetryPolicy.Factory retryPolicyFactory;
        private int maxDownloadCount;
        private Logger logger;

        public Builder() {
        }

        public Builder defaultDestination(String defaultDestination) {
            this.defaultDestination = defaultDestination;
            return this;
        }

        public Builder chunkDownloadThreadPool(ExecutorService chunkDownloadThreadPool) {
            this.chunkDownloadThreadPool = chunkDownloadThreadPool;
            return this;
        }

        public Builder persistenceAdapter(PersistenceAdapter persistenceAdapter) {
            this.persistenceAdapter = persistenceAdapter;
            return this;
        }

        public Builder splitter(Splitter splitter) {
            this.splitter = splitter;
            return this;
        }

        public Builder downloadAPI(DownloadAPI downloadAPI) {
            this.downloadAPI = downloadAPI;
            return this;
        }

        public Builder retryPolicyFactory(RetryPolicy.Factory retryPolicyFactory) {
            this.retryPolicyFactory = retryPolicyFactory;
            return this;
        }

        public Builder maxDownloadCount(int maxDownloadCount) {
            if (maxDownloadCount < 1) {
                throw new IllegalArgumentException("The max download count can not be zero.");
            }
            this.maxDownloadCount = maxDownloadCount;
            return this;
        }

        public Builder logger(Logger logger) {
            this.logger = logger;
            return this;
        }

        public DownloadManager build() {
            if (Utils.isEmpty(defaultDestination))
                throw new NullPointerException("The default download destination is empty.");
            if (chunkDownloadThreadPool == null) {
                ThreadPoolExecutor temp = new ThreadPoolExecutor(6, 6, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
                temp.allowCoreThreadTimeOut(true);
                chunkDownloadThreadPool = temp;
            }
            if (persistenceAdapter == null) {
                persistenceAdapter = new SerializablePersistenceAdapter(defaultDestination);
            }
            if (splitter == null) {
                splitter = new SimpleSplitter();
            }
            if (downloadAPI == null) {
                downloadAPI = new HttpURLConnectionDownloadAPI();
            }
            if (retryPolicyFactory == null) {
                retryPolicyFactory = SimpleRetryPolicy.Factory.create(3);
            }
            if (maxDownloadCount == 0) {
                maxDownloadCount = 5;
            }
            if (logger == null) {
                logger = Logger.getLogger("DownloadManager");
                logger.setLevel(Level.ALL);
            }
            return new DownloadManager(this);
        }
    }

}
