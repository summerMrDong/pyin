package com.pyin.plugin.file.model;

import java.io.InputStream;

public class FileBinaryResource {

    private final FileInfoRecord record;
    private final InputStream inputStream;

    public FileBinaryResource(FileInfoRecord record, InputStream inputStream) {
        this.record = record;
        this.inputStream = inputStream;
    }

    public FileInfoRecord getRecord() {
        return record;
    }

    public InputStream getInputStream() {
        return inputStream;
    }
}
