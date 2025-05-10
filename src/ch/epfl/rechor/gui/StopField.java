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
public record StopField(TextField textField, ObservableValue<String> stopO) {

    /**
     * Creates a StopField bound to the given StopIndex.
     */
    public static StopField create(StopIndex index) {
        TextField textField = new TextField();
        StringProperty selectedStop = new SimpleStringProperty("");

        ListView<String> listView = new ListView<>();
        listView.setFocusTraversable(false);
        listView.setMaxHeight(240);

        Popup popup = new Popup();
        popup.setHideOnEscape(false);
        popup.getContent().add(listView);

        ChangeListener<String> queryListener = (obs, old, nw) -> {
            List<String> matches = index.stopsMatching(nw == null ? "" : nw, 30);
            listView.getItems().setAll(matches);
            if (!matches.isEmpty()) {
                listView.getSelectionModel().selectFirst();
                listView.scrollTo(0);
            }
        };

        ChangeListener<Bounds> layoutListener = (obs,
                                                 oldB, newB) -> {
            Bounds screen = textField.localToScreen(newB);
            if (screen != null) {
                popup.setAnchorX(screen.getMinX());
                popup.setAnchorY(screen.getMaxY());
            }
        };

        Runnable commit = () -> {
            String sel = listView.getSelectionModel().getSelectedItem();
            String val = (sel != null && !sel.isBlank()) ? sel : "";
            textField.setText(val);
            selectedStop.set(val);
            popup.hide();
        };

        // Show popup and start listening when focused
        textField.focusedProperty().addListener((obs,
                                                 wasFocused, nowFocused) -> {
            if (nowFocused) {
                textField.textProperty().addListener(queryListener);
                textField.boundsInLocalProperty().addListener(layoutListener);
                Bounds b = textField.localToScreen(textField.getBoundsInLocal());
                popup.show(textField, b.getMinX(), b.getMaxY());
                queryListener.changed(null, null, textField.getText());
            } else {
                textField.textProperty().removeListener(queryListener);
                textField.boundsInLocalProperty().removeListener(layoutListener);
                commit.run();
            }
        });

        // Keyboard navigation
        textField.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (!popup.isShowing()) return;
            MultipleSelectionModel<String> selModel = listView.getSelectionModel();
            if ((event.getCode() == KeyCode.UP && selModel.getSelectedIndex() > 0) ||
                    (event.getCode() == KeyCode.DOWN
                            && selModel.getSelectedIndex() < listView.getItems().size() - 1)) {
                if (event.getCode() == KeyCode.UP) selModel.selectPrevious();
                else selModel.selectNext();
                listView.scrollTo(selModel.getSelectedIndex());
                String s = selModel.getSelectedItem();
                if (s != null) {
                    textField.textProperty().removeListener(queryListener);
                    textField.setText(s);
                    textField.positionCaret(s.length());
                    textField.textProperty().addListener(queryListener);
                }
                event.consume();
            }
        });

        // Mouse selection
        listView.setOnMouseClicked(e -> commit.run());

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