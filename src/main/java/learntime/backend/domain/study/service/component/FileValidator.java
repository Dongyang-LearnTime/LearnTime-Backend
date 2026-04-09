package learntime.backend.domain.study.service.component;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.util.Arrays;
import java.util.List;

@Component
public class FileValidator {

    private static final List<String> ALLOWED_TYPES = Arrays.asList("image/jpeg", "image/png", "image/webp");

    public void validateImage(MultipartFile file) {
        // 1. 존재 여부 검증
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일을 첨부해주세요.");
        }

        // 2. MIME 타입 검증
        String contentType = file.getContentType();
        if (contentType == null || !isSupportedType(contentType)) {
            throw new IllegalArgumentException("지원하지 않는 형식입니다 (jpg, png, webp만 가능).");
        }
    }

    private boolean isSupportedType(String contentType) {
        return ALLOWED_TYPES.contains(contentType);
    }

}