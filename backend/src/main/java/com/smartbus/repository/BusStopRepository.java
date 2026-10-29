package com.smartbus.repository;

import com.smartbus.model.BusStop;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BusStopRepository extends JpaRepository<BusStop, Long> {
    List<BusStop> findAllByOrderByNameAsc();
}
