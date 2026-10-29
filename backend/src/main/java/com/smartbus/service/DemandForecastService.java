package com.smartbus.service;

import com.smartbus.dto.Dto;
import com.smartbus.model.*;
import com.smartbus.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Passenger-demand forecasting and fleet advice.
 *
 * Method (deliberately explainable, no black box):
 *  1. Take every historical record for the same route, same hour and same day type
 *     (weekday / weekend).
 *  2. Average them with recency weights  w = 1 / (1 + daysOld / 7)  so last week
 *     counts more than a month ago.
 *  3. Apply a live surge factor when the buses currently on the route are already
 *     more than 80% full  (surge = 1 + (occupancy - 80) / 100).
 *  4. Compare the forecast with the seats the running buses can really supply
 *     per hour and recommend how many buses the route needs.
 */
@Service
public class DemandForecastService {

    public static final double LOAD_FACTOR = 0.85;       // target maximum fill
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final int FIRST_HOUR = 5, LAST_HOUR = 23;

    private final BusRepository busRepo;
    private final RouteRepository routeRepo;
    private final DemandRecordRepository demandRepo;
    private final TripRepository tripRepo;

    /** History only changes through batch imports, so hourly curves are cached. */
    private final Map<String, double[]> curveCache = new ConcurrentHashMap<>();

    public DemandForecastService(BusRepository busRepo, RouteRepository routeRepo,
                                 DemandRecordRepository demandRepo, TripRepository tripRepo) {
        this.busRepo = busRepo;
        this.routeRepo = routeRepo;
        this.demandRepo = demandRepo;
        this.tripRepo = tripRepo;
    }

    // ---------- public API ----------

    public double[] hourlyCurve(Long routeId, DemandRecord.DayType dayType) {
        return curveCache.computeIfAbsent(routeId + ":" + dayType, k -> buildCurve(routeId, dayType));
    }

    /** 0.1 .. 1.0 : how busy is this hour compared to the route's own busiest hour. */
    public double demandFactor(Long routeId) {
        ZonedDateTime now = ZonedDateTime.now(IST);
        double[] curve = hourlyCurve(routeId, dayTypeOf(now.toLocalDate()));
        double max = Arrays.stream(curve).max().orElse(0);
        if (max <= 0) return 0.1;
        return Math.max(0.1, Math.min(1.0, curve[now.getHour()] / max));
    }

    public static double capacityPerBusPerHour(Route route, BusType type, int seats) {
        double oneWayMinutes = Math.max(1, type.travelMinutes(route.totalMeters()));
        return seats * (60.0 / oneWayMinutes) * LOAD_FACTOR;
    }

    @Transactional(readOnly = true)
    public Dto.RouteForecast forecast(Long routeId) {
        Route route = routeRepo.findById(routeId)
                .orElseThrow(() -> new NoSuchElementException("Route not found: " + routeId));
        return analyse(route, busRepo.countByStatus(Bus.Status.IDLE));
    }

    @Transactional(readOnly = true)
    public Dto.Insights insights() {
        long idle = busRepo.countByStatus(Bus.Status.IDLE);
        List<Dto.RouteForecast> routes = routeRepo.findAll().stream()
                .sorted(Comparator.comparing(Route::getRouteNumber))
                .map(r -> analyse(r, idle)).toList();

        List<Bus> running = busRepo.findByStatus(Bus.Status.RUNNING);
        int onboard = running.stream().mapToInt(Bus::getPassengersOnboard).sum();
        int avgOcc = (int) Math.round(running.stream().mapToInt(Bus::occupancyPercent).average().orElse(0));
        int alerts = (int) routes.stream().filter(r -> "ADD_BUSES".equals(r.status())).count();

        Dto.Kpis kpis = new Dto.Kpis(running.size(), (int) idle, onboard, avgOcc, alerts,
                tripRepo.countByStatus(Trip.Status.COMPLETED));
        return new Dto.Insights(ZonedDateTime.now(IST).toString(), kpis, routes);
    }

    // ---------- internals ----------

