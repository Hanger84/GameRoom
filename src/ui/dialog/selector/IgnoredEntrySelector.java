package ui.dialog.selector;

import data.game.entry.GameEntry;
import data.game.entry.GameEntryUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import ui.Main;
import ui.control.button.gamebutton.GameButton;
import ui.dialog.GameRoomDialog;
import ui.pane.SelectListPane;

import java.io.File;
import java.io.IOException;

import static ui.Main.GENERAL_SETTINGS;

/**
 * Created by LM on 19/08/2016.
 */
public class IgnoredEntrySelector extends GameRoomDialog<ButtonType> {
    public final static int MODE_REMOVE_FROM_LIST = 0;
    public final static int MODE_ADD_TO_LIST = 1;
    private GameEntry[] selectedList;
    private Label statusLabel;

    public IgnoredEntrySelector() throws IOException {
        statusLabel = new Label(Main.getString("loading") + "...");
        rootStackPane.getChildren().add(statusLabel);
        Label titleLabel = new Label(Main.getString("select_games_ignore"));
        titleLabel.setWrapText(true);
        titleLabel.setTooltip(new Tooltip(Main.getString("select_games_ignore")));
        titleLabel.setPadding(new Insets(0 * Main.SCREEN_HEIGHT / 1080
                , 20 * Main.SCREEN_WIDTH / 1920
                , 20 * Main.SCREEN_HEIGHT / 1080
                , 20 * Main.SCREEN_WIDTH / 1920));
        mainPane.setTop(titleLabel);
        mainPane.setPadding(new Insets(30 * Main.SCREEN_HEIGHT / 1080
                , 30 * Main.SCREEN_WIDTH / 1920
                , 20 * Main.SCREEN_HEIGHT / 1080
                , 30 * Main.SCREEN_WIDTH / 1920));
        BorderPane.setAlignment(titleLabel, Pos.CENTER);

        mainPane.setPrefWidth(1.0 / 3.5 * Main.SCREEN_WIDTH);
        mainPane.setPrefHeight(2.0 / 3 * Main.SCREEN_HEIGHT);

        GameEntryList list = new GameEntryList();

        list.addItems(GameEntryUtils.loadIgnoredGames());
        statusLabel.setText(null);

        mainPane.setCenter(list);
        list.setPrefWidth(mainPane.getPrefWidth());

        getDialogPane().getButtonTypes().addAll(
                new ButtonType(Main.getString("ok"), ButtonBar.ButtonData.OK_DONE)
                , new ButtonType(Main.getString("cancel"), ButtonBar.ButtonData.CANCEL_CLOSE));

        setOnHiding(event -> {
            GameEntry[] temp_entries = new GameEntry[list.getSelectedValues().size()];
            for (int i = 0; i < temp_entries.length; i++) {
                temp_entries[i] = (GameEntry) list.getSelectedValues().get(i);
            }
            selectedList = temp_entries;
        });
    }

    public GameEntry[] getSelectedEntries() {
        return selectedList;
    }

    private static class GameEntryList<T> extends SelectListPane {

        public GameEntryList() {
            super(true);

        }

        @Override
        protected ListItem createListItem(Object value) {
            GameFolderItem item = new GameFolderItem(value, this);
            item.setSelected(((GameEntry) value).isIgnored());
            return item;
        }
    }

    private static class GameFolderItem extends SelectListPane.ListItem {
        private GameEntry entry;
        private final static int IMAGE_WIDTH = 45;
        private final static int IMAGE_HEIGHT = 45;
        private StackPane coverPane = new StackPane();

        public GameFolderItem(Object value, SelectListPane parentList) {
            super(value, parentList);
            entry = ((GameEntry) value);
            addContent();
        }

        @Override
        protected void addContent() {
            ImageView iconView = new ImageView();
            double scale = GENERAL_SETTINGS.getUIScale().getScale();
            iconView.setFitHeight(32 * scale);
            iconView.setFitWidth(32 * scale);

            File gamePath = new File(entry.getPath());
            if (!gamePath.exists() || gamePath.isDirectory()) {
                iconView.setImage(entry.getImage(0, 32, 32* GameButton.COVER_HEIGHT_WIDTH_RATIO, true, false));
            } else {
                iconView.setImage(AppSelectorDialog.getIcon(gamePath));
            }
            coverPane.getChildren().add(iconView);

            GridPane.setMargin(coverPane, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 0 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 10 * Main.SCREEN_WIDTH / 1920));
            add(coverPane, columnCount++, 0);

            VBox vbox = new VBox(5 * Main.SCREEN_HEIGHT / 1080);
            Label titleLabel = new Label(entry.getName());
            titleLabel.setTooltip(new Tooltip(gamePath.getAbsolutePath()));

            Label idLabel = new Label(gamePath.getParent());
            idLabel.setStyle("-fx-font-size: 0.7em;");

            vbox.getChildren().addAll(titleLabel, idLabel);
            GridPane.setMargin(vbox, new Insets(10 * Main.SCREEN_HEIGHT / 1080, 0 * Main.SCREEN_WIDTH / 1920, 10 * Main.SCREEN_HEIGHT / 1080, 10 * Main.SCREEN_WIDTH / 1920));
            add(vbox, columnCount++, 0);
        }

    }
}
