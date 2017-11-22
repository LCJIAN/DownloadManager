package com.lcjian.lib.download;

import com.lcjian.lib.download.exception.ConnectException;

import java.io.InputStream;
import java.util.Map;

public interface DownloadAPI {

    DownloadInfo.InitInfo getDownloadInitInfo(String url, Map<String, String> headers) throws ConnectException;

    DownloadInfo.RangeInfo getDownloadRangeInfo(String url, Map<String, String> headers) throws ConnectException;

    boolean serverFileChanged(String url, Map<String, String> headers, String lastModified) throws ConnectException;

    InputStream getInputStream(String url, Map<String, String> headers, long start, long end) throws ConnectException;

    InputStream getInputStream(String url, Map<String, String> headers) throws ConnectException;

}
