package com.lcjian.lib.download;

import java.util.List;

public class Test {

    public static void main(String[] args) throws Exception {

        final DownloadManager downloadManager = new DownloadManager.Builder()
                .defaultDestination("D:\\parser\\download")
                .build();
        downloadManager.addListener(new DownloadManager.Listener() {

            @Override
            public void onDownloadDestroy(Download download) {
            }

            @Override
            public void onDownloadCreate(Download download) {
                download.addDownloadListener(new DownloadListener() {

                    @Override
                    public void onProgress(Download download, long downloadedBytes) {
                        System.out.println("downloadedBytes:" + downloadedBytes);
                    }

                    @Override
                    public void onDownloadStatusChanged(Download download, DownloadStatus downloadStatus) {
                        System.out.println("downloadStatus:" + downloadStatus.getStatus());
                        if (downloadStatus.getStatus() == 5) {
                            downloadStatus.getThrowable().printStackTrace();
                        }/* else if (downloadStatus.getStatus() == DownloadStatus.MERGE_END) {
                            downloadManager.shutdown();
                        }*/
                    }

                    @Override
                    public void onChunkDownloadsCreate(Download download, List<ChunkDownload> chunkDownloads) {

                    }

                    @Override
                    public void onRetry(Download download, Throwable throwable) {

                    }

                    @Override
                    public void onChunkDownloadsDestroy(Download download, List<ChunkDownload> chunkDownloads) {

                    }


                });
            }
        });
        downloadManager.enqueue(new Request.Builder().url("https://file.zhen22.com/app_dev/android/assets/4101/map/house_filter.json").build());
    }
}
