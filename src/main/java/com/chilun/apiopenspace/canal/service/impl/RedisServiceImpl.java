package com.chilun.apiopenspace.canal.service.impl;

import com.chilun.apiopenspace.canal.model.InterfaceAccess;
import com.chilun.apiopenspace.canal.service.RedisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @author 齿轮
 * @date 2024-03-07-17:04
 */
@Slf4j
@Service
public class RedisServiceImpl implements RedisService {
    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public void updateInterfaceAccess(InterfaceAccess interfaceAccess) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            redisTemplate.opsForValue().set(RedisService.ACCESS_PREFIX + interfaceAccess.getAccesskey(), mapper.writeValueAsString(interfaceAccess), 2, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.error("生成InterfaceAccess的Json信息失败", e);
        }
    }

    @Override
    public void deleteInterfaceAccess(InterfaceAccess interfaceAccess) {
        redisTemplate.delete(RedisService.ACCESS_PREFIX + interfaceAccess.getAccesskey());
    }
}
