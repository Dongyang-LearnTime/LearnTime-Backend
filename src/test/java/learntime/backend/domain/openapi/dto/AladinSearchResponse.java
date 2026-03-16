package learntime.backend.domain.openapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.*;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "object")
public class AladinSearchResponse {

    private String title;
    private String link;
    private String logo;

    private int totalResults;
    private int startIndex;
    private int itemsPerPage;

    private String query;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "item")
    private List<AladinBook> items;
}