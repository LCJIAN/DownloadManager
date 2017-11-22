package com.lcjian.lib.download;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple implementation of {@link PersistenceAdapter}. It get the whole data firstly,
 * then modify the specific item, re-write the whole data at last, so don't use this to
 * download excessively.
 */
public class SerializablePersistenceAdapter implements PersistenceAdapter {

    private final String dataFile;

    public SerializablePersistenceAdapter(String destination) {
        File folder = new File(destination);
        File file = new File(folder, "download.data");
        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                throw new RuntimeException("Can not create download data file folder.");
            }
        }
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    throw new RuntimeException("Can not create download data file.");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        this.dataFile = file.getAbsolutePath();
    }

    private static Object deserialize(String filePath) {
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(filePath)));
            return in.readObject();
        } catch (ClassNotFoundException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void serialize(String filePath, Object obj) {
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(filePath)));
            out.writeObject(obj);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public List<DownloadRecord> getDownloadRecords() {
        synchronized (this) {
            return getData();
        }
    }

    @Override
    public void deleteRequest(Request request) {
        synchronized (this) {
            List<DownloadRecord> downloadRecords = getData();
            DownloadRecord item = null;
            for (DownloadRecord downloadRecord : downloadRecords) {
                if (Utils.equals(request.id(), downloadRecord.getRequest().id())) {
                    item = downloadRecord;
                    break;
                }
            }
            if (item != null) {
                downloadRecords.remove(item);
                serialize(dataFile, new ArrayList<>(downloadRecords));
            }
        }
    }

    @Override
    public void saveRequest(Request request) {
        synchronized (this) {
            List<DownloadRecord> downloadRecords = getData();
            for (DownloadRecord downloadRecord : downloadRecords) {
                if (Utils.equals(request.id(), downloadRecord.getRequest().id())) {
                    return;
                }
            }
            downloadRecords.add(new DownloadRecord(request, null, null, null));
            serialize(dataFile, new ArrayList<>(downloadRecords));
        }
    }

    @Override
    public void saveDownloadInfo(Request request, DownloadInfo downloadInfo, List<Chunk> chunks) {
        synchronized (this) {
            List<DownloadRecord> downloadRecords = getData();
            for (DownloadRecord downloadRecord : downloadRecords) {
                if (Utils.equals(request.id(), downloadRecord.getRequest().id())) {
                    downloadRecord.setDownloadInfo(downloadInfo);
                    List<DownloadRecord.ChunkRecord> chunkRecords = new ArrayList<>();
                    for (Chunk chunk : chunks) {
                        chunkRecords.add(new DownloadRecord.ChunkRecord(chunk, null));
                    }
                    downloadRecord.setChunkRecords(chunkRecords);
                    break;
                }
            }
            serialize(dataFile, new ArrayList<>(downloadRecords));
        }
    }

    @Override
    public void saveDownloadStatus(Request request, DownloadStatus downloadStatus) {
        synchronized (this) {
            List<DownloadRecord> downloadRecords = getData();
            for (DownloadRecord downloadRecord : downloadRecords) {
                if (Utils.equals(request.id(), downloadRecord.getRequest().id())) {
                    downloadRecord.setDownloadStatus(downloadStatus);
                    break;
                }
            }
            serialize(dataFile, new ArrayList<>(downloadRecords));
        }
    }

    @Override
    public void saveChunkDownloadStatus(Request request, Chunk chunk, ChunkDownloadStatus chunkDownloadStatus) {
        synchronized (this) {
            List<DownloadRecord> downloadRecords = getData();
            for (DownloadRecord downloadRecord : downloadRecords) {
                if (Utils.equals(request.id(), downloadRecord.getRequest().id())) {
                    for (DownloadRecord.ChunkRecord chunkRecord : downloadRecord.getChunkRecords()) {
                        if (Utils.equals(chunk.file(), chunkRecord.getChunk().file())) {
                            chunkRecord.setChunkDownloadStatus(chunkDownloadStatus);
                            break;
                        }
                    }
                    break;
                }
            }
            serialize(dataFile, new ArrayList<>(downloadRecords));
        }
    }

    @SuppressWarnings("unchecked")
    private List<DownloadRecord> getData() {
        try {
            return (List<DownloadRecord>) deserialize(dataFile);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
