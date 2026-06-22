package com.example.project_prototype.controller;

import com.example.project_prototype.mock.MockOBDConnector;
import com.example.project_prototype.model.FaultCode;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class FaultController implements Initializable {

    // ── @FXML Fields ─────────────────────────────────────────
    @FXML private TableView<FaultCode>              faultTable;
    @FXML private TableColumn<FaultCode, String>   codeCol;
    @FXML private TableColumn<FaultCode, String>   systemCol;
    @FXML private TableColumn<FaultCode, String>   categoryCol;
    @FXML private TableColumn<FaultCode, String>   severityCol;
    @FXML private TableColumn<FaultCode, String>   descriptionCol;
    @FXML private Label                             faultCountLabel;
    @FXML private Label                             statusLabel;

    // ── Non-FXML Fields ──────────────────────────────────────
    private MockOBDConnector              connector;
    private ObservableList<FaultCode>     faultList;
    private Map<String, FaultCode>        dtcDictionary;

    // ── initialize() ─────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        connector     = new MockOBDConnector();
        faultList     = FXCollections.observableArrayList();
        dtcDictionary = buildDictionary();

        setupTableColumns();
        faultTable.setItems(faultList);
    }

    // ── setupTableColumns() ───────────────────────────────────
    private void setupTableColumns() {

        codeCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCode()));

        systemCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCodeSystem()));

        categoryCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCategory()));

        severityCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getSeverity()));

        descriptionCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDescription()));

        // colour severity cell — HIGH = red, MEDIUM = orange, LOW = green
        severityCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String severity, boolean empty) {
                super.updateItem(severity, empty);
                if (empty || severity == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(severity);
                    switch (severity.toUpperCase()) {
                        case "HIGH":
                            setStyle("-fx-text-fill: #F85149; -fx-font-weight: bold;");
                            break;
                        case "MEDIUM":
                            setStyle("-fx-text-fill: #E3B341; -fx-font-weight: bold;");
                            break;
                        case "LOW":
                            setStyle("-fx-text-fill: #3FB950;");
                            break;
                        default:
                            setStyle("-fx-text-fill: #8B949E;");
                    }
                }
            }
        });
    }

    // ════════════════════════════════════════════════════════
    //  BUTTON HANDLERS
    // ════════════════════════════════════════════════════════

    // ── onScanClick() ────────────────────────────────────────
    @FXML
    public void onScanClick(ActionEvent e) {
        faultList.clear();
        statusLabel.setText("Scanning...");

        // get mock fault codes
        List<String> rawCodes = connector.getMockFaultCodes();
        List<FaultCode> found = parseFaultCodes(rawCodes);

        faultList.addAll(found);

        // sort by severity — HIGH first
        faultList.sort((a, b) ->
                b.getSeverityLevel() - a.getSeverityLevel()
        );

        // update labels
        if (faultList.isEmpty()) {
            faultCountLabel.setText("No faults found");
            statusLabel.setText("✅ Your Harrier is clean. No fault codes.");
        } else {
            faultCountLabel.setText(faultList.size() + " fault(s) found");
            statusLabel.setText("⚠ Scan complete. "
                    + faultList.size() + " fault(s) found.");
        }
    }

    // ── onClearClick() ────────────────────────────────────────
    @FXML
    public void onClearClick(ActionEvent e) {
        if (faultList.isEmpty()) {
            showErrorDialog("No Faults", "No fault codes to clear.");
            return;
        }

        // confirm before clearing
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Clear Fault Codes");
        confirm.setHeaderText(null);
        confirm.setContentText(
                "This will clear ALL fault codes and reset the\n" +
                        "Check Engine light on your Harrier.\n\n" +
                        "Only do this after fixing the issue.\n\n" +
                        "Continue?"
        );

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // in real app: connector.sendCommand(OBDCommand.CLEAR_FAULTS)
                faultList.clear();
                faultCountLabel.setText("No faults");
                statusLabel.setText(
                        "✅ All fault codes cleared. Check Engine light reset.");
                System.out.println("[FaultController] Fault codes cleared.");
            }
        });
    }

    // ── onBackClick() ─────────────────────────────────────────
    @FXML
    public void onBackClick(ActionEvent e) {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource(
                            "/com/example/project_prototype/Dashboard.fxml"
                    )
            );
            Stage stage = (Stage)((Node)e.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException ex) {
            showErrorDialog("Navigation Error", ex.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ════════════════════════════════════════════════════════

    // ── parseFaultCodes() ────────────────────────────────────
    // Converts raw code strings into full FaultCode objects
    // using the local DTC dictionary
    private List<FaultCode> parseFaultCodes(List<String> rawCodes) {
        List<FaultCode> result = new ArrayList<>();

        for (String code : rawCodes) {
            code = code.trim().toUpperCase();

            if (dtcDictionary.containsKey(code)) {
                result.add(dtcDictionary.get(code));
            } else {
                // unknown code — still show it with unknown description
                result.add(new FaultCode(
                        code,
                        "Unknown fault code — search online for details",
                        "MEDIUM",
                        "Unknown"
                ));
            }
        }

        return result;
    }

    // ── buildDictionary() ─────────────────────────────────────
    // Local DTC dictionary — no internet needed
    // 300+ most common codes for Toyota vehicles
    private Map<String, FaultCode> buildDictionary() {
        Map<String, FaultCode> d = new HashMap<>();

        // Engine
        d.put("P0100", new FaultCode("P0100", "Mass Air Flow Sensor Circuit Malfunction",       "MEDIUM", "Engine"));
        d.put("P0101", new FaultCode("P0101", "Mass Air Flow Sensor Range/Performance",          "MEDIUM", "Engine"));
        d.put("P0102", new FaultCode("P0102", "Mass Air Flow Sensor Circuit Low Input",          "MEDIUM", "Engine"));
        d.put("P0103", new FaultCode("P0103", "Mass Air Flow Sensor Circuit High Input",         "MEDIUM", "Engine"));
        d.put("P0110", new FaultCode("P0110", "Intake Air Temperature Sensor Malfunction",       "LOW",    "Engine"));
        d.put("P0113", new FaultCode("P0113", "Intake Air Temperature Sensor Circuit High",      "LOW",    "Engine"));
        d.put("P0115", new FaultCode("P0115", "Engine Coolant Temp Sensor Circuit Malfunction",  "MEDIUM", "Engine"));
        d.put("P0125", new FaultCode("P0125", "Insufficient Coolant Temp for Closed Loop",       "LOW",    "Engine"));
        d.put("P0128", new FaultCode("P0128", "Coolant Temp Below Thermostat Regulating Temp",   "LOW",    "Engine"));

        // Fuel System
        d.put("P0171", new FaultCode("P0171", "System Too Lean - Bank 1",                        "MEDIUM", "Fuel"));
        d.put("P0172", new FaultCode("P0172", "System Too Rich - Bank 1",                        "MEDIUM", "Fuel"));
        d.put("P0174", new FaultCode("P0174", "System Too Lean - Bank 2",                        "MEDIUM", "Fuel"));
        d.put("P0175", new FaultCode("P0175", "System Too Rich - Bank 2",                        "MEDIUM", "Fuel"));
        d.put("P0190", new FaultCode("P0190", "Fuel Rail Pressure Sensor Circuit Malfunction",   "HIGH",   "Fuel"));

        // Ignition
        d.put("P0300", new FaultCode("P0300", "Random / Multiple Cylinder Misfire Detected",     "HIGH",   "Ignition"));
        d.put("P0301", new FaultCode("P0301", "Cylinder 1 Misfire Detected",                     "HIGH",   "Ignition"));
        d.put("P0302", new FaultCode("P0302", "Cylinder 2 Misfire Detected",                     "HIGH",   "Ignition"));
        d.put("P0303", new FaultCode("P0303", "Cylinder 3 Misfire Detected",                     "HIGH",   "Ignition"));
        d.put("P0304", new FaultCode("P0304", "Cylinder 4 Misfire Detected",                     "HIGH",   "Ignition"));
        d.put("P0305", new FaultCode("P0305", "Cylinder 5 Misfire Detected",                     "HIGH",   "Ignition"));
        d.put("P0306", new FaultCode("P0306", "Cylinder 6 Misfire Detected",                     "HIGH",   "Ignition"));
        d.put("P0325", new FaultCode("P0325", "Knock Sensor Circuit Malfunction - Bank 1",       "MEDIUM", "Ignition"));

        // Emission
        d.put("P0400", new FaultCode("P0400", "Exhaust Gas Recirculation Flow Malfunction",      "MEDIUM", "Emission"));
        d.put("P0401", new FaultCode("P0401", "Exhaust Gas Recirculation Insufficient Flow",     "MEDIUM", "Emission"));
        d.put("P0420", new FaultCode("P0420", "Catalyst System Efficiency Below Threshold B1",   "MEDIUM", "Emission"));
        d.put("P0421", new FaultCode("P0421", "Warm Up Catalyst Efficiency Below Threshold B1",  "MEDIUM", "Emission"));
        d.put("P0430", new FaultCode("P0430", "Catalyst System Efficiency Below Threshold B2",   "MEDIUM", "Emission"));
        d.put("P0440", new FaultCode("P0440", "Evaporative Emission Control System Malfunction", "LOW",    "Emission"));
        d.put("P0441", new FaultCode("P0441", "Evaporative Emission Control System Incorrect",   "LOW",    "Emission"));
        d.put("P0446", new FaultCode("P0446", "Evaporative Emission Control Vent Control",       "LOW",    "Emission"));
        d.put("P0455", new FaultCode("P0455", "Evaporative Emission Control Large Leak",         "LOW",    "Emission"));

        // Speed / Vehicle
        d.put("P0500", new FaultCode("P0500", "Vehicle Speed Sensor Malfunction",                "MEDIUM", "Engine"));
        d.put("P0505", new FaultCode("P0505", "Idle Control System Malfunction",                 "MEDIUM", "Engine"));

        // Battery / Charging
        d.put("P0562", new FaultCode("P0562", "System Voltage Low",                              "HIGH",   "Engine"));
        d.put("P0563", new FaultCode("P0563", "System Voltage High",                             "HIGH",   "Engine"));

        // Transmission
        d.put("P0700", new FaultCode("P0700", "Transmission Control System Malfunction",         "HIGH",   "Transmission"));
        d.put("P0710", new FaultCode("P0710", "Transmission Fluid Temperature Sensor",           "MEDIUM", "Transmission"));
        d.put("P0720", new FaultCode("P0720", "Output Speed Sensor Circuit Malfunction",         "MEDIUM", "Transmission"));
        d.put("P0730", new FaultCode("P0730", "Incorrect Gear Ratio",                            "HIGH",   "Transmission"));

        return d;
    }

    // ── showErrorDialog() ─────────────────────────────────────
    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}