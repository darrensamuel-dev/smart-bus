package com.smartbus.model;

/**
 * Polymorphic behaviour lives in the enum: every bus type has its own cruising
 * speed and fare multiplier, so callers never need if/else on the type.
 */
public enum BusType {
    ORDINARY("Ordinary", 22, 1.0),
    EXPRESS("Express", 30, 1.5),
    AC("AC low-floor", 26, 2.0);

    /** Traffic, signals and dwell time make real trips slower than cruising speed. */
    private static final double URBAN_FACTOR = 1.25;

    private final String label;
    private final double speedKmph;
    private final double fareMultiplier;

    BusType(String label, double speedKmph, double fareMultiplier) {
        this.label = label;
        this.speedKmph = speedKmph;
        this.fareMultiplier = fareMultiplier;
    }

    /** Minutes needed to cover the given distance, including urban delays. */
    public double travelMinutes(double meters) {
        return meters / 1000.0 / speedKmph * 60.0 * URBAN_FACTOR;
    }

    public String getLabel() { return label; }
    public double getSpeedKmph() { return speedKmph; }
    public double getFareMultiplier() { return fareMultiplier; }
}
