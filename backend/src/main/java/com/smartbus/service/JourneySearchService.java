package com.smartbus.service;

import com.smartbus.dto.Dto;
import com.smartbus.model.*;
import com.smartbus.repository.BusRepository;
import com.smartbus.repository.BusStopRepository;
import com.smartbus.repository.RouteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Journey planner: direct routes first, then one-change journeys.
 * Every leg is priced and timed using the real next bus on that route
 * (its type decides speed and fare multiplier) and shows how crowded it is.
 */
@Service
public class JourneySearchService {

    private static final int TRANSFER_WAIT_MIN = 5;
    private static final int DEFAULT_WAIT_MIN = 10;   // when no bus is running on the route
    private static final int MAX_OPTIONS = 6;

    private final RouteRepository routeRepo;
    private final BusStopRepository stopRepo;
    private final BusRepository busRepo;
    private final BusTrackingService tracking;

    public JourneySearchService(RouteRepository routeRepo, BusStopRepository stopRepo,
                                BusRepository busRepo, BusTrackingService tracking) {
        this.routeRepo = routeRepo;
        this.stopRepo = stopRepo;
        this.busRepo = busRepo;
        this.tracking = tracking;
    }

    @Transactional(readOnly = true)
    public Dto.JourneyResult search(Long fromId, Long toId) {
        if (fromId.equals(toId)) throw new IllegalArgumentException("Choose two different stops");
        BusStop from = stopRepo.findById(fromId)
                .orElseThrow(() -> new NoSuchElementException("Unknown start stop: " + fromId));
        BusStop to = stopRepo.findById(toId)
                .orElseThrow(() -> new NoSuchElementException("Unknown destination stop: " + toId));

        List<Route> routes = routeRepo.findAll();
        List<Dto.JourneyOption> direct = new ArrayList<>();
        Map<String, Dto.JourneyOption> bestTransfer = new HashMap<>();

        // 1) direct journeys
        for (Route r : routes) {
            int a = r.indexOfStop(fromId), b = r.indexOfStop(toId);
            if (a >= 0 && b >= 0) {
                direct.add(toOption("DIRECT", List.of(buildLeg(r, a, b))));
            }
        }

        // 2) one change: ride A to a shared stop, then ride B
        if (direct.size() < MAX_OPTIONS) {
            for (Route ra : routes) {
                int a = ra.indexOfStop(fromId);
                if (a < 0 || ra.indexOfStop(toId) >= 0) continue;
                for (Route rb : routes) {
                    int b = rb.indexOfStop(toId);
                    if (rb.getId().equals(ra.getId()) || b < 0 || rb.indexOfStop(fromId) >= 0) continue;
                    for (int i = 0; i < ra.getStops().size(); i++) {
                        if (i == a) continue;
                        int j = rb.indexOfStop(ra.getStops().get(i).getId());
                        if (j < 0 || j == b) continue;
                        Dto.JourneyOption option = toOption("TRANSFER",
                                List.of(buildLeg(ra, a, i), buildLeg(rb, j, b)));
                        String key = ra.getId() + ">" + rb.getId();
                        Dto.JourneyOption known = bestTransfer.get(key);
                        if (known == null || option.totalMinutes() < known.totalMinutes()) {
                            bestTransfer.put(key, option);
                        }
                    }
                }
            }
        }

        List<Dto.JourneyOption> all = new ArrayList<>(direct);
        all.addAll(bestTransfer.values());
        all.sort(Comparator.comparingInt(Dto.JourneyOption::totalMinutes));
        List<Dto.JourneyOption> top = all.size() > MAX_OPTIONS ? all.subList(0, MAX_OPTIONS) : all;
        return new Dto.JourneyResult(Dto.stop(from), Dto.stop(to), new ArrayList<>(top));
    }

    private Dto.Leg buildLeg(Route route, int a, int b) {
        int journeyDir = b > a ? 1 : -1;
        double[] cum = route.cumulativeMeters();
        double meters = Math.abs(cum[b] - cum[a]);
        int stopsCount = Math.abs(b - a);

        Bus best = null;
        double bestEta = Double.MAX_VALUE;
        for (Bus bus : busRepo.findByRouteId(route.getId())) {
            if (!bus.isRunning()) continue;
            double eta = tracking.etaMinutes(bus, route, a, journeyDir);
            if (eta < bestEta) { bestEta = eta; best = bus; }
        }

        BusType type = best != null ? best.getBusType() : BusType.ORDINARY;
        int ride = Math.max(1, (int) Math.round(type.travelMinutes(meters)));
        int fare = (int) Math.round(route.fareFor(stopsCount) * type.getFareMultiplier());

        return new Dto.Leg(route.getId(), route.getRouteNumber(), route.getName(),
                route.getStops().get(a).getName(), route.getStops().get(b).getName(),
                stopsCount, Math.round(meters / 100.0) / 10.0, ride, fare,
                best == null ? null : (int) Math.max(1, Math.round(bestEta)),
                best == null ? null : best.getRegistrationNo(),
                best == null ? null : type.getLabel(),
                best == null ? null : best.crowdLevel(),
                route.pathBetween(a, b));
    }

    private Dto.JourneyOption toOption(String type, List<Dto.Leg> legs) {
        int wait = legs.get(0).nextBusEtaMinutes() != null ? legs.get(0).nextBusEtaMinutes() : DEFAULT_WAIT_MIN;
        int total = wait + (legs.size() - 1) * TRANSFER_WAIT_MIN;
        int fare = 0;
        for (Dto.Leg leg : legs) {
            total += leg.rideMinutes();
            fare += leg.fare();
        }
        return new Dto.JourneyOption(type, total, fare, legs.size() - 1, legs);
    }
}
