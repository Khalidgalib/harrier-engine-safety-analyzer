package com.example.project_prototype.obd;

public class OBDCommand {

    // ── Initialisation Commands ───────────────────────────────
    // These are sent ONCE inside OBDConnector.connect()
    // They set up the ELM327 adapter before any data is read

    public static final String RESET          = "ATZ";    // reset the ELM327 chip
    public static final String ECHO_OFF       = "ATE0";   // turn echo off — cleaner responses
    public static final String LINEFEEDS_OFF  = "ATL0";   // turn line feeds off
    public static final String SPACES_OFF     = "ATS0";   // turn spaces off in responses
    public static final String AUTO_PROTOCOL  = "ATSP0";  // auto-detect car protocol (CAN for Harrier)
    public static final String BATTERY_VOLT   = "ATRV";   // read battery voltage (ELM327 built-in)

    // ── Live Data PIDs ────────────────────────────────────────
    // These are sent EVERY SECOND inside the JavaFX Timeline
    // Each one asks the Harrier ECU for one specific reading

    public static final String SPEED          = "010D";   // vehicle speed         → km/h
    public static final String RPM            = "010C";   // engine RPM             → rev/min
    public static final String COOLANT_TEMP   = "0105";   // coolant temperature    → °C
    public static final String FUEL_LEVEL     = "012F";   // fuel tank level        → %

    // ── Fault Code Commands ───────────────────────────────────
    // These are sent ONLY when user clicks "Scan" or "Clear" button

    public static final String READ_FAULTS    = "03";     // read stored fault codes (DTC)
    public static final String CLEAR_FAULTS   = "04";     // clear all fault codes + reset Check Engine light

    // ── Private Constructor ───────────────────────────────────
    // Prevents anyone from creating an OBDCommand object
    // This class is only meant to be used as constants — never instantiated
    private OBDCommand() {}
}