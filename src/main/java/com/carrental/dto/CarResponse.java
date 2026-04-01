package com.carrental.dto;

import com.carrental.enums.CarType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarResponse {
    private Long id;
    private String carCode;
    private CarType carType;
    private String make;
    private String model;
    private int year;
    private BigDecimal dailyRate;
    private int passengerCapacity;
    private String description;
}