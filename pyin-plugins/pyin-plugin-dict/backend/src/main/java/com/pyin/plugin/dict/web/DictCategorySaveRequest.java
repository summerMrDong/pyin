package com.pyin.plugin.dict.web;

import lombok.Getter;
import lombok.Setter;

/**
 * 字典分类保存请求。
 */
@Getter
@Setter
public class DictCategorySaveRequest {

    private Long id;
    private String categoryName;
    private Integer sortOrder;
}
