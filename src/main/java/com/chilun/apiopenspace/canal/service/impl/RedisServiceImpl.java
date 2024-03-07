package com.chilun.apiopenspace.canal.service.impl;

import com.chilun.apiopenspace.canal.model.InterfaceAccess;
import com.chilun.apiopenspace.canal.service.RedisService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @author 齿轮
 * @date 2024-03-07-17:04
 */
@Service
public class RedisServiceImpl implements RedisService {
    @Resource
    private RedisTemplate<String, InterfaceAccess> redisTemplate;

    @Override
    public void updateInterfaceAccess(InterfaceAccess interfaceAccess) {
        redisTemplate.opsForValue().set(RedisService.ACCESS_PREFIX + interfaceAccess.getAccesskey(), interfaceAccess, 2, TimeUnit.MINUTES);
    }

    @Override
    public void deleteInterfaceAccess(InterfaceAccess interfaceAccess) {
        redisTemplate.delete(RedisService.ACCESS_PREFIX + interfaceAccess.getAccesskey());
    }
}
