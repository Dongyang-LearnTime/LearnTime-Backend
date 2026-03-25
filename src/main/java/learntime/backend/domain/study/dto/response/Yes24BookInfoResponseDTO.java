package learntime.backend.domain.study.dto.response;

public record Yes24BookInfoResponseDTO(
        String bookToc,   // 책 목차 (크롤링 결과)
        int pageCount     // 총 페이지 수
) {}
