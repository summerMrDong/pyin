package com.pyin.plugin.system.role;

import com.pyin.plugin.system.user.UserRoleView;
import java.util.List;

public interface RoleService {

    List<RoleSummary> findAll(RoleQuery query);

    List<RoleOption> findOptions();

    RoleDetail findDetail(Long roleId);

    List<String> findPermissionCodes(Long roleId);

    List<UserRoleView> findAssignedUsers(Long roleId);

    boolean exists(Long roleId);

    boolean existsAll(List<Long> roleIds);

    RoleEntity create(CreateRoleRequest request);

    RoleEntity update(Long roleId, UpdateRoleRequest request);

    void delete(Long roleId);

    void replacePermissions(Long roleId, List<String> permissionCodes);

    void replaceUsers(Long roleId, List<Long> userIds);
}
