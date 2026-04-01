package com.carrental.service;

import com.carrental.dto.CarResponse;
import com.carrental.enums.CarType;
import com.carrental.exception.ReservationNotFoundException;
import com.carrental.model.Car;
import com.carrental.repository.CarRepository;
import com.carrental.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CarServiceImpl implements CarService {

    private final CarRepository carRepository;
    private final ReservationRepository reservationRepository;

    @Override
    public List<CarResponse> getAllCars(CarType carType) {
        List<Car> cars = (carType != null)
                ? carRepository.findByCarType(carType)
                : carRepository.findAll();
        return cars.stream().map(this::toResponse).toList();
    }

    @Override
    public List<CarResponse> getAvailableCars(LocalDateTime pickup,
                                              int numberOfDays,
                                              CarType carType) {
        LocalDateTime dropOff = pickup.plusDays(numberOfDays);

        // Fetch IDs of every car already booked in this window
        List<Long> occupiedIds = reservationRepository
                .findAllOccupiedCarIds(pickup, dropOff);

        // Start from all cars (or filtered by type), then exclude occupied ones
        List<Car> candidates = (carType != null)
                ? carRepository.findByCarType(carType)
                : carRepository.findAll();

        return candidates.stream()
                .filter(c -> !occupiedIds.contains(c.getId()))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CarResponse getCarById(Long id) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));
        return toResponse(car);
    }

    private CarResponse toResponse(Car car) {
        return CarResponse.builder()
                .id(car.getId())
                .carCode(car.getCarCode())
                .carType(car.getCarType())
                .make(car.getMake())
                .model(car.getModel())
                .year(car.getYear())
                .dailyRate(car.getDailyRentalRate())
                .passengerCapacity(car.getCarPassengerCapacity())
                .description(car.getDescription())
                .build();
    }
}