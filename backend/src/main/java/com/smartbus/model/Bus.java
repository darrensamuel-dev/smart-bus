package com.smartbus.model;

import jakarta.persistence.*;

@Entity
@Table(name = "buses")
public class Bus {

    public enum Status { RUNNING, IDLE, MAINTENANCE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registration_no", nullable = false, unique = true, length = 20)
    private String registrationNo;

    @Column(nullable = false)
    private int capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "bus_type", nullable = false, length = 16)
    private BusType busType = BusType.ORDINARY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.IDLE;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "route_id")
    private Route route;

    /** Position along the route in stop units (see Route). */
    @Column(nullable = false)
    private double progress = 0;

    /** +1 = towards the last stop, -1 = towards the first stop. */
    @Column(nullable = false)
    private int direction = 1;

    @Column(name = "passengers_onboard", nullable = false)
    private int passengersOnboard = 0;

    @Column(name = "current_lat")
    private Double currentLat;

    @Column(name = "current_lng")
    private Double currentLng;

    protected Bus() {}

    public Bus(String registrationNo, int capacity, BusType busType, Route route) {
        this.registrationNo = registrationNo;
        this.capacity = capacity;
        this.busType = busType;
        this.route = route;
    }

    // ---------- behaviour ----------

    public boolean isRunning() { return status == Status.RUNNING; }

    public int occupancyPercent() {
        return capacity == 0 ? 0 : (int) Math.round(100.0 * passengersOnboard / capacity);
    }

    public String crowdLevel() {
        int pct = occupancyPercent();
        if (pct < 50) return "SEATS_AVAILABLE";
        if (pct < 85) return "STANDING_ROOM";
        return "CROWDED";
    }

    /** Boards up to {@code wanted} riders, never beyond capacity. Returns how many boarded. */
    public int board(int wanted) {
        int boarded = Math.max(0, Math.min(wanted, capacity - passengersOnboard));
        passengersOnboard += boarded;
        return boarded;
    }

    public void alight(int count) {
        passengersOnboard = Math.max(0, passengersOnboard - Math.max(0, count));
    }

    public void reverseDirection() { direction = -direction; }

    public void moveTo(double progress, double lat, double lng) {
        this.progress = progress;
        this.currentLat = lat;
        this.currentLng = lng;
    }

    // ---------- accessors ----------
    public Long getId() { return id; }
    public String getRegistrationNo() { return registrationNo; }
    public int getCapacity() { return capacity; }
    public BusType getBusType() { return busType; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Route getRoute() { return route; }
    public void setRoute(Route route) { this.route = route; }
    public double getProgress() { return progress; }
    public int getDirection() { return direction; }
    public int getPassengersOnboard() { return passengersOnboard; }
    public Double getCurrentLat() { return currentLat; }
    public Double getCurrentLng() { return currentLng; }
}
