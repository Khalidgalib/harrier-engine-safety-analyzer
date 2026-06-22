package com.example.project_prototype.obd;

public class OBDParser {

    // ── parseSpeed() ─────────────────────────────────────────
    // Raw response example: "41 0D 32"
    // 41 = response header (always)
    // 0D = confirms speed PID
    // 32 = actual data byte  → 0x32 = 50 km/h
    public int parseSpeed(String raw) {
        try {
            String[] bytes = cleanResponse(raw).split(" ");
            // bytes[0] = "41"  bytes[1] = "0D"  bytes[2] = "32"
            String hexValue = bytes[2];
            return Integer.parseInt(hexValue, 16);

        } catch (Exception e) {
            System.err.println("parseSpeed failed on: " + raw);
            return 0; // safe fallback
        }
    }

    // ── parseRPM() ───────────────────────────────────────────
    // Raw response example: "41 0C 1A F8"
    // 41 = header
    // 0C = confirms RPM PID
    // 1A = byte A
    // F8 = byte B
    // Formula: ((A * 256) + B) / 4 = RPM
    public int parseRPM(String raw) {
        try {
            String[] bytes = cleanResponse(raw).split(" ");
            // bytes[0] = "41"  bytes[1] = "0C"
            // bytes[2] = "1A"  bytes[3] = "F8"
            int A = Integer.parseInt(bytes[2], 16);
            int B = Integer.parseInt(bytes[3], 16);
            return ((A * 256) + B) / 4;

        } catch (Exception e) {
            System.err.println("parseRPM failed on: " + raw);
            return 0; // safe fallback
        }
    }

    // ── parseCoolantTemp() ───────────────────────────────────
    // Raw response example: "41 05 7B"
    // 41 = header
    // 05 = confirms temp PID
    // 7B = actual data byte → 0x7B = 123 → 123 - 40 = 83°C
    // Formula: hex_value - 40 = Celsius
    // Why -40? OBD-II stores temp offset by 40 to allow negative values
    public int parseCoolantTemp(String raw) {
        try {
            String[] bytes = cleanResponse(raw).split(" ");
            // bytes[0] = "41"  bytes[1] = "05"  bytes[2] = "7B"
            int hexValue = Integer.parseInt(bytes[2], 16);
            return hexValue - 40;

        } catch (Exception e) {
            System.err.println("parseCoolantTemp failed on: " + raw);
            return 0; // safe fallback
        }
    }

    // ── parseFuelLevel() ─────────────────────────────────────
    // Raw response example: "41 2F 6E"
    // 41 = header
    // 2F = confirms fuel PID
    // 6E = actual data byte → 0x6E = 110
    // Formula: (hex_value / 255.0) * 100 = percentage
    // 255 = max possible value (100% full tank)
    public int parseFuelLevel(String raw) {
        try {
            String[] bytes = cleanResponse(raw).split(" ");
            // bytes[0] = "41"  bytes[1] = "2F"  bytes[2] = "6E"
            int hexValue = Integer.parseInt(bytes[2], 16);
            return (int) ((hexValue / 255.0) * 100);

        } catch (Exception e) {
            System.err.println("parseFuelLevel failed on: " + raw);
            return 0; // safe fallback
        }
    }

    // ── parseBatteryVoltage() ────────────────────────────────
    // Raw response example: "12.4V"
    // Unlike other PIDs, ATRV returns a readable float string directly
    // No hex conversion needed — just remove "V" and parse as double
    public double parseBatteryVoltage(String raw) {
        try {
            String cleaned = raw
                    .replace("V", "")   // remove the V unit
                    .replace(">", "")   // remove prompt
                    .trim();
            return Double.parseDouble(cleaned);

        } catch (Exception e) {
            System.err.println("parseBatteryVoltage failed on: " + raw);
            return 0.0; // safe fallback
        }
    }

    // ── cleanResponse() ──────────────────────────────────────
    // Every raw response from ELM327 needs cleaning before parsing
    //
    // Raw might look like:  "41 0D 32\r\nOK\r\n>"
    // After cleaning:       "41 0D 32"
    //
    // Removes:
    // '>'       = ELM327 ready prompt
    // 'OK'      = acknowledgement text
    // '\r' '\n' = carriage returns and newlines
    // extra spaces = normalised to single spaces
    public String cleanResponse(String raw) {
        return raw
                .replace(">", "")      // remove prompt character
                .replace("OK", "")     // remove OK text
                .replace("\r", "")     // remove carriage return
                .replace("\n", "")     // remove newline
                .replaceAll("\\s+", " ") // collapse multiple spaces into one
                .trim();               // remove leading/trailing spaces
    }
}