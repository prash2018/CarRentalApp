package com.carrental;

import com.carrental.dto.AvailabilityResponse;
import com.carrental.dto.ReservationRequest;
import com.carrental.dto.ReservationResponse;
import com.carrental.enums.CarType;
import com.carrental.enums.ReservationStatus;
import com.carrental.exception.NoCarAvailableException;
import com.carrental.service.ReservationService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

//
//  Integration tests — full Spring context, H2 in-memory database.
//  Uses application-test.properties: 2 Sedans, 2 SUVs, 1 Van.
//
//  Each test class maps to a requirement:
//
//    1: There are 3 types of cars (sedan, SUV and van)
//    2: Reservation captures car type, date/time, and number of days
//    3: The number of cars of each type is limited
//    4: Cannot reserve when the cars fully booked for the window
//    5: Non-overlapping reservations for the same type are allowed
//    6: Cancellation frees up capacity for new reservations
//
@SpringBootTest
@ActiveProfiles("test")           // fleet: 2 Sedans, 2 SUVs, 1 Van
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("Car Rental System — Requirements Integration Tests")
class CarRentalIntegrationTest {

    @Autowired
    private ReservationService reservationService;

    private static final LocalDateTime PICKUP = LocalDateTime.now().plusDays(4);

    private ReservationResponse reserve(CarType type, LocalDateTime pickup, int days) {
        return reservationService.makeReservation(ReservationRequest.builder()
                .customerName("Test Customer")
                .customerEmail("test@example.com")
                .carType(type)
                .pickupDateTime(pickup)
                .numberOfDays(days)
                .build());
    }


    // Test Three car types
    @Test
    @DisplayName("Test reserving a Sedan")
    void testCanReserveSedan() {
        ReservationResponse res = reserve(CarType.SEDAN, PICKUP, 3);
        assertEquals(CarType.SEDAN, res.getCarType());
        assertEquals(ReservationStatus.CONFIRMED, res.getStatus());
        assertNotNull(res.getCarCode());
        assertTrue(res.getCarCode().startsWith("SEDAN"));
    }

    @Test
    @DisplayName("Test reserving an SUV")
    void testCanReserveSUV() {
        ReservationResponse res = reserve(CarType.SUV, PICKUP, 2);
        assertEquals(CarType.SUV, res.getCarType());
        assertEquals(ReservationStatus.CONFIRMED, res.getStatus());
        assertTrue(res.getCarCode().startsWith("SUV"));
    }

    @Test
    @DisplayName("Test reserving a Van")
    void testCanReserveVan() {
        ReservationResponse res = reserve(CarType.VAN, PICKUP, 1);
        assertEquals(CarType.VAN, res.getCarType());
        assertEquals(ReservationStatus.CONFIRMED, res.getStatus());
        assertTrue(res.getCarCode().startsWith("VAN"));
    }


    // Test Reservation stores type, date/time, and number of days
    @Test
    @DisplayName("Reservation stores the requested car type")
    void testStoresCarType() {
        ReservationResponse res = reserve(CarType.SUV, PICKUP, 5);
        assertEquals(CarType.SUV, res.getCarType());
    }

    @Test
    @DisplayName("Reservation stores the exact pickup date and time")
    void testStoresPickupDateTime() {
        LocalDateTime exact = LocalDateTime.of(2027, 8, 15, 14, 30);
        ReservationResponse res = reserve(CarType.SEDAN, exact, 2);
        assertEquals(exact, res.getPickupDateTime());
    }

    @Test
    @DisplayName("Reservation stores the number of days")
    void testStoresNumberOfDays() {
        ReservationResponse res = reserve(CarType.VAN, PICKUP, 7);
        assertEquals(7, res.getNumberOfDays());
    }

    @Test
    @DisplayName("Drop-off date = pickup + numberOfDays")
    void testDropOffEqualsPickupPlusDays() {
        ReservationResponse res = reserve(CarType.SEDAN, PICKUP, 4);
        assertEquals(PICKUP.plusDays(4), res.getDropOffDateTime());
    }

    @Test
    @DisplayName("Total cost = daily rate × number of days")
    void testTotalCostIsRateTimeDays() {
        ReservationResponse res = reserve(CarType.SEDAN, PICKUP, 3);
        // Sedan: $49.99/day × 3 = $149.97
        assertEquals(0, res.getTotalCost().compareTo(
            res.getDailyRate().multiply(java.math.BigDecimal.valueOf(3))));
    }


    // Fleet is limited — availability reflects booked cars
    @Test
    @DisplayName("Availability shows correct total fleet size per type")
    void testFleetSizeReflectedInAvailability() {
        List<AvailabilityResponse> avail =
                reservationService.checkAvailability(PICKUP, 3);

        AvailabilityResponse sedan = avail.stream()
                .filter(a -> a.getCarType() == CarType.SEDAN).findFirst().orElseThrow();
        AvailabilityResponse van = avail.stream()
                .filter(a -> a.getCarType() == CarType.VAN).findFirst().orElseThrow();

        assertEquals(2, sedan.getTotalCars());   // test profile: 2 Sedans
        assertEquals(1, van.getTotalCars());     // test profile: 1 Van
    }

