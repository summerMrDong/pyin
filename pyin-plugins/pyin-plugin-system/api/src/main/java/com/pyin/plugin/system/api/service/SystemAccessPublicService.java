package com.pyin.plugin.system.api.service;

import com.pyin.plugin.system.api.model.SystemResourceView;
import com.pyin.plugin.system.api.model.SystemRoleView;
import java.util.List;
import java.util.Set;

/**
 * 系统插件对外暴露的后台访问控制公共能力契约。
 *
 * <p>该接口聚合用户权限码、角色、资源授权信息，供 auth、网关、插件运行时等模块做鉴权
 * 或构造当前用户访问上下文。调用方只依赖此契约，不依赖 system backend 内部角色、权限、
 * 资源领域实现。</p>
 */
public interface SystemAccessPublicService {

    /**
     * 查询指定用户拥有的后台权限编码集合。
     *
     * <p>权限编码用于接口级鉴权，例如 {@code user:view}、{@code role:update}。用户不存在、
     * 被禁用或未绑定任何角色时，应返回空集合。</p>
     *
     * @param userId 后台用户主键 ID。
     * @return 用户拥有的权限编码集合；无权限时返回空集合。
     */
    Set<String> findPermissionCodesByUserId(Long userId);

    /**
     * 查询指定用户绑定的角色列表。
     *
     * <p>返回结果面向跨模块展示和鉴权上下文使用，只包含角色 ID、编码和名称。用户不存在、
     * 被禁用或未绑定角色时，应返回空列表。</p>
     *
     * @param userId 后台用户主键 ID。
     * @return 用户绑定的角色视图列表；无角色时返回空列表。
     */
    List<SystemRoleView> findRolesByUserId(Long userId);

    /**
     * 查询指定用户可访问的资源键集合。
     *
     * <p>资源键用于页面、按钮、插件资源等资源级访问控制。资源键格式由 system 插件统一
     * 维护，例如 {@code SYSTEM:users} 或 {@code PLUGIN:config/items}。</p>
     *
     * @param userId 后台用户主键 ID。
     * @return 用户可访问的资源键集合；无资源授权时返回空集合。
     */
    Set<String> findResourceKeysByUserId(Long userId);

    /**
     * 查询指定用户可访问的资源明细列表。
     *
     * <p>该方法适合需要展示资源来源、插件 ID、资源编码、关联权限码的场景。用户不存在、
     * 被禁用或未授予资源时，应返回空列表。</p>
     *
     * @param userId 后台用户主键 ID。
     * @return 用户可访问的资源视图列表；无资源授权时返回空列表。
     */
    List<SystemResourceView> findResourcesByUserId(Long userId);
}
