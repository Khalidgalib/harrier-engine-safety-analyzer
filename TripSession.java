package com.example.project_prototype.model;

import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TripSession {

    // ── Fields ───────────────────────────────────────────────
    private LocalDateTime startTime;       // when trip started
    private LocalDateTime endTime;         // when trip ended
    private List<CarData> readings;        // every CarData snapshot during trip
    private double fuelPricePerLitre;      // BDT per litre — user enters this

    // ── Constructor ──────────────────────────────────────────
    // Called when user clicks "Start Trip" button
    public TripSession(double fuelPricePerLitre) {
        this.startTime          = LocalDateTime.now(); // auto set
        this.endTime            = null;                // not ended yet
        this.readings           = new ArrayList<>();   // empty list — fills as car drives
        this.fuelPricePerLitre  = fuelPricePerLitre;
    }

    // ── addReading() ─────────────────────────────────────────
    // Called every second from DashboardController Timeline
    // Adds one CarData snapshot to the list
    public void addReading(CarData data) {
        readings.add(data);
    }

    // ── endTrip() ────────────────────────────────────────────
    // Called when user clicks "Stop Trip" button
    public void endTrip() {
        this.endTime = LocalDateTime.now();
    }

    // ── getTripDurationMinutes() ──────────────────────────────
    // How long the trip lasted in minutes
    // Uses Java Duration to calculate difference between start and end
    public long getTripDurationMinutes() {
        if (endTime == null) {
            // trip still ongoing — calculate from now
            return Duration.between(startTime, LocalDateTime.now()).toMinutes();
        }
        return Duration.between(startTime, endTime).toMinutes();
    }

    // ── getAverageSpeed() ─────────────────────────────────────
    // Adds all speed readings and divides by count
    // Example: [60, 80, 40, 70] → (60+80+40+70) / 4 = 62 km/h
    public double getAverageSpeed() {
        if (readings.isEmpty()) return 0;

        int total = 0;
        for (CarData data : readings) {
            total += data.getSpeed();
        }
        return (double) total / readings.size();
    }

    // ── getMaxSpeed() ────────────────────────────────────────
    // Finds the highest speed recorded during the trip
    public int getMaxSpeed() {
        if (readings.isEmpty()) return 0;

        int max = 0;
        for (CarData data : readings) {
            if (data.getSpeed() > max) {
                max = data.getSpeed();
            }
        }
        return max;
    }

    // ── getAverageRPM() ──────────────────────────────────────
    // Average engine RPM across the whole trip
    public double getAverageRPM() {
        if (readings.isEmpty()) return 0;

        int total = 0;
        for (CarData data : readings) {
            total += data.getRpm();
        }
        return (double) total / readings.size();
    }

    // ── getAverageCoolantTemp() ───────────────────────────────
    // Average engine temperature across the whole trip
    public double getAverageCoolantTemp() {
        if (readings.isEmpty()) return 0;

        int total = 0;
        for (CarData data : readings) {
            total += data.getCoolantTemp();
        }
        return (double) total / readings.size();
    }

    // ── getEstimatedFuelUsed() ────────────────────────────────
    // Estimates litres of fuel burned during this trip
    // Looks at fuel level drop from first reading to last reading
    // Example: started at 65%, ended at 60% → dropped 5%
    // Harrier 2018 tank = 60 litres → 5% of 60 = 3 litres used
    public double getEstimatedFuelUsed() {
        if (readings.size() < 2) return 0;

        int startFuel = readings.get(0).getFuelLevel();           // first reading
        int endFuel   = readings.get(readings.size() - 1).getFuelLevel(); // last reading

        int fuelDropPercent = startFuel - endFuel;
        if (fuelDropPercent <= 0) return 0; // fuel didn't drop

        double harrierTankLitres = 60.0; // Toyota Harrier 2018 tank size
        return (fuelDropPercent / 100.0) * harrierTankLitres;
    }

    // ── getEstimatedFuelCostBDT() ─────────────────────────────
    // Calculates how much this trip cost in BDT
    // Formula: litres used × price per litre
    public double getEstimatedFuelCostBDT() {
        return getEstimatedFuelUsed() * fuelPricePerLitre;
    }

    // ── getTotalReadings() ────────────────────────────────────
    // How many CarData snapshots were recorded
    // 1 reading per second → 60 readings = 1 minute of driving
    public int getTotalReadings() {
        return readings.size();
    }

    // ── isOngoing() ──────────────────────────────────────────
    // Returns true if trip has not ended yet
    public boolean isOngoing() {
        return endTime == null;
    }

    // ── Getters ──────────────────────────────────────────────
    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public List<CarData> getReadings() {
        return readings;
    }

    public double getFuelPricePerLitre() {
        return fuelPricePerLitre;
    }

    // ── toString() ───────────────────────────────────────────
    // Quick trip summary for debugging
    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return "TripSession {" +
                "\n  startTime      = " + startTime.format(formatter) +
                "\n  endTime        = " + (endTime != null ? endTime.format(formatter) : "ongoing") +
                "\n  duration       = " + getTripDurationMinutes() + " minutes" +
                "\n  totalReadings  = " + getTotalReadings() +
                "\n  avgSpeed       = " + String.format("%.1f", getAverageSpeed()) + " km/h" +
                "\n  maxSpeed       = " + getMaxSpeed() + " km/h" +
                "\n  avgRPM         = " + String.format("%.0f", getAverageRPM()) + " rpm" +
                "\n  fuelUsed       = " + String.format("%.2f", getEstimatedFuelUsed()) + " litres" +
                "\n  tripCost       = " + String.format("%.2f", getEstimatedFuelCostBDT()) + " BDT" +
                "\n}";
    }
    // ── Add this field at the top with other fields ───────────
    private double distanceKm;   // total km driven this trip

    // ── Add this method ───────────────────────────────────────
// Called every second from DashboardController Timeline
// speed is in km/h — divide by 3600 to get km per second
    public void addDistanceFromSpeed(int speedKmh) {
        distanceKm += speedKmh / 3600.0;
    }

    // ── Add this getter ───────────────────────────────────────
    public double getDistanceKm() {
        return distanceKm;
    }

    // ── Add this method — cost per km ────────────────────────
// Only meaningful if distance > 0
    public double getFuelCostPerKm() {
        if (distanceKm <= 0) return 0;
        return getEstimatedFuelCostBDT() / distanceKm;
    }
}