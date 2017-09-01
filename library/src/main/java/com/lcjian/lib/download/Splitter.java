package com.lcjian.lib.download;

import java.util.List;

public interface Splitter {

    List<Chunk> split(String file, long fileLength, boolean rangeSupportable);
}
