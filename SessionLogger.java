package com.example.project_prototype.logger;

import com.example.project_prototype.model.TripSession;

import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

public class SessionLogger {

    // ── Fields ───────────────────────────────────────────────
    private final String sessionFolderPath; // folder where all session JSON files live
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ── Constructor ──────────────────────────────────────────
    public SessionLogger(String sessionFolderPath) {
        this.sessionFolderPath = sessionFolderPath;

        // create folder automatically if it doesn't exist
        File folder = new File(sessionFolderPath);
        if (!folder.exists()) {
            folder.mkdirs();
            System.out.println("Session folder created: " + sessionFolderPath);
        }
    }

    // ── saveSession() ─────────────────────────────────────────
    // Saves a TripSession summary as a JSON file
    // Called when user clicks "Stop Trip"
    //
    // IMPORTANT: We do NOT save all 1800 CarData readings here
    // The raw readings are already saved in the CSV file (CSVLogger)
    // Here we only save the trip SUMMARY + link to the CSV file
    //
    // Example file saved: sessions/session_2024-03-15_14-32-01.json
    public void saveSession(TripSession trip, String linkedCSVPath) throws IOException {

        // generate unique filename for this session
        String fileName = generateSessionFileName();
        String fullPath = sessionFolderPath + "/" + fileName;

        // build JSON string manually — no extra library needed
        String json = buildJSON(trip, linkedCSVPath);

        // write to file
        BufferedWriter writer = new BufferedWriter(new FileWriter(fullPath));
        writer.write(json);
        writer.flush();
        writer.close();

        System.out.println("Session saved: " + fullPath);
    }

    // ── loadSession() ─────────────────────────────────────────
    // Reads a JSON file and returns a SessionSummary object
    // Called when user opens the Trip History screen
    //
    // Returns SessionSummary — a simple data holder
    // We cannot fully restore TripSession (readings are in CSV)
    // but we can restore all summary data for display
    public SessionSummary loadSession(String filePath) throws IOException {

        // read entire file into one string
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        StringBuilder content = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            content.append(line).append("\n");
        }
        reader.close();

