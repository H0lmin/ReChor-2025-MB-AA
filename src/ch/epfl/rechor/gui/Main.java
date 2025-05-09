package ch.epfl.rechor.gui;

import ch.epfl.rechor.StopIndex;
import ch.epfl.rechor.journey.Journey;
import ch.epfl.rechor.journey.JourneyExtractor;
import ch.epfl.rechor.journey.Router;
import ch.epfl.rechor.journey.Profile;
import ch.epfl.rechor.timetable.StationAliases;
import ch.epfl.rechor.timetable.Stations;
import ch.epfl.rechor.timetable.TimeTable;
import ch.epfl.rechor.timetable.mapped.FileTimeTable;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main application class that composes the query, summary, and detail UIs
 * and wires them together to provide an interactive journey search interface.
 */
public class Main extends Application {

    private ObservableValue<List<Journey>> journeysO;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load timetable from the "timetable" subfolder in the working directory
        TimeTable timeTable = FileTimeTable.in(Path.of("timetable"));

        // Build stop index from station names and aliases
        Stations stations = timeTable.stations();
        StationAliases aliases = timeTable.stationAliases();
        // Map main station names
        List<String> mainStops = new java.util.ArrayList<>();
        for (int i = 0; i < stations.size(); i++) {
            mainStops.add(stations.name(i));
        }
        // Map aliases to main names
        Map<String, String> altNames = new HashMap<>();
        for (int i = 0; i < aliases.size(); i++) {
            altNames.put(aliases.alias(i), aliases.stationName(i));
        }
        StopIndex index = new StopIndex(mainStops, altNames);

        // Create the query UI
        QueryUI queryUI = QueryUI.create(index);

        // Precompute a name-to-ID map for station lookup
        Map<String, Integer> nameToId = new HashMap<>();
        for (int i = 0; i < stations.size(); i++) {
            nameToId.put(stations.name(i), i);
        }

        // Create observable list of journeys based on query parameters
        journeysO = Bindings.createObjectBinding(() -> {
            String dep = queryUI.depStopO().getValue();
            String arr = queryUI.arrStopO().getValue();
            LocalDate date = queryUI.dateO().getValue();
            if (dep == null || arr == null || dep.isEmpty() || arr.isEmpty()) {
                return List.of();
            }
            Integer depId = nameToId.get(dep);
            Integer arrId = nameToId.get(arr);
            if (depId == null || arrId == null) {
                return List.of();
            }
            // Build profile and extract journeys
            Router router = new Router(timeTable);
            Profile profile = router.profile(date, arrId);
            return JourneyExtractor.journeys(profile, depId);
        }, queryUI.depStopO(), queryUI.arrStopO(), queryUI.dateO());

        // 4. Create summary & detail UIs
        SummaryUI summaryUI = SummaryUI.create(journeysO, queryUI.timeO());
        DetailUI detailUI = DetailUI.create(summaryUI.selectedJourneyO());

        // 5. Assemble scene graph with SplitPane at center
        BorderPane root = new BorderPane();
        root.setTop(queryUI.root());
        SplitPane centerSplit = new SplitPane(summaryUI.rootNode(), detailUI.rootNode());
        root.setCenter(centerSplit);

        Scene scene = new Scene(root);

        // 6. Configure stage
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.setTitle("ReCHor");
        primaryStage.show();

        // 7. Focus departure field
        Platform.runLater(() -> scene.lookup("#depStop").requestFocus());
    }
}