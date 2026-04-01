package com.carrental.model;

import com.carrental.enums.CarType;
import com.carrental.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/*
  A Reservation binds a customer to a specific Car for a date/time window.

  Business rules encapsulated here (ENCAPSULATION):
    - A reservation can only be cancelled while CONFIRMED
    - Total cost is derived from the car's polymorphic daily rate
    - Overlap rental detection uses [pickupDateTime, dropOffDateTime)
 */
@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Customer name — kept simple; a real system would FK to a Customer entity.
    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "car_type", nullable = false)
    private CarType carType;

    // The specific car assigned from the available vehicles.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;

    //  Desired pickup date and time.
    @Column(name = "pickup_datetime", nullable = false)
    private LocalDateTime pickupDateTime;

    // Number of rental days requested.
    @Column(name = "number_of_days", nullable = false)
    private int numberOfDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.CONFIRMED;

    @Column(name = "total_cost", precision = 10, scale = 2)
    private BigDecimal totalCost;

    // Derived values
    // Computed drop-off time — pickup + numberOfDays.
    public LocalDateTime getDropOffDateTime() {
        return pickupDateTime.plusDays(numberOfDays);
    }

    /**
     * Returns true when this reservation's window overlaps with [start, end).
     * Used to enforce fleet limits when creating new reservations.
     *
     * Two windows overlap when: thisStart < otherEnd AND thisEnd > otherStart
     */
    public boolean overlapsWith(LocalDateTime start, LocalDateTime end) {
        return pickupDateTime.isBefore(end) && getDropOffDateTime().isAfter(start);
    }

    // Reservation State transitions
    public void cancel() {
        if (status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException(
                "Only CONFIRMED reservations can be cancelled. Current status: " + status);
        }
        this.status = ReservationStatus.CANCELLED;
    }

    public void complete() {
        if (status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException(
                "Only CONFIRMED reservations can be completed. Current status: " + status);
        }
        this.status = ReservationStatus.COMPLETED;
    }

    public boolean isActive() {
        return status == ReservationStatus.CONFIRMED;
    }
}
