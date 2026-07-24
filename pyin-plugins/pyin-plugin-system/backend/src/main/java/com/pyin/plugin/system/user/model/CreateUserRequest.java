package com.pyin.plugin.system.user.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateUserRequest {

    private String username;

    private String displayName;

    private String password;

    private String status;

    private List<Long> roleIds;

}
