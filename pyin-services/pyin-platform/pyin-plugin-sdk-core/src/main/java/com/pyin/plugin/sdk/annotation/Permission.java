package com.pyin.plugin.sdk.annotation;

import com.pyin.plugin.spi.model.PluginPermissionResourceType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限定义注解，用于声明接口所需的权限信息。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Permission {

    /**
     * 简短权限编码写法。
     */
    String value() default "";

    /**
     * 权限编码，全局唯一标识符。
     * 例如：dict:type:create
     */
    String code() default "";

    /**
     * 权限名称，用于展示。
     * 例如：创建字典类型
     */
    String name() default "";

    /**
     * 权限资源类型，默认为 API。
     */
    PluginPermissionResourceType resourceType() default PluginPermissionResourceType.API;

    /**
     * 是否启用审计日志，默认启用。
     */
    boolean auditEnabled() default true;
}