        // parse the JSON string back into a SessionSummary
        return parseJSON(content.toString(), filePath);
    }

    // ── listAllSessions() ─────────────────────────────────────
    // Returns a list of all saved SessionSummary objects
    // Called when LogViewController loads the trip history table
    public List<SessionSummary> listAllSessions() {
        List<SessionSummary> sessions = new ArrayList<>();

        File folder = new File(sessionFolderPath);
        File[] files = folder.listFiles();

        if (files == null || files.length == 0) {
            System.out.println("No sessions found in: " + sessionFolderPath);
            return sessions;
        }

        for (File file : files) {
            // only load .json files
            if (file.getName().endsWith(".json")) {
                try {
                    SessionSummary summary = loadSession(file.getAbsolutePath());
                    sessions.add(summary);
                    System.out.println("Loaded session: " + file.getName());

                } catch (IOException e) {
                    System.err.println("Failed to load session: "
                            + file.getName() + " → " + e.getMessage());
                }
            }
        }

        System.out.println("Total sessions loaded: " + sessions.size());
        return sessions;
    }

    // ── deleteSession() ───────────────────────────────────────
    // Deletes a session JSON file from disk
    // Called when user clicks "Delete" on a trip in history
    public boolean deleteSession(String filePath) {
        File file = new File(filePath);

        if (!file.exists()) {
            System.err.println("Session file not found: " + filePath);
            return false;
        }

        boolean deleted = file.delete();

        if (deleted) {
            System.out.println("Session deleted: " + filePath);
        } else {
            System.err.println("Could not delete session: " + filePath);
        }

        return deleted;
    }

    // ── getTotalTrips() ───────────────────────────────────────
    // Returns how many saved sessions exist
    public int getTotalTrips() {
        File folder = new File(sessionFolderPath);
        File[] files = folder.listFiles(
                (dir, name) -> name.endsWith(".json")
        );
        return files != null ? files.length : 0;
    }

    // ════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ════════════════════════════════════════════════════════

    // ── buildJSON() ───────────────────────────────────────────
    // Manually builds a JSON string from TripSession data
    // Saves only summary fields — not all 1800 CarData readings
    //
    // Example output:
    // {
    //   "startTime": "2024-03-15 14:32:01",
    //   "endTime": "2024-03-15 15:07:01",
    //   "durationMinutes": 35,
    //   "totalReadings": 2100,
    //   "averageSpeed": 58.4,
    //   "maxSpeed": 110,
    //   "averageRPM": 2340.0,
    //   "averageCoolantTemp": 88.5,
    //   "estimatedFuelUsed": 2.40,
    //   "fuelPricePerLitre": 114.0,
    //   "estimatedFuelCostBDT": 273.60,
    //   "linkedCSVPath": "logs/trip_2024-03-15_14-32-01.csv"
    // }
    private String buildJSON(TripSession trip, String linkedCSVPath) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");

        json.append("  \"startTime\": \""
                + trip.getStartTime().format(FORMATTER) + "\",\n");

        json.append("  \"endTime\": \""
                + (trip.getEndTime() != null
                ? trip.getEndTime().format(FORMATTER)
                : "ongoing") + "\",\n");

        json.append("  \"durationMinutes\": "
                + trip.getTripDurationMinutes() + ",\n");

        json.append("  \"totalReadings\": "
                + trip.getTotalReadings() + ",\n");

        json.append("  \"averageSpeed\": "
                + String.format("%.1f", trip.getAverageSpeed()) + ",\n");

        json.append("  \"maxSpeed\": "
                + trip.getMaxSpeed() + ",\n");

        json.append("  \"averageRPM\": "
                + String.format("%.1f", trip.getAverageRPM()) + ",\n");

        json.append("  \"averageCoolantTemp\": "
                + String.format("%.1f", trip.getAverageCoolantTemp()) + ",\n");

        json.append("  \"estimatedFuelUsed\": "
                + String.format("%.2f", trip.getEstimatedFuelUsed()) + ",\n");

        json.append("  \"fuelPricePerLitre\": "
                + trip.getFuelPricePerLitre() + ",\n");

        json.append("  \"estimatedFuelCostBDT\": "
                + String.format("%.2f", trip.getEstimatedFuelCostBDT()) + ",\n");

        json.append("  \"linkedCSVPath\": \""
                + linkedCSVPath + "\"\n");
        // ── ADD these two lines inside buildJSON() ────────────────
        json.append("  \"distanceKm\": "
                + String.format("%.2f", trip.getDistanceKm()) + ",\n");

        json.append("  \"fuelCostPerKm\": "
                + String.format("%.2f", trip.getFuelCostPerKm()) + ",\n");

        json.append("}");
        return json.toString();
    }

    // ── parseJSON() ───────────────────────────────────────────
    // Reads a JSON string and extracts values into SessionSummary
    // Simple line-by-line parsing — no library needed
    private SessionSummary parseJSON(String json, String filePath) {
        SessionSummary summary = new SessionSummary();
        summary.setFilePath(filePath);

        // split into lines and extract each value
        String[] lines = json.split("\n");

        for (String line : lines) {
            line = line.trim();

            if (line.startsWith("\"startTime\""))
                summary.setStartTime(extractString(line));

            else if (line.startsWith("\"endTime\""))
                summary.setEndTime(extractString(line));

            else if (line.startsWith("\"durationMinutes\""))
                summary.setDurationMinutes(extractLong(line));

            else if (line.startsWith("\"totalReadings\""))
                summary.setTotalReadings(extractInt(line));

            else if (line.startsWith("\"averageSpeed\""))
                summary.setAverageSpeed(extractDouble(line));

            else if (line.startsWith("\"maxSpeed\""))
                summary.setMaxSpeed(extractInt(line));

            else if (line.startsWith("\"averageRPM\""))
                summary.setAverageRPM(extractDouble(line));

            else if (line.startsWith("\"averageCoolantTemp\""))
                summary.setAverageCoolantTemp(extractDouble(line));

            else if (line.startsWith("\"estimatedFuelUsed\""))
                summary.setEstimatedFuelUsed(extractDouble(line));

            else if (line.startsWith("\"fuelPricePerLitre\""))
                summary.setFuelPricePerLitre(extractDouble(line));

            else if (line.startsWith("\"estimatedFuelCostBDT\""))
                summary.setEstimatedFuelCostBDT(extractDouble(line));

            else if (line.startsWith("\"linkedCSVPath\""))
                summary.setLinkedCSVPath(extractString(line));
                // ── ADD inside parseJSON() for loop ──────────────────────
            else if (line.startsWith("\"distanceKm\""))
                summary.setDistanceKm(extractDouble(line));

            else if (line.startsWith("\"fuelCostPerKm\""))
                summary.setFuelCostPerKm(extractDouble(line));
        }

        return summary;
    }

    // ── Extract Helpers ───────────────────────────────────────
    // Each one reads one value from one JSON line

    // extracts: "startTime": "2024-03-15 14:32:01"  → "2024-03-15 14:32:01"
    private String extractString(String line) {
        int first = line.indexOf("\"", line.indexOf(":")) + 1;
        int last  = line.lastIndexOf("\"");
        if (first < 0 || last < 0 || first >= last) return "";
        return line.substring(first, last);
    }

    // extracts: "averageSpeed": 58.4  → 58.4
    private double extractDouble(String line) {
        try {
            String value = line.substring(line.indexOf(":") + 1)
                    .replace(",", "").trim();
            return Double.parseDouble(value);
        } catch (Exception e) { return 0.0; }
    }

    // extracts: "maxSpeed": 110  → 110
    private int extractInt(String line) {
        try {
            String value = line.substring(line.indexOf(":") + 1)
                    .replace(",", "").trim();
            return Integer.parseInt(value);
        } catch (Exception e) { return 0; }
    }

    // extracts: "durationMinutes": 35  → 35L
    private long extractLong(String line) {
        try {
            String value = line.substring(line.indexOf(":") + 1)
                    .replace(",", "").trim();
            return Long.parseLong(value);
        } catch (Exception e) { return 0L; }
    }

    // ── generateSessionFileName() ─────────────────────────────
    // Unique filename for each session using timestamp
    // Example: "session_2024-03-15_14-32-01.json"
    private String generateSessionFileName() {
        String timestamp = java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        return "session_" + timestamp + ".json";
    }

    // ════════════════════════════════════════════════════════
    //  INNER CLASS — SessionSummary
    // ════════════════════════════════════════════════════════
    // A simple data holder for what was loaded from JSON
    // Used by LogViewController to populate the history table
    public static class SessionSummary {

        private String filePath;
        private String startTime;
        private String endTime;
        private long   durationMinutes;
        private int    totalReadings;
        private double averageSpeed;
        private int    maxSpeed;
        private double averageRPM;
        private double averageCoolantTemp;
        private double estimatedFuelUsed;
        private double fuelPricePerLitre;
        private double estimatedFuelCostBDT;
        private String linkedCSVPath;
        private double distanceKm;
        private double fuelCostPerKm;

        // Getters
        public String getFilePath()             { return filePath; }
        public String getStartTime()            { return startTime; }
        public String getEndTime()              { return endTime; }
        public long   getDurationMinutes()      { return durationMinutes; }
        public int    getTotalReadings()        { return totalReadings; }
        public double getAverageSpeed()         { return averageSpeed; }
        public int    getMaxSpeed()             { return maxSpeed; }
        public double getAverageRPM()           { return averageRPM; }
        public double getAverageCoolantTemp()   { return averageCoolantTemp; }
        public double getEstimatedFuelUsed()    { return estimatedFuelUsed; }
        public double getFuelPricePerLitre()    { return fuelPricePerLitre; }
        public double getEstimatedFuelCostBDT() { return estimatedFuelCostBDT; }
        public String getLinkedCSVPath()        { return linkedCSVPath; }
        public double getDistanceKm()    { return distanceKm; }
        public double getFuelCostPerKm() { return fuelCostPerKm; }

        // Setters
        public void setFilePath(String v)             { this.filePath = v; }
        public void setStartTime(String v)            { this.startTime = v; }
        public void setEndTime(String v)              { this.endTime = v; }
        public void setDurationMinutes(long v)        { this.durationMinutes = v; }
        public void setTotalReadings(int v)           { this.totalReadings = v; }
        public void setAverageSpeed(double v)         { this.averageSpeed = v; }
        public void setMaxSpeed(int v)                { this.maxSpeed = v; }
        public void setAverageRPM(double v)           { this.averageRPM = v; }
        public void setAverageCoolantTemp(double v)   { this.averageCoolantTemp = v; }
        public void setEstimatedFuelUsed(double v)    { this.estimatedFuelUsed = v; }
        public void setFuelPricePerLitre(double v)    { this.fuelPricePerLitre = v; }
        public void setEstimatedFuelCostBDT(double v) { this.estimatedFuelCostBDT = v; }
        public void setLinkedCSVPath(String v)
        { this.linkedCSVPath = v; }
        public void setDistanceKm(double v)    { this.distanceKm = v; }
        public void setFuelCostPerKm(double v) { this.fuelCostPerKm = v; }

        // Quick summary for debugging
        @Override
        public String toString() {
            return "SessionSummary {" +
                    "\n  startTime          = " + startTime +
                    "\n  endTime            = " + endTime +
                    "\n  durationMinutes    = " + durationMinutes +
                    "\n  averageSpeed       = " + averageSpeed + " km/h" +
                    "\n  maxSpeed           = " + maxSpeed + " km/h" +
                    "\n  estimatedFuelUsed  = " + estimatedFuelUsed + " L" +
                    "\n  estimatedFuelCost  = " + estimatedFuelCostBDT + " BDT" +
                    "\n  linkedCSVPath      = " + linkedCSVPath +
                    // ── ADD these two lines to SessionSummary toString() ──────
                    "\n  distanceKm         = " + distanceKm + " km" +
                    "\n  fuelCostPerKm      = " + fuelCostPerKm + " BDT/km" +
                    "\n}";
        }
    }
}