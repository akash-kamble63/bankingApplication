package com.user_service.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
public @interface RateLimit {
	int limit() default 100;
    String key() default "";
}
