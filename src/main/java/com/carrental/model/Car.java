package com.carrental.model;

import com.carrental.enums.CarType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/*
  Abstract base class for all car types.

 OOP Principles applied:
    ABSTRACTION  — clients program to Car, not to Sedan/SUV/Van
    INHERITANCE  — Sedan, SUV, Van all extend abstract class Car
    POLYMORPHISM — getDailyRate() and getPassengerCapacity() are overridden per car subtype
    ENCAPSULATION — identity and type fields are immutable after construction
 */
@Entity
@Table(name = "cars")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "car_category", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
@ToString
public abstract class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // car code identifier, e.g. "SEDAN-1", "SUV-3"
    @Column(name = "car_code", unique = true, nullable = false)
    private String carCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "car_type", nullable = false)
    private CarType carType;

    @Column(nullable = false)
    private String make;

    @Column(nullable = false)
    private String model;

    @Column(name = "manufacture_year", nullable = false)
    private int year;

    // Polymorphic contract, child class implements these abstract methods
    /**
     * Returns the daily rental rate for this car type.
     * Each subclass defines its own rate — POLYMORPHISM.
     */
    public abstract BigDecimal getDailyRentalRate();

    /**
     * Returns the maximum number of passengers.
     * Each subclass defines its own capacity — POLYMORPHISM.
     */
    public abstract int getCarPassengerCapacity();

    /**
     * Method to calculate total cost for a given number of days.
     * Subclasses influence the result only by overriding getDailyRate().
     */
    public BigDecimal calculateTotalRentalCost(int days) {
        if (days <= 0)
            throw new IllegalArgumentException("Days must be positive.");
        return getDailyRentalRate().multiply(BigDecimal.valueOf(days));
    }

    public String getDescription() {
        return year + " " + make + " " + model
               + " (" + carType.displayName() + ", "
               + getCarPassengerCapacity() + " seats, $"
               + getDailyRentalRate() + "/day)";
    }
}