    @Test
    @DisplayName("Booking a car reduces available count by 1")
    void testBookingReducesAvailability() {
        int before = reservationService.countAvailable(CarType.SEDAN, PICKUP, 3);
        reserve(CarType.SEDAN, PICKUP, 3);
        int after = reservationService.countAvailable(CarType.SEDAN, PICKUP, 3);

        assertEquals(before - 1, after);
    }

    @Test
    @DisplayName("Booking all cars of a type reduces available count to 0")
    void testBookingAllCarsGivesZeroAvailability() {
        // Test profile has 2 SUVs
        reserve(CarType.SUV, PICKUP, 2);
        reserve(CarType.SUV, PICKUP, 2);

        assertEquals(0, reservationService.countAvailable(CarType.SUV, PICKUP, 2));
    }

    @Test
    @DisplayName("Different car types have independent fleet limits")
    void testCarTypesHaveIndependentLimits() {
        // Book the only Van
        reserve(CarType.VAN, PICKUP, 3);

        // Van is fully booked, but Sedans and SUVs are still available
        assertEquals(0, reservationService.countAvailable(CarType.VAN,   PICKUP, 3));
        assertEquals(2, reservationService.countAvailable(CarType.SEDAN, PICKUP, 3));
        assertEquals(2, reservationService.countAvailable(CarType.SUV,   PICKUP, 3));
    }


    // Cannot reserve when fleet is fully booked for the window
    @Test
    @DisplayName("Booking beyond fleet capacity throws NoCarAvailableException")
    void testOverbookingThrowsException() {
        // Test profile: 1 Van — book it
        reserve(CarType.VAN, PICKUP, 3);

        // Second booking for the same overlapping window must fail
        assertThrows(NoCarAvailableException.class,
                () -> {
            reserve(CarType.VAN, PICKUP, 3);
                });
    }

    @Test
    @DisplayName("Exception message identifies the car type and window")
    void testExceptionMessageIsInformative() {
        reserve(CarType.VAN, PICKUP, 3);

        NoCarAvailableException ex = assertThrows(NoCarAvailableException.class,
                () -> {
                    reserve(CarType.VAN, PICKUP, 3);
                });

        assertTrue(ex.getMessage().contains("Van"),
            "Message should mention the car type: " + ex.getMessage());
    }

    @Test
    @DisplayName("Fully booked for a window — availability shows 0 and available=false")
    void testAvailabilityShowsFalseWhenFull() {
        reserve(CarType.VAN, PICKUP, 3);

        List<AvailabilityResponse> avail =
                reservationService.checkAvailability(PICKUP, 3);
        AvailabilityResponse van = avail.stream()
                .filter(a -> a.getCarType() == CarType.VAN).findFirst().orElseThrow();

        assertEquals(0, van.getAvailableCars());
        assertFalse(van.isAvailable());
    }

    @Test
    @DisplayName("Can fill fleet to exact capacity (no under-booking)")
    void testCanFillFleetToCapacity() {
        // Test profile: 2 Sedans — both should succeed
        ReservationResponse r1 = reserve(CarType.SEDAN, PICKUP, 2);
        ReservationResponse r2 = reserve(CarType.SEDAN, PICKUP, 2);

        assertEquals(ReservationStatus.CONFIRMED, r1.getStatus());
        assertEquals(ReservationStatus.CONFIRMED, r2.getStatus());
        // Each gets a different car
        assertNotEquals(r1.getCarCode(), r2.getCarCode());
    }


    // Non-overlapping windows for the same type DO succees
    @Test
    @DisplayName("Sequential bookings for the same car type and same car succeed")
    void testSequentialBookingsSameTypeSucceed() {
        // Test profile: 1 Van
        LocalDateTime firstPickup  = PICKUP;               // week 1
        LocalDateTime secondPickup = PICKUP.plusDays(7);   // week 2 — no overlap

        ReservationResponse r1 = reserve(CarType.VAN, firstPickup, 5);
        ReservationResponse r2 = reserve(CarType.VAN, secondPickup, 5);

        assertEquals(ReservationStatus.CONFIRMED, r1.getStatus());
        assertEquals(ReservationStatus.CONFIRMED, r2.getStatus());
        // The same Van can be re-used in a non-overlapping window
        assertEquals(r1.getCarCode(), r2.getCarCode());
    }

    @Test
    @DisplayName("Back-to-back bookings (end == start of next) succeed")
    void testBackToBackBookingsSucceed() {
        // Test profile: 1 Van
        ReservationResponse r1 = reserve(CarType.VAN, PICKUP, 3);         // days 0–3
        ReservationResponse r2 = reserve(CarType.VAN, PICKUP.plusDays(3), 3); // days 3–6

        assertEquals(ReservationStatus.CONFIRMED, r1.getStatus());
        assertEquals(ReservationStatus.CONFIRMED, r2.getStatus());
    }

