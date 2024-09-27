package org.apache.hugegraph.service.auth;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
@Log4j2
@Service
public class UserService {
    public File multipartFileToFile(MultipartFile multiFile) {
        long currentTime=System.currentTimeMillis();
        String fileName = multiFile.getOriginalFilename().concat(Long.toString(currentTime));
        String prefix = fileName.substring(fileName.lastIndexOf("."));
        try {
            File file = File.createTempFile(fileName, prefix);
            multiFile.transferTo(file);
            return file;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
