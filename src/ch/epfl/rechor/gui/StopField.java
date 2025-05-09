package ch.epfl.rechor.gui;

import ch.epfl.rechor.StopIndex;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Popup;

import java.util.List;

/**
 * UI component combining a TextField and a Popup for searching and selecting stops.
 */
public record StopField(TextField textField,
                        ObservableValue<String> stopO) {

    /**
     * Create a StopField bound to the given StopIndex.
     */
    public static StopField create(StopIndex index) {
        TextField textField = new TextField();
        StringProperty selectedStop = new SimpleStringProperty("");

        Popup popup = new Popup();
        popup.setHideOnEscape(false);

        ListView<String> listView = new ListView<>();
        listView.setFocusTraversable(false);
        listView.setMaxHeight(240);
        popup.getContent().add(listView);

        // Listener to update suggestions
        ChangeListener<String> textListener = (obs, old, nw) -> {
            List<String> results = index.stopsMatching(nw == null ? "" : nw, 30);
            listView.getItems().setAll(results);
            if (!results.isEmpty()) {
                listView.getSelectionModel().selectFirst();
                listView.scrollTo(0);
            }
        };

        // Reposition popup when field moves/resizes
        ChangeListener<Bounds> boundsListener = (obs, oldB, newB) -> {
            Bounds screen = textField.localToScreen(newB);
            if (screen != null) {
                popup.setAnchorX(screen.getMinX());
                popup.setAnchorY(screen.getMaxY());
            }
        };

        // Commit selection helper: only commit if a suggestion is selected; otherwise clear
        Runnable commitSelection = () -> {
            String sel = listView.getSelectionModel().getSelectedItem();
            if (sel != null && !sel.isBlank()) {
                textField.setText(sel);
                selectedStop.set(sel);
            } else {
                textField.setText("");
                selectedStop.set("");
            }
            popup.hide();
        };

        // Mouse click commits
        listView.setOnMouseClicked(e -> commitSelection.run());

        // Arrow keys navigate suggestions without resetting
        textField.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (!popup.isShowing()) return;
            MultipleSelectionModel<String> selModel = listView.getSelectionModel();
            if (event.getCode() == KeyCode.UP && selModel.getSelectedIndex() > 0) {
                selModel.selectPrevious();
                listView.scrollTo(selModel.getSelectedIndex());
                String s = selModel.getSelectedItem();
                if (s != null) {
                    textField.textProperty().removeListener(textListener);
                    textField.setText(s);
                    textField.positionCaret(s.length());
                    textField.textProperty().addListener(textListener);
                }
                event.consume();
            } else if (event.getCode() == KeyCode.DOWN && selModel.getSelectedIndex() < listView.getItems().size() - 1) {
                selModel.selectNext();
                listView.scrollTo(selModel.getSelectedIndex());
                String s = selModel.getSelectedItem();
                if (s != null) {
                    textField.textProperty().removeListener(textListener);
                    textField.setText(s);
                    textField.positionCaret(s.length());
                    textField.textProperty().addListener(textListener);
                }
                event.consume();
            }
        });

        // Show/hide popup on focus changes
        textField.focusedProperty().addListener((obs, wasFocused, nowFocused) -> {
            if (nowFocused) {
                Bounds b = textField.localToScreen(textField.getBoundsInLocal());
                if (b != null) popup.show(textField, b.getMinX(), b.getMaxY());
                textField.textProperty().addListener(textListener);
                textField.boundsInLocalProperty().addListener(boundsListener);
                textListener.changed(null, null, textField.getText());
            } else {
                textField.textProperty().removeListener(textListener);
                textField.boundsInLocalProperty().removeListener(boundsListener);
                commitSelection.run();
            }
        });

        return new StopField(textField, selectedStop);
    }

    /**
     * Programmatically set the field to a stop name.
     */
    public void setTo(String stopName) {
        textField.setText(stopName);
        ((StringProperty) stopO).set(stopName);
    }
}