package com.pyin.plugin.file.model;

import lombok.Getter;
import lombok.Setter;

import java.io.InputStream;

@Getter
@Setter
public class FileBinaryResource {

    private final FileInfoRecord record;
    private final InputStream inputStream;

    public FileBinaryResource(FileInfoRecord record, InputStream inputStream) {
        this.record = record;
        this.inputStream = inputStream;
    }

}
