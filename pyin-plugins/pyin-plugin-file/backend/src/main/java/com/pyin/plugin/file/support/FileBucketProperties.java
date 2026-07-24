package com.pyin.plugin.file.support;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class FileBucketProperties {

    private String description;
    private boolean publicRead;
    private List<String> allowedTypes = new ArrayList<>();

}