    private Dto.RouteForecast analyse(Route route, long idleReserve) {
        ZonedDateTime now = ZonedDateTime.now(IST);
        DemandRecord.DayType dayType = dayTypeOf(now.toLocalDate());
        double[] curve = hourlyCurve(route.getId(), dayType);

        List<Bus> running = busRepo.findByRouteId(route.getId()).stream().filter(Bus::isRunning).toList();
        double avgOcc = running.stream().mapToInt(Bus::occupancyPercent).average().orElse(0);
        double surge = 1 + Math.max(0, (avgOcc - 80) / 100.0);

        double supplyNow = running.stream()
                .mapToDouble(b -> capacityPerBusPerHour(route, b.getBusType(), b.getCapacity())).sum();
        double perBus = running.isEmpty()
                ? capacityPerBusPerHour(route, BusType.ORDINARY, 60)
                : supplyNow / running.size();

        int hour = now.getHour();
        double predictedNow = curve[hour] * surge;

        int peakHour = FIRST_HOUR;
        for (int h = FIRST_HOUR; h <= LAST_HOUR; h++) if (curve[h] > curve[peakHour]) peakHour = h;

        List<Dto.HourForecast> hours = new ArrayList<>();
        for (int h = FIRST_HOUR; h <= LAST_HOUR; h++) {
            hours.add(new Dto.HourForecast(h, (int) Math.round(curve[h]), (int) Math.ceil(curve[h] / perBus)));
        }

        int required = (int) Math.ceil(predictedNow / perBus);
        int need = Math.max(0, required - running.size());
        double load = supplyNow > 0 ? predictedNow / supplyNow * 100 : (predictedNow > 0 ? 999 : 0);

        String status;
        String advice;
        if (predictedNow <= 0) {
            status = "OK";
            advice = "Off-peak hour: no demand expected on this route.";
        } else if (load > 100) {
            status = "ADD_BUSES";
            advice = "Expected " + Math.round(predictedNow) + " riders this hour but running buses can carry about "
                    + Math.round(supplyNow) + ". Add " + need + " bus" + (need == 1 ? "" : "es")
                    + (idleReserve > 0 ? " (" + idleReserve + " idle in the depot)." : ".");
        } else if (load > 70) {
            status = "WATCH";
            advice = "Running at " + Math.round(load) + "% of comfortable capacity. Keep a reserve bus ready.";
        } else {
            status = "OK";
            advice = "Service level is sufficient for the expected demand.";
        }

        return new Dto.RouteForecast(route.getId(), route.getRouteNumber(), route.getName(), dayType.name(),
                hour, (int) Math.round(predictedNow), (int) Math.round(supplyNow),
                (int) Math.min(999, Math.round(load)), peakHour, (int) Math.round(curve[peakHour]),
                running.size(), Math.max(required, predictedNow > 0 ? 1 : 0), status, advice, hours);
    }

    private double[] buildCurve(Long routeId, DemandRecord.DayType dayType) {
        List<DemandRecord> records = demandRepo.findByRouteIdAndDayType(routeId, dayType);
        double[] curve = new double[24];
        if (records.isEmpty()) return curve;

        LocalDate latest = records.stream().map(DemandRecord::getRecordedOn)
                .max(Comparator.naturalOrder()).orElseThrow();
        double[] weighted = new double[24];
        double[] weights = new double[24];
        for (DemandRecord rec : records) {
            long daysOld = ChronoUnit.DAYS.between(rec.getRecordedOn(), latest);
            double w = 1.0 / (1.0 + daysOld / 7.0);
            weighted[rec.getHourOfDay()] += w * rec.getPassengers();
            weights[rec.getHourOfDay()] += w;
        }
        for (int h = 0; h < 24; h++) curve[h] = weights[h] > 0 ? weighted[h] / weights[h] : 0;
        return curve;
    }

    private static DemandRecord.DayType dayTypeOf(LocalDate date) {
        DayOfWeek d = date.getDayOfWeek();
        return (d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY)
                ? DemandRecord.DayType.WEEKEND : DemandRecord.DayType.WEEKDAY;
    }
}
