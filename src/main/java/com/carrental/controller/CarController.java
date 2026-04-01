package com.carrental.controller;

import com.carrental.dto.CarResponse;
import com.carrental.enums.CarType;
import com.carrental.service.CarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cars")
@RequiredArgsConstructor
@Tag(name = "Cars", description = "Browse the rental cars")
public class CarController {

    private final CarService carService;

    @GetMapping
    @Operation(summary = "Get cars — optionally filter by type and/or availability window",
            description = """
                   Without parameters: returns all cars in the fleet.
                   
                   With ?pickupDateTime + ?numberOfDays: returns only cars free for that window.
                   
                   Add ?type=SEDAN/SUV/VAN to narrow search by car type.
                   
                   Examples:
                     GET /api/v1/cars
                   
                     GET /api/v1/cars?type=SUV
                   
                     GET /api/v1/cars?pickupDateTime=2026-04-30T22:30&numberOfDays=3
                   
                     GET /api/v1/cars?type=VAN&pickupDateTime=2026-04-30T22:30&numberOfDays=3
                   
                   """)
    public ResponseEntity<List<CarResponse>> getCars(
           // @Parameter(description = "Filter by car type (SEDAN, SUV, VAN)")
            @RequestParam(required = false) CarType type,
            @Parameter(description = "Pickup date/time in US format — e.g. 2026-04-30T22:30")
            @RequestParam(required = false) LocalDateTime pickupDateTime,
            @Parameter(description = "Number of rental days (required when pickupDateTime is given)")
            @RequestParam(required = false) Integer numberOfDays) {

        if (pickupDateTime != null && numberOfDays != null) {
            return ResponseEntity.ok(
                    carService.getAvailableCars(pickupDateTime, numberOfDays, type));
        }

        if (pickupDateTime != null || numberOfDays != null) {
            throw new IllegalArgumentException(
                    "Both pickupDateTime and numberOfDays are required together.");
        }

        return ResponseEntity.ok(carService.getAllCars(type));
    }

    /**
     * Get car details using its ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get a car by its ID")
    public ResponseEntity<CarResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(carService.getCarById(id));
    }
}
