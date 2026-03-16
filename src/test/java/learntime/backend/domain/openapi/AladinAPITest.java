package learntime.backend.domain.openapi;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import learntime.backend.domain.openapi.dto.AladinSearchResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RestClientRealApiTest {

    private static final Logger log =
            LoggerFactory.getLogger(RestClientRealApiTest.class);

    // RestClient Bean 주입
    @Autowired
    private RestClient customRestClient;

    // application.properties 값 주입
    @Value("${aladin.key}")
    private String aladinKey;

    @Test
    void aladin_real_api_xml_parsing() throws Exception {

        String url = String.format(
                "http://www.aladin.co.kr/ttb/api/ItemSearch.aspx?ttbkey=%s&Query=aladdin&QueryType=Title&MaxResults=10&start=1&SearchTarget=Book&output=xml&Version=20131101",
                aladinKey
        );

        String xml = customRestClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);

        XmlMapper xmlMapper = new XmlMapper();

        AladinSearchResponse response =
                xmlMapper.readValue(xml, AladinSearchResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.getItems()).isNotEmpty();

        log.info("검색 결과 총 개수: {}", response.getTotalResults());

        response.getItems().forEach(book ->
                log.info("책: {} / {} / {}", book.getTitle(), book.getAuthor(), book.getPublisher())
        );
    }
}