package com.pyin.plugin.sdk.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 功能点标记注解，用于标注接口所属的业务功能点。
 * <p>
 * 此注解仅做元数据标记，不影响运行时逻辑，用于追溯接口与需求功能点的对应关系。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FunctionalPoint {

    /**
     * 功能点 ID，如 "FP-001"。
     */
    String id();

    /**
     * 功能点名称，如 "手机号注册"。
     */
    String name();

    /**
     * 模块 ID，如 "MOD-001"。
     */
    String module();
}
