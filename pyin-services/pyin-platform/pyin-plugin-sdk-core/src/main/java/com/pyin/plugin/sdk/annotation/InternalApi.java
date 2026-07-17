package com.pyin.plugin.sdk.annotation;

import com.pyin.plugin.spi.model.PluginAccessMode;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 内部接口注解，用于标记插件内部使用的 API。
 * <p>
 * path 和 method 会自动从 Spring MVC 注解（@GetMapping、@PostMapping 等）中解析，无需重复声明。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InternalApi {

    /**
     * 访问模式，默认为仅内部可访问。
     */
    PluginAccessMode accessMode() default PluginAccessMode.INTERNAL_ONLY;
}
