package com.chilun.apiopenspace.canal.service;

import com.chilun.apiopenspace.canal.model.InterfaceAccess;

/**
 * @author 齿轮
 * @date 2024-03-07-17:03
 */
public interface RedisService {
    String ACCESS_PREFIX = "InterfaceAccess_";
    void updateInterfaceAccess(InterfaceAccess interfaceAccess);
    void deleteInterfaceAccess(InterfaceAccess interfaceAccess);
}
