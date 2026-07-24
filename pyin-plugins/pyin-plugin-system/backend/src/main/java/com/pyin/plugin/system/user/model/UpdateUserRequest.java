package com.pyin.plugin.system.user.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateUserRequest {

    private String displayName;

    private String status;

    private List<Long> roleIds;

}
