package com.lcjian.lib.download;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DownloadMonitor {

    private ScheduledExecutorService scheduledExecutorService;

    private DownloadManager downloadManager;

    private Map<Download, Statistics> statisticsMap = new HashMap<>();

    private boolean started;

    DownloadMonitor(DownloadManager downloadManager) {
        this.downloadManager = downloadManager;
    }

    public void start() {
        if (!started) {
            if (scheduledExecutorService == null || scheduledExecutorService.isShutdown()) {
                scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            }
            scheduledExecutorService.scheduleAtFixedRate(new Runnable() {

                private List<Download> temp = new ArrayList<>(5);
                private List<Download> temp2 = new ArrayList<>(5);

                @Override
                public void run() {
                    temp.clear();
                    temp2.clear();
                    for (Download download : downloadManager.getDownloads()) {
                        if (download.getDownloadStatus().getStatus() == DownloadStatus.DOWNLOADING) {
                            temp.add(download);
                        }
                    }

                    temp2.addAll(statisticsMap.keySet());
                    temp2.removeAll(temp);
                    for (Download download : temp2) {
                        statisticsMap.remove(download);
                    }

                    for (Download download : temp) {
                        Statistics statistics = statisticsMap.get(download);

                        long length = download.getDownloadInfo().initInfo().contentLength();
                        long downloadedBytes = download.getDownloadedBytes();
                        if (statistics == null) {
                            statistics = new Statistics();
                            statistics.delta = -1;
                        } else {
                            statistics.delta = downloadedBytes - statistics.lastDownloadedBytes;
                        }
                        statistics.lastDownloadedBytes = downloadedBytes;
                        statistics.estimatedTime = statistics.delta == -1 || length == -1 ? -1 : (int) (length / (float) statistics.delta);
                        statisticsMap.put(download, statistics);
                    }
                }
            }, 1, 1, TimeUnit.SECONDS);
            started = true;
        }
    }

    public void stop() {
        if (started) {
            scheduledExecutorService.shutdown();
            started = false;
        }
    }

    public Set<Download> getDownloads() {
        return statisticsMap.keySet();
    }

    public long getDownloadDelta(Download download) {
        if (statisticsMap.get(download) == null) {
            return -1;
        } else {
            return statisticsMap.get(download).delta;
        }
    }

    public long getDownloadEstimatedTime(Download download) {
        if (statisticsMap.get(download) == null) {
            return -1;
        } else {
            return statisticsMap.get(download).estimatedTime;
        }
    }

    private static class Statistics {

        long lastDownloadedBytes;

        long delta;
        long estimatedTime;
    }
}
