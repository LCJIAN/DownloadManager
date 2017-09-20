package com.lcjian.lib.download;

import java.util.ArrayList;
import java.util.List;

public class SimpleSplitter implements Splitter {

    private static final long MB = 1024 * 1024;

    @Override
    public List<Chunk> split(String file, long fileLength, boolean rangeSupportable) {
        List<Chunk> chunks = new ArrayList<>(4);
        if (rangeSupportable) {
            int chunkCount;
            if (fileLength < MB * 64) {
                chunkCount = 1;
            } else if (fileLength < MB * 128) {
                chunkCount = 2;
            } else {
                chunkCount = 3;
            }
            long chunkSize = fileLength / chunkCount;
            long start = 0;
            long end = start + chunkSize - 1;
            int i = 1;
            while (true) {
                chunks.add(new Chunk.Builder().start(start).end(end).file(file + ".download.part" + i).build());
                i++;
                if (i > chunkCount) {
                    break;
                }
                if (i == chunkCount) {
                    start = end + 1;
                    end = fileLength - 1;
                } else {
                    start = end + 1;
                    end = start + chunkSize - 1;
                }
            }
        } else {
            chunks.add(new Chunk.Builder().start(0).end(-1).file(file + ".download.part" + 1).build());
        }
        return chunks;
    }
}
