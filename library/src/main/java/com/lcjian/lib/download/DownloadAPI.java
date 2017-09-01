package com.lcjian.lib.download;

import java.io.InputStream;
import java.util.Map;

public interface DownloadAPI {

    DownloadInfo.InitInfo getDownloadInitInfo(String url, Map<String, String> headers) throws Exception;

    DownloadInfo.RangeInfo getDownloadRangeInfo(String url, Map<String, String> headers) throws Exception;

    boolean serverFileChanged(String url, Map<String, String> headers, String lastModified) throws Exception;

    InputStream getInputStream(String url, Map<String, String> headers, long start, long end) throws Exception;

    InputStream getInputStream(String url, Map<String, String> headers) throws Exception;

}
