package com.lh.annotation;

import java.lang.annotation.*;

/**
 * Created by Linhao on 2018/4/22.
 自定义Controller注解
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LHAutowrited {

    String value() default "";
}
