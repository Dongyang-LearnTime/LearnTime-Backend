package learntime.backend.domain.study.service.component;

import learntime.backend.domain.study.dto.response.Yes24BookResponseDTO;
import learntime.backend.global.error.BusinessException;
import learntime.backend.global.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class Yes24BookCrawler {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";
    private static final int TIMEOUT_MS = 5000;

    // 책 목록 정보 크롤링
    public List<Yes24BookResponseDTO> crawlBookList(String targetUrl) {
        List<Yes24BookResponseDTO> bookList = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(targetUrl)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .get();

            Elements bookElements = doc.select("#yesSchList > li");

            for (Element element : bookElements) {
                Element titleEl = element.selectFirst(".info_name .gd_name");
                Element authorEl = element.selectFirst(".info_pubGrp .info_auth");
                Element pubEl = element.selectFirst(".info_pubGrp .info_pub");

                if (titleEl != null) {
                    bookList.add(Yes24BookResponseDTO.builder()
                            .title(titleEl.text())
                            .author(authorEl != null ? authorEl.text() : "저자 미상")
                            .publisher(pubEl != null ? pubEl.text() : "출판사 미상")
                            .linkUrl("https://www.yes24.com" + titleEl.attr("href"))
                            .build());
                }
            }
        } catch (IOException e) {
            log.error("도서 목록 크롤링 중 I/O 오류 발생. URL: {}", targetUrl, e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }

        return bookList;
    }


    // 책 목차 정보 크롤링
    public String crawlToc(String linkUrl) {
        try {
            Document doc = Jsoup.connect(linkUrl)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .get();

            Element textAreaTag = doc.selectFirst("#infoset_toc .txtContentText");
            if (textAreaTag == null) {
                log.warn("목차 요소를 찾을 수 없습니다. URL: {}", linkUrl);
                return "목차 정보 없음";
            }

            String htmlWithBr = textAreaTag.html().replaceAll("(?i)<br\\s*/?>", "\n");

            return Jsoup.parse(htmlWithBr).text();

        } catch (IOException e) {
            log.error("목차 크롤링 중 I/O 오류 발생: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }
}