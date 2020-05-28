package com.miguan.ballvideo.common.config.db;

/**
 * @Author shixh
 * @Date 2020/2/23
 **/

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "spring.datasource.hikari")
@Data
public class Hikari1Properties {
    private long connectionTimeout;
    private int maximumPoolSize;
    private long maxLifetime;
    private int minimumIdle;
    private long validationTimeout;
    private long idleTimeout;
}
