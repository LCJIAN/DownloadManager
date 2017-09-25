package com.lcjian.lib.download;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpURLConnectionDownloadAPI implements DownloadAPI {

    /**
     * Regex used to parse content-disposition headers
     */
    private static final Pattern CONTENT_DISPOSITION_PATTERN = Pattern
            .compile("attachment;\\s*filename\\s*=\\s*\"([^\"]*)\"");

    /*
     * Parse the Content-Disposition HTTP Header. The format of the header is
     * defined here: http://www.w3.org/Protocols/rfc2616/rfc2616-sec19.html This
     * header provides a filename for content that is going to be downloaded to
     * the file system. We only support the attachment type.
     */
    private static String fileName(String contentDisposition) {
        try {
            Matcher m = CONTENT_DISPOSITION_PATTERN.matcher(contentDisposition);
            if (m.find()) {
                return m.group(1);
            }
        } catch (IllegalStateException ex) {
            // This function is defined as returning null when it can't parse
            // the header
        }
        return null;
    }

    private static String fileName(String url, String contentDisposition) {
        String fileName = "";
        if (!Utils.isEmpty(contentDisposition)) {
            fileName = fileName(contentDisposition);
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
        if (Utils.isEmpty(fileName)) {
            fileName = String.valueOf(System.currentTimeMillis());
        }
        return fileName;
    }

    private static HttpURLConnection buildConnection(String url, Map<String, String> headers, String method) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(method);
        connection.setInstanceFollowRedirects(true);
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        return connection;
    }

    private static boolean isSuccessful(int code) {
        return code >= 200 && code < 300;
    }

    @Override
    public DownloadInfo.InitInfo getDownloadInitInfo(String url, Map<String, String> headers) throws Exception {
        HttpURLConnection connection = buildConnection(url, headers, "HEAD");
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
                throw new RuntimeException("Connect failed, code:" + connection.getResponseCode());
            }
        }
    }

    @Override
    public DownloadInfo.RangeInfo getDownloadRangeInfo(String url, Map<String, String> headers) throws Exception {
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put("Range", "bytes=0-");
        HttpURLConnection connection = buildConnection(url, headers, "HEAD");
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
                throw new RuntimeException("Connect failed, code:" + connection.getResponseCode());
            }
        }
    }

    @Override
    public boolean serverFileChanged(String url, Map<String, String> headers, String lastModified) throws Exception {
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put("If-Modified-Since", lastModified);
        HttpURLConnection connection = buildConnection(url, headers, "HEAD");
        connection.connect();
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
            return false;
        } else if (responseCode == HttpURLConnection.HTTP_OK) {
            return true;
        } else {
            throw new RuntimeException("Connect failed, code:" + responseCode);
        }
    }

    @Override
    public InputStream getInputStream(String url, Map<String, String> headers, long start, long end) throws Exception {
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put("Range", "bytes=" + start + "-" + end);
        HttpURLConnection connection = buildConnection(url, headers, "GET");
        connection.connect();
        if (isSuccessful(connection.getResponseCode())) {
            return connection.getInputStream();
        } else {
            throw new RuntimeException("Connect failed, code:" + connection.getResponseCode());
        }
    }

    @Override
    public InputStream getInputStream(String url, Map<String, String> headers) throws Exception {
        HttpURLConnection connection = buildConnection(url, headers, "GET");
        connection.connect();
        if (isSuccessful(connection.getResponseCode())) {
            return connection.getInputStream();
        } else {
            throw new RuntimeException("Connect failed, code:" + connection.getResponseCode());
        }
    }
}
