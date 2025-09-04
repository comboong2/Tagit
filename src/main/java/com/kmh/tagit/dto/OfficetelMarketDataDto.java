package com.kmh.tagit.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OfficetelMarketDataDto {
    private String neighborhood;        // 동명
    private String district;           // 구명
    private double avgDeposit;         // 평균 보증금
    private double avgMonthlyRent;     // 평균 월세
    private double medianDeposit;      // 중간값 보증금
    private double medianMonthlyRent;  // 중간값 월세
    private int transactionCount;      // 거래 건수
    private String recentTransactionDate; // 최근 거래일
}