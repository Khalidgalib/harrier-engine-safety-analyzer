package com.example.project_prototype.controller;

import com.example.project_prototype.mock.MockOBDConnector;
import com.example.project_prototype.model.CarData;
import com.example.project_prototype.model.TripSession;
import com.example.project_prototype.logger.CSVLogger;
import com.example.project_prototype.logger.SessionLogger;
import com.example.project_prototype.exception.ConnectionException;
import com.example.project_prototype.exception.OBDTimeoutException;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    // ── @FXML Fields — must match fx:id in Dashboard.fxml ───
    @FXML private Label      statusLabel;
    @FXML private Label      speedLabel;
    @FXML private ProgressBar speedBar;
    @FXML private Label      rpmLabel;
    @FXML private ProgressBar rpmBar;
    @FXML private Label      tempLabel;
    @FXML private ProgressBar tempBar;
    @FXML private Label      fuelLabel;
    @FXML private ProgressBar fuelBar;
    @FXML private Label      voltLabel;
    @FXML private ProgressBar voltBar;
    @FXML private Label      faultSummaryLabel;
    @FXML private Label      faultCodesLabel;
    @FXML private Label      durationLabel;
    @FXML private Label      fuelCostLabel;
    // ── ADD these two new @FXML fields ───────────────────────
    @FXML private Label distanceLabel;
    @FXML private Label costPerKmLabel;
    @FXML private TextField  fuelPriceField;
    @FXML private Button     connectBtn;
    @FXML private Button     disconnectBtn;
    @FXML private Button     startTripBtn;
    @FXML private Button     stopTripBtn;
    @FXML private Button     scanFaultsBtn;

    // ── Non-FXML Fields ──────────────────────────────────────
    private MockOBDConnector connector;   // swap to OBDConnector when adapter arrives
    private Timeline         dataTimeline;// polls OBD data every 1 second
    private Timeline         clockTimeline;// updates trip duration every 1 second
    private TripSession      currentTrip; // current active trip
    private CSVLogger        csvLogger;   // logs data to CSV file
    private SessionLogger    sessionLogger;// saves trip summary as JSON
    private int              tripSeconds; // counts seconds for duration display

    // ── initialize() ─────────────────────────────────────────
    // JavaFX calls this automatically when Dashboard.fxml loads
    // Set up initial state of all UI elements here
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        connector     = new MockOBDConnector();
        sessionLogger = new SessionLogger("sessions");
        tripSeconds   = 0;

        // buttons start in correct disabled/enabled state
        setConnectedState(false);

        System.out.println("DashboardController initialized.");
    }

    // ════════════════════════════════════════════════════════
    //  BUTTON HANDLERS — @FXML methods called from Dashboard.fxml
    // ════════════════════════════════════════════════════════

    // ── onConnectClick() ─────────────────────────────────────
    // User clicks "Connect" button
    @FXML
    public void onConnectClick(ActionEvent e) {

        // run connection on background thread
        // so UI does not freeze during connection delay
        Thread connectThread = new Thread(() -> {
            try {
                connector.connect(); // takes ~800ms (simulated delay)

                // back on JavaFX thread — update UI
                Platform.runLater(() -> {
                    setConnectedState(true);
                    startDataTimeline(); // begin polling every 1 second
                    showInfo("Connected to ELM327 successfully.");
                });

            } catch (ConnectionException ex) {
                Platform.runLater(() ->
                        showErrorDialog("Connection Failed", ex.getMessage())
                );
            }
        });

        connectThread.setDaemon(true);
        connectThread.start();
    }

    // ── onDisconnectClick() ───────────────────────────────────
    // User clicks "Disconnect" button
    @FXML
    public void onDisconnectClick(ActionEvent e) {

        // if trip is still running — stop it first
        if (currentTrip != null && currentTrip.isOngoing()) {
            stopTrip();
        }

        stopDataTimeline();
        connector.disconnect();
        setConnectedState(false);
        resetGauges();
        showInfo("Disconnected.");
    }

    // ── onStartTripClick() ────────────────────────────────────
    // User clicks "Start Trip" button
    @FXML
    public void onStartTripClick(ActionEvent e) {

        // read fuel price from TextField
        double fuelPrice = 114.0; // default
        try {
            fuelPrice = Double.parseDouble(fuelPriceField.getText().trim());
        } catch (NumberFormatException ex) {
            showErrorDialog("Invalid Input",
                    "Please enter a valid fuel price.\nUsing default: 114 BDT/L");
        }

        // create new trip session
        currentTrip = new TripSession(fuelPrice);
        tripSeconds  = 0;

        // open CSV logger
        String csvPath = CSVLogger.generateFileName();
        csvLogger = new CSVLogger(csvPath);
        try {
            csvLogger.open();
        } catch (IOException ex) {
            showErrorDialog("File Error",
                    "Could not create log file:\n" + ex.getMessage());
            return;
        }

        // start trip clock
        startClockTimeline();

        // update button states
        startTripBtn.setDisable(true);
        stopTripBtn.setDisable(false);

        showInfo("Trip started. Logging to: " + csvPath);
    }

    // ── onStopTripClick() ─────────────────────────────────────
    // User clicks "Stop Trip" button
    @FXML
    public void onStopTripClick(ActionEvent e) {
        stopTrip();
    }

    // ── onScanFaultsClick() ───────────────────────────────────
    // User clicks "Scan Faults" — navigate to FaultView screen
    @FXML
    public void onScanFaultsClick(ActionEvent e) {
        switchScene(e, "FaultView.fxml");
    }

    // ── onViewLogsClick() ─────────────────────────────────────
    // User clicks "View Logs" — navigate to LogView screen
    @FXML
    public void onViewLogsClick(ActionEvent e) {
        switchScene(e, "LogView.fxml");
    }

    // ════════════════════════════════════════════════════════
    //  TIMELINE METHODS
    // ════════════════════════════════════════════════════════

    // ── startDataTimeline() ───────────────────────────────────
    // Fetches OBD data every 1 second and updates UI
    private void startDataTimeline() {
        dataTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    try {
                        // fetch one full snapshot of car data
                        CarData data = connector.fetchAllData();

                        // update all gauges on JavaFX thread
                        Platform.runLater(() -> updateUI(data));

                        // log to CSV if trip is active
                        if (currentTrip != null && currentTrip.isOngoing()) {
                            currentTrip.addReading(data);
                            //new add//
                            currentTrip.addDistanceFromSpeed(data.getSpeed());
                            // ──────────────────────────────────────────
                            try {
                                csvLogger.log(data);
                            } catch (IOException ex) {
                                System.err.println("CSV log error: " + ex.getMessage());
                            }
                        }

                    } catch (OBDTimeoutException ex) {
                        Platform.runLater(() -> {
                            showErrorDialog("Connection Lost",
                                    ex.getMessage() + "\nReconnect the adapter.");
                            stopDataTimeline();
                            setConnectedState(false);
                        });
                    }
                })
        );

        dataTimeline.setCycleCount(Timeline.INDEFINITE);
        dataTimeline.play();
    }

    // ── stopDataTimeline() ────────────────────────────────────
    private void stopDataTimeline() {
        if (dataTimeline != null) {
            dataTimeline.stop();
            dataTimeline = null;
        }
    }

    // ── startClockTimeline() ──────────────────────────────────
    // Updates the trip duration label every 1 second
    private void startClockTimeline() {
        clockTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    tripSeconds++;
                    int hours   = tripSeconds / 3600;
                    int minutes = (tripSeconds % 3600) / 60;
                    int seconds = tripSeconds % 60;

                    // format as HH:MM:SS
                    durationLabel.setText(
                            String.format("%02d:%02d:%02d", hours, minutes, seconds)
                    );
                })
        );

        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();
    }

    // ── stopClockTimeline() ───────────────────────────────────
    private void stopClockTimeline() {
        if (clockTimeline != null) {
            clockTimeline.stop();
            clockTimeline = null;
        }
    }

    // ════════════════════════════════════════════════════════
    //  UI UPDATE METHODS
    // ════════════════════════════════════════════════════════

    // ── updateUI() ───────────────────────────────────────────
    // Pushes one CarData snapshot to all gauge labels and bars
    // Always called via Platform.runLater() — never from background thread
    private void updateUI(CarData data) {

        // Speed — max 120 km/h
        speedLabel.setText(data.getSpeed() + " km/h");
        speedBar.setProgress(data.getSpeed() / 120.0);

        // RPM — max 6000 rpm
        rpmLabel.setText(data.getRpm() + " rpm");
        rpmBar.setProgress(data.getRpm() / 6000.0);

        // Coolant Temp — 40°C min, 120°C max
        tempLabel.setText(data.getCoolantTemp() + " °C");
        tempBar.setProgress((data.getCoolantTemp() - 40.0) / 80.0);

        // Fuel Level — 0 to 100%
        fuelLabel.setText(data.getFuelLevel() + " %");
        fuelBar.setProgress(data.getFuelLevel() / 100.0);

        // Battery Voltage — 11V min, 15V max
        voltLabel.setText(data.getBatteryVoltage() + " V");
        voltBar.setProgress((data.getBatteryVoltage() - 11.0) / 4.0);

        // Fuel cost — only update if trip is active
        // ── ADD at the bottom of updateUI() ──────────────────────
        if (currentTrip != null && currentTrip.isOngoing()) {
            double cost    = currentTrip.getEstimatedFuelCostBDT();
            double km      = currentTrip.getDistanceKm();
            double perKm   = currentTrip.getFuelCostPerKm();

            fuelCostLabel.setText(String.format("%.2f BDT", cost));
            distanceLabel.setText(String.format("%.2f km", km));
            costPerKmLabel.setText(String.format("%.2f BDT/km", perKm));
        }
        if (currentTrip != null && currentTrip.isOngoing()) {
            double cost = currentTrip.getEstimatedFuelCostBDT();
            fuelCostLabel.setText(String.format("%.2f BDT", cost));
        }

        // temperature warning — turn label red if overheating
        if (data.getCoolantTemp() > 100) {
            tempLabel.setStyle(tempLabel.getStyle()
                    .replace("#FFA657", "#F85149")); // red alert
        }
    }

    // ── setConnectedState() ───────────────────────────────────
    // Enables/disables buttons based on connection state
    private void setConnectedState(boolean connected) {
        if (connected) {
            // connected — update status label to green
            statusLabel.setText("● Connected");
            statusLabel.setStyle(
                    "-fx-text-fill: #3FB950;" +
                            "-fx-font-size: 12px;" +
                            "-fx-background-color: #1A2E1A;" +
                            "-fx-border-color: #3FB950;" +
                            "-fx-border-radius: 20;" +
                            "-fx-background-radius: 20;" +
                            "-fx-padding: 4 12 4 12;"
            );
            connectBtn.setDisable(true);
            disconnectBtn.setDisable(false);
            startTripBtn.setDisable(false);
            scanFaultsBtn.setDisable(false);

        } else {
            // disconnected — update status label to red
            statusLabel.setText("● Disconnected");
            statusLabel.setStyle(
                    "-fx-text-fill: #F85149;" +
                            "-fx-font-size: 12px;" +
                            "-fx-background-color: #2D1A1A;" +
                            "-fx-border-color: #F85149;" +
                            "-fx-border-radius: 20;" +
                            "-fx-background-radius: 20;" +
                            "-fx-padding: 4 12 4 12;"
            );
            connectBtn.setDisable(false);
            disconnectBtn.setDisable(true);
            startTripBtn.setDisable(true);
            stopTripBtn.setDisable(true);
            scanFaultsBtn.setDisable(true);
        }
    }

    // ── resetGauges() ─────────────────────────────────────────
    // Resets all gauges to zero after disconnect
    private void resetGauges() {
        speedLabel.setText("0 km/h");
        speedBar.setProgress(0);
        rpmLabel.setText("0 rpm");
        rpmBar.setProgress(0);
        tempLabel.setText("0 °C");
        tempBar.setProgress(0);
        fuelLabel.setText("0 %");
        fuelBar.setProgress(0);
        voltLabel.setText("0.0 V");
        voltBar.setProgress(0);
        durationLabel.setText("00:00:00");
        fuelCostLabel.setText("0.00 BDT");
        // ── ADD inside resetGauges() ──────────────────────────────
        distanceLabel.setText("0.00 km");
        costPerKmLabel.setText("0.00 BDT/km");
    }

    // ════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ════════════════════════════════════════════════════════

    // ── stopTrip() ───────────────────────────────────────────
    // Stops an active trip — saves session and closes CSV
    private void stopTrip() {
        if (currentTrip == null) return;

        currentTrip.endTrip();
        stopClockTimeline();
        csvLogger.close();

        // save session summary as JSON
        try {
            sessionLogger.saveSession(currentTrip, csvLogger.getFilePath());
            // ── REPLACE existing showInfo line in stopTrip() ──────────
            showInfo("Trip saved."
                    + " Distance: "  + String.format("%.2f", currentTrip.getDistanceKm()) + " km"
                    + " | Duration: " + currentTrip.getTripDurationMinutes() + " min"
                    + " | Cost: "     + String.format("%.2f", currentTrip.getEstimatedFuelCostBDT()) + " BDT"
                    + " | Per km: "   + String.format("%.2f", currentTrip.getFuelCostPerKm()) + " BDT/km"
            );
        } catch (IOException ex) {
            showErrorDialog("Save Error",
                    "Could not save session:\n" + ex.getMessage());
        }

        // update button states
        startTripBtn.setDisable(false);
        stopTripBtn.setDisable(true);

        currentTrip = null;
    }

    // ── switchScene() ─────────────────────────────────────────
    // Navigates to another screen
    private void switchScene(ActionEvent e, String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource(
                            "/com/example/project_prototype/" + fxmlFile
                    )
            );
            Stage stage = (Stage)((Node)e.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException ex) {
            showErrorDialog("Navigation Error",
                    "Could not load: " + fxmlFile + "\n" + ex.getMessage());
        }
    }

    // ── showErrorDialog() ─────────────────────────────────────
    // Shows a red error popup to the user
    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ── showInfo() ────────────────────────────────────────────
    // Prints info message to IntelliJ console
    private void showInfo(String message) {
        System.out.println("[Dashboard] " + message);
    }
}