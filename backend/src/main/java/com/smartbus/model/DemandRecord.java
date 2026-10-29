package com.smartbus.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/** Historical boarding count for one route in one hour of one day. Feeds the forecaster. */
@Entity
@Table(name = "demand_records")
public class DemandRecord {

    public enum DayType { WEEKDAY, WEEKEND }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id")
    private Route route;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_type", nullable = false, length = 16)
    private DayType dayType;

    @Column(name = "hour_of_day", nullable = false)
    private int hourOfDay;

    @Column(nullable = false)
    private int passengers;

    @Column(name = "recorded_on", nullable = false)
    private LocalDate recordedOn;

    protected DemandRecord() {}

    public Long getId() { return id; }
    public DayType getDayType() { return dayType; }
    public int getHourOfDay() { return hourOfDay; }
    public int getPassengers() { return passengers; }
    public LocalDate getRecordedOn() { return recordedOn; }
}
