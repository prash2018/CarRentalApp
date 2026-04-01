package com.carrental.repository;

import com.carrental.enums.CarType;
import com.carrental.model.Car;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {

    // Returns List of cars of specified type
    List<Car> findByCarType(CarType carType);


    // Returns count of cars of specified type
    long countByCarType(CarType carType);

    /**
     * Loads all cars of the given type and acquires a PESSIMISTIC_WRITE lock
     * on each row for the duration of the current transaction.
     *
     * Any concurrent transaction that calls this method for the same car type
     * will block here until the first transaction commits or rolls back.
     * This eliminates the check-then-act race condition in makeReservation().
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT car FROM Car car WHERE car.carType = :type")
    List<Car> findByCarTypeForUpdate(@Param("type") CarType carType);
}
