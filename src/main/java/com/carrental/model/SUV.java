package com.carrental.model;

import com.carrental.enums.CarType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

/*
   SUV: up to 7 passengers, mid-range daily rate.
 */
@Entity
@DiscriminatorValue("SUV CATEGORY")
@NoArgsConstructor
@ToString(callSuper = true)
public class SUV extends Car {

    private static final BigDecimal DAILY_RATE = new BigDecimal("79.99");
    private static final int PASSENGER_CAPACITY = 5;

    public SUV(String carCode, String make, String model, int year) {
        setCarCode(carCode);
        setCarType(CarType.SUV);
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
