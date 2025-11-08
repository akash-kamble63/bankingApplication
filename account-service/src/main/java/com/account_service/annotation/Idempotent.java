package com.account_service.annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    /**
     * TTL in hours for idempotency record
     */
    int ttlHours() default 24;
    
    /**
     * Whether to return cached response
     */
    boolean returnCached() default true;
}
