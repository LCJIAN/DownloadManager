package com.lcjian.lib.download.exception;

import java.io.File;

public class FileExistsException extends Exception {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 1L;

    public FileExistsException(File file) {
        super(file.getAbsolutePath() + " exists");
    }
}
