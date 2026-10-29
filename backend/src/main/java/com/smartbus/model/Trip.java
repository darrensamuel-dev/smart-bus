package com.smartbus.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** One end-to-end run of a bus along its route. Opened and closed by the tracking service. */
@Entity
@Table(name = "trips")
public class Trip {

    public enum Status { SCHEDULED, IN_PROGRESS, COMPLETED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "bus_id")
    private Bus bus;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "route_id")
    private Route route;

    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;

    @Column(name = "arrival_time")
    private LocalDateTime arrivalTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.IN_PROGRESS;

    @Column(name = "passengers_boarded", nullable = false)
    private int passengersBoarded = 0;

    protected Trip() {}

    public Trip(Bus bus, Route route, LocalDateTime departureTime) {
        this.bus = bus;
        this.route = route;
        this.departureTime = departureTime;
    }

    public void addBoarded(int n) { passengersBoarded += Math.max(0, n); }

    public void complete(LocalDateTime when) {
        this.arrivalTime = when;
        this.status = Status.COMPLETED;
    }

    public Long getId() { return id; }
    public Bus getBus() { return bus; }
    public Route getRoute() { return route; }
    public LocalDateTime getDepartureTime() { return departureTime; }
    public LocalDateTime getArrivalTime() { return arrivalTime; }
    public Status getStatus() { return status; }
    public int getPassengersBoarded() { return passengersBoarded; }
}
