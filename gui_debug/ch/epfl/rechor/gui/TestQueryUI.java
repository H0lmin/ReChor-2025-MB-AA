package ch.epfl.rechor.gui;

import ch.epfl.rechor.StopIndex;
import ch.epfl.rechor.timetable.CachedTimeTable;
import ch.epfl.rechor.timetable.StationAliases;
import ch.epfl.rechor.timetable.Stations;
import ch.epfl.rechor.timetable.TimeTable;
import ch.epfl.rechor.timetable.mapped.FileTimeTable;
import javafx.application.Application;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class TestQueryUI extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Chargement des données horaires
        TimeTable timeTable = new CachedTimeTable(
                FileTimeTable.in(Path.of("timetable16"))
        );

        Stations stations = timeTable.stations();
        StationAliases aliases = timeTable.stationAliases();

        // Extraire les noms des gares
        List<String> stopNames = new ArrayList<>();
        for (int i = 0; i < stations.size(); i++) {
            stopNames.add(stations.name(i));
        }

        // Extraire les alias (sous forme Map<String alias, String mainName>)
        Map<String, String> aliasMap = new HashMap<>();
        for (int i = 0; i < aliases.size(); i++) {
            aliasMap.put(aliases.alias(i), aliases.stationName(i));
        }

        // Créer le StopIndex complet
        StopIndex stopIndex = new StopIndex(stopNames, aliasMap);

        // Création de l'interface de requête
        QueryUI queryUI = QueryUI.create(stopIndex);

        // Observateurs pour voir les interactions utilisateur (optionnel)
        ObservableValue<String> depStopO = queryUI.depStopO();
        ObservableValue<String> arrStopO = queryUI.arrStopO();
        ObservableValue<LocalDate> dateO = queryUI.dateO();
        ObservableValue<LocalTime> timeO = queryUI.timeO();

        depStopO.addListener((obs, oldV, newV) -> System.out.println("Départ : " + newV));
        arrStopO.addListener((obs, oldV, newV) -> System.out.println("Arrivée : " + newV));
        dateO.addListener((obs, oldV, newV) -> System.out.println("Date : " + newV));
        timeO.addListener((obs, oldV, newV) -> System.out.println("Heure : " + newV));

        // Création de la scène avec uniquement la partie ① (QueryUI)
        VBox root = new VBox(queryUI.root());
        Scene scene = new Scene(root);
        scene.getStylesheets().add("query.css"); // Si requis

        primaryStage.setTitle("Test QueryUI – ReCHor");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(120);
        primaryStage.show();
    }
}