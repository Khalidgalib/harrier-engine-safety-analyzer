package com.example.project_prototype.logger;

import com.example.project_prototype.model.CarData;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;

public class CSVLogger {

    // ── Fields ───────────────────────────────────────────────
    private final String filePath;   // where the CSV file is saved on disk
    private BufferedWriter writer;   // writes to the file
    private boolean isOpen;          // tracks if logger is currently active

    // ── Constructor ──────────────────────────────────────────
    // filePath example: "logs/trip_2024-03-15.csv"
    public CSVLogger(String filePath) {
        this.filePath = filePath;
        this.isOpen   = false;
    }

    // ── open() ───────────────────────────────────────────────
    // Creates the file and writes the header row
    // Called ONCE when user clicks "Start Trip"
    //
    // new FileWriter(filePath, true) ← second argument 'true' = append mode
    // append mode = if file already exists, add to it instead of overwriting
    public void open() throws IOException {

        // create logs folder if it doesn't exist yet
        File logFolder = new File("logs");
        if (!logFolder.exists()) {
            logFolder.mkdirs(); // creates the folder
        }

        // open the file in append mode
        writer = new BufferedWriter(new FileWriter(filePath, true));

        // write column headers as first line
        writer.write(CarData.getCSVHeader());
        writer.newLine();
        writer.flush(); // push header to disk immediately

        isOpen = true;
        System.out.println("CSVLogger opened: " + filePath);
    }

    // ── log() ────────────────────────────────────────────────
    // Writes one CarData row to the CSV file
    // Called EVERY SECOND from DashboardController Timeline
    //
    // Example line written:
    // "2024-03-15 14:32:01, 60, 2500, 88, 65, 12.4"
    public void log(CarData data) throws IOException {
        if (!isOpen) {
            System.err.println("CSVLogger is not open. Call open() first.");
            return;
        }

        writer.write(data.toCSVRow()); // CarData converts itself to CSV string
        writer.newLine();              // move to next line
        writer.flush();               // push to disk immediately — never lose data
    }

    // ── close() ──────────────────────────────────────────────
    // Closes the file properly
    // Called when user clicks "Stop Trip" or app closes
    //
    // Always close in finally block — never leave file open
    public void close() {
        if (writer != null) {
            try {
                writer.flush(); // write anything still in buffer
                writer.close(); // close the file
                isOpen = false;
                System.out.println("CSVLogger closed: " + filePath);

            } catch (IOException e) {
                System.err.println("Error closing CSVLogger: " + e.getMessage());
            }
        }
    }

    // ── isOpen() ─────────────────────────────────────────────
    // Check before logging — prevents crash if log() called too early
    public boolean isOpen() {
        return isOpen;
    }

    // ── getFilePath() ─────────────────────────────────────────
    // Returns the file path — useful for showing user where file was saved
    public String getFilePath() {
        return filePath;
    }

    // ── generateFileName() ────────────────────────────────────
    // Static helper — generates a unique filename using current date and time
    // So each trip gets its own file automatically
    //
    // Example output: "logs/trip_2024-03-15_14-32-01.csv"
    public static String generateFileName() {
        String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        return "logs/trip_" + timestamp + ".csv";
    }
}