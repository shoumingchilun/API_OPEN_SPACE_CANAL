package com.chilun.apiopenspace.canal.config;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;

/**
 * @author 齿轮
 * @date 2024-03-07-12:52
 */
@Configuration
public class CanalConfig {
    @Value("${mycanal.hostname}")
    private String hostname;
    @Value("${mycanal.port}")
    private Integer port;
    @Value("${mycanal.destination}")
    private String destination;
    @Value("${mycanal.username}")
    private String username = "";
    @Value("${mycanal.password}")
    private String password = "";


    @Bean
    public CanalConnector createCanalConnector() {
        return CanalConnectors.newSingleConnector(new InetSocketAddress(hostname,
                port), destination, username, password);
    }
}
