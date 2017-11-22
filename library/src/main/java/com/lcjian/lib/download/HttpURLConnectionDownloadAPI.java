package com.lcjian.lib.download;

import com.lcjian.lib.download.exception.ConnectException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class HttpURLConnectionDownloadAPI implements DownloadAPI {

    private static boolean isSuccessful(int code) {
        return code >= 200 && code < 300;
    }

    private static HttpURLConnection buildConnection(String url, Map<String, String> headers, String method) throws ConnectException {
        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod(method);
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(10 * 1000);
            connection.setReadTimeout(10 * 1000);
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
        } catch (IOException e) {
            throw new ConnectException(e);
        }
        return connection;
    }

    protected String fileName(String url, String contentDisposition) {
        String fileName = "";
        if (!Utils.isEmpty(contentDisposition)) {
            fileName = Utils.contentDispositionFileName(contentDisposition);
        }
        if (Utils.isEmpty(fileName)) {
            fileName = url.substring(url.lastIndexOf('/') + 1);
        }
        if (fileName.startsWith("\"")) {
            fileName = fileName.substring(1);
        }
        if (fileName.endsWith("\"")) {
            fileName = fileName.substring(0, fileName.length() - 1);
        }
        if (Utils.isEmpty(fileName) || !Utils.isValidFileName(fileName)) {
            fileName = String.valueOf(System.currentTimeMillis());
        }
        return fileName;
    }

    @Override
    public DownloadInfo.InitInfo getDownloadInitInfo(String url, Map<String, String> headers) throws ConnectException {
        HttpURLConnection connection = buildConnection(url, headers, "HEAD");
        try {
            connection.connect();
            if (isSuccessful(connection.getResponseCode())) {
                String contentLength = connection.getHeaderField("Content-Length");
                return new DownloadInfo.InitInfo.Builder()
                        .fileName(fileName(url, connection.getHeaderField("Content-Disposition")))
                        .mimeType(connection.getHeaderField("Content-Type"))
                        .lastModified(connection.getHeaderField("Last-Modified"))
                        .contentLength(Utils.isEmpty(contentLength) ? -1 : Long.parseLong(contentLength))
                        .build();
            } else {
                connection = buildConnection(url, headers, "GET");
                connection.connect();
                if (isSuccessful(connection.getResponseCode())) {
                    String contentLength = connection.getHeaderField("Content-Length");
                    return new DownloadInfo.InitInfo.Builder()
                            .fileName(fileName(url, connection.getHeaderField("Content-Disposition")))
                            .mimeType(connection.getHeaderField("Content-Type"))
                            .lastModified(connection.getHeaderField("Last-Modified"))
                            .contentLength(Utils.isEmpty(contentLength) ? -1 : Long.parseLong(contentLength))
                            .build();
                } else {
                    throw new ConnectException("Connect failed, code:" + connection.getResponseCode());
                }
            }
        } catch (IOException e) {
            throw new ConnectException(e);
        }
    }

    @Override
    public DownloadInfo.RangeInfo getDownloadRangeInfo(String url, Map<String, String> headers) throws ConnectException {
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put("Range", "bytes=0-");
        HttpURLConnection connection = buildConnection(url, headers, "HEAD");
        try {
            connection.connect();
            if (isSuccessful(connection.getResponseCode())) {
                String contentLength = connection.getHeaderField("Content-Length");
                boolean chunked = "chunked".equals(connection.getHeaderField("Transfer-Encoding"));
                boolean rangeSupportable = !((Utils.isEmpty(connection.getHeaderField("Content-Range"))
                        && !"bytes".equals(connection.getHeaderField("Accept-Ranges")))
                        || (Utils.isEmpty(contentLength) ? -1 : Long.parseLong(contentLength)) == -1
                        || "chunked".equals(connection.getHeaderField("Transfer-Encoding")));

                return new DownloadInfo.RangeInfo.Builder()
                        .chunked(chunked)
                        .rangeSupportable(rangeSupportable)
                        .build();
            } else {
                connection = buildConnection(url, headers, "GET");
                connection.connect();
                if (isSuccessful(connection.getResponseCode())) {
                    String contentLength = connection.getHeaderField("Content-Length");
                    boolean chunked = "chunked".equals(connection.getHeaderField("Transfer-Encoding"));
                    boolean rangeSupportable = !((Utils.isEmpty(connection.getHeaderField("Content-Range"))
                            && !"bytes".equals(connection.getHeaderField("Accept-Ranges")))
                            || (Utils.isEmpty(contentLength) ? -1 : Long.parseLong(contentLength)) == -1
                            || "chunked".equals(connection.getHeaderField("Transfer-Encoding")));

                    return new DownloadInfo.RangeInfo.Builder()
                            .chunked(chunked)
                            .rangeSupportable(rangeSupportable)
                            .build();
                } else {
                    throw new ConnectException("Connect failed, code:" + connection.getResponseCode());
                }
            }
        } catch (IOException e) {
            throw new ConnectException(e);
        }
    }

    @Override
    public boolean serverFileChanged(String url, Map<String, String> headers, String lastModified) throws ConnectException {
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put("If-Modified-Since", lastModified);
        HttpURLConnection connection = buildConnection(url, headers, "HEAD");
        try {
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                return false;
            } else if (responseCode == HttpURLConnection.HTTP_OK) {
                return true;
            } else {
                throw new ConnectException("Connect failed, code:" + responseCode);
            }
        } catch (IOException e) {
            throw new ConnectException(e);
        }
    }

    @Override
    public InputStream getInputStream(String url, Map<String, String> headers, long start, long end) throws ConnectException {
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put("Range", "bytes=" + start + "-" + end);
        HttpURLConnection connection = buildConnection(url, headers, "GET");
        try {
            connection.connect();
            if (isSuccessful(connection.getResponseCode())) {
                return connection.getInputStream();
            } else {
                throw new ConnectException("Connect failed, code:" + connection.getResponseCode());
            }
        } catch (IOException e) {
            throw new ConnectException(e);
        }
    }

    @Override
    public InputStream getInputStream(String url, Map<String, String> headers) throws ConnectException {
        HttpURLConnection connection = buildConnection(url, headers, "GET");
        try {
            connection.connect();
            if (isSuccessful(connection.getResponseCode())) {
                return connection.getInputStream();
            } else {
                throw new ConnectException("Connect failed, code:" + connection.getResponseCode());
            }
        } catch (IOException e) {
            throw new ConnectException(e);
        }
    }
}
