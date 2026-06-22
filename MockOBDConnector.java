package com.example.project_prototype.mock;

import com.example.project_prototype.model.CarData;
import com.example.project_prototype.exception.ConnectionException;
import com.example.project_prototype.exception.OBDTimeoutException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MockOBDConnector {

    // ── Fields ───────────────────────────────────────────────
    private boolean connected;       // simulates connection state
    private Random random;           // generates fake random values

    // Base values — change gradually like real driving
    private int baseSpeed;           // current simulated speed
    private int baseRPM;             // current simulated RPM
    private int baseCoolantTemp;     // current simulated temp
    private int baseFuelLevel;       // current simulated fuel
    private double baseBatteryVolt;  // current simulated voltage

    // ── Constructor ──────────────────────────────────────────
    public MockOBDConnector() {
        this.connected       = false;
        this.random          = new Random();

        // realistic starting values for Toyota Harrier 2018
        this.baseSpeed       = 0;
        this.baseRPM         = 800;    // idle RPM
        this.baseCoolantTemp = 70;     // cold engine start temp
        this.baseFuelLevel   = 65;     // 65% full tank
        this.baseBatteryVolt = 12.4;   // healthy battery
    }

    // ── connect()────────────────────────────────────────────
    // Simulates a successful WiFi connection to ELM327
    // No real socket — just sets connected = true
    public void connect() throws ConnectionException {
        System.out.println("MockOBDConnector: Simulating connection...");

        // simulate a small delay like a real connection would have
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        connected = true;
        System.out.println("MockOBDConnector: Connected! (fake)");
    }

    // ── disconnect() ─────────────────────────────────────────
    public void disconnect() {
        connected = false;
        System.out.println("MockOBDConnector: Disconnected (fake)");
    }

    // ── isConnected() ────────────────────────────────────────
    public boolean isConnected() {
        return connected;
    }

    // ── fetchAllData() ───────────────────────────────────────
    // The most important method in MockOBDConnector
    // Generates one complete CarData snapshot with realistic values
    // Called every second from DashboardController Timeline
    //
    // Values change gradually — not randomly jumping
    // This simulates real driving behaviour
    public CarData fetchAllData() throws OBDTimeoutException {
        if (!connected) {
            throw new OBDTimeoutException(
                    "MockOBDConnector: Not connected. Call connect() first.",
                    "fetchAllData"
            );
        }

        return new CarData(
                getSpeed(),
                getRPM(),
                getCoolantTemp(),
                getFuelLevel(),
                getBatteryVoltage()
        );
    }

    // ── getSpeed() ───────────────────────────────────────────
    // Speed changes gradually — ±10 km/h each second
    // Stays between 0 and 120 km/h
    public int getSpeed() {
        baseSpeed += random.nextInt(21) - 10; // -10 to +10
        baseSpeed  = Math.max(0, Math.min(120, baseSpeed));
        return baseSpeed;
    }

    // ── getRPM() ─────────────────────────────────────────────
    // RPM follows speed — higher speed = higher RPM
    // Stays between 800 (idle) and 5500
    public int getRPM() {
        baseRPM = 800 + (baseSpeed * 35) + random.nextInt(300) - 150;
        baseRPM = Math.max(800, Math.min(5500, baseRPM));
        return baseRPM;
    }

    // ── getCoolantTemp() ─────────────────────────────────────
    // Temp warms up slowly from 70°C to 90°C like a real engine
    // Once warm it stays between 85°C and 95°C
    public int getCoolantTemp() {
        if (baseCoolantTemp < 88) {
            baseCoolantTemp += 1; // warming up gradually
        } else {
            baseCoolantTemp += random.nextInt(3) - 1; // small fluctuation
            baseCoolantTemp = Math.max(85, Math.min(95, baseCoolantTemp));
        }
        return baseCoolantTemp;
    }

    // ── getFuelLevel() ───────────────────────────────────────
    // Fuel drops very slowly — 1% every 50 readings (50 seconds)
    // Stays between 0 and 100
    private int fuelReadingCounter = 0;

    public int getFuelLevel() {
        fuelReadingCounter++;
        if (fuelReadingCounter >= 50) {
            baseFuelLevel = Math.max(0, baseFuelLevel - 1);
            fuelReadingCounter = 0; // reset counter
        }
        return baseFuelLevel;
    }

    // ── getBatteryVoltage() ──────────────────────────────────
    // Battery voltage fluctuates slightly — 12.2V to 14.4V
    // Higher when engine running (alternator charges battery)
    public double getBatteryVoltage() {
        baseBatteryVolt = 13.8 + (random.nextDouble() * 0.6) - 0.3;
        // 13.8 = healthy running voltage (alternator active)
        // range: 13.5 to 14.1V
        baseBatteryVolt = Math.round(baseBatteryVolt * 10.0) / 10.0;
        return baseBatteryVolt;
    }

    // ── getMockFaultCodes() ───────────────────────────────────
    // Returns fake fault codes for testing FaultController UI
    // In real app this comes from OBDConnector sending "03" command
    public List<String> getMockFaultCodes() {
        List<String> codes = new ArrayList<>();
        codes.add("P0420"); // Catalyst System Efficiency Below Threshold
        codes.add("P0171"); // System Too Lean Bank 1
        return codes;
    }

    // ── simulateConnectionFailure() ───────────────────────────
    // FOR TESTING ONLY
    // Forces a ConnectionException to test your error dialogs
    public void simulateConnectionFailure() throws ConnectionException {
        throw new ConnectionException(
                "MockOBDConnector: Simulated connection failure.\n" +
                        "Use this to test your error dialog UI."
        );
    }

    // ── simulateTimeout() ────────────────────────────────────
    // FOR TESTING ONLY
    // Forces an OBDTimeoutException to test your timeout handling
    public void simulateTimeout() throws OBDTimeoutException {
        throw new OBDTimeoutException(
                "MockOBDConnector: Simulated timeout on 010D.",
                "010D"
        );
    }
}