package com.smartbus.repository;

import com.smartbus.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {
    Optional<Trip> findFirstByBusIdAndStatus(Long busId, Trip.Status status);
    List<Trip> findTop15ByOrderByDepartureTimeDesc();
    long countByStatus(Trip.Status status);
}
