package com.carrental.service;

import com.carrental.dto.CarResponse;
import com.carrental.enums.CarType;

import java.time.LocalDateTime;
import java.util.List;

public interface CarService {

    // Returns all cars in the fleet, optionally filtered by type.
    List<CarResponse> getAllCars(CarType carType);

    // Returns only cars that are free for the given date window,
    // optionally filtered by type.
    List<CarResponse> getAvailableCars(LocalDateTime pickup, int numberOfDays, CarType carType);

    // Returns a single car by its database ID.
    CarResponse getCarById(Long id);
}