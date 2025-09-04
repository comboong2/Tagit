package com.kmh.tagit.controller;

import com.kmh.tagit.dto.OfficetelTransactionDto;
import com.kmh.tagit.dto.OfficetelMarketDataDto;
import com.kmh.tagit.service.OfficetelReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/officetel")
@RequiredArgsConstructor
public class OfficetelReportController {

    private final OfficetelReportService officetelReportService;

    // 기존 API (건물별 거래 내역) - 유지
    @GetMapping(value = "/rent-data", produces = "application/json; charset=UTF-8")
    public ResponseEntity<Map<String, List<OfficetelTransactionDto>>> getRentData(
            @RequestParam("lawdCd") String lawdCd) {

        if (lawdCd == null || lawdCd.length() != 5) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Map<String, List<OfficetelTransactionDto>> data = officetelReportService.getOfficetelRentData(lawdCd);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // 전세 시세 API
    @GetMapping(value = "/jeonse-market", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<OfficetelMarketDataDto>> getJeonseMarket(
            @RequestParam("lawdCd") String lawdCd) {

        if (lawdCd == null || lawdCd.length() != 5) {
            return ResponseEntity.badRequest().build();
        }

        try {
            List<OfficetelMarketDataDto> data = officetelReportService.getJeonseMarketData(lawdCd);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // 월세 시세 API
    @GetMapping(value = "/monthly-rent-market", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<OfficetelMarketDataDto>> getMonthlyRentMarket(
            @RequestParam("lawdCd") String lawdCd) {

        if (lawdCd == null || lawdCd.length() != 5) {
            return ResponseEntity.badRequest().build();
        }

        try {
            List<OfficetelMarketDataDto> data = officetelReportService.getMonthlyRentMarketData(lawdCd);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}