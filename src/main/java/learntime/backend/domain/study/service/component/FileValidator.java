package learntime.backend.domain.study.service.component;

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

@Component
public class FileValidator {

    private final Tika tika = new Tika();

    private static final List<String> ALLOWED_TYPES = Arrays.asList("image/jpeg", "image/png", "image/webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    public void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileException(FileErrorCode.INVALID_INPUT_VALUE);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileException(FileErrorCode.FILE_SIZE_EXCEEDED);
        }

        // 파일명에 "../" 등을 포함시켜 상위 디렉토리로 접근하는 해킹 방지
        String originalFilename = file.getOriginalFilename();
        if (StringUtils.hasText(originalFilename) && originalFilename.contains("..")) {
            throw new FileException(FileErrorCode.FILE_NAME_INVALID);
        }

        // 1차 MIME 타입 검증 (
        String clientContentType = file.getContentType();
        if (clientContentType == null || !ALLOWED_TYPES.contains(clientContentType)) {
            throw new FileException(FileErrorCode.INVALID_FILE_FORMAT);
        }

        // File Signature (Magic Number) 기반 실제 포맷 검증
        try (InputStream inputStream = file.getInputStream()) {
            String actualMimeType = tika.detect(inputStream);  // Tika가 파일의 실제 헤더 바이트를 분석하여 진짜 MIME 타입을 반환

            if (!ALLOWED_TYPES.contains(actualMimeType)) {
                throw new FileException(FileErrorCode.FILE_CONTENT_MISMATCH);
            }
        } catch (IOException e) {
            // 스트림 읽기 실패 등 네트워크/I/O 예외 처리
            throw new FileException(FileErrorCode.FILE_READ_ERROR);
        }
    }
}