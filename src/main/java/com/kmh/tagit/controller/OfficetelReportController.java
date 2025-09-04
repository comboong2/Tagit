package com.kmh.tagit.controller;

import com.kmh.tagit.dto.OfficetelTransactionDto;
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

    // 👇 이 API의 응답(produces)은 반드시 UTF-8 인코딩의 JSON 타입이라고 명시합니다.
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
            // 서버 내부 오류 발생 시에도 어떤 에러인지 콘솔에 출력하도록 추가
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}