package com.kmh.tagit.service;

import com.kmh.tagit.dto.OfficetelTransactionDto;
import com.kmh.tagit.dto.PublicApiResponseDto;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OfficetelReportService {

    @Value("${api.serviceKey}")
    private String serviceKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final XmlMapper xmlMapper = new XmlMapper();
    private static final String API_URL = "https://apis.data.go.kr/1613000/RTMSDataSvcOffiRent/getRTMSDataSvcOffiRent";


    public Map<String, List<OfficetelTransactionDto>> getOfficetelRentData(String lawdCd) {
        List<PublicApiResponseDto.Item> allItems = new ArrayList<>();
        // 🚨 현재 날짜를 기준으로 최근 3개월치 데이터를 조회합니다.
        // 2025년 9월 4일 기준 -> 202509, 202508, 202507 월 데이터를 조회합니다.
        YearMonth currentMonth = YearMonth.now();

        for (int i = 0; i < 3; i++) {
            YearMonth targetMonth = currentMonth.minusMonths(i);
            String dealYmd = targetMonth.format(DateTimeFormatter.ofPattern("yyyyMM"));
            List<PublicApiResponseDto.Item> monthlyItems = callApiAndParseXml(lawdCd, dealYmd);
            if (monthlyItems != null) {
                allItems.addAll(monthlyItems);
            }
        }

        return allItems.stream()
                .map(this::convertToDto)
                .collect(Collectors.groupingBy(OfficetelTransactionDto::getBuildingName));
    }

    private List<PublicApiResponseDto.Item> callApiAndParseXml(String lawdCd, String dealYmd) {
        URI uri = UriComponentsBuilder.fromUriString(API_URL)
                .queryParam("serviceKey", serviceKey)
                .queryParam("LAWD_CD", lawdCd)
                .queryParam("DEAL_YMD", dealYmd)
                .queryParam("numOfRows", 100)
                .build(true)
                .toUri();

        System.out.println("Request URL: " + uri);

        String xmlResponse = null; // 변수를 try-catch 블록 밖으로 선언
        try {
            xmlResponse = restTemplate.getForObject(uri, String.class); // 값 할당
            if (xmlResponse != null) {
                PublicApiResponseDto responseDto = xmlMapper.readValue(xmlResponse, PublicApiResponseDto.class);
                if (responseDto != null && responseDto.getBody() != null && responseDto.getBody().getItems() != null) {
                    return responseDto.getBody().getItems().getItemList();
                }
            }
        } catch (Exception e) { // IOException 대신 Exception으로 변경하여 더 많은 에러를 잡습니다.
            System.err.println("======= API 호출 또는 XML 파싱 오류 발생 =======");
            System.err.println("요청 URL: " + uri);
            System.err.println("오류 메시지: " + e.getMessage());
            System.err.println("오류가 발생한 XML 응답 내용:\n" + xmlResponse); // 에러가 발생한 XML을 직접 출력
        }
        return Collections.emptyList();
    }

    private OfficetelTransactionDto convertToDto(PublicApiResponseDto.Item item) {
        String contractDate = String.format("%d-%d-%d", item.getYear(), item.getMonth(), item.getDay());
        return new OfficetelTransactionDto(
                item.getBuildingName() != null ? item.getBuildingName().trim() : "N/A",
                item.getDeposit() != null ? item.getDeposit().trim() : "0",
                item.getMonthlyRent() != null ? item.getMonthlyRent().trim() : "0",
                String.valueOf(item.getArea()),
                contractDate
        );
    }
}