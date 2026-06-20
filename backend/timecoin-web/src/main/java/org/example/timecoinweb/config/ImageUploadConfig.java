package org.example.timecoinweb.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 在应用启动时将图片目录解析为绝对路径，避免上传时相对路径落到 Tomcat 临时目录。
 */
@Component
@Slf4j
@Getter
public class ImageUploadConfig {

    private final Path uploadDir;

    public ImageUploadConfig(@Value("${app.upload.image-dir:./data/image}") String imageDir) throws IOException {
        Path path = Paths.get(imageDir);
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir")).resolve(path).normalize();
        }
        Files.createDirectories(path);
        this.uploadDir = path;
        log.info("图片上传目录: {}", uploadDir);
    }

    public String getUploadDirUri() {
        String location = uploadDir.toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }
}
