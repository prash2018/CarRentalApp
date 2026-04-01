package com.carrental;

import com.carrental.model.Car;
import com.carrental.model.Sedan;
import com.carrental.model.SUV;
import com.carrental.model.Van;
import com.carrental.enums.CarType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/*
  Requirement: There are 3 types of cars (sedan, SUV and van).
  Verifies:
    - Each type is a distinct class with the correct CarType
    - Polymorphic getDailyRate() returns type-specific rates
    - Polymorphic getPassengerCapacity() returns type-specific capacities
    - calculateTotalCost() multiplies correctly for any number of days
    - The Car abstraction works — all subtypes respond to the same method calls
 */
@DisplayName("Three car types exist (Sedan, SUV, Van)")
class CarModelTest {

    @Test
    @DisplayName("Car has correct CarType")
    void testCarHasCorrectCarType() {
        Car sedan = new Sedan("S-1", "Toyota", "Camry", 2025);
        assertEquals(CarType.SEDAN, sedan.getCarType());

        Car suv = new SUV("V-1", "Ford", "Explorer", 2024);
        assertEquals(CarType.SUV, suv.getCarType());

        Car van = new Van("N-1", "Chrysler", "Pacifica", 2026);
        assertEquals(CarType.VAN, van.getCarType());
    }

    @Test
    @DisplayName("Car daily rate is correct based on CarType")
    void testCarDailyRate() {
        Car sedan = new Sedan("S-1", "Toyota", "Camry", 2023);
        assertEquals(new BigDecimal("49.99"), sedan.getDailyRentalRate());

        Car suv = new SUV("V-1", "Ford", "Explorer", 2026);
        assertEquals(new BigDecimal("79.99"), suv.getDailyRentalRate());

        Car van = new Van("N-1", "Chrysler", "Pacifica", 2026);
        assertEquals(new BigDecimal("99.99"), van.getDailyRentalRate());
    }


    @Test
    @DisplayName("Car seating capacity is correct based on CarType")
    void testCarPassengerCapacity() {
        Car sedan = new Sedan("S-1", "Toyota", "Camry", 2025);
        assertEquals(5, sedan.getCarPassengerCapacity());

        Car suv = new SUV("V-1", "Ford", "Explorer", 2026);
        assertEquals(5, suv.getCarPassengerCapacity());

        Car van = new Van("N-1", "Honda", "Odyssey", 2026);
        assertEquals(7, van.getCarPassengerCapacity());
    }


    @Test
    @DisplayName("Sedan total cost = $49.99 × days")
    void checkSedanTotalCost() {
        Car sedan = new Sedan("S-1", "Toyota", "Camry", 2023);
        assertEquals(new BigDecimal("149.97"), sedan.calculateTotalRentalCost(3));
        assertEquals(new BigDecimal("49.99"),  sedan.calculateTotalRentalCost(1));
        assertEquals(new BigDecimal("349.93"), sedan.calculateTotalRentalCost(7));
    }

    @Test
    @DisplayName("SUV total cost = $79.99 × days")
    void checkSuvTotalCost() {
        Car suv = new SUV("V-1", "Ford", "Explorer", 2023);
        assertEquals(new BigDecimal("239.97"), suv.calculateTotalRentalCost(3));
    }

    @Test
    @DisplayName("Van total cost = 99.99 × days")
    void checkVanTotalCost() {
        Car van = new Van("N-1", "Chrysler", "Pacifica", 2023);
        assertEquals(new BigDecimal("299.97"), van.calculateTotalRentalCost(3));
    }

    @Test
    @DisplayName("calculateTotalCost with zero or negative days throws")
    void totalCostInvalidDaysThrowsException() {
        Car sedan = new Sedan("S-1", "Toyota", "Camry", 2023);
        assertThrows(IllegalArgumentException.class, () -> {
            sedan.calculateTotalRentalCost(0);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            sedan.calculateTotalRentalCost(-1);
        });
    }

    @Test
    @DisplayName("All three types respond to the same Car interface — polymorphism")
    void testAllCarTypesRespondToCarMethods() {
        Car[] cars = {
            new Sedan("S-1", "Toyota", "Camry", 2024),
            new SUV("V-1", "Ford", "Explorer", 2025),
            new Van("N-1", "Chrysler", "Pacifica", 2026)
        };

        for (Car car : cars) {
            assertNotNull(car.getDailyRentalRate(),  car.getCarType() + " must have a daily rate");
            assertTrue(car.getDailyRentalRate().compareTo(BigDecimal.ZERO) > 0,
                                                       car.getCarType() + " daily rate must be positive");
            assertTrue(car.getCarPassengerCapacity() > 0, car.getCarType() + " must have positive capacity");
            assertTrue(car.calculateTotalRentalCost(5).compareTo(BigDecimal.ZERO) > 0,
                                                       car.getCarType() + " total cost must be positive");
        }
    }
}
