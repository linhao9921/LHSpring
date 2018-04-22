package com.lh.annotation;

import java.lang.annotation.*;

/**
 * Created by Linhao on 2018/4/22.
 * 自定义Service注解
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LHService {


    String value() default "";
}
