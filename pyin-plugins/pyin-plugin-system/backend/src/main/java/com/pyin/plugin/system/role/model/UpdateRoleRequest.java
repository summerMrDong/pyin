package com.pyin.plugin.system.role.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateRoleRequest {

    private String name;

    private String description;

    private Integer sort;

    private List<String> permissionCodes;

}
