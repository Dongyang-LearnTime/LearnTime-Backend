package learntime.backend.global.infra.s3;

import learntime.backend.global.error.code.FileErrorCode;
import learntime.backend.global.error.exception.FileException;
import learntime.backend.global.utils.FileValidatorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public String uploadFile(MultipartFile file, String dirName) {
        String originalFileName = file.getOriginalFilename();
        String extension = getExtension(file);
        String uniqueFileName = dirName + "/" + UUID.randomUUID() + extension;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(uniqueFileName)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, 
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            GetUrlRequest getUrlRequest = GetUrlRequest.builder()
                    .bucket(bucketName)
                    .key(uniqueFileName)
                    .build();

            return s3Client.utilities().getUrl(getUrlRequest).toString();

        } catch (IOException e) {
            log.error("S3 File Upload Failed: {}", e.getMessage());
            throw new FileException(FileErrorCode.FILE_READ_ERROR);
        }
    }

    private String getExtension(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
        if (originalFileName != null && originalFileName.contains(".")) {
            return originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        return FileValidatorUtil.getExtension(file.getContentType());
    }
}
