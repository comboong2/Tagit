package com.kmh.tagit.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 최종적으로 JSON 응답에 사용될 DTO 입니다.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OfficetelTransactionDto {
    private String buildingName;
    private String deposit;
    private String monthlyRent;
    private String area;
    private String contractDate;
}