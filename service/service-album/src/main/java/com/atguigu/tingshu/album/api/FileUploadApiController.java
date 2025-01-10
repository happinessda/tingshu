package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.config.MinioConstantProperties;
import com.atguigu.tingshu.common.result.Result;
import io.minio.*;
import io.minio.errors.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Tag(name = "上传管理接口")
@RestController
@RequestMapping("api/album")
public class FileUploadApiController {

    @Autowired
    private MinioConstantProperties minioConstantProperties;

    /**
     * 上传文件
     *
     * @param file
     * @return
     */
    @Operation(summary = "上传文件")
    @PostMapping("/fileUpload")
    public Result fileUpload(MultipartFile file) {
        //  springmvc：文件上传类 MultipartFile , 调用minio api 实现文件上传功能！
        //  声明一个url 地址
        String url = "";
        //  上传完成之后，返回一个图片路径，将路径封装到result.
        try {
            //  创建客户端对象
            MinioClient minioClient =
                    MinioClient.builder()
                            .endpoint(minioConstantProperties.getEndpointUrl())
                            .credentials(minioConstantProperties.getAccessKey(), minioConstantProperties.getSecreKey())
                            .build();

            // Make 'asiatrip' bucket if not exist.
            boolean found =
                    minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioConstantProperties.getBucketName()).build());
            if (!found) {
                // Make a new bucket called 'asiatrip'.
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioConstantProperties.getBucketName()).build());
            } else {
                System.out.println("Bucket " + minioConstantProperties.getBucketName() + " already exists.");
            }
            //  调用文件上传方法
            //  获取到文件的后缀名，文件上传之后的名称不能重复！ at.gui.gu.jpg
            String suffix = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
            String fileName = UUID.randomUUID().toString().replaceAll("-", "") + suffix;
            //  获取到文件大小;
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(minioConstantProperties.getBucketName()).object(fileName).stream(
                                    file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
            //  获取到上传之后的结果：
            //  imgcps.jd.com/img-cubic/creative_server_cia_jdcloud/v2/2020217/10046194592722/FocusFullshop/CkJqZnMvdDEvMjUzNDYvMzAvMjI0MzgvMzU0NjEvNjZjNjA1OTJGZWM2Y2EwMzcvOTAyODc5ZjU2NjlhYTU1MS5wbmcSBTcwNS10MAI4-aZ7QhAKDOeRtua2temkkOahjBABQg0KCei0rei_h-eYvhACQhAKDOeri-WNs-aKoui0rRAGQgoKBuenjeiNiRAHWNLX8P6wpAI/cr/s/q.jpg
            //  http://192.168.200.130:9000/bjatguigutsminio/atguigu.jpg
            url = minioConstantProperties.getEndpointUrl() + "/" + minioConstantProperties.getBucketName() + "/" + fileName;
            System.out.println("url:"+url);
        } catch (ErrorResponseException e) {
            throw new RuntimeException(e);
        } catch (InsufficientDataException e) {
            throw new RuntimeException(e);
        } catch (InternalException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (InvalidResponseException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (ServerException e) {
            throw new RuntimeException(e);
        } catch (XmlParserException e) {
            throw new RuntimeException(e);
        }
        //  返回数据
        return Result.ok(url);
    }

}
