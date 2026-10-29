package com.smartbus.dto;

import com.smartbus.model.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Plain read-only views sent to the browser. Entities never leave the service
 * layer, which avoids lazy-loading and infinite-recursion JSON problems.
 */
public final class Dto {
    private Dto() {}

    // ----- map data -----
    public record StopView(Long id, String name, double latitude, double longitude, String zone) {}

    public record RouteView(Long id, String routeNumber, String name, double baseFare,
                            double farePerStop, double lengthKm, List<StopView> stops) {}

    public record BusView(Long id, String registrationNo, String busType, String busTypeLabel,
                          String status, Long routeId, String routeNumber, Double latitude,
                          Double longitude, int capacity, int passengersOnboard,
                          int occupancyPercent, String crowdLevel, int direction, String towards) {}

    public record TripView(Long id, String busRegistration, String routeNumber, String status,
                           String departureTime, String arrivalTime, int passengersBoarded) {}

    // ----- journey planner -----
    public record Leg(Long routeId, String routeNumber, String routeName, String boardStop,
                      String alightStop, int stopsCount, double distanceKm, int rideMinutes,
                      int fare, Integer nextBusEtaMinutes, String nextBusReg, String nextBusType,
                      String crowdLevel, List<double[]> path) {}

    public record JourneyOption(String type, int totalMinutes, int totalFare, int transfers,
                                List<Leg> legs) {}

    public record JourneyResult(StopView from, StopView to, List<JourneyOption> options) {}

    // ----- demand forecasting -----
    public record HourForecast(int hour, int predicted, int requiredBuses) {}

    public record RouteForecast(Long routeId, String routeNumber, String routeName, String dayType,
                                int currentHour, int predictedNow, int capacityNow, int loadPercent,
                                int peakHour, int peakPredicted, int activeBuses,
                                int recommendedBuses, String status, String advice,
                                List<HourForecast> hours) {}

    public record Kpis(int runningBuses, int idleBuses, int passengersOnboard,
                       int avgOccupancyPercent, int alerts, long tripsCompleted) {}

    public record Insights(String generatedAt, Kpis kpis, List<RouteForecast> routes) {}

    // ----- auth -----
    public record RegisterRequest(@NotBlank String name,
                                  @NotBlank @Email String email,
                                  @NotBlank @Size(min = 6, message = "Password must be at least 6 characters") String password,
                                  String role, String adminCode) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record AuthResponse(Long id, String name, String email, String role) {}

    // ----- mappers -----
    public static StopView stop(BusStop s) {
        return new StopView(s.getId(), s.getName(), s.getLatitude(), s.getLongitude(), s.getZone());
    }

    public static RouteView route(Route r) {
        double km = Math.round(r.totalMeters() / 100.0) / 10.0;
        return new RouteView(r.getId(), r.getRouteNumber(), r.getName(), r.getBaseFare(),
                r.getFarePerStop(), km, r.getStops().stream().map(Dto::stop).toList());
    }

    public static BusView bus(Bus b) {
        Route r = b.getRoute();
        String towards = null;
        if (r != null && !r.getStops().isEmpty()) {
            int idx = b.getDirection() > 0 ? r.getStops().size() - 1 : 0;
            towards = r.getStops().get(idx).getName();
        }
        return new BusView(b.getId(), b.getRegistrationNo(), b.getBusType().name(),
                b.getBusType().getLabel(), b.getStatus().name(),
                r == null ? null : r.getId(), r == null ? null : r.getRouteNumber(),
                b.getCurrentLat(), b.getCurrentLng(), b.getCapacity(), b.getPassengersOnboard(),
                b.occupancyPercent(), b.crowdLevel(), b.getDirection(), towards);
    }

    public static TripView trip(Trip t) {
        return new TripView(t.getId(), t.getBus().getRegistrationNo(), t.getRoute().getRouteNumber(),
                t.getStatus().name(), String.valueOf(t.getDepartureTime()),
                t.getArrivalTime() == null ? null : t.getArrivalTime().toString(),
                t.getPassengersBoarded());
    }
}
