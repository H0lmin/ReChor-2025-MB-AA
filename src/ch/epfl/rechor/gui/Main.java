package ch.epfl.rechor.gui;

import ch.epfl.rechor.StopIndex;
import ch.epfl.rechor.journey.Journey;
import ch.epfl.rechor.journey.JourneyExtractor;
import ch.epfl.rechor.journey.Profile;
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
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public class Main extends Application {

    // stored in a field so the binding stays active
    private ObservableValue<List<Journey>> journeysO;
    // caches for profile to avoid rebuilding when not needed
    private final Profile[] profileCache = new Profile[1];
    private final String[] lastArrStop = new String[1];
    private final LocalDate[] lastDate = new LocalDate[1];

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1. Load timetable
        TimeTable timeTable = FileTimeTable.in(Path.of("timetable"));

        // Build list of stop names
        List<String> mainStops = IntStream.range(0, timeTable.stations().size())
                .mapToObj(timeTable.stations()::name)
                .toList();
        StopIndex stopIndex = new StopIndex(mainStops, Collections.emptyMap());

        // 2. Create QueryUI
        QueryUI queryUI = QueryUI.create(stopIndex);

        // 3. Define observable journeys with guard for invalid stops
        journeysO = Bindings.createObjectBinding(() -> {
            String dep = queryUI.depStopO().getValue();
            String arr = queryUI.arrStopO().getValue();
            // if either stop not valid, show empty list
            if (!mainStops.contains(dep) || !mainStops.contains(arr)) {
                return List.of();
            }
            LocalDate date = queryUI.dateO().getValue();
            // rebuild profile only if arrival or date changed
            if (profileCache[0] == null
                    || !arr.equals(lastArrStop[0])
                    || !date.equals(lastDate[0])) {
                int arrId = mainStops.indexOf(arr);
                profileCache[0] = new Profile.Builder(timeTable, date, arrId).build();
                lastArrStop[0] = arr;
                lastDate[0] = date;
            }
            int depId = mainStops.indexOf(dep);
            return JourneyExtractor.journeys(profileCache[0], depId);
        }, queryUI.depStopO(), queryUI.arrStopO(), queryUI.dateO());

        // 4. Create summary & detail UIs
        SummaryUI summaryUI = SummaryUI.create(journeysO, queryUI.timeO());
        DetailUI detailUI = DetailUI.create(summaryUI.selectedJourneyO());

        // 5. Assemble scene graph with SplitPane at center
        BorderPane root = new BorderPane();
        root.setTop(queryUI.rootNode());
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