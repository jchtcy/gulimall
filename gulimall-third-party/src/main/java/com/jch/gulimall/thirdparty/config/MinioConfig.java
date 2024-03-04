package com.jch.gulimall.thirdparty.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Description minio配置类
 */
@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.username}")
    private String username;

    @Value("${minio.password}")
    private String password;

    @Value("${minio.defaultBucketName}")
    private String defaultBucketName;

    @Bean
    public MinioClient minioClient(){
        return MinioClient.builder().credentials(username, password).endpoint(endpoint).build();
    }

}
