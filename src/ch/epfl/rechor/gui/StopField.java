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
import java.util.Objects;

/**
 * UI component combining a TextField and an attached Popup for searching and selecting stops from a
 * StopIndex.
 *
 * @param textField            the text input field for stop names
 * @param selectedStopProperty observable providing the selected stop name
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public record StopField(TextField textField,
                        ObservableValue<String> selectedStopProperty) {
    private static final int MAX_SUGGESTIONS = 30;
    private static final double POPUP_LIST_MAX_HEIGHT = 240.0;

    /**
     * Creates a StopField bound to the given StopIndex.
     *
     * @param stopIndex index providing stop name suggestions (non-null)
     * @return configured StopField instance
     * @throws NullPointerException if stopIndex is null
     */
    public static StopField create(StopIndex stopIndex) {
        Objects.requireNonNull(stopIndex, "stopIndex");

        TextField field = new TextField();
        StringProperty selectedStop = new SimpleStringProperty("");

        ListView<String> suggestionList = new ListView<>();
        suggestionList.setFocusTraversable(false);
        suggestionList.setMaxHeight(POPUP_LIST_MAX_HEIGHT);

        Popup suggestionPopup = new Popup();
        suggestionPopup.setHideOnEscape(false);
        suggestionPopup.getContent().add(suggestionList);

        ChangeListener<String> queryListener =
                (obs, oldVal, newVal)
                        -> updateSuggestions(stopIndex, suggestionList, newVal);

        ChangeListener<Bounds> boundsListener =
                (obs, oldB, newB)
                        -> repositionPopup(field, suggestionPopup, newB);

        Runnable commitSelection = () -> commitSelection(field, suggestionList, selectedStop,
                suggestionPopup);

        configureFocusBehavior(field, queryListener, boundsListener, commitSelection,
                suggestionPopup);
        configureKeyBehavior(field, suggestionPopup, suggestionList, queryListener,
                commitSelection);
        suggestionList.setOnMouseClicked(e -> commitSelection.run());

        return new StopField(field, selectedStop);
    }

    private static void updateSuggestions(StopIndex stopIndex,
                                          ListView<String> listView,
                                          String query) {
        List<String> matches = stopIndex.stopsMatching(
                query == null ? "" : query, MAX_SUGGESTIONS);
        listView.getItems().setAll(matches);
        if (!matches.isEmpty()) {
            listView.getSelectionModel().selectFirst();
            listView.scrollTo(0);
        }
    }

    private static void repositionPopup(TextField field,
                                        Popup popup,
                                        Bounds localBounds) {
        Bounds screen = field.localToScreen(localBounds);
        if (screen != null) {
            popup.setAnchorX(screen.getMinX());
            popup.setAnchorY(screen.getMaxY());
        }
    }

    private static void commitSelection(TextField field,
                                        ListView<String> listView,
                                        StringProperty model,
                                        Popup popup) {
        String selection = listView.getSelectionModel().getSelectedItem();
        String value = (selection != null && !selection.isBlank()) ? selection : "";
        field.setText(value);
        model.set(value);
        popup.hide();
    }

    private static void configureFocusBehavior(TextField field,
                                               ChangeListener<String> queryListener,
                                               ChangeListener<Bounds> boundsListener,
                                               Runnable commit,
                                               Popup popup) {
        field.focusedProperty().addListener(
                (obs, wasFocus, nowFocus) -> {
            if (nowFocus) {
                field.textProperty().addListener(queryListener);
                field.boundsInLocalProperty().addListener(boundsListener);
                Bounds initBounds = field.localToScreen(field.getBoundsInLocal());
                popup.show(field, initBounds.getMinX(), initBounds.getMaxY());
                queryListener.changed(null, null, field.getText());
            } else {
                field.textProperty().removeListener(queryListener);
                field.boundsInLocalProperty().removeListener(boundsListener);
                commit.run();
            }
        });
    }

    private static void configureKeyBehavior(TextField field,
                                             Popup popup,
                                             ListView<String> listView,
                                             ChangeListener<String> queryListener,
                                             Runnable commit) {
        field.addEventHandler(KeyEvent.KEY_PRESSED, evt -> {
            if (!popup.isShowing()) return;
            MultipleSelectionModel<String> sel = listView.getSelectionModel();
            int idx = sel.getSelectedIndex();
            if (evt.getCode() == KeyCode.UP && idx > 0) {
                sel.selectPrevious();
            } else if (evt.getCode() == KeyCode.DOWN && idx < listView.getItems().size() - 1) {
                sel.selectNext();
            } else {
                return;
            }
            listView.scrollTo(sel.getSelectedIndex());
            String suggestion = sel.getSelectedItem();
            if (suggestion != null) {
                field.textProperty().removeListener(queryListener);
                field.setText(suggestion);
                field.positionCaret(suggestion.length());
                field.textProperty().addListener(queryListener);
            }
            evt.consume();
        });
    }

    /**
     * Programmatically sets the field to the given stop name.
     *
     * @param stopName the stop name to select
     */
    public void setTo(String stopName) {
        textField.setText(stopName);
        ((StringProperty) selectedStopProperty).set(stopName);
    }
}