package com.carrental.service;

import com.carrental.dto.AvailabilityResponse;
import com.carrental.dto.ReservationRequest;
import com.carrental.dto.ReservationResponse;
import com.carrental.enums.CarType;
import com.carrental.enums.ReservationStatus;
import com.carrental.exception.CarNotFoundException;
import com.carrental.exception.NoCarAvailableException;
import com.carrental.exception.ReservationNotFoundException;
import com.carrental.model.Car;
import com.carrental.model.Reservation;
import com.carrental.repository.CarRepository;
import com.carrental.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 *  Reservation logic.
 *
 * The number of CONFIRMED reservations overlapping any
 * given window must never exceed the total cars of that type in the fleet.
 *
 * OOP principles:
 *   SINGLE RESPONSIBILITY — only manages reservation lifecycle
 *   DEPENDENCY INJECTION  — repositories injected, not instantiated
 *   POLYMORPHISM          — car.getDailyRate() / car.calculateTotalCost() called
 *                           without knowing the concrete subtype
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final CarRepository carRepository;

    // Make a New Reservation
    @Override
    public ReservationResponse makeReservation(ReservationRequest req) {
        LocalDateTime pickup  = req.getPickupDateTime();
        LocalDateTime dropOff = pickup.plusDays(req.getNumberOfDays());
        CarType type          = req.getCarType();

        // Lock all car rows of this type — prevents concurrent double-booking.
        List<Car> fleet = carRepository.findByCarTypeForUpdate(type);
        long fleetSize  = fleet.size();

        if (fleetSize == 0) {
            throw new NoCarAvailableException(
                    "No " + type.displayName() + "s exist in the fleet.");
        }

        Car assignedCar;
        if (req.getCarId() != null) {
            // Specific car type requested
            // 1. Car must exist
            assignedCar = carRepository.findById(req.getCarId())
                    .orElseThrow(() -> new CarNotFoundException(req.getCarId()));

            // 2. Car must match the requested type
            if (assignedCar.getCarType() != type) {
                throw new IllegalArgumentException(
                        "Car " + assignedCar.getCarCode() + " is a "
                                + assignedCar.getCarType().displayName()
                                + ", not a " + type.displayName() + ".");
            }

            // 3. Car must be free in the requested date window
            List<Long> occupiedIds = reservationRepository.findOccupiedCarIds(type, pickup, dropOff);
            if (occupiedIds.contains(req.getCarId())) {
                throw new NoCarAvailableException(
                        "Car " + assignedCar.getCarCode()
                                + " is already booked from " + pickup + " to " + dropOff + ".");
            }
        } else {
            // Auto-assign
            // Enforce fleet limit first by getting the count of confirmed cars
            long alreadyBooked = reservationRepository.countOverlappingConfirmed(type, pickup, dropOff);
            if (alreadyBooked >= fleetSize) {
                throw new NoCarAvailableException(
                        "All " + fleetSize + " " + type.displayName()
                                + "(s) are fully booked from " + pickup + " to " + dropOff + ".");
            }
            assignedCar = pickAvailableCar(type, pickup, dropOff);
        }

        // Polymorphic cost calculation — works for Sedan, SUV, Van identically
        var totalCost = assignedCar.calculateTotalRentalCost(req.getNumberOfDays());

        // 6. Save the reservation
        Reservation reservation = Reservation.builder()
                .customerName(req.getCustomerName())
                .customerEmail(req.getCustomerEmail())
                .carType(type)
                .car(assignedCar)
                .pickupDateTime(pickup)
                .numberOfDays(req.getNumberOfDays())
                .status(ReservationStatus.CONFIRMED)
                .totalCost(totalCost)
                .build();

        Reservation saved = reservationRepository.save(reservation);
        log.info("Reservation #{} created: {} {} for {} day(s) — {}",
                saved.getId(), type.displayName(), assignedCar.getCarCode(),
                req.getNumberOfDays(), req.getCustomerName());

        return toResponse(saved);
    }


    // Cancel Existing Reservation
    @Override
    public ReservationResponse cancelReservation(Long id) {
        Reservation reservation = findById(id);
        // Mark Reservation staus as CANCELLED
        reservation.cancel();
        log.info("Reservation #{} cancelled.", id);
        return toResponse(reservationRepository.save(reservation));
    }

    // Complete Reservation/Return car
    @Override
    public ReservationResponse completeReservation(Long id) {
        Reservation reservation = findById(id);
        // Mark Reservation staus as COMPLETED
        reservation.complete();
        log.info("Reservation #{} completed.", id);
        return toResponse(reservationRepository.save(reservation));
    }

    //Queries
    // Get single Reservatoin by its ID
    @Override
    @Transactional(readOnly = true)
    public ReservationResponse getReservation(Long id) {
        return toResponse(findById(id));
    }

    // Get list of Reservations by optional customer email.
    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservations(String customerEmail) {
        List<Reservation> results = (customerEmail != null && !customerEmail.isBlank())
                ? reservationRepository.findByCustomerEmail(customerEmail)
                : reservationRepository.findAll();
        return results.stream()
                .map(this::toResponse)
                .toList();
    }

    // Check availability of cars
    @Override
    @Transactional(readOnly = true)
    public List<AvailabilityResponse> checkAvailability(
            LocalDateTime pickupDateTime, int numberOfDays) {

        return Arrays.stream(CarType.values())
                .map(type -> buildAvailability(type, pickupDateTime, numberOfDays))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public int countAvailable(CarType type, LocalDateTime pickup, int numberOfDays) {
        LocalDateTime dropOff = pickup.plusDays(numberOfDays);
        long fleet    = carRepository.countByCarType(type);
        long booked   = reservationRepository.countOverlappingConfirmed(type, pickup, dropOff);
        return (int) Math.max(0, fleet - booked);
    }


    // Private methods
    // Selects the first car of the requested type whose ID is NOT in the
    // set of already-occupied car IDs for the given time window.
    private Car pickAvailableCar(CarType type, LocalDateTime pickup, LocalDateTime dropOff) {
        List<Long> occupiedIds = reservationRepository
                .findOccupiedCarIds(type, pickup, dropOff);

        return carRepository.findByCarType(type).stream()
                .filter(c -> !occupiedIds.contains(c.getId()))
                .findFirst()
                .orElseThrow(() -> new NoCarAvailableException(
                    "No individual " + type.displayName() + " is free for the requested window."));
    }

    private AvailabilityResponse buildAvailability(
            CarType type, LocalDateTime pickup, int days) {

        LocalDateTime dropOff = pickup.plusDays(days);
        long fleet   = carRepository.countByCarType(type);
        long booked  = reservationRepository.countOverlappingConfirmed(type, pickup, dropOff);
        int available = (int) Math.max(0, fleet - booked);

        // Use a representative car of this type for rate/capacity info
        Car sample = carRepository.findByCarType(type).stream().findFirst().orElse(null);

        return AvailabilityResponse.builder()
                .carType(type)
                .totalCars((int) fleet)
                .reservedCars((int) booked)
                .availableCars(available)
                .dailyRate(sample != null ? sample.getDailyRentalRate() : null)
                .passengerCapacity(sample != null ? sample.getCarPassengerCapacity() : 0)
                .available(available > 0)
                .build();
    }

    private Reservation findById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));
    }

    private ReservationResponse toResponse(Reservation r) {
        Car car = r.getCar();
        return ReservationResponse.builder()
                .id(r.getId())
                .customerName(r.getCustomerName())
                .customerEmail(r.getCustomerEmail())
                .carType(r.getCarType())
                .carCode(car.getCarCode())
                .carDescription(car.getDescription())
                .pickupDateTime(r.getPickupDateTime())
                .dropOffDateTime(r.getDropOffDateTime())
                .numberOfDays(r.getNumberOfDays())
                .dailyRate(car.getDailyRentalRate())
                .totalCost(r.getTotalCost())
                .status(r.getStatus())
                .build();
    }
}
