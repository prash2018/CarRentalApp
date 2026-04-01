package com.carrental.repository;

import com.carrental.enums.CarType;
import com.carrental.enums.ReservationStatus;
import com.carrental.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // Returns List of all Customer Reservations filtered by optional email
    List<Reservation> findByCustomerEmail(String email);

    // Returns List of all Customer Reservations filtered by car type
    List<Reservation> findByCarType(CarType carType);

    // Returns List of all Customer Reservations filtered by Reservation Status
    List<Reservation> findByStatus(ReservationStatus status);

    List<Reservation> findByCarIdAndStatus(Long carId, ReservationStatus status);

    // Returns count of reserved cars of a specific type booked in the window.
    @Query("""
        SELECT COUNT(r) FROM Reservation r
        WHERE r.carType = :carType
        AND r.status = 'CONFIRMED'
        AND r.pickupDateTime < :dropOff
        AND FUNCTION('TIMESTAMPADD', DAY, r.numberOfDays, r.pickupDateTime) > :pickup
        """)
    long countOverlappingConfirmed(
            @Param("carType") CarType carType,
            @Param("pickup") LocalDateTime pickup,
            @Param("dropOff") LocalDateTime dropOff);

    // Returns IDs of cars of a specific type already booked in the window.
    // Used by makeReservation() to pick a free car.
    @Query("""
        SELECT r.car.id FROM Reservation r
        WHERE r.carType = :carType
        AND r.status = 'CONFIRMED'
        AND r.pickupDateTime < :dropOff
        AND FUNCTION('TIMESTAMPADD', DAY, r.numberOfDays, r.pickupDateTime) > :pickup
        """)
    List<Long> findOccupiedCarIds(
            @Param("carType") CarType carType,
            @Param("pickup") LocalDateTime pickup,
            @Param("dropOff") LocalDateTime dropOff);

     // Returns IDs of ALL cars (any type) already booked in the window.
     // Used by CarService to filter available cars across the whole fleet.
    @Query("""
        SELECT r.car.id FROM Reservation r
        WHERE r.status = 'CONFIRMED'
        AND r.pickupDateTime < :dropOff
        AND FUNCTION('TIMESTAMPADD', DAY, r.numberOfDays, r.pickupDateTime) > :pickup
        """)
    List<Long> findAllOccupiedCarIds(
            @Param("pickup") LocalDateTime pickup,
            @Param("dropOff") LocalDateTime dropOff);
}