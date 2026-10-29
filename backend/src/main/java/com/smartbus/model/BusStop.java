package com.smartbus.model;

import com.smartbus.util.GeoUtils;
import jakarta.persistence.*;

@Entity
@Table(name = "bus_stops")
public class BusStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    private String zone;

    protected BusStop() {}

    public BusStop(String name, double latitude, double longitude, String zone) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.zone = zone;
    }

    public double distanceTo(BusStop other) {
        return GeoUtils.haversineMeters(latitude, longitude, other.latitude, other.longitude);
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getZone() { return zone; }
}
