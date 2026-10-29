package com.smartbus.repository;

import com.smartbus.model.DemandRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DemandRecordRepository extends JpaRepository<DemandRecord, Long> {
    List<DemandRecord> findByRouteIdAndDayType(Long routeId, DemandRecord.DayType dayType);
}
