package com.defect.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Chroma 向量库配置类
 * <p>全部取值来自 application.yml 的 chroma.*；向量库不可达时检索自动降级为 MySQL 全文索引</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "chroma")
public class ChromaProperties {

    /** 服务主机 */
    private String host = "localhost";

    /** 服务端口 */
    private int port = 8000;

    /** API 路径前缀，Chroma 0.5+ / 1.x 为 /api/v2，0.4.x 为 /api/v1 */
    private String apiPath = "/api/v2";

    /** 租户（v2 路径使用） */
    private String tenant = "default_tenant";

    /** 数据库（v2 路径使用） */
    private String database = "default_database";

    /** 知识库集合名 */
    private String collection = "defect_knowledge";

    /** 连接与读取超时（毫秒） */
    private int timeout = 5000;

    /**
     * 服务根地址
     */
    public String baseUrl() {
        return "http://" + host + ":" + port;
    }
}
