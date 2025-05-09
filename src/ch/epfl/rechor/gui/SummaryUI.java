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
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Vue d'ensemble des voyages : icône/ligne, heures, graphique des disques, et durée.
 */
public record SummaryUI(Node rootNode, ObservableValue<Journey> selectedJourneyO) {
    private static final double DISK_RADIUS = 3.0;
    private static final double LINE_PADDING = 5.0;

    /**
     * Crée la ListView paramétrée avec son cell factory et sélection automatique,
     * et centre la vue sur le voyage sélectionné.
     */
    public static SummaryUI create(ObservableValue<List<Journey>> journeysO,
                                   ObservableValue<LocalTime> depTimeO) {
        ListView<Journey> listView = new ListView<>();
        listView.getStylesheets().add("summary.css");

        ObservableList<Journey> items = FXCollections.observableArrayList();
        listView.setItems(items);
        listView.setCellFactory(lv -> new SummaryCell());

        // Sélection automatique du voyage le plus proche
        Runnable selectNearest = () -> {
            LocalTime target = depTimeO.getValue();
            Journey sel = items.stream()
                    .filter(j -> !j.depTime().toLocalTime().isBefore(target))
                    .findFirst()
                    .orElseGet(() -> items.isEmpty() ? null : items.getLast());
            if (sel != null) {
                listView.getSelectionModel().select(sel);
                // Centrer la vue sur la sélection
                Platform.runLater(() -> {
                    int idx = listView.getSelectionModel().getSelectedIndex();
                    if (idx >= 0) {
                        listView.scrollTo(idx);
                    }
                });
            } else {
                listView.getSelectionModel().clearSelection();
            }
        };

        // Met à jour la liste et recalcule la sélection
        journeysO.subscribe(newJourneys -> {
            items.setAll(newJourneys);
            selectNearest.run();
        });
        depTimeO.subscribe(newTime -> selectNearest.run());

        return new SummaryUI(listView,
                listView.getSelectionModel().selectedItemProperty());
    }

    /**
     * Cellule affichant route, temps, graphique, et durée en trois lignes.
     */
    private static class SummaryCell extends ListCell<Journey> {
        private final ImageView iconView = new ImageView();
        private final Label routeLabel = new Label();
        private final Label depLabel = new Label();
        private final Label arrLabel = new Label();
        private final Label durLabel = new Label();

        private final Pane trackPane;
        private final VBox cellBox;

        private List<Circle> disks = List.of();
        private long totalMins;

        SummaryCell() {
            trackPane = new Pane() {
                @Override
                protected void layoutChildren() {
                    super.layoutChildren();
                    getChildren().clear();
                    double w = getWidth(), h = getHeight(), y = h / 2;
                    getChildren().add(new Line(LINE_PADDING, y, w - LINE_PADDING, y));
                    for (Circle c : disks) {
                        double off = (double) c.getUserData();
                        double x = LINE_PADDING + (off / totalMins) * (w - 2 * LINE_PADDING);
                        c.setCenterX(x);
                        c.setCenterY(y);
                        getChildren().add(c);
                    }
                }
            };
            trackPane.setPrefSize(0, 0);
            HBox.setHgrow(trackPane, Priority.ALWAYS);

            // ligne 1 : icône + route
            HBox routeRow = new HBox();
            routeRow.getStyleClass().add("route");
            routeRow.setAlignment(Pos.CENTER_LEFT);
            routeRow.getChildren().setAll(iconView, routeLabel);

            // ligne 2 : heures + graphique
            HBox timesRow = new HBox();
            timesRow.setAlignment(Pos.CENTER_LEFT);
            timesRow.getChildren().setAll(depLabel, trackPane, arrLabel);

            // ligne 3 : durée centrée
            HBox durRow = new HBox(durLabel);
            durRow.setAlignment(Pos.CENTER);

            cellBox = new VBox(routeRow, timesRow, durRow);
            cellBox.getStyleClass().add("journey");
            depLabel.getStyleClass().add("departure");
            durLabel.getStyleClass().add("duration");

            setGraphic(cellBox);
        }

        @Override
        protected void updateItem(Journey journey, boolean empty) {
            super.updateItem(journey, empty);
            if (empty || journey == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            updateTransport(journey);
            updateTimes(journey);
            updateDisks(journey);
            setGraphic(cellBox);
        }

        @Override
        public void updateSelected(boolean selected) {
            super.updateSelected(selected);
            if (selected) {
                routeLabel.setTextFill(Color.BLACK);
                depLabel.setTextFill(Color.BLACK);
                arrLabel.setTextFill(Color.BLACK);
                durLabel.setTextFill(Color.BLACK);
            }
        }

        private void updateTransport(Journey j) {
            Optional<Journey.Leg.Transport> opt = j.legs().stream()
                    .filter(l -> l instanceof Journey.Leg.Transport)
                    .map(l -> (Journey.Leg.Transport) l)
                    .findFirst();
            if (opt.isPresent()) {
                Journey.Leg.Transport t = opt.get();
                Image icon = VehicleIcons.iconFor(t.vehicle());
                iconView.setImage(icon);
                iconView.setFitWidth(20);
                iconView.setPreserveRatio(true);
                routeLabel.setText(FormatterFr.formatRouteDestination(t));
            } else {
                iconView.setImage(null);
                routeLabel.setText("");
            }
        }

        private void updateTimes(Journey j) {
            depLabel.setText(FormatterFr.formatTime(j.depTime()));
            arrLabel.setText(FormatterFr.formatTime(j.arrTime()));
            durLabel.setText(FormatterFr.formatDuration(j.duration()));
        }

        private void updateDisks(Journey j) {
            List<Circle> cs = new ArrayList<>();
            totalMins = Duration.between(j.depTime(), j.arrTime()).toMinutes();

            cs.add(createDisk("dep-arr", 0));
            List<Journey.Leg> legs = j.legs();
            for (int i = 1; i < legs.size() - 1; i++) {
                var prev = legs.get(i - 1);
                var curr = legs.get(i);
                var next = legs.get(i + 1);
                if (curr instanceof Journey.Leg.Foot
                        && prev instanceof Journey.Leg.Transport
                        && next instanceof Journey.Leg.Transport) {
                    double off = Duration.between(j.depTime(), curr.depTime()).toMinutes();
                    cs.add(createDisk("transfer", off));
                }
            }
            // arrivée
            cs.add(createDisk("dep-arr", totalMins));
            disks = cs;
            trackPane.requestLayout();
        }

        private Circle createDisk(String styleClass, double offset) {
            Circle c = new Circle(DISK_RADIUS);
            c.getStyleClass().add(styleClass);
            c.setUserData(offset);
            return c;
        }
    }
}