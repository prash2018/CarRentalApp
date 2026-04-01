package com.carrental.controller;

import com.carrental.dto.AvailabilityResponse;
import com.carrental.dto.ReservationRequest;
import com.carrental.dto.ReservationResponse;
import com.carrental.enums.CarType;
import com.carrental.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reservations")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "Reserve a Sedan, SUV, or Van for a given date/time and duration")
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * Creates a car reservation for specific date/time for N days
     */
    @PostMapping
    @Operation(summary = "Reserve a car of a given type at a desired date/time for N days")
    public ResponseEntity<ReservationResponse> makeReservation(
            @Valid @RequestBody ReservationRequest request) {
        log.info("Entered makeReservation()....");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservationService.makeReservation(request));
    }

    /**
     * Get a car reservation using Reservation ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get a reservation by Reservation ID")
    public ResponseEntity<ReservationResponse> getReservationById(@PathVariable Long id) {
        log.info("Entered getReservationById()....");
        return ResponseEntity.ok(reservationService.getReservation(id));
    }

    /**
     * Get all car reservations
     */
    @GetMapping
    @Operation(summary = "List all reservations, optionally filtered by customer email")
    public ResponseEntity<List<ReservationResponse>> getAllReservations(
                @RequestParam(required = false) String email) {
        log.info("Entered getAllReservations()....");
        return ResponseEntity.ok(reservationService.getReservations(email));
    }

    /**
     * Cancel an existing reservation
     */
    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel a confirmed reservation using Reservation ID")
    public ResponseEntity<ReservationResponse> cancelReservation(@PathVariable Long id) {
        log.info("Entered cancelReservation()....");
        return ResponseEntity.ok(reservationService.cancelReservation(id));
    }

    /**
     * Return a car to complete a reservation
     */
    @PatchMapping("/{id}/complete")
    @Operation(summary = "Mark a reservation as completed (car returned) using Reservation ID")
    public ResponseEntity<ReservationResponse> completeReservation(@PathVariable Long id) {
        log.info("Entered completeReservation()....");
        return ResponseEntity.ok(reservationService.completeReservation(id));
    }

    /**
     * Check car availabilty based on given date/time for N daus
     */
    @GetMapping("/availability")
    @Operation(summary = "Check how many cars of each type are available for a given window")
    public ResponseEntity<List<AvailabilityResponse>> checkCarAvailability(
            @Parameter(description = "Pickup date/time in US format — e.g. 2026-04-30T22:30")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime pickupDateTime,
            @Parameter(description = "Number of car rental days in the range of 1-30")
            @RequestParam int numberOfDays) {
        log.info("Entered checkCarAvailability()....");
        return ResponseEntity.ok(
                reservationService.checkAvailability(pickupDateTime, numberOfDays));
    }

    /**
     * Check specific car availabilty based on given date/time for N daus
     */
    @GetMapping("/availability/{carType}")
    @Operation(summary = "Check available count for a specific car type")
    public ResponseEntity<Map<String, Object>> checkCarAvailabilityByType(
            @PathVariable CarType carType,
            @Parameter(description = "Pickup date/time in US format — e.g. 2026-04-30T22:30")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime pickupDateTime,
            @Parameter(description = "Number of car rental days in the range of 1-30")
            @RequestParam int numberOfDays) {
        log.info("Entered checkCarAvailabilityByType()....");
        int count = reservationService.countAvailable(carType, pickupDateTime, numberOfDays);
        return ResponseEntity.ok(Map.of(
            "carType", carType,
            "availableCars", count,
            "available", count > 0
        ));
    }
}
