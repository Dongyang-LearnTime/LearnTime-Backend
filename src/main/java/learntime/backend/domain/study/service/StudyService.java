package learntime.backend.domain.study.service;

import learntime.backend.domain.study.dto.response.Yes24BookListResponseDTO;
import learntime.backend.domain.study.service.component.Yes24BookCrawler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyService {

    private final Yes24BookCrawler yes24BookCrawler;

    private static final String YES24_BASE_URI = "https://www.yes24.com/product/search?domain=BOOK";
    private static final String BOOK_LIST_SIZE = "24"; // 한번에 몇개의 책을 볼지

    public List<Yes24BookListResponseDTO> getYes24BookList(String bookTitle, int page) {
        String targetUrl = UriComponentsBuilder.fromUriString(YES24_BASE_URI)
                .queryParam("query", bookTitle)
                .queryParam("page", page)
                .queryParam("size", BOOK_LIST_SIZE)
                .build()
                .toUriString();

        return yes24BookCrawler.crawlBookList(targetUrl);
    }
}