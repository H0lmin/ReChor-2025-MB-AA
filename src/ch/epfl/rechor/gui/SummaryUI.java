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
 * UI component showing an overview of journeys in a list.
 * Exposes the root JavaFX node and an observable for the selected journey.
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public record SummaryUI(Node rootNode, ObservableValue<Journey> selectedJourneyO) {
    private static final double RADIUS = 3.0;
    private static final double PADDING = 5.0;
    private static final String CSS = "summary.css";

    /**
     * Creates a SummaryUI bound to the given journeys and departure-time observables.
     *
     * @param journeysObs source of journey lists to display
     * @param timeObs     source of desired departure times
     * @return a new SummaryUI record
     */
    public static SummaryUI create(ObservableValue<List<Journey>> journeysObs,
                                   ObservableValue<LocalTime> timeObs) {
        ListView<Journey> lv = new ListView<>();
        lv.getStylesheets().add(CSS);

        ObservableList<Journey> items = FXCollections.observableArrayList();
        lv.setItems(items);
        lv.setCellFactory(v -> new JourneyCell());

        Runnable selectNearest = () -> {
            LocalTime desired = timeObs.getValue();
            Journey sel = items.stream()
                    .filter(j -> !j.depTime().toLocalTime().isBefore(desired))
                    .findFirst()
                    .orElse(items.isEmpty() ? null : items.get(items.size() - 1));
            if (sel != null) {
                lv.getSelectionModel().select(sel);
                Platform.runLater(() ->
                        lv.scrollTo(lv.getSelectionModel().getSelectedIndex())
                );
            } else {
                lv.getSelectionModel().clearSelection();
            }
        };

        journeysObs.subscribe(js -> { items.setAll(js); selectNearest.run(); });
        timeObs.subscribe(t -> selectNearest.run());

        return new SummaryUI(lv, lv.getSelectionModel().selectedItemProperty());
    }

    /**
     * Renders each Journey as a cell with route, times, duration, and a timeline.
     */
    private static class JourneyCell extends ListCell<Journey> {
        private final ImageView icon = new ImageView();
        private final Label route = new Label();
        private final Label dep = new Label();
        private final Label arr = new Label();
        private final Label dur = new Label();
        private final Pane timeline = new Pane();
        private final VBox box;

        private List<Circle> markers = List.of();
        private long totalMin;

        JourneyCell() {
            icon.setFitWidth(20);
            icon.setPreserveRatio(true);

            dep.getStyleClass().add("departure");
            dur.getStyleClass().add("duration");

            HBox.setHgrow(timeline, Priority.ALWAYS);
            timeline.setPrefSize(0, 0);

            box = new VBox(
                    styledHBox(new HBox(icon, route), "route", Pos.CENTER_LEFT),
                    align(new HBox(dep, timeline, arr), Pos.CENTER_LEFT),
                    styledHBox(new HBox(dur), "duration-row", Pos.CENTER)
            );
            box.getStyleClass().add("journey");
            setGraphic(box);
        }

        @Override
        protected void updateItem(Journey j, boolean empty) {
            super.updateItem(j, empty);
            if (empty || j == null) {
                setGraphic(null);
            } else {
                apply(j);
                setGraphic(box);
            }
        }

        @Override
        public void updateSelected(boolean sel) {
            super.updateSelected(sel);
            if (sel) {
                List.of(route, dep, arr, dur).forEach(l -> l.setTextFill(Color.BLACK));
            }
        }

        // Consolidates all updates per journey
        private void apply(Journey j) {
            updateTransport(j);
            dep.setText(FormatterFr.formatTime(j.depTime()));
            arr.setText(FormatterFr.formatTime(j.arrTime()));
            dur.setText(FormatterFr.formatDuration(j.duration()));
            updateDisks(j);
            timeline.requestLayout();
        }

        // Finds first transport leg and updates icon + route label
        private void updateTransport(Journey j) {
            Optional<Journey.Leg.Transport> t = j.legs().stream()
                    .filter(l -> l instanceof Journey.Leg.Transport)
                    .map(l -> (Journey.Leg.Transport) l)
                    .findFirst();
            if (t.isPresent()) {
                icon.setImage(VehicleIcons.iconFor(t.get().vehicle()));
                route.setText(FormatterFr.formatRouteDestination(t.get()));
            } else {
                icon.setImage(null);
                route.setText("");
            }
        }

        // Builds timeline markers for departure, transfers, and arrival
        private void updateDisks(Journey j) {
            List<Circle> newM = new ArrayList<>();
            totalMin = j.duration().toMinutes();
            newM.add(disk("dep-arr", 0));
            var legs = j.legs();
            for (int i = 1; i < legs.size()-1; i++) {
                if (isTransfer(legs, i)) {
                    long off = Duration.between(j.depTime(), legs.get(i).depTime()).toMinutes();
                    newM.add(disk("transfer", off));
                }
            }
            newM.add(disk("dep-arr", totalMin));
            markers = newM;
        }

        private boolean isTransfer(List<Journey.Leg> legs, int i) {
            return legs.get(i) instanceof Journey.Leg.Foot
                    && legs.get(i-1) instanceof Journey.Leg.Transport
                    && legs.get(i+1) instanceof Journey.Leg.Transport;
        }

        private Circle disk(String cls, double off) {
            Circle c = new Circle(RADIUS);
            c.getStyleClass().add(cls);
            c.setUserData(off);
            return c;
        }

        @Override
        protected void layoutChildren() {
            super.layoutChildren();
            timeline.getChildren().clear();
            double w = timeline.getWidth();
            double mid = timeline.getHeight()/2;
            timeline.getChildren().add(new Line(PADDING, mid, w-PADDING, mid));
            for (var c : markers) {
                double x = PADDING + ((double)c.getUserData()/totalMin)*(w-2*PADDING);
                c.setCenterX(x);
                c.setCenterY(mid);
                timeline.getChildren().add(c);
            }
        }

        // Helper to style and align an HBox
        private HBox styledHBox(HBox h, String style, Pos p) {
            h.getStyleClass().add(style);
            h.setAlignment(p);
            return h;
        }
        private HBox align(HBox h, Pos p) {
            h.setAlignment(p);
            return h;
        }
    }
}
