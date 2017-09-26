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
                        System.out.println(Utils.formatString("Download(%s) : %s, %s",
                                download.getRequest().simplifiedId(),
                                Utils.formatBytes(downloadedBytes, 2),
                                download.getDownloadInfo().initInfo().contentLength() > 0
                                        ? Utils.formatPercent(downloadedBytes / (double) download.getDownloadInfo().initInfo().contentLength())
                                        : "unknown"));
                    }

                    @Override
                    public void onDownloadStatusChanged(Download download, DownloadStatus downloadStatus) {
                        String statusStr = "";
                        switch (downloadStatus.getStatus()) {
                            case DownloadStatus.IDLE:
                                statusStr = "下载暂停";
                                break;
                            case DownloadStatus.PENDING:
                                statusStr = "下载准备中";
                                break;
                            case DownloadStatus.INITIALIZING:
                                statusStr = "下载初始化中";
                                break;
                            case DownloadStatus.CHUNK_PENDING:
                                statusStr = "下载分块准备中";
                                break;
                            case DownloadStatus.DOWNLOADING:
                                statusStr = "下载中";
                                break;
                            case DownloadStatus.ERROR:
                                statusStr = "下载出错了";
                                break;
                            case DownloadStatus.MERGING:
                                statusStr = "下载合并中";
                                break;
                            case DownloadStatus.MERGE_ERROR:
                                statusStr = "下载合并出错了";
                                break;
                            case DownloadStatus.COMPLETE:
                                statusStr = "下载完成";
                                break;
                            default:
                                break;
                        }
                        System.out.println(Utils.formatString("Download(%s):%s", download.getRequest().simplifiedId(), statusStr));
                        if (downloadStatus.getStatus() == DownloadStatus.ERROR) {
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
        downloadManager.enqueue(new Request.Builder().url("http://videodownload.vrgameserver.com/videos/video/video-65ce58ee9b1849db8edf6480351533fb/video-65ce58ee9b1849db8edf6480351533fb.mp4").build());
        downloadManager.enqueue(new Request.Builder().url("http://p.gdown.baidu.com/4ce40b0168c8638f9b0343d13d4f86ac5e4ade86dff3de0fc1250aa4f4faf9712567f652f4334a3e3f72bebfb8dd5d3aeade61a92ad308d2329745d277aa52a260d4f44f27564aa48932676c20f08d1c93821d2a89f65ff69622f5e771170f83bcc1365b6767ccff13c650e843d54771").build());
        downloadManager.enqueue(new Request.Builder().url("http://w73.xitongxz.net:808/201709/02/LB_GHOST_WIN7_SP1_X86_V2017_09.iso").build());
        downloadManager.enqueue(new Request.Builder().url("http://imtt.dd.qq.com/16891/D5C206B2E152D565B05E086800B7DC99.apk").build());
        downloadManager.enqueue(new Request.Builder().url("https://file.zhen22.com/app_dev/android/assets/4101/map/house_filter.json").build());
    }
}
