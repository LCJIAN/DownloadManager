package com.lcjian.lib.download;

import java.io.Serializable;

public class DownloadInfo implements Serializable {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 1L;

    private final InitInfo initInfo;
    private final RangeInfo rangeInfo;

    private final long createTime;
    private final boolean serverFileChanged;

    private DownloadInfo(Builder builder) {
        this.initInfo = builder.initInfo;
        this.rangeInfo = builder.rangeInfo;
        this.createTime = builder.createTime;
        this.serverFileChanged = builder.serverFileChanged;
    }

    public InitInfo initInfo() {
        return initInfo;
    }

    public RangeInfo rangeInfo() {
        return rangeInfo;
    }

    public long createTime() {
        return createTime;
    }

    public boolean serverFileChanged() {
        return serverFileChanged;
    }

    public Builder newBuilder() {
        return new Builder(this);
    }

    public static class Builder {

        private InitInfo initInfo;
        private RangeInfo rangeInfo;
        private long createTime;
        private boolean serverFileChanged;

        public Builder() {
        }

        private Builder(DownloadInfo downloadInfo) {
            this.initInfo = downloadInfo.initInfo;
            this.rangeInfo = downloadInfo.rangeInfo;
            this.createTime = downloadInfo.createTime;
            this.serverFileChanged = downloadInfo.serverFileChanged;
        }

        public Builder initInfo(InitInfo initInfo) {
            this.initInfo = initInfo;
            return this;
        }

        public Builder rangeInfo(RangeInfo rangeInfo) {
            this.rangeInfo = rangeInfo;
            return this;
        }

        public Builder createTime(long createTime) {
            this.createTime = createTime;
            return this;
        }

        public Builder serverFileChanged(boolean serverFileChanged) {
            this.serverFileChanged = serverFileChanged;
            return this;
        }

        public DownloadInfo build() {
            return new DownloadInfo(this);
        }

    }

    public static class InitInfo implements Serializable {

        /**
         * serialVersionUID
         */
        private static final long serialVersionUID = 1L;

        private final String fileName;
        private final String mimeType;
        private final String lastModified;
        private final long contentLength;

        private InitInfo(Builder builder) {
            this.fileName = builder.fileName;
            this.mimeType = builder.mimeType;
            this.lastModified = builder.lastModified;
            this.contentLength = builder.contentLength;
        }

        public String fileName() {
            return fileName;
        }

        public String mimeType() {
            return mimeType;
        }

        public String lastModified() {
            return lastModified;
        }

        public long contentLength() {
            return contentLength;
        }

        public Builder newBuilder() {
            return new Builder(this);
        }

        public static class Builder {

            private String fileName;
            private String mimeType;
            private String lastModified;
            private long contentLength;

            public Builder() {
            }

            private Builder(InitInfo initInfo) {
                this.fileName = initInfo.fileName;
                this.mimeType = initInfo.mimeType;
                this.lastModified = initInfo.lastModified;
                this.contentLength = initInfo.contentLength;
            }

            public Builder fileName(String fileName) {
                this.fileName = fileName;
                return this;
            }

            public Builder mimeType(String mimeType) {
                this.mimeType = mimeType;
                return this;
            }

            public Builder lastModified(String lastModified) {
                this.lastModified = lastModified;
                return this;
            }

            public Builder contentLength(long contentLength) {
                this.contentLength = contentLength;
                return this;
            }

            public InitInfo build() {
                return new InitInfo(this);
            }
        }

    }

    public static class RangeInfo implements Serializable {

        /**
         * serialVersionUID
         */
        private static final long serialVersionUID = 1L;

        private final boolean rangeSupportable;
        private final boolean chunked;

        private RangeInfo(Builder builder) {
            this.rangeSupportable = builder.rangeSupportable;
            this.chunked = builder.chunked;
        }

        public boolean rangeSupportable() {
            return rangeSupportable;
        }

        public boolean chunked() {
            return chunked;
        }

        public Builder newBuilder() {
            return new Builder(this);
        }

        public static class Builder {

            private boolean rangeSupportable;
            private boolean chunked;

            public Builder() {
            }

            private Builder(RangeInfo rangeInfo) {
                this.rangeSupportable = rangeInfo.rangeSupportable;
                this.chunked = rangeInfo.chunked;
            }

            public Builder rangeSupportable(boolean rangeSupportable) {
                this.rangeSupportable = rangeSupportable;
                return this;
            }

            public Builder chunked(boolean chunked) {
                this.chunked = chunked;
                return this;
            }

            public RangeInfo build() {
                return new RangeInfo(this);
            }
        }
    }
}
