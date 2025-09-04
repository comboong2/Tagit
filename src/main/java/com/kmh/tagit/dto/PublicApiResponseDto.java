package com.kmh.tagit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JacksonXmlRootElement(localName = "response")
@JsonIgnoreProperties(ignoreUnknown = true)
public class PublicApiResponseDto {

    // body 필드를 다시 추가합니다.
    private Body body;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        private Items items;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items {
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "item")
        private List<Item> itemList;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        @JacksonXmlProperty(localName = "offiNm")
        private String buildingName;

        @JacksonXmlProperty(localName = "deposit")
        private String deposit;

        @JacksonXmlProperty(localName = "monthlyRent")
        private String monthlyRent;

        @JacksonXmlProperty(localName = "excluUseAr")
        private double area;

        @JacksonXmlProperty(localName = "dealYear")
        private int year;

        @JacksonXmlProperty(localName = "dealMonth")
        private int month;

        @JacksonXmlProperty(localName = "dealDay")
        private int day;

        @JacksonXmlProperty(localName = "sggNm")
        private String district; // 시군구

        @JacksonXmlProperty(localName = "umdNm")
        private String neighborhood; // 법정동
    }
}