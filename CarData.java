package com.example.project_prototype.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CarData {

    // ── Fields ───────────────────────────────────────────────
    // One complete snapshot of all sensor readings at one moment
    private int speed;            // km/h        → from OBDCommand.SPEED
    private int rpm;              // rev/min      → from OBDCommand.RPM
    private int coolantTemp;      // celsius      → from OBDCommand.COOLANT_TEMP
    private int fuelLevel;        // percentage   → from OBDCommand.FUEL_LEVEL
    private double batteryVoltage;// volts        → from OBDCommand.BATTERY_VOLT
    private LocalDateTime timestamp; // when this reading was taken → auto set

    // ── Constructor ──────────────────────────────────────────
    // timestamp is set automatically — you never pass it manually
    public CarData(int speed, int rpm, int coolantTemp,
                   int fuelLevel, double batteryVoltage) {
        this.speed          = speed;
        this.rpm            = rpm;
        this.coolantTemp    = coolantTemp;
        this.fuelLevel      = fuelLevel;
        this.batteryVoltage = batteryVoltage;
        this.timestamp      = LocalDateTime.now(); // auto set at creation time
    }

    // ── Getters ──────────────────────────────────────────────
    public int getSpeed() {
        return speed;
    }

    public int getRpm() {
        return rpm;
    }

    public int getCoolantTemp() {
        return coolantTemp;
    }

    public int getFuelLevel() {
        return fuelLevel;
    }

    public double getBatteryVoltage() {
        return batteryVoltage;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    // ── Setters ──────────────────────────────────────────────
    // Needed in case a single reading fails and needs updating
    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public void setRpm(int rpm) {
        this.rpm = rpm;
    }

    public void setCoolantTemp(int coolantTemp) {
        this.coolantTemp = coolantTemp;
    }

    public void setFuelLevel(int fuelLevel) {
        this.fuelLevel = fuelLevel;
    }

    public void setBatteryVoltage(double batteryVoltage) {
        this.batteryVoltage = batteryVoltage;
    }

    // ── toCSVRow() ───────────────────────────────────────────
    // Converts this object into one CSV line for CSVLogger
    // Example output:
    // "2024-03-15 14:32:01, 60, 2500, 88, 65, 12.4"
    public String toCSVRow() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return timestamp.format(formatter) + ", "
                + speed          + ", "
                + rpm            + ", "
                + coolantTemp    + ", "
                + fuelLevel      + ", "
                + batteryVoltage;
    }

    // ── CSV Header ───────────────────────────────────────────
    // Written once at the top of the CSV file by CSVLogger
    // Must match the exact order of toCSVRow()
    public static String getCSVHeader() {
        return "Timestamp, Speed(km/h), RPM, CoolantTemp(C), FuelLevel(%), BatteryVoltage(V)";
    }

    // ── toString() ───────────────────────────────────────────
    // For quick debugging in IntelliJ console
    // System.out.println(carData) → calls this automatically
    @Override
    public String toString() {
        return "CarData {" +
                "timestamp="      + timestamp      + ", " +
                "speed="          + speed          + " km/h, " +
                "rpm="            + rpm            + " rpm, " +
                "coolantTemp="    + coolantTemp    + "°C, " +
                "fuelLevel="      + fuelLevel      + "%, " +
                "batteryVoltage=" + batteryVoltage + "V" +
                "}";
    }
}