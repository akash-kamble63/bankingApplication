package com.user_service.config;

import java.lang.reflect.Method;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;
@Component("customCacheKeyGenerator")
public class CustomCacheKeyGenerator implements KeyGenerator {
    
    @Override
    public Object generate(Object target, Method method, Object... params) {
        StringBuilder sb = new StringBuilder();
        sb.append(target.getClass().getSimpleName());
        sb.append("_");
        sb.append(method.getName());
        
        for (Object param : params) {
            if (param != null) {
                sb.append("_");
                sb.append(param.toString());
            }
        }
        
        return sb.toString();
    }
}