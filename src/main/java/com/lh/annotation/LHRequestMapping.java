package com.lh.annotation;

import java.lang.annotation.*;

/**
 * Created by Linhao on 2018/4/22.
 * 自定义RequestMapping注解
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LHRequestMapping {

    String value() default "";
}
