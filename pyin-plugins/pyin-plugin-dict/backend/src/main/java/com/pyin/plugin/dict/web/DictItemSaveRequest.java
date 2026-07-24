package com.pyin.plugin.dict.web;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DictItemSaveRequest {

    private Long id;
    private Long typeId;
    private String itemValue;
    private String itemLabel;
    private Integer itemSort;
    private String itemStatus;
    private String description;

}
