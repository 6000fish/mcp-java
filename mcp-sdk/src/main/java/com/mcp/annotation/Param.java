package com.mcp.annotation;

import java.lang.annotation.*;

/**
 * 标记方法参数
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Param {

    /**
     * 参数名称
     */
    String name();

    /**
     * 参数描述
     */
    String description() default "";

    /**
     * 是否必填
     */
    boolean required() default true;
}
