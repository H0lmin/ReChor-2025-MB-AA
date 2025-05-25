package ch.epfl.rechor.gui;

import ch.epfl.rechor.StopIndex;
import ch.epfl.rechor.journey.Journey;
import ch.epfl.rechor.journey.JourneyExtractor;
import ch.epfl.rechor.journey.Profile;
import ch.epfl.rechor.journey.Router;
import ch.epfl.rechor.timetable.CachedTimeTable;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main application class that composes the query, summary, and detail UIs
 * to provide an interactive journey search interface.
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public final class Main extends Application {

    /** Observable list of journeys matching the current query parameters. */
    private ObservableValue<List<Journey>> journeys0;

    /**
     * Application entry point; delegates to JavaFX.
     *
     * @param args command-line arguments
     */
    public static void main(final String[] args) {
        launch(args);
    }

    @Override
    public void start(final Stage primaryStage) throws Exception {
        TimeTable timeTable  = new CachedTimeTable(FileTimeTable.in(Path.of("timetable")));
        StopIndex stopIndex  = buildStopIndex(timeTable);
        QueryUI queryUI      = QueryUI.create(stopIndex);
        Map<String, Integer> nameToId = buildNameToIdMap(timeTable.stations());

        journeys0 = createJourneyBinding(queryUI, timeTable, nameToId);

        SummaryUI summaryUI = SummaryUI.create(journeys0, queryUI.timeO());
        DetailUI  detailUI  = DetailUI.create(summaryUI.selectedJourneyO());

        BorderPane root = new BorderPane();
        root.setTop(queryUI.root());
        root.setCenter(new SplitPane(summaryUI.rootNode(), detailUI.rootNode()));

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.setTitle("ReCHor");
        primaryStage.show();

        // Focus departure field once scene is rendered
        Platform.runLater(() -> scene.lookup("#depStop").requestFocus());
    }

    /**
     * Builds a StopIndex from station names and their aliases.
     *
     * @param timeTable source of Stations and StationAliases
     * @return a new StopIndex instance
     */
    private static StopIndex buildStopIndex(final TimeTable timeTable) {
        Stations stations = timeTable.stations();
        StationAliases aliases = timeTable.stationAliases();

        List<String> mainStopNames = new ArrayList<>(stations.size());
        for (int i = 0; i < stations.size(); i++) {
            mainStopNames.add(stations.name(i));
        }

        Map<String, String> aliasToMain = new HashMap<>(aliases.size());
        for (int i = 0; i < aliases.size(); i++) {
            aliasToMain.put(aliases.alias(i), aliases.stationName(i));
        }

        return new StopIndex(mainStopNames, aliasToMain);
    }

    /**
     * Builds a map from station name to its integer identifier.
     *
     * @param stations provides names by index
     * @return mapping of station name → station index
     */
    private static Map<String, Integer> buildNameToIdMap(final Stations stations) {
        Map<String, Integer> nameToId = new HashMap<>(stations.size());
        for (int i = 0; i < stations.size(); i++) {
            nameToId.put(stations.name(i), i);
        }
        return nameToId;
    }

    /**
     * Creates an observable binding of journey search results based
     * on the query UI’s departure, arrival and date fields.
     *
     * @param queryUI    source of departure, arrival and date observables
     * @param timeTable  timetable used to compute routes
     * @param nameToId   map for translating station names to IDs
     * @return an ObservableValue of List&lt;Journey&gt; filtered by current query
     */
    private static ObservableValue<List<Journey>> createJourneyBinding(
            QueryUI queryUI,
            TimeTable timeTable,
            Map<String, Integer> nameToId) {

        return Bindings.createObjectBinding(() -> {
            String depStation = queryUI.depStopO().getValue();
            String arrivalName = queryUI.arrStopO().getValue();
            LocalDate date = queryUI.dateO().getValue();

            if (depStation == null || depStation.isEmpty()
                    || arrivalName == null || arrivalName.isEmpty()) {
                return List.of();
            }

            Integer departureId = nameToId.get(depStation);
            Integer arrivalId = nameToId.get(arrivalName);
            if (departureId == null || arrivalId == null) {
                return List.of();
            }

            Router router  = new Router(timeTable);
            Profile profile = router.profile(date, arrivalId);
            return JourneyExtractor.journeys(profile, departureId);
        }, queryUI.depStopO(), queryUI.arrStopO(), queryUI.dateO());
    }
}