package com.smartbus.model;

import com.smartbus.util.GeoUtils;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A route is an ordered chain of stops. It owns its own geometry: buses ask the
 * route "where am I?" instead of doing map maths themselves (encapsulation).
 * <p>
 * "progress" is a position along the route measured in stop units:
 * 0.0 = first stop, 1.5 = halfway between stop 1 and stop 2, n-1 = last stop.
 */
@Entity
@Table(name = "routes")
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_number", nullable = false, unique = true, length = 16)
    private String routeNumber;

    @Column(nullable = false)
    private String name;

    @Column(name = "base_fare", nullable = false)
    private double baseFare;

    @Column(name = "fare_per_stop", nullable = false)
    private double farePerStop;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "route_stops",
            joinColumns = @JoinColumn(name = "route_id"),
            inverseJoinColumns = @JoinColumn(name = "stop_id"))
    @OrderColumn(name = "stop_order")
    private List<BusStop> stops = new ArrayList<>();

    protected Route() {}

    public Route(String routeNumber, String name, double baseFare, double farePerStop) {
        this.routeNumber = routeNumber;
        this.name = name;
        this.baseFare = baseFare;
        this.farePerStop = farePerStop;
    }

    // ---------- geometry ----------

    /** Distance from the first stop to each stop, in metres. */
    public double[] cumulativeMeters() {
        double[] c = new double[stops.size()];
        for (int i = 1; i < c.length; i++) {
            c[i] = c[i - 1] + stops.get(i - 1).distanceTo(stops.get(i));
        }
        return c;
    }

    public double totalMeters() {
        double[] c = cumulativeMeters();
        return c.length == 0 ? 0 : c[c.length - 1];
    }

    /** Metres travelled from the first stop at a given progress value. */
    public double metersAt(double progress) {
        double[] c = cumulativeMeters();
        if (c.length < 2) return 0;
        double p = GeoUtils.clamp(progress, 0, c.length - 1);
        int i = (int) Math.floor(p);
        if (i >= c.length - 1) return c[c.length - 1];
        return c[i] + (p - i) * (c[i + 1] - c[i]);
    }

    /** Inverse of {@link #metersAt(double)}. */
    public double progressAtMeters(double meters) {
        double[] c = cumulativeMeters();
        if (c.length < 2) return 0;
        double m = GeoUtils.clamp(meters, 0, c[c.length - 1]);
        for (int i = 0; i < c.length - 1; i++) {
            if (m <= c[i + 1]) {
                double seg = c[i + 1] - c[i];
                return seg == 0 ? i : i + (m - c[i]) / seg;
            }
        }
        return c.length - 1;
    }

    /** Interpolated {latitude, longitude} for a progress value. */
    public double[] positionAt(double progress) {
        if (stops.isEmpty()) return new double[]{0, 0};
        double p = GeoUtils.clamp(progress, 0, stops.size() - 1);
        int i = (int) Math.floor(p);
        if (i >= stops.size() - 1) {
            BusStop last = stops.get(stops.size() - 1);
            return new double[]{last.getLatitude(), last.getLongitude()};
        }
        double f = p - i;
        BusStop a = stops.get(i), b = stops.get(i + 1);
        return new double[]{
                a.getLatitude() + f * (b.getLatitude() - a.getLatitude()),
                a.getLongitude() + f * (b.getLongitude() - a.getLongitude())};
    }

    /** Polyline of {lat, lng} points from stop index a to stop index b (either direction). */
    public List<double[]> pathBetween(int a, int b) {
        List<double[]> path = new ArrayList<>();
        int step = b >= a ? 1 : -1;
        for (int i = a; i != b + step; i += step) {
            BusStop s = stops.get(i);
            path.add(new double[]{s.getLatitude(), s.getLongitude()});
        }
        return path;
    }

    public int indexOfStop(Long stopId) {
        for (int i = 0; i < stops.size(); i++) {
            if (stops.get(i).getId().equals(stopId)) return i;
        }
        return -1;
    }

    public double fareFor(int stopsTravelled) {
        return baseFare + farePerStop * stopsTravelled;
    }

    public Long getId() { return id; }
    public String getRouteNumber() { return routeNumber; }
    public String getName() { return name; }
    public double getBaseFare() { return baseFare; }
    public double getFarePerStop() { return farePerStop; }
    public List<BusStop> getStops() { return stops; }
}
