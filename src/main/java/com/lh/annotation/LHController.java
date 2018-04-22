package com.lh.annotation;

import java.lang.annotation.*;

/**
 * Created by Linhao on 2018/4/22.
 * 自定义Controller注解
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LHController {


    String value() default "";
}
