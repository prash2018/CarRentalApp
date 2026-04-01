package com.carrental;

import com.carrental.dto.CarResponse;
import com.carrental.enums.CarType;
import com.carrental.exception.ReservationNotFoundException;
import com.carrental.model.Sedan;
import com.carrental.model.SUV;
import com.carrental.model.Van;
import com.carrental.repository.CarRepository;
import com.carrental.repository.ReservationRepository;
import com.carrental.service.CarServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/*
  Unit tests for CarServiceImpl.
  Uses Mockito — no Spring context, no database.

  Covers:
    getAllCars()       — no filter, type filter
    getAvailableCars() — none booked, some booked, all booked, type filter
    getCarById()       — found, not found
    DTO mapping        — all fields mapped correctly
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CarService unit tests")
class CarServiceTest {

    @Mock
    private CarRepository carRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private CarServiceImpl carService;

    private static final LocalDateTime PICKUP = LocalDateTime.of(2027, 7, 20, 9, 0);
    private static final LocalDateTime DROP_OFF = PICKUP.plusDays(3);

    // Test fixtures
    private Sedan sedan1, sedan2;
    private SUV   suv1;
    private Van   van1;

    @BeforeEach
    void setUp() {
        sedan1 = new Sedan("SEDAN-1", "Toyota", "Camry",  2023); sedan1.setId(1L);
        sedan2 = new Sedan("SEDAN-2", "Toyota", "Camry",  2023); sedan2.setId(2L);
        suv1   = new SUV  ("SUV-1",   "Ford",   "Explorer", 2023); suv1.setId(3L);
        van1   = new Van  ("VAN-1",   "Chrysler","Pacifica", 2023); van1.setId(4L);
    }

    // TEst getAllCars()
    @Test
    @DisplayName("getAllCars(null) — returns all cars from repository")
    void testGetAllCarsNoFilterReturnsAllCars() {
        when(carRepository.findAll()).thenReturn(List.of(sedan1, sedan2, suv1, van1));

        List<CarResponse> result = carService.getAllCars(null);

        assertEquals(4, result.size());
        verify(carRepository).findAll();
        verify(carRepository, never()).findByCarType(any());
    }

    @Test
    @DisplayName("getAllCars(SEDAN) — calls findByCarType and returns only sedans")
    void testGetAllCarsSedanFilterReturnsOnlySedans() {
        when(carRepository.findByCarType(CarType.SEDAN)).thenReturn(List.of(sedan1, sedan2));

        List<CarResponse> result = carService.getAllCars(CarType.SEDAN);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(r -> r.getCarType() == CarType.SEDAN));
        verify(carRepository).findByCarType(CarType.SEDAN);
        verify(carRepository, never()).findAll();
    }

    @Test
    @DisplayName("getAllCars(SUV) — returns only SUVs")
    void testGetAllCarsSuvFilterReturnsOnlySUVs() {
        when(carRepository.findByCarType(CarType.SUV)).thenReturn(List.of(suv1));

        List<CarResponse> result = carService.getAllCars(CarType.SUV);

        assertEquals(1, result.size());
        assertEquals(CarType.SUV, result.get(0).getCarType());
    }

    @Test
    @DisplayName("getAllCars(VAN) — returns only Vans")
    void testGetAllCarsVanFilterReturnsOnlyVans() {
        when(carRepository.findByCarType(CarType.VAN)).thenReturn(List.of(van1));

        List<CarResponse> result = carService.getAllCars(CarType.VAN);

        assertEquals(1, result.size());
        assertEquals(CarType.VAN, result.get(0).getCarType());
    }

    @Test
    @DisplayName("getAllCars — returns empty list when fleet is empty")
    void testGetAllCarsEmptyFleetReturnsEmptyList() {
        when(carRepository.findAll()).thenReturn(List.of());

        List<CarResponse> result = carService.getAllCars(null);

        assertTrue(result.isEmpty());
    }

    // Test getAvailableCars()
    @Test
    @DisplayName("getAvailableCars — returns all cars when none are booked")
    void testGetAvailableCarsNoneBookedReturnsAllCars() {
        when(reservationRepository.findAllOccupiedCarIds(PICKUP, DROP_OFF))
                .thenReturn(List.of()); // no cars occupied
        when(carRepository.findAll()).thenReturn(List.of(sedan1, sedan2, suv1, van1));

        List<CarResponse> result = carService.getAvailableCars(PICKUP, 3, null);

        assertEquals(4, result.size());
    }

    @Test
    @DisplayName("getAvailableCars — excludes cars that are booked in the window")
    void testGetAvailableCarsSomeBookedExcludesOccupied() {
        // sedan1 and suv1 are booked
        when(reservationRepository.findAllOccupiedCarIds(PICKUP, DROP_OFF))
                .thenReturn(List.of(1L, 3L));
        when(carRepository.findAll()).thenReturn(List.of(sedan1, sedan2, suv1, van1));

        List<CarResponse> result = carService.getAvailableCars(PICKUP, 3, null);

        assertEquals(2, result.size());
        assertTrue(result.stream().noneMatch(r -> r.getId().equals(1L)));
        assertTrue(result.stream().noneMatch(r -> r.getId().equals(3L)));
        assertTrue(result.stream().anyMatch(r -> r.getId().equals(2L)));  // sedan2 free
        assertTrue(result.stream().anyMatch(r -> r.getId().equals(4L)));  // van1 free
    }

    @Test
    @DisplayName("getAvailableCars — returns empty list when all cars are booked")
    void testGetAvailableCarsAllBookedReturnsEmptyList() {
        when(reservationRepository.findAllOccupiedCarIds(PICKUP, DROP_OFF))
                .thenReturn(List.of(1L, 2L, 3L, 4L));
        when(carRepository.findAll()).thenReturn(List.of(sedan1, sedan2, suv1, van1));

        List<CarResponse> result = carService.getAvailableCars(PICKUP, 3, null);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getAvailableCars — with type filter uses findByCarType, not findAll")
    void testGetAvailableCarsWithTypeFilterUsesFindByCarType() {
        when(reservationRepository.findAllOccupiedCarIds(PICKUP, DROP_OFF))
                .thenReturn(List.of());
        when(carRepository.findByCarType(CarType.SEDAN)).thenReturn(List.of(sedan1, sedan2));

        List<CarResponse> result = carService.getAvailableCars(PICKUP, 3, CarType.SEDAN);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(r -> r.getCarType() == CarType.SEDAN));
        verify(carRepository).findByCarType(CarType.SEDAN);
        verify(carRepository, never()).findAll();
    }

    @Test
    @DisplayName("getAvailableCars — type filter + partial booking returns correct subset")
    void testGetAvailableCarsTypeFilterWithPartialBookingReturnsCorrectSubset() {
        // sedan1 is booked, sedan2 is free
        when(reservationRepository.findAllOccupiedCarIds(PICKUP, DROP_OFF))
                .thenReturn(List.of(1L));
        when(carRepository.findByCarType(CarType.SEDAN)).thenReturn(List.of(sedan1, sedan2));

        List<CarResponse> result = carService.getAvailableCars(PICKUP, 3, CarType.SEDAN);

        assertEquals(1, result.size());
        assertEquals("SEDAN-2", result.get(0).getCarCode());
    }

    @Test
    @DisplayName("getAvailableCars — dropOff computed as pickup + numberOfDays")
    void testGetAvailableCarsDropOffComputedCorrectly() {
        LocalDateTime pickup  = LocalDateTime.of(2027, 8, 1, 10, 0);
        LocalDateTime dropOff = pickup.plusDays(5);
        when(reservationRepository.findAllOccupiedCarIds(pickup, dropOff))
                .thenReturn(List.of());
        when(carRepository.findAll()).thenReturn(List.of(sedan1));

        carService.getAvailableCars(pickup, 5, null);

        verify(reservationRepository).findAllOccupiedCarIds(pickup, dropOff);
    }

    // Test getCarById()
    @Test
    @DisplayName("getCarById — returns correct car when found")
    void testGetCarByIdFoundReturnsResponse() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(sedan1));

        CarResponse result = carService.getCarById(1L);

        assertEquals(1L, result.getId());
        assertEquals("SEDAN-1", result.getCarCode());
        assertEquals(CarType.SEDAN, result.getCarType());
    }

    @Test
    @DisplayName("getCarById — throws ReservationNotFoundException when not found")
    void testGetCarByIdNotFoundThrowsException() {
        when(carRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ReservationNotFoundException.class,
                () -> carService.getCarById(99L));
    }

    @Test
    @DisplayName("getCarById — repository called exactly once")
    void testGetCarByIdRepositoryCalledOnce() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(sedan1));

        carService.getCarById(1L);

        verify(carRepository, times(1)).findById(1L);
    }

    // DTO mapping — all fields
    @Test
    @DisplayName("DTO mapping — Sedan fields all mapped correctly")
    void testDtoMappingSedanAllFieldsMapped() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(sedan1));

        CarResponse r = carService.getCarById(1L);

        assertAll("Sedan DTO fields",
                () -> assertEquals(1L,             r.getId()),
                () -> assertEquals("SEDAN-1",                 r.getCarCode()),
                () -> assertEquals(CarType.SEDAN,             r.getCarType()),
                () -> assertEquals("Toyota",                  r.getMake()),
                () -> assertEquals("Camry",                   r.getModel()),
                () -> assertEquals(2023,                      r.getYear()),
                () -> assertEquals(new BigDecimal("49.99"),   r.getDailyRate()),
                () -> assertEquals(5,                         r.getPassengerCapacity()),
                () -> assertNotNull(r.getDescription())
        );
    }

    @Test
    @DisplayName("DTO mapping — SUV daily rate and capacity are correct")
    void testDtoMappingSuvRateAndCapacityCorrect() {
        when(carRepository.findById(3L)).thenReturn(Optional.of(suv1));

        CarResponse r = carService.getCarById(3L);

        assertEquals(new BigDecimal("79.99"), r.getDailyRate());
        assertEquals(5,r.getPassengerCapacity());
        assertEquals(CarType.SUV, r.getCarType());
    }

    @Test
    @DisplayName("DTO mapping — Van daily rate and capacity are correct")
    void testDtoMappingVanRateAndCapacityCorrect() {
        when(carRepository.findById(4L)).thenReturn(Optional.of(van1));

        CarResponse r = carService.getCarById(4L);

        assertEquals(new BigDecimal("99.99"), r.getDailyRate());
        assertEquals(7, r.getPassengerCapacity());
        assertEquals(CarType.VAN, r.getCarType());
    }

    @Test
    @DisplayName("DTO mapping — description is non-null and contains car type")
    void testDtoMappingDescriptionContainsCarType() {
        when(carRepository.findAll()).thenReturn(List.of(sedan1, suv1, van1));

        List<CarResponse> result = carService.getAllCars(null);

        result.forEach(r -> {
            assertNotNull(r.getDescription(), r.getCarCode() + " description must not be null");
            assertTrue(r.getDescription().contains(r.getCarType().displayName()),
                    r.getCarCode() + " description must contain car type");
        });
    }
}
