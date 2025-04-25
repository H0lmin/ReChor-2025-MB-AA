package ch.epfl.rechor.gui;

import ch.epfl.rechor.FormatterFr;
import ch.epfl.rechor.journey.Journey;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;


public record DetailUI(Node rootNode) {

    public static DetailUI create(ObservableValue<Journey> journeyProperty) {
        Text noJourneyText = new Text("Aucun voyage");
        StackPane.setAlignment(noJourneyText, Pos.CENTER);

        StackPane mainPane = new StackPane();
        Pane detailPane = new Pane();
        VBox detailsVBox = new VBox();
        detailPane.getChildren().add(detailsVBox);

        mainPane.getChildren().addAll(noJourneyText, detailPane);

        Runnable updateUI = () -> {
            Journey journey = journeyProperty.getValue();
            boolean hasJourney = journey != null;
            noJourneyText.setVisible(!hasJourney);
            detailPane.setVisible(hasJourney);
            detailsVBox.getChildren().clear();

            if (!hasJourney) return;

            List<Pair<Circle, Circle>> annotations = new ArrayList<>();

            GridPane grid = new GridPane() {
                @Override
                protected void layoutChildren() {
                    super.layoutChildren();
                    getChildren().removeIf(n -> n instanceof Line);
                    for (Pair<Circle, Circle> pair : annotations) {
                        Bounds start = pair.getKey().getBoundsInParent();
                        Bounds end = pair.getValue().getBoundsInParent();
                        double x1 = start.getMinX() + start.getWidth() / 2;
                        double y1 = start.getMinY() + start.getHeight() / 2;
                        double x2 = end.getMinX() + end.getWidth() / 2;
                        double y2 = end.getMinY() + end.getHeight() / 2;
                        Line line = new Line(x1, y1, x2, y2);
                        line.setStroke(Color.RED);
                        line.setStrokeWidth(2);
                        getChildren().add(line);
                    }
                }
            };

            grid.setHgap(5);
            grid.setVgap(10);

            int row = 0;
            for (Journey.Leg leg : journey.legs()) {
                Journey.Leg.Foot foot=new Journey.Leg.Foot(leg.depStop(),leg.depTime(),leg.arrStop(),leg.arrTime());
                if ( foot.isTransfer()) {
                    Text walkText = new Text(FormatterFr.formatLeg(foot));
                    GridPane.setColumnSpan(walkText, 2);
                    grid.add(walkText, 2, row);
                    row++;
                } else {
                    Text depTime = new Text(FormatterFr.formatTime(leg.depTime()));
                    depTime.getStyleClass().add("departure");
                    grid.add(depTime, 0, row);

                    Circle depCircle = new Circle(3);
                    grid.add(depCircle, 1, row);

                    Text depStation = new Text(leg.depStop().name());
                    grid.add(depStation, 2, row);

                    Text depPlatform = new Text(FormatterFr.formatPlatformName(leg.depStop()));
                    depPlatform.getStyleClass().add("departure");
                    grid.add(depPlatform, 3, row);

                    row++;

                    Journey.Leg.Transport transportLeg = (Journey.Leg.Transport) leg;
                    ImageView icon = new ImageView(VehicleIcons.iconFor(transportLeg.vehicle()));
                    icon.setFitWidth(31);
                    icon.setFitHeight(31);

                    if (!leg.intermediateStops().isEmpty()) {
                        GridPane.setRowSpan(icon, 2);
                    }

                    grid.add(icon, 0, row);

                    if (leg instanceof Journey.Leg.Transport) {

                        Text destText = new Text(FormatterFr.formatRouteDestination(transportLeg));
                        GridPane.setColumnSpan(destText, 2);
                        grid.add(destText, 2, row);

                        row++;
                    }

                    if (!leg.intermediateStops().isEmpty()) {
                        GridPane stopGrid = new GridPane();
                        stopGrid.getStyleClass().add("intermediate-stops");
                        int i = 0;
                        for (var stop : leg.intermediateStops()) {
                            stopGrid.add(new Text(FormatterFr.formatTime(stop.arrTime())), 0, i);
                            stopGrid.add(new Text(FormatterFr.formatTime(stop.depTime())), 1, i);
                            stopGrid.add(new Text(FormatterFr.formatPlatformName(stop.stop())), 2, i);
                            i++;
                        }

                        TitledPane accordionPane = new TitledPane(
                                formatIntermediateStops(leg),
                                stopGrid
                        );
                        Accordion accordion = new Accordion(accordionPane);
                        GridPane.setColumnSpan(accordion, 2);
                        grid.add(accordion, 2, row);
                        row++;
                    }

                    Text arrTime = new Text(FormatterFr.formatTime(leg.arrTime()));
                    grid.add(arrTime, 0, row);

                    Circle arrCircle = new Circle(3);
                    grid.add(arrCircle, 1, row);

                    Text arrStation = new Text(leg.arrStop().name());
                    grid.add(arrStation, 2, row);

                    Text arrPlatform = new Text(FormatterFr.formatPlatformName(leg.arrStop()));
                    grid.add(arrPlatform, 3, row);

                    annotations.add(new Pair<>(depCircle, arrCircle));

                    row++;
                }
            }

            detailsVBox.getChildren().add(grid);
        };

        updateUI.run();
        journeyProperty.addListener((obs, oldVal, newVal) -> updateUI.run());

        return new DetailUI(mainPane);
    }

    private static String formatIntermediateStops(Journey.Leg leg) {
        StringBuilder sb = new StringBuilder();

        for (Journey.Leg.IntermediateStop intermediateStop : leg.intermediateStops()) {
            String formattedStop = FormatterFr.formatPlatformName(intermediateStop.stop());
            sb.append(formattedStop).append("\n");
        }

        return sb.toString();
    }

}
