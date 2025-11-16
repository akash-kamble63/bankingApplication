package com.payment_service.aspect;

import java.util.concurrent.TimeUnit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import com.payment_service.annotation.DistributedLock;
import com.payment_service.exception.LockAcquisitionException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAspect {
	private final RedissonClient redissonClient;
	private final ExpressionParser parser = new SpelExpressionParser();

	@Around("@annotation(distributedLock)")
	public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {

		// Parse SpEL expression
		String lockKey = parseLockKey(joinPoint, distributedLock.key());
		if (lockKey == null || lockKey.isBlank()) {
			throw new IllegalArgumentException("Distributed lock key cannot be null or empty");
		}
		RLock lock = redissonClient.getLock(lockKey);

		boolean acquired = false;
		try {
			acquired = lock.tryLock(distributedLock.waitTime(), distributedLock.timeout(), TimeUnit.MILLISECONDS);

			if (!acquired) {
				log.warn("Failed to acquire lock: {}", lockKey);
				throw new LockAcquisitionException("Could not acquire lock: " + lockKey);
			}

			log.debug("Lock acquired: {}", lockKey);
			return joinPoint.proceed();

		} finally {
			if (acquired && lock.isHeldByCurrentThread()) {
				lock.unlock();
				log.debug("Lock released: {}", lockKey);
			}
		}
	}

	private String parseLockKey(ProceedingJoinPoint joinPoint, String keyExpression) {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		String[] parameterNames = signature.getParameterNames();
		Object[] args = joinPoint.getArgs();

		StandardEvaluationContext context = new StandardEvaluationContext();
		for (int i = 0; i < parameterNames.length; i++) {
			context.setVariable(parameterNames[i], args[i]);
		}

		return parser.parseExpression(keyExpression).getValue(context, String.class);
	}
}
