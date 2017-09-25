package com.lcjian.lib.download;

import java.io.Serializable;

/**
 * Instances of this class are immutable, safer.
 */
public final class Chunk implements Serializable {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 1L;

    private final long start;
    private final long end;
    private final String file;

    private Chunk(Builder builder) {
        this.start = builder.start;
        this.end = builder.end;
        this.file = builder.file;
    }

    public long start() {
        return start;
    }

    public long end() {
        return end;
    }

    public String file() {
        return file;
    }

    public Builder newBuilder() {
        return new Builder(this);
    }

    public static class Builder {

        private long start;
        private long end;
        private String file;

        public Builder() {
        }

        private Builder(Chunk chunk) {
            this.start = chunk.start;
            this.end = chunk.end;
            this.file = chunk.file;
        }

        public Builder start(long start) {
            this.start = start;
            return this;
        }

        public Builder end(long end) {
            this.end = end;
            return this;
        }

        public Builder file(String file) {
            if (Utils.isEmpty(file))
                throw new NullPointerException("Chunk file path is empty");
            this.file = file;
            return this;
        }

        public Chunk build() {
            if (Utils.isEmpty(file))
                throw new NullPointerException("Chunk file path is empty");
            return new Chunk(this);
        }
    }
}
