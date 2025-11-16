package com.payment_service.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
	String key(); // SpEL expression

	long timeout() default 10000; // milliseconds

	long waitTime() default 5000; // milliseconds
}
