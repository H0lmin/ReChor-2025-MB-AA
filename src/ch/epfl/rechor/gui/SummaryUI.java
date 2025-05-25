package ch.epfl.rechor.gui;

import ch.epfl.rechor.FormatterFr;
import ch.epfl.rechor.journey.Journey;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * UI component that displays a list of journeys and exposes the currently selected journey.
 *
 * @param rootNode  the JavaFX node containing the journey list view
 * @param selectedJourneyO   observable providing the selected {@link Journey}
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public record SummaryUI(Node rootNode, ObservableValue<Journey> selectedJourneyO) {
    private static final double DISK_RADIUS = 3.0;
    private static final double LINE_PADDING = 5.0;
    private static final String STYLE_SHEET = "summary.css";

    /**
     * Creates a SummaryUI bound to the given journeys and departure-time observables.
     *
     * @param journeysObservable        source of journey lists to display
     * @param depTimeObservable   source of desired departure times
     * @return a new SummaryUI record
     */
    public static SummaryUI create(ObservableValue<List<Journey>> journeysObservable,
                                   ObservableValue<LocalTime> depTimeObservable) {
        ListView<Journey> journeyListView = new ListView<>();
        journeyListView.getStylesheets().add(STYLE_SHEET);

        ObservableList<Journey> journeyItems = FXCollections.observableArrayList();
        journeyListView.setItems(journeyItems);
        journeyListView.setCellFactory(view -> new JourneyCell());

        Runnable selectNearestJourney = () -> {
            LocalTime desired = depTimeObservable.getValue();
            Journey nearest = journeyItems.stream()
                    .filter(item -> !item.depTime().toLocalTime().isBefore(desired))
                    .findFirst()
                    .orElseGet(() -> journeyItems.isEmpty() ? null : journeyItems.getLast());

            if (nearest != null) {
                journeyListView.getSelectionModel().select(nearest);
                Platform.runLater(() -> {
                    int index = journeyListView.getSelectionModel().getSelectedIndex();
                    if (index >= 0) {
                        journeyListView.scrollTo(index);
                    }
                });
            } else {
                journeyListView.getSelectionModel().clearSelection();
            }
        };

        journeysObservable.subscribe(updatedJourneys -> {
            journeyItems.setAll(updatedJourneys);
            selectNearestJourney.run();
        });
        depTimeObservable.subscribe(newTime -> selectNearestJourney.run());

        return new SummaryUI(journeyListView,
                journeyListView.getSelectionModel().selectedItemProperty()
        );
    }

    /**
     * Renders each Journey as a cell with route, times, duration, and a timeline.
     */
    private static class JourneyCell extends ListCell<Journey> {
        private final ImageView iconView = new ImageView();
        private final Label routeLabel = new Label();
        private final Label depTimeLabel = new Label();
        private final Label arrTimeLabel = new Label();
        private final Label durationLabel = new Label();

        private final Pane timelinePane;
        private final VBox cellBox;

        private List<Circle> timelineMarkers = List.of();
        private long totalJourneyMinutes;

        /**
         * Initializes the visual components and layout for the cell.
         */
        JourneyCell() {
            timelinePane = createTimelinePane();
            timelinePane.setPrefSize(0, 0);
            HBox.setHgrow(timelinePane, Priority.ALWAYS);

            HBox routeRow = new HBox(iconView, routeLabel);
            routeRow.getStyleClass().add("route");
            routeRow.setAlignment(Pos.CENTER_LEFT);

            HBox timeRow = new HBox(depTimeLabel, timelinePane, arrTimeLabel);
            timeRow.setAlignment(Pos.CENTER_LEFT);

            HBox durationRow = new HBox(durationLabel);
            durationRow.setAlignment(Pos.CENTER);

            cellBox = new VBox(routeRow, timeRow, durationRow);
            cellBox.getStyleClass().add("journey");

            depTimeLabel.getStyleClass().add("departure");
            durationLabel.getStyleClass().add("duration");

            setGraphic(cellBox);
        }

        /**
         * Updates the cell's content when the item or its empty state changes.
         *
         * @param journey the new Journey to display, or null if empty
         * @param empty   whether this cell should represent no data
         */
        @Override
        protected void updateItem(Journey journey, boolean empty) {
            super.updateItem(journey, empty);
            if (empty || journey == null) {
                setGraphic(null);
                return;
            }
            updateTransport(journey);
            updateTimeDisplay(journey);
            updateDisks(journey);
            setGraphic(cellBox);
        }

        /**
         * Applies visual styling when the cell's selected state changes.
         *
         * @param selected true if the cell is selected, false otherwise
         */
        @Override
        public void updateSelected(boolean selected) {
            super.updateSelected(selected);
            if (selected) {
                routeLabel.setTextFill(Color.BLACK);
                depTimeLabel.setTextFill(Color.BLACK);
                arrTimeLabel.setTextFill(Color.BLACK);
                durationLabel.setTextFill(Color.BLACK);
            }
        }

        private Pane createTimelinePane() {
            return new Pane() {
                @Override
                protected void layoutChildren() {
                    super.layoutChildren();
                    getChildren().clear();
                    double width = getWidth();
                    double midY = getHeight() / 2;
                    getChildren().add(new Line(LINE_PADDING, midY,
                            width - LINE_PADDING, midY));
                    for (Circle marker : timelineMarkers) {
                        double offset = (double) marker.getUserData();
                        double xPos = LINE_PADDING +
                                (offset / totalJourneyMinutes) * (width - 2 * LINE_PADDING);
                        marker.setCenterX(xPos);
                        marker.setCenterY(midY);
                        getChildren().add(marker);
                    }
                }
            };
        }

        private void updateTransport(Journey journey) {
            Optional<Journey.Leg.Transport> transportLeg = journey.legs().stream()
                    .filter(leg -> leg instanceof Journey.Leg.Transport)
                    .map(leg -> (Journey.Leg.Transport) leg)
                    .findFirst();

            if (transportLeg.isPresent()) {
                Journey.Leg.Transport leg = transportLeg.get();
                Image icon = VehicleIcons.iconFor(leg.vehicle());
                iconView.setImage(icon);
                iconView.setFitWidth(20);
                iconView.setPreserveRatio(true);
                routeLabel.setText(
                        FormatterFr.formatRouteDestination(leg)
                );
            } else {
                iconView.setImage(null);
                routeLabel.setText("");
            }
        }

        private void updateTimeDisplay(Journey journey) {
            depTimeLabel.setText(FormatterFr.formatTime(journey.depTime()));
            arrTimeLabel.setText(FormatterFr.formatTime(journey.arrTime()));
            durationLabel.setText(FormatterFr.formatDuration(journey.duration()));
        }

        private void updateDisks(Journey journey) {
            List<Circle> newMarkers = new ArrayList<>();
            totalJourneyMinutes = journey.duration().toMinutes();

            newMarkers.add(createDisk("dep-arr", 0));
            List<Journey.Leg> legs = journey.legs();
            for (int i = 1; i < legs.size() - 1; i++) {
                Journey.Leg previousLeg = legs.get(i - 1);
                Journey.Leg currentLeg = legs.get(i);
                Journey.Leg nextLeg = legs.get(i + 1);
                if (currentLeg instanceof Journey.Leg.Foot
                        && previousLeg instanceof Journey.Leg.Transport
                        && nextLeg instanceof Journey.Leg.Transport) {
                    long offset = Duration.between(
                            journey.depTime(), currentLeg.depTime()
                    ).toMinutes();
                    newMarkers.add(createDisk("transfer", offset));
                }
            }
            newMarkers.add(createDisk("dep-arr", totalJourneyMinutes));
            timelineMarkers = newMarkers;
            timelinePane.requestLayout();
        }

        private Circle createDisk(String styleClass, double offsetMinutes) {
            Circle marker = new Circle(DISK_RADIUS);
            marker.getStyleClass().add(styleClass);
            marker.setUserData(offsetMinutes);
            return marker;
        }
    }
}