package com.carrental.model;

import com.carrental.enums.CarType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

/*
  Van: up to 7 passengers and has highest daily rate.
 */
@Entity
@DiscriminatorValue("VAN CATEGORY")
@NoArgsConstructor
@ToString(callSuper = true)
public class Van extends Car {

    private static final BigDecimal DAILY_RATE = new BigDecimal("99.99");
    private static final int PASSENGER_CAPACITY = 7;

    public Van(String carCode, String make, String model, int year) {
        setCarCode(carCode);
        setCarType(CarType.VAN);
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
