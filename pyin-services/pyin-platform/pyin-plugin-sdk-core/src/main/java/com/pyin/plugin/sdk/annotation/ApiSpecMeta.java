package com.pyin.plugin.sdk.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiSpecMeta {

    String apiId();

    String moduleId();

    String functionPointId();

    String roleId();

    String pluginId();
}
