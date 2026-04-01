package com.carrental.model;

import com.carrental.enums.CarType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

/*
  Sedan: up to 5 passengers, economy daily rate.
  Demonstrates INHERITANCE and POLYMORPHISM.
 */
@Entity
@DiscriminatorValue("SEDAN CATEGORY")
@NoArgsConstructor
@ToString(callSuper = true)
public class Sedan extends Car {

    private static final BigDecimal DAILY_RATE = new BigDecimal("49.99");
    private static final int PASSENGER_CAPACITY = 5;

    public Sedan(String carCode, String make, String model, int year) {
        setCarCode(carCode);
        setCarType(CarType.SEDAN);
        setMake(make);
        setModel(model);
        setYear(year);
    }

    @Override
    public BigDecimal getDailyRentalRate() {
        return DAILY_RATE;
    }

    @Override
    public int getCarPassengerCapacity() {
        return PASSENGER_CAPACITY;
    }
}
