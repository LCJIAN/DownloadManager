package com.lcjian.lib.download;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Instances of this class are immutable, safer.
 */
public final class Request implements Serializable {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String url;
    private final String destination;
    private final String fileName;
    private final Map<String, String> headers;
    private final int priority;
    private final String simplifiedId;
    /**
     * Use for customization. You can use JSON string or some else to save more info.
     */
    private final String extra;

    private Request(Builder builder) {
        this.id = builder.id;
        this.url = builder.url;
        this.destination = builder.destination;
        this.fileName = builder.fileName;
        this.headers = builder.headers;
        this.priority = builder.priority;
        this.extra = builder.extra;
        this.simplifiedId = id.length() - 10 < 0 ? id : id.substring(id.length() - 10);
    }

    public String id() {
        return id;
    }

    public String url() {
        return url;
    }

    public String destination() {
        return destination;
    }

    public String fileName() {
        return fileName;
    }

    public Map<String, String> headers() {
        return headers == null ? null : Collections.unmodifiableMap(headers);
    }

    public int priority() {
        return priority;
    }

    public String extra() {
        return extra;
    }

    public String simplifiedId() {
        return simplifiedId;
    }

    public String header(String name) {
        return headers.get(name);
    }

    public Builder newBuilder() {
        return new Builder(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Request request = (Request) o;

        return id.equals(request.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public static class Builder {
        private String id;
        private String url;
        private String destination;
        private String fileName;
        private Map<String, String> headers;
        private int priority;
        private String extra;

        public Builder() {
        }

        private Builder(Request request) {
            this.url = request.url;
            this.destination = request.destination;
            this.fileName = request.fileName;
            this.headers = request.headers;
            this.priority = request.priority;
            this.extra = request.extra;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            if (Utils.isEmpty(id)) {
                this.id = url;
            }
            return this;
        }

        public Builder destination(String destination) {
            this.destination = destination;
            return this;
        }

        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder extra(String extra) {
            this.extra = extra;
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public Builder header(String name, String value) {
            if (headers == null) {
                headers = new HashMap<>();
            }
            headers.put(name, value);
            return this;
        }

        public Builder removeHeader(String name) {
            if (headers != null) {
                headers.remove(name);
            }
            return this;
        }

        public Request build() {
            if (Utils.isEmpty(id))
                throw new NullPointerException("empty id");
            if (Utils.isEmpty(url))
                throw new NullPointerException("empty url");
            return new Request(this);
        }
    }
}
