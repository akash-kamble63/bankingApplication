package com.user_service.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.temporal.ChronoUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
	int limit() default 100;

	String key() default "";

	long duration() default 1;

	/**
	 * Time unit for the duration (default: MINUTES)
	 */
	ChronoUnit unit() default ChronoUnit.MINUTES;

	/**
	 * Custom error message when rate limit is exceeded
	 */
	String message() default "Rate limit exceeded. Please try again later.";
}
