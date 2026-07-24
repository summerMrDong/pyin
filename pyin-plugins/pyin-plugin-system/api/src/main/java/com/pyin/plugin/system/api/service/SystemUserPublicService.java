package com.pyin.plugin.system.api.service;

import com.pyin.plugin.system.api.model.SystemUserAuthInfo;
import com.pyin.plugin.system.api.model.SystemUserView;

/**
 * 系统插件对外暴露的用户公共能力契约。
 *
 * <p>该接口面向同进程内的其他模块或插件使用，只暴露登录鉴权、当前用户查询等跨领域
 * 必需的最小用户信息。调用方不应依赖 system backend 的用户实体、仓储或内部 Service。</p>
 */
public interface SystemUserPublicService {

    /**
     * 根据用户名查询后台用户的认证信息。
     *
     * <p>该方法主要供后台登录链路使用，返回值包含密码哈希和启停状态。用户名不存在时返回
     * {@code null}，由调用方决定抛出认证失败还是继续其他认证策略。</p>
     *
     * @param username 后台登录用户名，通常为用户唯一账号。
     * @return 用户认证信息；用户不存在时返回 {@code null}。
     */
    SystemUserAuthInfo findAuthInfoByUsername(String username);

    /**
     * 根据用户 ID 查询后台用户的认证信息。
     *
     * <p>该方法主要供 token/session 恢复后的用户有效性校验使用。用户不存在时返回
     * {@code null}；用户被禁用时返回对象中的 {@code enabled} 为 {@code false}。</p>
     *
     * @param userId 后台用户主键 ID。
     * @return 用户认证信息；用户不存在时返回 {@code null}。
     */
    SystemUserAuthInfo findAuthInfoById(Long userId);

    /**
     * 根据用户 ID 查询后台用户展示视图。
     *
     * <p>该方法不返回密码哈希等认证敏感字段，适合当前用户信息、审计展示、权限上下文等
     * 只需要基础用户信息的场景。</p>
     *
     * @param userId 后台用户主键 ID。
     * @return 用户展示视图；用户不存在时返回 {@code null}。
     */
    SystemUserView findUserById(Long userId);
}
