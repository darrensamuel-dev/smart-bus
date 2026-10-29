package com.smartbus.controller;

import com.smartbus.dto.Dto;
import com.smartbus.model.Bus;
import com.smartbus.repository.*;
import com.smartbus.service.JourneySearchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

/** Read-only REST API for the map and the journey planner. */
@RestController
@RequestMapping("/api")
public class BusController {

    private final BusRepository busRepo;
    private final BusStopRepository stopRepo;
    private final RouteRepository routeRepo;
    private final TripRepository tripRepo;
    private final JourneySearchService journeyService;

    public BusController(BusRepository busRepo, BusStopRepository stopRepo, RouteRepository routeRepo,
                         TripRepository tripRepo, JourneySearchService journeyService) {
        this.busRepo = busRepo;
        this.stopRepo = stopRepo;
        this.routeRepo = routeRepo;
        this.tripRepo = tripRepo;
        this.journeyService = journeyService;
    }

    @GetMapping("/stops")
    public List<Dto.StopView> stops() {
        return stopRepo.findAllByOrderByNameAsc().stream().map(Dto::stop).toList();
    }

    @GetMapping("/routes")
    public List<Dto.RouteView> routes() {
        return routeRepo.findAll().stream().map(Dto::route).toList();
    }

    @GetMapping("/routes/{id}")
    public Dto.RouteView route(@PathVariable Long id) {
        return routeRepo.findById(id).map(Dto::route)
                .orElseThrow(() -> new NoSuchElementException("Route not found: " + id));
    }

    @GetMapping("/buses")
    public List<Dto.BusView> allBuses() {
        return busRepo.findAll().stream().map(Dto::bus).toList();
    }

    /** Only buses currently in service and with a known position. */
    @GetMapping("/buses/live")
    public List<Dto.BusView> liveBuses() {
        return busRepo.findByStatus(Bus.Status.RUNNING).stream()
                .filter(b -> b.getCurrentLat() != null && b.getCurrentLng() != null)
                .map(Dto::bus).toList();
    }

    @GetMapping("/journey")
    public Dto.JourneyResult journey(@RequestParam Long from, @RequestParam Long to) {
        return journeyService.search(from, to);
    }

    @GetMapping("/trips/recent")
    public List<Dto.TripView> recentTrips() {
        return tripRepo.findTop15ByOrderByDepartureTimeDesc().stream().map(Dto::trip).toList();
    }
}
