package com.carrental.dto;

import com.carrental.enums.CarType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationRequest {

    /**
     * Customer name
     */
    @NotBlank(message = "Customer name is required")
    private String customerName;

    /**
     * Customer email
     */
    @Email(message = "Valid email is required")
    @NotBlank(message = "Customer email is required")
    private String customerEmail;

    /**
     * Required car type for the rental
     */
    @NotNull(message = "Car type is required (SEDAN, SUV, VAN)")
    private CarType carType;

    /**
     * Rental pickup date and time
     */
    @NotNull(message = "Pickup date and time is required")
    @Future(message = "Pickup must be in the future")
    private LocalDateTime pickupDateTime;

    /**
     * Number of days requested for the rental
     */
    @Min(value = 1, message = "Minimum rental period is 1 day")
    @Max(value = 30, message = "Maximum rental period is 30 days")
    private int numberOfDays;

    /**
     * Optional. If provided, the system reserves this specific car rather than
     * auto-assigning one. The car must exist, match the requested carType,
     * and be free for the requested window — otherwise the request is rejected.
     */
    @Positive(message = "Car ID must be a positive number")
    private Long carId;
}
