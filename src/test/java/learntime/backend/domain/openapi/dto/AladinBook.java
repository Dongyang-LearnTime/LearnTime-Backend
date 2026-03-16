package learntime.backend.domain.openapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AladinBook {

    @JacksonXmlProperty(isAttribute = true)
    private String itemId;

    private String title;
    private String link;
    private String author;
    private String pubDate;
    private String description;

    private String isbn;
    private String isbn13;

    private int priceSales;
    private int priceStandard;

    private String cover;
    private String publisher;

    private int salesPoint;
}