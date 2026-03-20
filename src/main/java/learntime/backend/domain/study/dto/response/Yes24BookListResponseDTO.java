package learntime.backend.domain.study.dto.response;

import lombok.Builder;

@Builder
public record Yes24BookListResponseDTO(
        String title,      // 책 이름
        String author,     // 작가
        String publisher,  // 출판사
        String linkUrl     // 상세 정보 링크
) { }
