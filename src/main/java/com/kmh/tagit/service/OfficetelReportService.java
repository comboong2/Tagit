package com.kmh.tagit.service;

import com.kmh.tagit.dto.OfficetelTransactionDto;
import com.kmh.tagit.dto.OfficetelMarketDataDto;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OfficetelReportService {

    @Value("${api.serviceKey}")
    private String serviceKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final XmlMapper xmlMapper = new XmlMapper();
    private static final String API_URL = "https://apis.data.go.kr/1613000/RTMSDataSvcOffiRent/getRTMSDataSvcOffiRent";

    // 기존 메서드 (건물별 그룹핑) - 유지
    public Map<String, List<OfficetelTransactionDto>> getOfficetelRentData(String lawdCd) {
        List<PublicApiResponseDto.Item> allItems = new ArrayList<>();
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

    // 전체 시세 메서드 (동별 시세) - 기존
    public List<OfficetelMarketDataDto> getOfficetelMarketData(String lawdCd) {
        List<PublicApiResponseDto.Item> allItems = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();

        for (int i = 0; i < 3; i++) {
            YearMonth targetMonth = currentMonth.minusMonths(i);
            String dealYmd = targetMonth.format(DateTimeFormatter.ofPattern("yyyyMM"));
            List<PublicApiResponseDto.Item> monthlyItems = callApiAndParseXml(lawdCd, dealYmd);
            if (monthlyItems != null) {
                allItems.addAll(monthlyItems);
            }
        }

        Map<String, List<PublicApiResponseDto.Item>> groupedByNeighborhood =
                allItems.stream()
                        .filter(item -> item.getNeighborhood() != null && !item.getNeighborhood().trim().isEmpty())
                        .collect(Collectors.groupingBy(PublicApiResponseDto.Item::getNeighborhood));

        return groupedByNeighborhood.entrySet().stream()
                .map(entry -> calculateMarketData(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    // 전세 시세 데이터 - 새로 추가
    public List<OfficetelMarketDataDto> getJeonseMarketData(String lawdCd) {
        List<PublicApiResponseDto.Item> allItems = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();

        for (int i = 0; i < 3; i++) {
            YearMonth targetMonth = currentMonth.minusMonths(i);
            String dealYmd = targetMonth.format(DateTimeFormatter.ofPattern("yyyyMM"));
            List<PublicApiResponseDto.Item> monthlyItems = callApiAndParseXml(lawdCd, dealYmd);
            if (monthlyItems != null) {
                allItems.addAll(monthlyItems);
            }
        }

        // 전세만 필터링 (월세가 0인 경우)
        List<PublicApiResponseDto.Item> jeonseItems = allItems.stream()
                .filter(item -> item.getNeighborhood() != null && !item.getNeighborhood().trim().isEmpty())
                .filter(item -> parseAmount(item.getMonthlyRent()) == 0.0 && parseAmount(item.getDeposit()) > 0.0)
                .collect(Collectors.toList());

        Map<String, List<PublicApiResponseDto.Item>> groupedByNeighborhood =
                jeonseItems.stream().collect(Collectors.groupingBy(PublicApiResponseDto.Item::getNeighborhood));

        return groupedByNeighborhood.entrySet().stream()
                .map(entry -> calculateJeonseMarketData(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    // 월세 시세 데이터 - 새로 추가
    public List<OfficetelMarketDataDto> getMonthlyRentMarketData(String lawdCd) {
        List<PublicApiResponseDto.Item> allItems = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();

        for (int i = 0; i < 3; i++) {
            YearMonth targetMonth = currentMonth.minusMonths(i);
            String dealYmd = targetMonth.format(DateTimeFormatter.ofPattern("yyyyMM"));
            List<PublicApiResponseDto.Item> monthlyItems = callApiAndParseXml(lawdCd, dealYmd);
            if (monthlyItems != null) {
                allItems.addAll(monthlyItems);
            }
        }

        // 월세만 필터링 (월세가 0보다 큰 경우)
        List<PublicApiResponseDto.Item> monthlyRentItems = allItems.stream()
                .filter(item -> item.getNeighborhood() != null && !item.getNeighborhood().trim().isEmpty())
                .filter(item -> parseAmount(item.getMonthlyRent()) > 0.0)
                .collect(Collectors.toList());

        Map<String, List<PublicApiResponseDto.Item>> groupedByNeighborhood =
                monthlyRentItems.stream().collect(Collectors.groupingBy(PublicApiResponseDto.Item::getNeighborhood));

        return groupedByNeighborhood.entrySet().stream()
                .map(entry -> calculateMonthlyRentMarketData(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    // 전체 시세 계산 메서드 - 기존
    private OfficetelMarketDataDto calculateMarketData(String neighborhood, List<PublicApiResponseDto.Item> items) {
        List<Double> deposits = items.stream()
                .map(item -> parseAmount(item.getDeposit()))
                .filter(deposit -> deposit > 0)
                .collect(Collectors.toList());

        List<Double> monthlyRents = items.stream()
                .map(item -> parseAmount(item.getMonthlyRent()))
                .collect(Collectors.toList());

        double avgDeposit = deposits.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double avgMonthlyRent = monthlyRents.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        double medianDeposit = calculateMedian(deposits);
        double medianMonthlyRent = calculateMedian(monthlyRents);

        String recentDate = items.stream()
                .map(item -> String.format("%d-%02d-%02d", item.getYear(), item.getMonth(), item.getDay()))
                .max(String::compareTo)
                .orElse("N/A");

        String district = items.isEmpty() ? "N/A" : items.get(0).getDistrict();

        return new OfficetelMarketDataDto(
                neighborhood,
                district,
                Math.round(avgDeposit * 100.0) / 100.0,
                Math.round(avgMonthlyRent * 100.0) / 100.0,
                Math.round(medianDeposit * 100.0) / 100.0,
                Math.round(medianMonthlyRent * 100.0) / 100.0,
                items.size(),
                recentDate
        );
    }

    // 전세 시세 계산 메서드 - 새로 추가
    private OfficetelMarketDataDto calculateJeonseMarketData(String neighborhood, List<PublicApiResponseDto.Item> items) {
        List<Double> deposits = items.stream()
                .map(item -> parseAmount(item.getDeposit()))
                .filter(deposit -> deposit > 0)
                .collect(Collectors.toList());

        double avgDeposit = deposits.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double medianDeposit = calculateMedian(deposits);

        String recentDate = items.stream()
                .map(item -> String.format("%d-%02d-%02d", item.getYear(), item.getMonth(), item.getDay()))
                .max(String::compareTo)
                .orElse("N/A");

        String district = items.isEmpty() ? "N/A" : items.get(0).getDistrict();

        return new OfficetelMarketDataDto(
                neighborhood,
                district,
                Math.round(avgDeposit * 100.0) / 100.0,
                0.0,  // 월세는 0
                Math.round(medianDeposit * 100.0) / 100.0,
                0.0,  // 월세는 0
                items.size(),
                recentDate
        );
    }

    // 월세 시세 계산 메서드 - 새로 추가
    private OfficetelMarketDataDto calculateMonthlyRentMarketData(String neighborhood, List<PublicApiResponseDto.Item> items) {
        List<Double> deposits = items.stream()
                .map(item -> parseAmount(item.getDeposit()))
                .collect(Collectors.toList());

        List<Double> monthlyRents = items.stream()
                .map(item -> parseAmount(item.getMonthlyRent()))
                .filter(rent -> rent > 0)
                .collect(Collectors.toList());

        double avgDeposit = deposits.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double avgMonthlyRent = monthlyRents.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        double medianDeposit = calculateMedian(deposits);
        double medianMonthlyRent = calculateMedian(monthlyRents);

        String recentDate = items.stream()
                .map(item -> String.format("%d-%02d-%02d", item.getYear(), item.getMonth(), item.getDay()))
                .max(String::compareTo)
                .orElse("N/A");

        String district = items.isEmpty() ? "N/A" : items.get(0).getDistrict();

        return new OfficetelMarketDataDto(
                neighborhood,
                district,
                Math.round(avgDeposit * 100.0) / 100.0,
                Math.round(avgMonthlyRent * 100.0) / 100.0,
                Math.round(medianDeposit * 100.0) / 100.0,
                Math.round(medianMonthlyRent * 100.0) / 100.0,
                items.size(),
                recentDate
        );
    }

    // 금액 파싱 헬퍼 메서드 - 기존
    private double parseAmount(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(amount.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // 중간값 계산 헬퍼 메서드 - 기존
    private double calculateMedian(List<Double> values) {
        if (values.isEmpty()) return 0.0;

        Collections.sort(values);
        int size = values.size();

        if (size % 2 == 0) {
            return (values.get(size / 2 - 1) + values.get(size / 2)) / 2.0;
        } else {
            return values.get(size / 2);
        }
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

        String xmlResponse = null;
        try {
            xmlResponse = restTemplate.getForObject(uri, String.class);
            if (xmlResponse != null) {
                PublicApiResponseDto responseDto = xmlMapper.readValue(xmlResponse, PublicApiResponseDto.class);
                if (responseDto != null && responseDto.getBody() != null && responseDto.getBody().getItems() != null) {
                    return responseDto.getBody().getItems().getItemList();
                }
            }
        } catch (Exception e) {
            System.err.println("======= API 호출 또는 XML 파싱 오류 발생 =======");
            System.err.println("요청 URL: " + uri);
            System.err.println("오류 메시지: " + e.getMessage());
            System.err.println("오류가 발생한 XML 응답 내용:\n" + xmlResponse);
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