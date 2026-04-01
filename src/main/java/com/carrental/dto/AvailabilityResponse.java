package com.carrental.dto;

import com.carrental.enums.CarType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityResponse {
    private CarType carType;
    private int totalCars;
    private int availableCars;
    private int reservedCars;
    private BigDecimal dailyRate;
    private int passengerCapacity;
    private boolean available;
}
