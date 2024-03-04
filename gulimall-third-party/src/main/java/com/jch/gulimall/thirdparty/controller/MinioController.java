package com.jch.gulimall.thirdparty.controller;

import com.jch.common.utils.R;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @Author: jievhaha
 * @Date: 2022/5/20 8:56
 */
@RestController
public class MinioController {
    @Value("${minio.endpoint}")
    private String endpoint;
    @Value("${minio.username}")
    private String accessKey;
    @Value("${minio.password}")
    private String secretKey;
    @Value("${minio.defaultBucketName}")
    private String bucket;

    @GetMapping("/minio/policy")
    private R policy(@RequestParam("pic")String pic) {
        String name  = UUID.randomUUID() + "-" + pic; // 修改文件名防止上传相同文件被覆盖
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String format = dateTimeFormatter.format(LocalDateTime.now());
        String dir = format; // 用户上传文件时指定的前缀，即上传目录。
        pic = dir + "/" + name;// 文件上传目录+名称
        String url = "";
        String path = "/" + bucket + "/" +  pic;// 该值暂没用到，前端不熟直接使用没用域名的图片地址不知道怎么处理

        Map<String, String> respMap=null;
        try {

            MinioClient minioClient = MinioClient.builder().endpoint(endpoint)
                    .credentials(accessKey,secretKey).build();
            Map<String, String> reqParams = new HashMap<>();
            reqParams.put("response-content-type", "application/json");
            url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)//这里必须是PUT，如果是GET的话就是文件访问地址了。如果是POST上传会报错.
                            .bucket(bucket)
                            .object(pic)
                            .expiry(60 * 60 * 24)
                            .extraQueryParams(reqParams)
                            .build());
            System.out.println(url); // 前端直传需要的url地址
            respMap= new LinkedHashMap<>();
            respMap.put("name", name);
            respMap.put("host", url);
            respMap.put("path", path);
            respMap.put("url", endpoint + path);// Constant.MINIO_URL自己的minio服务器地址
        } catch (Exception e) {
            System.out.println("Error occurred: " + e);
        }
        return R.ok().put("data", respMap);
    }
}
