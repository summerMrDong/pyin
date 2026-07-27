package com.pyin.plugin.dict.web;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DictTypeSaveRequest {

    private Long id;
    private Long categoryId;
    private String typeCode;
    private String typeName;
    private String status;
    private String description;

}
