package com.smartbus.controller;

import com.smartbus.dto.Dto;
import com.smartbus.service.DemandForecastService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/demand")
public class DemandController {

    private final DemandForecastService forecastService;

    public DemandController(DemandForecastService forecastService) {
        this.forecastService = forecastService;
    }

    /** Hour-by-hour forecast and bus recommendation for one route. */
    @GetMapping("/forecast")
    public Dto.RouteForecast forecast(@RequestParam Long routeId) {
        return forecastService.forecast(routeId);
    }

    /** Network-wide KPIs plus a forecast for every route (operations dashboard). */
    @GetMapping("/insights")
    public Dto.Insights insights() {
        return forecastService.insights();
    }
}
