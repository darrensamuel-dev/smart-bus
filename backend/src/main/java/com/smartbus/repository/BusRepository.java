package com.smartbus.repository;

import com.smartbus.model.Bus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BusRepository extends JpaRepository<Bus, Long> {
    List<Bus> findByStatus(Bus.Status status);
    List<Bus> findByRouteId(Long routeId);
    long countByStatus(Bus.Status status);
}
