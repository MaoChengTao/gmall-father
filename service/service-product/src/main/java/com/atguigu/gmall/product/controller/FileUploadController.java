package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient1;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Api(tags = "fastDFS文件布式文件上传系统")
@RestController //  @ResponseBody + @Controller
@RequestMapping("admin/product")
public class FileUploadController {

    @Value("${fileServer.url}")
    private String fileUrl; // http://192.168.200.128:8080/ 保存文件的服务器

    /**
     * http://api.gmall.com/admin/product/fileUpload
     * 文件布式文件上传系统
     *
     * @param file 上传的文件
     * @return 上传之后返回图片的地址
     */
    @RequestMapping("fileUpload")
    public Result<String> fileUpload(MultipartFile file) throws IOException, MyException {
        // 1 读取配置文件 获取上传时使用的地址
        String configFile = this.getClass().getResource("/tracker.conf").getFile();

        // 2 声明上传文件后返回路径的变量
        String path = "";

        if (configFile != null) {
            // 3 初始化
            ClientGlobal.init(configFile);

            // 4 创建 trackerClient
            TrackerClient trackerClient = new TrackerClient();

            // 5 用 trackerClient 获取 trackerServer
            TrackerServer trackerServer = trackerClient.getConnection();

            // 6 创建 storageClient1
            StorageClient1 storageClient1 = new StorageClient1(trackerServer, null);

            // 7 调用方法上传文件
            String extension = FilenameUtils.getExtension(file.getOriginalFilename());
            path = storageClient1.upload_appender_file1(file.getBytes(), extension, null);

            log.info("上传后返回的完整的文件路径" + fileUrl + path);
        }
        // 8 返回地址：拼接上回显的地址
        return Result.ok(fileUrl + path);
    }
}