    @Test
    @DisplayName("Overlapping window with free slot in fleet is allowed")
    void testOverlappingWindowWithFreeSlotSucceeds() {
        // Test profile: 2 Sedans — book one, second should still succeed
        reserve(CarType.SEDAN, PICKUP, 5);
        ReservationResponse second = reserve(CarType.SEDAN, PICKUP, 5);

        assertEquals(ReservationStatus.CONFIRMED, second.getStatus());
    }


    // Cancellation frees car capacity
    @Test
    @DisplayName("Cancelling a reservation frees its slot for a new booking")
    void testCancellationFreesCapacity() {
        // Fill the only Van
        ReservationResponse original = reserve(CarType.VAN, PICKUP, 3);
        assertThrows(NoCarAvailableException.class,
                () -> reserve(CarType.VAN, PICKUP, 3)); // full

        // Cancel the original
        reservationService.cancelReservation(original.getId());

        // Now a new booking should succeed
        ReservationResponse newBooking = reserve(CarType.VAN, PICKUP, 3);
        assertEquals(ReservationStatus.CONFIRMED, newBooking.getStatus());
    }

    @Test
    @DisplayName("Cancelling one of two bookings restores count to 1")
    void testCancellationRestoresCount() {
        // Test profile: 2 Sedans — book both
        ReservationResponse r1 = reserve(CarType.SEDAN, PICKUP, 3);
        reserve(CarType.SEDAN, PICKUP, 3);
        assertEquals(0, reservationService.countAvailable(CarType.SEDAN, PICKUP, 3));

        reservationService.cancelReservation(r1.getId());

        assertEquals(1, reservationService.countAvailable(CarType.SEDAN, PICKUP, 3));
    }

    @Test
    @DisplayName("Completed reservation also frees capacity")
    void testCompletedReservationFreesCapacity() {
        ReservationResponse original = reserve(CarType.VAN, PICKUP, 3);
        reservationService.completeReservation(original.getId());

        // Capacity is restored — new booking succeeds
        ReservationResponse newBooking = reserve(CarType.VAN, PICKUP, 3);
        assertEquals(ReservationStatus.CONFIRMED, newBooking.getStatus());
    }

    @Test
    @DisplayName("Cancelled reservation has status CANCELLED")
    void testCancelledStatusIsCorrect() {
        ReservationResponse res = reserve(CarType.SEDAN, PICKUP, 2);
        ReservationResponse cancelled = reservationService.cancelReservation(res.getId());

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
    }

    @Test
    @DisplayName("Cannot cancel an already-cancelled reservation")
    void testDoubleCancelThrowsException() {
        ReservationResponse res = reserve(CarType.SEDAN, PICKUP, 2);
        reservationService.cancelReservation(res.getId());

        assertThrows(IllegalStateException.class,
                () -> {
                    reservationService.cancelReservation(res.getId());
                });
    }


    // Tests for additional edge cases
    @Test
    @DisplayName("Reservation for 1 day is valid")
    void testEdgeOneDayReservation() {
        ReservationResponse res = reserve(CarType.SEDAN, PICKUP, 1);
        assertEquals(1, res.getNumberOfDays());
        assertEquals(PICKUP.plusDays(1), res.getDropOffDateTime());
    }

    @Test
    @DisplayName("Two customers can each get their own car of the same type")
    void testEdgeTwoCustomersBothGetCars() {
        ReservationResponse r1 = reservationService.makeReservation(
            ReservationRequest.builder()
                .customerName("Virat").customerEmail("virat@test.com")
                .carType(CarType.SEDAN).pickupDateTime(PICKUP).numberOfDays(3)
                .build());

        ReservationResponse r2 = reservationService.makeReservation(
            ReservationRequest.builder()
                .customerName("Sachin").customerEmail("sachin@test.com")
                .carType(CarType.SEDAN).pickupDateTime(PICKUP).numberOfDays(3)
                .build());

        // Both confirmed, different cars
        assertEquals(ReservationStatus.CONFIRMED, r1.getStatus());
        assertEquals(ReservationStatus.CONFIRMED, r2.getStatus());
        assertNotEquals(r1.getCarCode(), r2.getCarCode());
    }

    @Test
    @DisplayName("Can retrieve a reservation by ID")
    void testRetrieveByIdReturnsCorrectReservation() {
        ReservationResponse created = reserve(CarType.SUV, PICKUP, 2);
        ReservationResponse fetched = reservationService.getReservation(created.getId());

        assertEquals(created.getId(), fetched.getId());
        assertEquals(CarType.SUV, fetched.getCarType());
        assertEquals(2, fetched.getNumberOfDays());
    }

    @Test
    @DisplayName("CheckAvailability covers all three car types")
    void testAvailabilityCoversAllThreeTypes() {
        List<AvailabilityResponse> avail =
                reservationService.checkAvailability(PICKUP, 2);

        assertTrue(avail.stream().anyMatch(a -> a.getCarType() == CarType.SEDAN));
        assertTrue(avail.stream().anyMatch(a -> a.getCarType() == CarType.SUV));
        assertTrue(avail.stream().anyMatch(a -> a.getCarType() == CarType.VAN));
    }
}
