package learntime.backend.domain.study.service;

import learntime.backend.domain.study.dto.response.Yes24BookResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudyService {

    private static final String YES24_BASE_URI = "https://www.yes24.com/product/search?domain=BOOK";

    public List<Yes24BookResponseDTO> getYes24BookList(String bookTitle, int page) {
        final String BOOK_LIST_SIZE = "24"; // 한번에 몇개의 책을 크롤링 할지
        List<Yes24BookResponseDTO> bookList = new ArrayList<>();

        String yes24Url = UriComponentsBuilder.fromUriString(YES24_BASE_URI)
                .queryParam("query", bookTitle)
                .queryParam("page", page)
                .queryParam("size", BOOK_LIST_SIZE)
                .build()
                .toUriString();

        try {
            Document doc = Jsoup.connect(yes24Url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .get();

            Elements bookElements = doc.select("#yesSchList > li");

            for (Element element : bookElements) {
                Element titleEl = element.selectFirst(".info_name .gd_name");
                Element authorEl = element.selectFirst(".info_pubGrp .info_auth");
                Element pubEl = element.selectFirst(".info_pubGrp .info_pub");

                if (titleEl != null) {
                    String title = titleEl.text();
                    String linkUrl = "https://www.yes24.com" + titleEl.attr("href");
                    String author = authorEl != null ? authorEl.text() : "저자 미상";
                    String publisher = pubEl != null ? pubEl.text() : "출판사 미상";

                    bookList.add(Yes24BookResponseDTO.builder()
                            .title(title)
                            .author(author)
                            .publisher(publisher)
                            .linkUrl(linkUrl)
                            .build());
                }
            }
        } catch (IOException e) {
            System.err.println("크롤링 중 오류: " + e.getMessage());
        }

        return bookList;
    }
}
