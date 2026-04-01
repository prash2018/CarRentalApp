package com.carrental.dto;

import com.carrental.enums.CarType;
import com.carrental.enums.ReservationStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponse {
    private Long id;
    private String customerName;
    private String customerEmail;
    private CarType carType;
    private String carCode;
    private String carDescription;
    private LocalDateTime pickupDateTime;
    private LocalDateTime dropOffDateTime;
    private int numberOfDays;
    private BigDecimal dailyRate;
    private BigDecimal totalCost;
    private ReservationStatus status;
}
