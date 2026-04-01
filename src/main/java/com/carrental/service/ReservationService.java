package com.carrental.service;

import com.carrental.dto.AvailabilityResponse;
import com.carrental.dto.ReservationRequest;
import com.carrental.dto.ReservationResponse;
import com.carrental.enums.CarType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reservation contract.
 * Demonstrates: ABSTRACTION — callers depend on the interface, not the implementation.
 */
public interface ReservationService {

    // Reserve a car of the given type at the requested date/time for numberOfDays.
    // Throws NoCarAvailableException if the fleet is fully booked for that window.
    ReservationResponse makeReservation(ReservationRequest request);

    // Cancel an existing confirmed reservation.
    ReservationResponse cancelReservation(Long reservationId);

    // Mark a reservation as completed (car returned).
    ReservationResponse completeReservation(Long reservationId);

    // Get a single reservation by ID.
    ReservationResponse getReservation(Long reservationId);

    // List all reservations, optionally filtered by customer email.
    List<ReservationResponse> getReservations(String customerEmail);

    // Returns availability for each car type at the given date/time window.
    // Key requirement: shows how many cars remain in the limited fleet.
    List<AvailabilityResponse> checkAvailability(LocalDateTime pickupDateTime, int numberOfDays);

    // Counts remaining available cars of a type for a given window.
    int countAvailable(CarType carType, LocalDateTime pickupDateTime, int numberOfDays);
}
