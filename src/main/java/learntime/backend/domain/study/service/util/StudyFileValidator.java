package learntime.backend.domain.study.service.util;

import learntime.backend.domain.study.error.code.FileErrorCode;
import learntime.backend.domain.study.error.exception.FileException;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

//학습 관련 파일 검증 유틸리티
@Component
public class StudyFileValidator {

    private final Tika tika = new Tika();

    private static final List<String> ALLOWED_TYPES = Arrays.asList("image/jpeg", "image/png", "image/webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    // 업로드된 이미지 파일의 크기, 확장자, 그리고 실제 콘텐츠 타입을 검증합니다.
    public void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileException(FileErrorCode.INVALID_INPUT_VALUE);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileException(FileErrorCode.FILE_SIZE_EXCEEDED);
        }

        String originalFilename = file.getOriginalFilename();
        if (StringUtils.hasText(originalFilename) && originalFilename.contains("..")) {
            throw new FileException(FileErrorCode.FILE_NAME_INVALID);
        }

        String clientContentType = file.getContentType();
        if (clientContentType == null || !ALLOWED_TYPES.contains(clientContentType)) {
            throw new FileException(FileErrorCode.INVALID_FILE_FORMAT);
        }

        try (InputStream inputStream = file.getInputStream()) {
            String actualMimeType = tika.detect(inputStream);

            if (!ALLOWED_TYPES.contains(actualMimeType)) {
                throw new FileException(FileErrorCode.FILE_CONTENT_MISMATCH);
            }
        } catch (IOException e) {
            throw new FileException(FileErrorCode.FILE_READ_ERROR);
        }
    }
}
