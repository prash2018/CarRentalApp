package com.carrental.config;

import com.carrental.model.Sedan;
import com.carrental.model.SUV;
import com.carrental.model.Van;
import com.carrental.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Initializes the fixed number of cars on startup.
 *
 * Vehicle sizes are defined in application.properties:
 *   vehicle.sedan.count=8
 *   vehicle.suv.count=5
 *   vehicle.van.count=3
 *
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VehicleInitializer implements CommandLineRunner {

    private final CarRepository carRepository;

    @Value("${vehicle.sedan.count:8}")
    private int sedanCount;

    @Value("${vehicle.suv.count:5}")
    private int suvCount;

    @Value("${vehicle.van.count:3}")
    private int vanCount;

    record CarInfo(String make, Integer year, String model) {}

    //  Define 10 sedan types
    static final List<CarInfo> sedanCarTypes = List.of(
            new CarInfo("Toyota", 2025, "Camry"),
            new CarInfo("Honda", 2024, "Accord"),
            new CarInfo("Subaru", 2024, "Legacy"),
            new CarInfo("Tesla", 2025, "Model 3"),
            new CarInfo("Ford", 2024, "Fusion"),
            new CarInfo("Audi", 2025, "A4"),
            new CarInfo("Chevrolet", 2023, "Malibu"),
            new CarInfo("Nissan", 2025, "Altima"),
            new CarInfo("Hyundai", 2024, "Elantra"),
            new CarInfo("Kia", 2025, "K4")
    );

    //  Define 10 SUV types
    static final List<CarInfo> suvCarTypes = List.of(
            new CarInfo("Toyota", 2025, "RAV4"),
            new CarInfo("Honda", 2024, "CRV"),
            new CarInfo("Nissan", 2024, "ROGUE"),
            new CarInfo("Hyundai", 2025, "Santa Fe"),
            new CarInfo("Tesla", 2024, "Model Y"),
            new CarInfo("Audi", 2025, "Q4"),
            new CarInfo("Chevrolet", 2023, "Equinox"),
            new CarInfo("Subaru", 2024, "Outback"),
            new CarInfo("Ford", 2023, "Escape"),
            new CarInfo("Kia", 2025, "Sorento")
    );

    //  Define 5 VAN types
    static final List<CarInfo> vanCarTypes = List.of(
            new CarInfo("Toyota", 2025, "Sienna"),
            new CarInfo("Honda", 2024, "Odyssey"),
            new CarInfo("Nissan", 2024, "Quest"),
            new CarInfo("Chrysler", 2025, "Pacifica"),
            new CarInfo("Kia", 2026, "Carnival")
    );

    @Override
    public void run(String @NonNull ... args) {
        if (carRepository.count() > 0)
            return; 

        // Sedans
        for (int i = 0; i < sedanCount; i++) {
            carRepository.save(new Sedan("SEDAN-" + (i + 1),
                    sedanCarTypes.get(i).make, sedanCarTypes.get(i).model, sedanCarTypes.get(i).year));
        }

        // SUVs
        for (int i = 0; i < suvCount; i++) {
            carRepository.save(new SUV("SUV-" + (i + 1),
                    suvCarTypes.get(i).make, suvCarTypes.get(i).model, suvCarTypes.get(i).year));
        }

        // Vans
        for (int i = 0; i < vanCount; i++) {
            carRepository.save(new Van("VAN-" + (i + 1),
                    vanCarTypes.get(i).make, vanCarTypes.get(i).model, vanCarTypes.get(i).year));
        }

        log.info("Vehicles initialized: {} Sedan(s), {} SUV(s), {} Van(s)",
                sedanCount, suvCount, vanCount);
    }
}
