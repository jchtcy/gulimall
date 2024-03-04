package com.jch.gulimall.thirdparty.utils.file;

import com.jch.gulimall.thirdparty.domain.MinioReturn;
import io.minio.*;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Component
public class MinioTemplate {

    @Autowired
    private MinioClient minioClient;

    private static final String SLASH = "/";

    @Value("${minio.defaultBucketName}")
    private String defaultBucketName;

    @Value("${minio.endpoint}")
    private String endpoint;

    /**
     * 创建桶
     *
     * @param bucketName
     * @throws Exception
     */
    public void makeBucket(String bucketName) throws Exception {
        BucketExistsArgs args = BucketExistsArgs.builder().bucket(bucketName).build();
        if (!minioClient.bucketExists(args)) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    /**
     * 上传文件
     *
     * @param file
     * @return
     * @throws Exception
     */
    public MinioReturn putFile(MultipartFile file) throws Exception {
        return putFile(file, file.getOriginalFilename(), defaultBucketName);
    }

    public MinioReturn putFile(MultipartFile file, String fileName, String bucketName) throws Exception {
        if (bucketName == null || bucketName.length() == 0) {
            bucketName = defaultBucketName;
        }
        makeBucket(bucketName);
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketName)
                .object(fileName)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build());
        return new MinioReturn(fileLink(bucketName, fileName), file.getOriginalFilename(), fileName);
    }

    /**
     * 删除文件
     *
     * @param bucketName
     * @param fileName
     * @throws Exception
     */
    public void removeFile(String bucketName, String fileName) throws Exception {
        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(bucketName == null || bucketName.length() == 0 ? defaultBucketName : bucketName)
                .object(fileName)
                .build());
    }

    @SneakyThrows
    private String fileLink(String bucketName, String fileName) {
        return endpoint.concat(SLASH).concat(bucketName).concat(SLASH).concat(fileName);
    }

    private String getFileName(String fileName) {
        return getFileName(null, fileName);
    }

    private String getFileName(String prefix, String fileName) {
        String fileNamePre = fileName;
        String fileType = "";
        int index = fileName.lastIndexOf(".");
        if (index > 0) {
            fileNamePre = fileName.substring(0, index);
            fileType = fileName.substring(index);
        }
        String name = UUID.randomUUID().toString().replace("-", "");
        if (!org.springframework.util.StringUtils.isEmpty(fileNamePre)) {
            name = fileNamePre + "-" + name + fileType;
        }
        if (!StringUtils.isEmpty(prefix)) {
            name = prefix + "-" + name;
        }
        return name;
    }


}
