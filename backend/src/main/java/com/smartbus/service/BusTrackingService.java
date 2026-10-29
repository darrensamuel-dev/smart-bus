package com.smartbus.service;

import com.smartbus.model.*;
import com.smartbus.repository.BusRepository;
import com.smartbus.repository.TripRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/**
 * GPS simulator + ETA calculator.
 *
 * Every 3 seconds each RUNNING bus is moved along its route at its own
 * BusType speed. When it crosses a stop, riders get off and new riders board
 * in proportion to the forecast demand for that hour. When it reaches the end
 * of the line the trip is closed, the bus turns around and a new trip starts.
 *
 * To plug in real hardware, replace {@link #tick()} with a method that receives
 * latitude/longitude from GPS devices and calls {@link Bus#moveTo}.
 */
@Service
public class BusTrackingService {

    private static final double TICK_SECONDS = 3.0;

    private final BusRepository busRepo;
    private final TripRepository tripRepo;
    private final DemandForecastService forecast;
    private final Random random = new Random();

    @Value("${smartbus.sim.speed-multiplier:8}")
    private double speedMultiplier;

    public BusTrackingService(BusRepository busRepo, TripRepository tripRepo, DemandForecastService forecast) {
        this.busRepo = busRepo;
        this.tripRepo = tripRepo;
        this.forecast = forecast;
    }

    @Scheduled(fixedDelay = 3000, initialDelay = 1500)
    @Transactional
    public void tick() {
        List<Bus> running = busRepo.findByStatus(Bus.Status.RUNNING);
        for (Bus bus : running) {
            advance(bus);
        }
        busRepo.saveAll(running);
    }

    private void advance(Bus bus) {
        Route route = bus.getRoute();
        if (route == null || route.getStops().size() < 2) return;

        int lastIndex = route.getStops().size() - 1;
        double total = route.totalMeters();
        double stepMeters = bus.getBusType().getSpeedKmph() / 3.6 * TICK_SECONDS * speedMultiplier;

        double before = bus.getProgress();
        double meters = route.metersAt(before) + bus.getDirection() * stepMeters;
        boolean endOfLine = meters >= total || meters <= 0;
        double after = route.progressAtMeters(endOfLine ? (meters <= 0 ? 0 : total) : meters);

        Trip trip = tripRepo.findFirstByBusIdAndStatus(bus.getId(), Trip.Status.IN_PROGRESS)
                .orElseGet(() -> new Trip(bus, route, LocalDateTime.now()));

        // Stops passed during this tick
        for (int i = 0; i <= lastIndex; i++) {
            boolean crossed = bus.getDirection() > 0
                    ? (before < i && i <= after)
                    : (after <= i && i < before);
            if (crossed && !(endOfLine && (i == 0 || i == lastIndex))) {
                handleStop(bus, route, trip);
            }
        }

        double[] pos = route.positionAt(after);
        bus.moveTo(after, pos[0], pos[1]);

        if (endOfLine) {
            bus.alight(bus.getPassengersOnboard());   // terminus: everyone gets off
            trip.complete(LocalDateTime.now());
            tripRepo.save(trip);
            bus.reverseDirection();
            handleStop(bus, route, startNextTrip(bus, route));
        } else {
            tripRepo.save(trip);
        }
    }

    private Trip startNextTrip(Bus bus, Route route) {
        return tripRepo.save(new Trip(bus, route, LocalDateTime.now()));
    }

    /** Riders get off, then riders board according to forecast demand for this hour. */
    private void handleStop(Bus bus, Route route, Trip trip) {
        int getOff = (int) Math.round(bus.getPassengersOnboard() * (0.15 + random.nextDouble() * 0.25));
        bus.alight(getOff);
        double factor = forecast.demandFactor(route.getId());
        int maxBoard = (int) Math.max(1, Math.round(bus.getCapacity() * 0.30 * factor));
        int boarded = bus.board(random.nextInt(maxBoard + 1));
        trip.addBoarded(boarded);
    }

    /**
     * Minutes until {@code bus} reaches stop {@code stopIdx} so that a rider can
     * travel in direction {@code journeyDir} (+1 / -1). Buses that already passed,
     * or are heading the other way, must first finish their leg and turn around.
     */
    public double etaMinutes(Bus bus, Route route, int stopIdx, int journeyDir) {
        double total = route.totalMeters();
        double busAt = route.metersAt(bus.getProgress());
        double stopAt = route.cumulativeMeters()[stopIdx];
        int dir = bus.getDirection();
        boolean ahead = dir > 0 ? busAt <= stopAt : busAt >= stopAt;
        double endOfLeg = dir > 0 ? total : 0;
        double meters;

        if (dir == journeyDir) {
            if (ahead) {
                meters = Math.abs(stopAt - busAt);
            } else {
                double otherEnd = dir > 0 ? 0 : total;
                meters = Math.abs(endOfLeg - busAt) + total + Math.abs(stopAt - otherEnd);
            }
        } else {
            meters = Math.abs(endOfLeg - busAt) + Math.abs(endOfLeg - stopAt);
        }
        return bus.getBusType().travelMinutes(meters);
    }
}
