package ch.epfl.rechor.gui;

import ch.epfl.rechor.FormatterFr;
import ch.epfl.rechor.journey.Journey;
import ch.epfl.rechor.journey.JourneyGeoJsonConverter;
import ch.epfl.rechor.journey.JourneyIcalConverter;
import ch.epfl.rechor.journey.Vehicle;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays detailed information for a Journey object
 *
 * @param rootNode the root JavaFX Node containing the detail UI
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public record DetailUI(Node rootNode) {
    private static final double CIRCLE_RADIUS = 3;
    private static final int ICON_SIZE = 31;

    /**
     * Creates a DetailUI instance bound to an observable Journey
     *
     * @param journeyObs an observable value of Journey
     * @return a configured DetailUI
     */
    public static DetailUI create(ObservableValue<Journey> journeyObs) {
        StackPane root = new StackPane();
        root.setId("detail");
        root.getStylesheets().add("detail.css");

        Label none = new Label("Aucun voyage");
        none.setId("no-journey");

        VBox content = new VBox();
        ScrollPane scroll = new ScrollPane(content);
        scroll.setPannable(true);
        root.getChildren().setAll(scroll, none);

        journeyObs.addListener((o, oldV, newV) -> updateView(newV, none, content));
        updateView(journeyObs.getValue(), none, content);
        return new DetailUI(root);
    }

    /**
     * Updates the visibility of the content pane
     *
     * @param j       the current Journey (null if none)
     * @param none    the label when no journey is present
     * @param content the container for journey details
     */
    private static void updateView(Journey j, Label none, VBox content) {
        boolean present = j != null;
        none.setVisible(!present);
        content.setVisible(present);
        content.getChildren().clear();
        if (present) {
            content.getChildren().addAll(buildSteps(j), buildButtons(j));
        }
    }

    /**
     * Builds the grid of journey steps including walking and transport legs
     *
     * @param j the Journey to render
     * @return a Node containing the annotated steps grid
     */
    private static Node buildSteps(Journey j) {
        Pane ann = new Pane();
        StepsGrid grid = new StepsGrid(ann);
        grid.setId("legs");
        int row = 0;
        for (var leg : j.legs()) {
            if (leg instanceof Journey.Leg.Foot foot) {
                Label lbl = new Label(FormatterFr.formatLeg(foot));
                grid.add(lbl, 2, row, 2, 1);
                row++;
            } else if (leg instanceof Journey.Leg.Transport tr) {
                row = addTransport(grid, tr, row);
            }
        }
        return new StackPane(ann, grid);
    }

    /**
     * Adds a transport leg to the grid including departure, optional intermediate stops, and arrival
     *
     * @param grid the StepsGrid
     * @param tr   the transport leg data
     * @param row  the starting row index
     * @return the next available row index after adding this leg
     */
    private static int addTransport(StepsGrid grid, Journey.Leg.Transport tr, int row) {
        // departure
        Label depTime = styledLabel(FormatterFr.formatTime(tr.depTime()), "departure");
        GridPane.setHalignment(depTime, HPos.RIGHT);
        Circle depC = circle();
        Label depSt = new Label(tr.depStop().name());
        Label depPlat = styledLabel(FormatterFr.formatPlatformName(tr.depStop()), "departure");
        GridPane.setHalignment(depPlat, HPos.CENTER);
        grid.addRow(row++, depTime, depC, depSt, depPlat);

        // icon & route
        ImageView icon = iconFor(tr.vehicle());
        boolean mid = !tr.intermediateStops().isEmpty();
        grid.add(icon, 0, row, 1, mid ? 2 : 1);
        Label rd = new Label(FormatterFr.formatRouteDestination(tr));
        grid.add(rd, 2, row++, 2, 1);

        if (mid) {
            long dur = Duration.between(tr.depTime(), tr.arrTime()).toMinutes();
            TitledPane tp = new TitledPane(
                    tr.intermediateStops().size() + " arrêts, " + dur + " min",
                    buildIntermediate(tr));
            Accordion acc = new Accordion(tp);
            grid.add(acc, 2, row++, 2, 1);
        }

        // arrival
        Label arrTime = new Label(FormatterFr.formatTime(tr.arrTime()));
        GridPane.setHalignment(arrTime, HPos.RIGHT);
        Circle arrC = circle();
        Label arrSt = new Label(tr.arrStop().name());
        Label arrPlat = new Label(FormatterFr.formatPlatformName(tr.arrStop()));
        GridPane.setHalignment(arrPlat, HPos.CENTER);
        grid.addRow(row, arrTime, arrC, arrSt, arrPlat);
        grid.connect(depC, arrC);
        return row + 1;
    }

    /**
     * Builds a grid of intermediate stops for a transport leg
     *
     * @param tr the transport leg containing intermediate stops
     * @return a GridPane listing each intermediate stop with times
     */
    private static GridPane buildIntermediate(Journey.Leg.Transport tr) {
        GridPane g = new GridPane();
        g.getStyleClass().add("intermediate-stops");
        int r = 0;
        for (var s : tr.intermediateStops()) {
            g.addRow(r++, new Label(FormatterFr.formatTime(s.arrTime())),
                    new Label(FormatterFr.formatTime(s.depTime())),
                    new Label(s.stop().name()));
        }
        return g;
    }

    /**
     * Builds the action buttons bar for map and calendar export
     *
     * @param j the Journey to use for export actions
     * @return an HBox containing the action buttons
     */
    private static Node buildButtons(Journey j) {
        HBox bar = new HBox(10);
        bar.setId("buttons");
        bar.setAlignment(Pos.BOTTOM_CENTER);
        bar.getChildren().addAll(btn("Carte", e -> openMap(j)), btn("Calendrier", e -> exportIcs(j)));
        return bar;
    }

    /**
     * Creates a Button with text and click handler
     *
     * @param txt the button label text
     * @param h   the event handler for button clicks
     * @return a configured Button
     */
    private static Button btn(String txt, javafx.event.EventHandler<javafx.event.ActionEvent> h) {
        Button b = new Button(txt);
        b.setOnAction(h);
        return b;
    }

    /**
     * Opens the journey in an external map service by encoding GeoJSON data
     *
     * @param j the Journey to display on the map
     */
    private static void openMap(Journey j) {
        try {
            String geo = URLEncoder.encode(JourneyGeoJsonConverter.toGeoJson(j).toString().replaceAll("\\s+", ""),
                    StandardCharsets.UTF_8);
            Desktop.getDesktop().browse(new URI("https", "umap.osm.ch", "/fr/map/", "data=" + geo, null));
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * Exports the journey to an iCalendar file
     *
     * @param j the Journey to export
     */
    private static void exportIcs(Journey j) {
        FileChooser ch = new FileChooser();
        ch.setInitialFileName("voyage_" + j.depTime().toLocalDate() + ".ics");
        ch.setTitle("Exporter iCal");
        File f = ch.showSaveDialog(null);
        if (f != null) {
            try {
                Files.writeString(f.toPath(), JourneyIcalConverter.toIcalendar(j));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Creates an ImageView for a vehicle icon
     *
     * @param v the Vehicle type
     * @return a configured ImageView showing the icon
     */
    private static ImageView iconFor(Vehicle v) {
        ImageView iv = new ImageView(VehicleIcons.iconFor(v));
        iv.setFitWidth(ICON_SIZE);
        iv.setFitHeight(ICON_SIZE);
        iv.setPreserveRatio(true);
        GridPane.setHalignment(iv, HPos.CENTER);
        GridPane.setValignment(iv, VPos.CENTER);
        return iv;
    }

    /**
     * Creates a styled Label with a given CSS class
     *
     * @param txt   the label text
     * @param style the CSS style class to apply
     * @return a styled Label
     */
    private static Label styledLabel(String txt, String style) {
        Label l = new Label(txt);
        l.getStyleClass().add(style);
        return l;
    }

    /**
     * Creates a small black circle used for step annotations
     *
     * @return a Circle of fixed radius and color
     */
    private static Circle circle() {
        return new Circle(CIRCLE_RADIUS, Color.BLACK);
    }

    /**
     * Custom GridPane that draws connecting lines between circles after layout
     */
    private static class StepsGrid extends GridPane {
        private final Pane ann;
        private final List<Circle[]> conns = new ArrayList<>();

        /**
         * Constructs a StepsGrid with an annotation pane for lines
         *
         * @param ann the Pane to draw connecting lines
         */
        StepsGrid(Pane ann) { this.ann = ann; }

        /**
         * Records a connection between two circles
         *
         * @param a the start circle
         * @param b the end circle
         */
        void connect(Circle a, Circle b) { conns.add(new Circle[]{a, b}); }

        /**
         * Lays out children and then draws red lines between connected circles
         */
        @Override protected void layoutChildren() {
            super.layoutChildren();
            ann.getChildren().removeIf(Line.class::isInstance);
            for (Circle[] p : conns) {
                Point2D s = p[0].localToParent(p[0].getCenterX(), p[0].getCenterY());
                Point2D e = p[1].localToParent(p[1].getCenterX(), p[1].getCenterY());
                Line l = new Line(s.getX(), s.getY() + 4, e.getX(), e.getY() - 4);
                l.setStrokeWidth(2);
                l.setStroke(Color.RED);
                ann.getChildren().add(l);
            }
        }
    }
}
