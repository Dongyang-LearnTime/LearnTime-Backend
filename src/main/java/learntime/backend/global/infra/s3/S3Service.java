package learntime.backend.global.infra.s3;

import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.S3Exception;
import learntime.backend.global.utils.FileValidatorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
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

            log.info("originalFileName={}", file.getOriginalFilename());
            log.info("contentType={}", file.getContentType());
            log.info("size={}", file.getSize());

            s3Client.putObject(putObjectRequest, 
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            GetUrlRequest getUrlRequest = GetUrlRequest.builder()
                    .bucket(bucketName)
                    .key(uniqueFileName)
                    .build();

            return s3Client.utilities().getUrl(getUrlRequest).toString();

        } catch (Exception e) {
            log.error("S3 File Upload Failed: {}", e.getMessage());
            throw new S3Exception(ErrorCode.S3_UPLOAD_FAILED);
        }
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        String key;
        try {
            URI uri = new URI(fileUrl);
            key = uri.getPath();
            if (key.startsWith("/")) {
                key = key.substring(1);
            }
        } catch (Exception e) {
            log.error("Invalid S3 File URL {}: {}", fileUrl, e.getMessage());
            throw new S3Exception(ErrorCode.S3_URL_INVALID);
        }

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("S3 File Deleted: {}", key);
        } catch (Exception e) {
            log.error("S3 File Delete Failed for {}: {}", key, e.getMessage());
            throw new S3Exception(ErrorCode.S3_DELETE_FAILED);
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
