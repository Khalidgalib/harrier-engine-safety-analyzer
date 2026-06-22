package com.example.project_prototype.controller;

import com.example.project_prototype.logger.SessionLogger;
import com.example.project_prototype.logger.SessionLogger.SessionSummary;

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
import java.util.List;
import java.util.ResourceBundle;

public class LogViewController implements Initializable {

    // ── @FXML Fields ─────────────────────────────────────────
    @FXML private TableView<SessionSummary>         tripTable;
    @FXML private TableColumn<SessionSummary, String> startTimeCol;
    @FXML private TableColumn<SessionSummary, String> durationCol;
    @FXML private TableColumn<SessionSummary, String> avgSpeedCol;
    @FXML private TableColumn<SessionSummary, String> maxSpeedCol;
    @FXML private TableColumn<SessionSummary, String> fuelUsedCol;
    @FXML private TableColumn<SessionSummary, String> fuelCostCol;
    @FXML private TableColumn<SessionSummary, String> avgTempCol;
    @FXML private Label tripCountLabel;
    @FXML private Label totalTripsLabel;
    @FXML private Label totalCostLabel;
    @FXML private Label totalFuelLabel;
    // ── ADD these two fields ──────────────────────────────────
    @FXML private TableColumn<SessionSummary, String> distanceCol;
    @FXML private TableColumn<SessionSummary, String> costPerKmCol;

    // ── Non-FXML Fields ──────────────────────────────────────
    private SessionLogger sessionLogger;
    private ObservableList<SessionSummary> sessionList;

    // ── initialize() ─────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        sessionLogger = new SessionLogger("sessions");
        sessionList   = FXCollections.observableArrayList();

        setupTableColumns();
        loadAllSessions();
    }

    // ── setupTableColumns() ───────────────────────────────────
    // Tells each column which field to display from SessionSummary
    // Uses SimpleStringProperty — converts any value to String for display
    private void setupTableColumns() {

        startTimeCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStartTime()));

        durationCol.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getDurationMinutes() + " min"));

        avgSpeedCol.setCellValueFactory(data ->
                new SimpleStringProperty(
                        String.format("%.1f km/h",
                                data.getValue().getAverageSpeed())));

        maxSpeedCol.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getMaxSpeed() + " km/h"));

        fuelUsedCol.setCellValueFactory(data ->
                new SimpleStringProperty(
                        String.format("%.2f L",
                                data.getValue().getEstimatedFuelUsed())));

        fuelCostCol.setCellValueFactory(data ->
                new SimpleStringProperty(
                        String.format("%.2f",
                                data.getValue().getEstimatedFuelCostBDT())));

        avgTempCol.setCellValueFactory(data ->
                new SimpleStringProperty(
                        String.format("%.1f °C",
                                data.getValue().getAverageCoolantTemp())));
        // ── ADD these two inside setupTableColumns() ──────────────
        distanceCol.setCellValueFactory(data ->
                new SimpleStringProperty(
                        String.format("%.2f km",
                                data.getValue().getDistanceKm())));

        costPerKmCol.setCellValueFactory(data ->
                new SimpleStringProperty(
                        String.format("%.2f",
                                data.getValue().getFuelCostPerKm())));
    }

    // ── loadAllSessions() ─────────────────────────────────────
    // Loads all saved JSON sessions and populates the table
    private void loadAllSessions() {
        sessionList.clear();

        List<SessionSummary> sessions = sessionLogger.listAllSessions();
        sessionList.addAll(sessions);
        tripTable.setItems(sessionList);

        updateSummaryCards();
    }

    // ── updateSummaryCards() ──────────────────────────────────
    // Updates the total trips, cost, fuel labels at the bottom
    private void updateSummaryCards() {
        int    totalTrips = sessionList.size();
        double totalCost  = 0;
        double totalFuel  = 0;

        for (SessionSummary s : sessionList) {
            totalCost += s.getEstimatedFuelCostBDT();
            totalFuel += s.getEstimatedFuelUsed();
        }

        tripCountLabel.setText(totalTrips + " trips");
        totalTripsLabel.setText(String.valueOf(totalTrips));
        totalCostLabel.setText(String.format("%.2f BDT", totalCost));
        totalFuelLabel.setText(String.format("%.2f L", totalFuel));
    }

    // ════════════════════════════════════════════════════════
    //  BUTTON HANDLERS
    // ════════════════════════════════════════════════════════

    // ── onDeleteClick() ───────────────────────────────────────
    @FXML
    public void onDeleteClick(ActionEvent e) {
        SessionSummary selected =
                tripTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showErrorDialog("No Selection",
                    "Please select a trip to delete.");
            return;
        }

        // confirm before deleting
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Trip");
        confirm.setHeaderText(null);
        confirm.setContentText(
                "Delete trip from " + selected.getStartTime() + "?\n" +
                        "This cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean deleted =
                        sessionLogger.deleteSession(selected.getFilePath());

                if (deleted) {
                    sessionList.remove(selected);
                    updateSummaryCards();
                } else {
                    showErrorDialog("Delete Failed",
                            "Could not delete the selected trip.");
                }
            }
        });
    }

    // ── onRefreshClick() ──────────────────────────────────────
    @FXML
    public void onRefreshClick(ActionEvent e) {
        loadAllSessions();
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

    // ── showErrorDialog() ─────────────────────────────────────
    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}