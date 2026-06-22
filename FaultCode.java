package com.example.project_prototype.model;

public class FaultCode {

    // ── Fields ───────────────────────────────────────────────
    private String code;          // e.g. "P0420"
    private String description;   // e.g. "Catalyst System Efficiency Below Threshold"
    private String severity;      // "LOW" / "MEDIUM" / "HIGH"
    private String category;      // "Engine" / "Emission" / "Fuel" / "Ignition"

    // ── Constructor ──────────────────────────────────────────
    public FaultCode(String code, String description, String severity, String category) {
        this.code        = code;
        this.description = description;
        this.severity    = severity;
        this.category    = category;
    }

    // ── Getters ──────────────────────────────────────────────
    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getSeverity() {
        return severity;
    }

    public String getCategory() {
        return category;
    }

    // ── Setters ──────────────────────────────────────────────
    public void setCode(String code) {
        this.code = code;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    // ── getSeverityLevel() ───────────────────────────────────
    // Returns severity as a number — useful for sorting by urgency
    // HIGH = 3, MEDIUM = 2, LOW = 1, UNKNOWN = 0
    public int getSeverityLevel() {
        switch (severity.toUpperCase()) {
            case "HIGH":   return 3;
            case "MEDIUM": return 2;
            case "LOW":    return 1;
            default:       return 0;
        }
    }

    // ── isHighSeverity() ─────────────────────────────────────
    // Quick check — used to show red alert in UI
    // if (fault.isHighSeverity()) → show red warning dialog
    public boolean isHighSeverity() {
        return severity.equalsIgnoreCase("HIGH");
    }

    // ── getCodePrefix() ──────────────────────────────────────
    // Returns just the first letter of the code
    // P = Powertrain  B = Body  C = Chassis  U = Network
    // e.g. "P0420" → "P"
    public String getCodePrefix() {
        if (code == null || code.isEmpty()) return "Unknown";
        return String.valueOf(code.charAt(0));
    }

    // ── getCodeSystem() ──────────────────────────────────────
    // Translates the code prefix letter into a readable system name
    // e.g. "P0420" → "Powertrain"
    public String getCodeSystem() {
        switch (getCodePrefix().toUpperCase()) {
            case "P": return "Powertrain";  // engine, transmission
            case "B": return "Body";        // airbags, seatbelts
            case "C": return "Chassis";     // brakes, suspension
            case "U": return "Network";     // communication between ECUs
            default:  return "Unknown";
        }
    }

    // ── toString() ───────────────────────────────────────────
    // Quick fault summary for debugging
    @Override
    public String toString() {
        return "FaultCode {" +
                "code="        + code        + ", " +
                "severity="    + severity    + ", " +
                "category="    + category    + ", " +
                "description=" + description +
                "}";
    }
}