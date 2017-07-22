package ui.scene;

import com.mashape.unirest.http.exceptions.UnirestException;
import data.game.entry.*;
import data.game.scraper.IGDBScraper;
import data.game.scraper.OnDLDoneHandler;
import data.http.YoutubeSoundtrackScrapper;
import data.http.images.ImageUtils;
import data.io.FileUtils;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.apache.commons.lang.StringEscapeUtils;
import org.controlsfx.control.CheckComboBox;
import org.json.JSONException;
import org.json.JSONObject;
import system.application.settings.PredefinedSetting;
import system.os.Terminal;
import system.os.WindowsShortcut;
import ui.GeneralToast;
import ui.Main;
import ui.control.ValidEntryCondition;
import ui.control.button.HelpButton;
import ui.control.button.ImageButton;
import ui.control.button.gamebutton.GameButton;
import ui.control.textfield.AppPathField;
import ui.control.textfield.CMDTextField;
import ui.control.textfield.PathTextField;
import ui.control.textfield.PlayTimeField;
import ui.dialog.GameRoomAlert;
import ui.dialog.SearchDialog;
import ui.dialog.selector.AppSelectorDialog;
import ui.dialog.selector.IGDBImageSelector;
import ui.pane.OnItemSelectedHandler;
import ui.pane.SelectListPane;
import ui.scene.exitaction.ClassicExitAction;
import ui.scene.exitaction.ExitAction;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Optional;
import java.util.regex.Pattern;

import static data.io.FileUtils.getExtension;
import static system.application.settings.GeneralSettings.settings;
import static ui.Main.*;
import static ui.control.button.gamebutton.GameButton.COVER_SCALE_EFFECT_FACTOR;

/**
 * Created by LM on 03/07/2016.
 */
public class GameEditScene extends BaseScene {
    public final static int MODE_ADD = 0;
    private final static int MODE_EDIT = 1;

    private final static double COVER_BLUR_EFFECT_RADIUS = 10;
    private final static double COVER_BRIGHTNESS_EFFECT_FACTOR = 0.1;

    private BorderPane wrappingPane;
    private GridPane contentPane;

    private HBox buttonsBox;

    private ImageView coverView;
    private File[] chosenImageFiles = new File[GameEntry.DEFAULT_IMAGES_PATHS.length];
    private FileChooser imageChooser = new FileChooser();

    private ArrayList<ValidEntryCondition> validEntriesConditions = new ArrayList<>();

    private GameEntry entry;
    private int mode;

    private ExitAction onExitAction;

    private int row_count = 0;

    public GameEditScene(BaseScene previousScene, File chosenFile) {
        super(new StackPane(), previousScene.getParentStage());
        mode = MODE_ADD;

        String name = chosenFile.getName();
        if (!getExtension(name).equals("")) {
            int extensionIndex = name.lastIndexOf(getExtension(name));
            if (extensionIndex != -1) {
                name = name.substring(0, extensionIndex - 1);
            }
        }
        String path = chosenFile.getAbsolutePath();
        try {
            if (FileUtils.getExtension(chosenFile).equals("lnk")) {
                WindowsShortcut shortcut = new WindowsShortcut(chosenFile);
                chosenFile = new File(shortcut.getRealFilename());
                path = chosenFile.getAbsolutePath();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            //not a shortcut :)
        }
        entry = new GameEntry(name);
        entry.setPath(path);
        init(previousScene, null);
    }

    public GameEditScene(BaseScene previousScene, GameEntry entry, Image coverImage) {
        this(previousScene, entry, MODE_EDIT, coverImage);
    }

    public GameEditScene(BaseScene previousScene, GameEntry entry, int mode, Image coverImage) {
        super(new StackPane(), previousScene.getParentStage());
        this.mode = mode;
        this.entry = entry;
        this.entry.setSavedLocally(false);
        init(previousScene, coverImage);
    }

    private void init(BaseScene previousScene, Image coverImage) {
        onExitAction = new ClassicExitAction(this, previousScene.getParentStage(), previousScene);

        for (int i = 0; i < chosenImageFiles.length; i++) {
            if (entry.getImagePath(i) != null) {
                this.chosenImageFiles[i] = entry.getImagePath(i);
            }
        }

        imageChooser = new FileChooser();
        imageChooser.setTitle(Main.getString("select_picture"));
        imageChooser.setInitialDirectory(
                new File(System.getProperty("user.home"))
        );
        imageChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Images", "*.*"),
                new FileChooser.ExtensionFilter("JPG", "*.jpg"),
                new FileChooser.ExtensionFilter("BMP", "*.bmp"),
                new FileChooser.ExtensionFilter("PNG", "*.png")
        );
        this.previousScene = previousScene;
        initTop();
        initCenter(coverImage);
        initBottom();
    }

    private void initBottom() {
        buttonsBox = new HBox();
        buttonsBox.setSpacing(30 * SCREEN_WIDTH / 1920);
        Button addButton = new Button(Main.getString("add") + "!");
        if (mode == MODE_EDIT) {
            addButton.setText(Main.getString("save") + "!");
        }
        addButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                boolean allConditionsMet = true;
                for (ValidEntryCondition condition : validEntriesConditions) {
                    boolean conditionValid = condition.isValid();
                    allConditionsMet = allConditionsMet && conditionValid;
                    if (!conditionValid) {
                        condition.onInvalid();
                        GameRoomAlert.error(condition.message.toString());
                    }
                }
                if (allConditionsMet) {
                    if (entry.isToAdd()) {
                        entry.setToAdd(false);
                    }
                    GeneralToast.displayToast(Main.getString("saving") + " " + entry.getName(), getParentStage(), GeneralToast.DURATION_SHORT);

                    entry.setSavedLocally(true);
                    entry.saveEntry();

                    for (int i = 0; i < chosenImageFiles.length; i++) {
                        if (chosenImageFiles[i] != null) {
                            try {
                                entry.updateImage(i, chosenImageFiles[i]);
                            } catch (IOException e) {
                                GameRoomAlert.error(Main.getString("error_move_images"));
                            }
                        }
                    }

                    switch (mode) {
                        case MODE_ADD:
                            entry.setAddedDate(LocalDateTime.now());
                            MAIN_SCENE.addGame(entry);
                            GeneralToast.displayToast(entry.getName() + Main.getString("added_to_your_lib"), getParentStage());
                            break;
                        case MODE_EDIT:
                            MAIN_SCENE.updateGame(entry);
                            GeneralToast.displayToast(Main.getString("changes_saved"), getParentStage());
                            break;
                        default:
                            break;
                    }
                    //fadeTransitionTo(MAIN_SCENE, getParentStage());
                    if (previousScene instanceof GameInfoScene) {
                        ((GameInfoScene) previousScene).updateWithEditedEntry(entry);
                    }
                    onExitAction.run();
                }
            }
        });
        Button igdbButton = new Button(Main.getString("fetch_from_igdb"));
        igdbButton.setOnAction(new EventHandler<ActionEvent>() {
                                   @Override
                                   public void handle(ActionEvent event) {
                                       SearchDialog dialog = new SearchDialog(createDoNotUpdateFielsMap(), entry.getName());
                                       Optional<ButtonType> result = dialog.showAndWait();
                                       result.ifPresent(val -> {
                                           if (!val.getButtonData().isCancelButton()) {
                                               GameEntry gameEntry = dialog.getSelectedEntry();
                                               if (gameEntry != null) {
                                                   onNewEntryData(gameEntry, dialog.getDoNotUpdateFieldsMap());
                                               }
                                           }
                                       });

                                   }
                               }

        );

        buttonsBox.getChildren().addAll(igdbButton, addButton);

        BorderPane.setMargin(buttonsBox, new Insets(10 * SCREEN_WIDTH / 1920, 30 * SCREEN_WIDTH / 1920, 30 * SCREEN_WIDTH / 1920, 30 * SCREEN_WIDTH / 1920));
        buttonsBox.setAlignment(Pos.BOTTOM_RIGHT);
        wrappingPane.setBottom(buttonsBox);

    }

    private void initCenter(Image coverImage) {
        ImageUtils.setWindowBackground(entry.getImagePath(1), backgroundView);

        contentPane = new GridPane();
        //contentPane.setGridLinesVisible(true);
        contentPane.setVgap(20 * SCREEN_WIDTH / 1920);
        contentPane.setHgap(10 * SCREEN_WIDTH / 1920);
        ColumnConstraints cc1 = new ColumnConstraints();
        cc1.setPercentWidth(20);
        contentPane.getColumnConstraints().add(cc1);
        ColumnConstraints cc2 = new ColumnConstraints();
        cc2.setPercentWidth(80);
        contentPane.getColumnConstraints().add(cc2);

        /**************************NAME*********************************************/
        createLineForProperty("game_name", entry.getName(), new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                entry.setName(newValue);
            }
        });
        validEntriesConditions.add(new ValidEntryCondition() {

            @Override
            public boolean isValid() {
                if (entry.getName().equals("")) {
                    message.replace(0, message.length(), Main.getString("invalid_name_empty"));
                    return false;
                }
                return true;
            }

            @Override
            public void onInvalid() {
                setLineInvalid("game_name");
            }
        });
        /**************************SERIE*********************************************/

        // create the data to show in the CheckComboBox
        final ObservableList<Serie> allSeries = FXCollections.observableArrayList();
        allSeries.addAll(Serie.values());
        allSeries.sort(new Comparator<Serie>() {
            @Override
            public int compare(Serie o1, Serie o2) {
                if (o1 == null || o1.getName() == null) {
                    return -1;
                }
                if (o2 == null || o2.getName() == null) {
                    return 1;
                }
                return o1.getName().compareTo(o2.getName());
            }
        });
        // Create the CheckComboBox with the data
        final ComboBox<Serie> serieComboBox = new ComboBox<Serie>(allSeries);
        serieComboBox.setId("serie");
        if (entry.getGenres() != null) {
            serieComboBox.getSelectionModel().select(entry.getSerie());
        }
        serieComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            entry.setSerie(newValue);
        });

        serieComboBox.getSelectionModel().select(entry.getSerie());

        contentPane.add(createTitleLabel("serie", false), 0, row_count);
        contentPane.add(serieComboBox, 1, row_count);
        row_count++;

        /**************************PATH*********************************************/
        contentPane.add(createTitleLabel("game_path", false), 0, row_count);
        Node pathNode = new Label();
        if (!entry.isSteamGame()) {
            AppPathField gamePathField = new AppPathField(entry.getPath(), getWindow(), PathTextField.FILE_CHOOSER_APPS, Main.getString("select_a_file"), entry.getPlatform().getSupportedExtensions());
            gamePathField.getTextField().setPrefColumnCount(50);
            gamePathField.setId("game_path");
            gamePathField.getTextField().textProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                    entry.setPath(newValue);
                }
            });
            pathNode = gamePathField;
        } else {
            pathNode = new Label(entry.getPath());
            pathNode.setFocusTraversable(false);
        }

        validEntriesConditions.add(new ValidEntryCondition() {
            @Override
            public boolean isValid() {
                if (entry.getPath().equals("")) {
                    message.replace(0, message.length(), Main.getString("invalid_path_empty"));
                    return false;
                }
                File file = new File(entry.getPath());
                Pattern pattern = Pattern.compile("^steam:\\/\\/rungameid\\/\\d*$");
                boolean isSteamGame = pattern.matcher(entry.getPath().trim()).matches();
                if (!isSteamGame && !file.exists()) {
                    message.replace(0, message.length(), Main.getString("invalid_path_not_file"));
                    return false;
                } else if (!isSteamGame && file.isDirectory()) {
                    try {
                        AppSelectorDialog selector = new AppSelectorDialog(new File(entry.getPath()), entry.getPlatform().getSupportedExtensions());
                        selector.searchApps();
                        Optional<ButtonType> appOptionnal = selector.showAndWait();

                        final boolean[] result = {true};

                        appOptionnal.ifPresent(pairs -> {
                            if (pairs.getButtonData().equals(ButtonBar.ButtonData.OK_DONE)) {
                                entry.setPath(selector.getSelectedFile().getAbsolutePath());
                            } else {
                                result[0] = false;
                            }
                        });
                        if (!result[0]) {
                            message.replace(0, message.length(), Main.getString("invalid_path_not_file"));
                            return result[0];
                        }
                    } catch (IllegalArgumentException e) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public void onInvalid() {
                setLineInvalid("game_path");
            }
        });

        contentPane.add(pathNode, 1, row_count);
        row_count++;

        /**************************RUN AS ADMIN ****************************************/
        contentPane.add(createTitleLabel("run_as_admin", false), 0, row_count);
        CheckBox adminCheckBox = new CheckBox();
        adminCheckBox.setSelected(entry.mustRunAsAdmin());
        adminCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            entry.setRunAsAdmin(newValue);
        });
        adminCheckBox.setDisable(!checkPowershellAvailable());

        contentPane.add(adminCheckBox, 1, row_count);
        row_count++;
        /**************************PLAYTIME*********************************************/
        contentPane.add(createTitleLabel("play_time", false), 0, row_count);
        PlayTimeField playTimeField = new PlayTimeField(entry);

        playTimeField.setId("play_time");
        contentPane.add(playTimeField, 1, row_count);
        row_count++;

        /***************************SEPARATORS******************************************/
        Separator s1 = new Separator();
        contentPane.add(s1, 0, row_count);
        row_count++;

        /**************************RELEASE DATE*********************************************/
        TextField releaseDateField = createLineForProperty("release_date", entry.getReleaseDate() != null ? GameEntry.DATE_DISPLAY_FORMAT.format(entry.getReleaseDate()) : "", null);
        releaseDateField.setPromptText(Main.getString("date_example"));
        releaseDateField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (!releaseDateField.getText().equals("")) {
                    try {
                        LocalDateTime time = LocalDateTime.from(LocalDate.parse(releaseDateField.getText(), GameEntry.DATE_DISPLAY_FORMAT).atStartOfDay());
                        entry.setReleaseDate(time);
                    } catch (DateTimeParseException e) {
                        //well date not valid yet
                    }
                } else {
                    entry.setReleaseDate(null);
                }
            }
        });
        validEntriesConditions.add(new ValidEntryCondition() {
            @Override
            public boolean isValid() {
                if (!releaseDateField.getText().equals("")) {
                    try {
                        LocalDateTime time = LocalDateTime.from(LocalDate.parse(releaseDateField.getText(), GameEntry.DATE_DISPLAY_FORMAT).atStartOfDay());
                        entry.setReleaseDate(time);
                    } catch (DateTimeParseException e) {
                        message.replace(0, message.length(), Main.getString("invalid_release_date"));
                        return false;
                    }
                } else {
                    entry.setReleaseDate(null);
                }
                return true;
            }

            @Override
            public void onInvalid() {
                setLineInvalid("year");
            }
        });

        /**************************PLATFORM*********************************************/
        if (!entry.getPlatform().isPC() || entry.getPlatform().equals(data.game.entry.Platform.NONE) && SUPPORTER_MODE) {
            try {
                data.game.entry.Platform.initWithDb();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            final ObservableList<data.game.entry.Platform> platforms = FXCollections.observableArrayList(data.game.entry.Platform.getEmulablePlatforms());
            platforms.add(0, data.game.entry.Platform.NONE);

            // Create the CheckComboBox with the data
            final ComboBox<data.game.entry.Platform> platformComboBox = new ComboBox<data.game.entry.Platform>(platforms);
            platformComboBox.setId("platform");
            if (entry.getPlatform() != null) {
                platformComboBox.getSelectionModel().select(entry.getPlatform());
            }
            Node finalPathNode = pathNode;
            platformComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                entry.setPlatform(newValue);
                if (finalPathNode instanceof PathTextField) {
                    ((PathTextField) finalPathNode).setExtensions(newValue.getSupportedExtensions());
                }
            });
            contentPane.add(createTitleLabel("platform", false), 0, row_count);
            contentPane.add(platformComboBox, 1, row_count);
            row_count++;
        }

        /************************** DEVS AND PUBLISHER*********************************************/

        final ObservableList<Company> allCompanies = FXCollections.observableArrayList();
        allCompanies.addAll(Company.values());
        allCompanies.sort(Comparator.comparing(Company::getName));

        /**************************DEVELOPER*********************************************/
        // Create the CheckComboBox with the data
        final CheckComboBox<Company> devComboBox = new CheckComboBox<Company>(allCompanies);
        devComboBox.setId("developer");
        if (entry.getDevelopers() != null) {
            for (Company developer : entry.getDevelopers()) {
                devComboBox.getCheckModel().check(developer);
            }
        }
        devComboBox.getCheckModel().getCheckedItems().addListener((ListChangeListener<Company>) c -> {
            entry.setDevelopers(devComboBox.getCheckModel().getCheckedItems());
        });
        contentPane.add(createTitleLabel("developer", false), 0, row_count);
        contentPane.add(devComboBox, 1, row_count);
        row_count++;

        /**************************PUBLISHER*********************************************/
        // Create the CheckComboBox with the data
        final CheckComboBox<Company> pubComboBox = new CheckComboBox<Company>(allCompanies);
        pubComboBox.setId("publisher");
        if (entry.getPublishers() != null) {
            for (Company publisher : entry.getPublishers()) {
                pubComboBox.getCheckModel().check(publisher);
            }
        }
        pubComboBox.getCheckModel().getCheckedItems().addListener((ListChangeListener<Company>) c -> {
            entry.setPublishers(pubComboBox.getCheckModel().getCheckedItems());
        });
        contentPane.add(createTitleLabel("publisher", false), 0, row_count);
        contentPane.add(pubComboBox, 1, row_count);
        row_count++;

        /**************************GENRE*********************************************/

        // create the data to show in the CheckComboBox
        final ObservableList<GameGenre> allGamesGenre = FXCollections.observableArrayList();
        allGamesGenre.addAll(GameGenre.values());
        allGamesGenre.sort(new Comparator<GameGenre>() {
            @Override
            public int compare(GameGenre o1, GameGenre o2) {
                return o1.getDisplayName().compareTo(o2.getDisplayName());
            }
        });
        // Create the CheckComboBox with the data
        final CheckComboBox<GameGenre> genreComboBox = new CheckComboBox<GameGenre>(allGamesGenre);
        genreComboBox.setId("genre");
        if (entry.getGenres() != null) {
            for (GameGenre genre : entry.getGenres()) {
                genreComboBox.getCheckModel().check(genreComboBox.getCheckModel().getItemIndex(genre));
            }
        }
        genreComboBox.getCheckModel().getCheckedItems().addListener(new ListChangeListener<GameGenre>() {
            @Override
            public void onChanged(Change<? extends GameGenre> c) {
                ArrayList<GameGenre> newGenres = new ArrayList<>();
                newGenres.addAll(genreComboBox.getCheckModel().getCheckedItems());
                entry.setGenres(newGenres);
            }
        });
        contentPane.add(createTitleLabel("genre", false), 0, row_count);
        contentPane.add(genreComboBox, 1, row_count);
        row_count++;

        /**************************THEME*********************************************/

        // create the data to show in the CheckComboBox
        final ObservableList<GameTheme> allGamesTheme = FXCollections.observableArrayList();
        allGamesTheme.addAll(GameTheme.values());
        allGamesTheme.sort(new Comparator<GameTheme>() {
            @Override
            public int compare(GameTheme o1, GameTheme o2) {
                return o1.getDisplayName().compareTo(o2.getDisplayName());
            }
        });
        // Create the CheckComboBox with the data
        final CheckComboBox<GameTheme> themeComboBox = new CheckComboBox<GameTheme>(allGamesTheme);
        themeComboBox.setId("theme");
        if (entry.getThemes() != null) {
            for (GameTheme theme : entry.getThemes()) {
                themeComboBox.getCheckModel().check(themeComboBox.getCheckModel().getItemIndex(theme));
            }
        }
        themeComboBox.getCheckModel().getCheckedItems().addListener(new ListChangeListener<GameTheme>() {
            @Override
            public void onChanged(Change<? extends GameTheme> c) {
                ArrayList<GameTheme> newThemes = new ArrayList<>();
                newThemes.addAll(themeComboBox.getCheckModel().getCheckedItems());
                entry.setThemes(newThemes);
            }
        });
        contentPane.add(createTitleLabel("theme", false), 0, row_count);
        contentPane.add(themeComboBox, 1, row_count);
        row_count++;

        /**************************SCREENSHOT*********************************************/
        OnDLDoneHandler screenshotDlDoneHandler = outputFile -> Platform.runLater(() -> {
            ImageUtils.transitionToWindowBackground(outputFile, backgroundView);
            chosenImageFiles[1] = outputFile;
        });
        contentPane.add(createTitleLabel("wallpaper", false), 0, row_count);

        HBox screenShotButtonsBox = new HBox();
        screenShotButtonsBox.setSpacing(20 * Main.SCREEN_WIDTH / 1920);
        screenShotButtonsBox.setAlignment(Pos.CENTER_LEFT);

        double imgSize = settings().getWindowWidth() / 24;
        //ImageButton screenshotFileButton = new ImageButton(new Image("res/ui/folderButton.png", , settings().getWindowWidth() / 24, false, true));
        ImageButton screenshotFileButton = new ImageButton("folder-button", imgSize, imgSize);
        screenshotFileButton.setOnAction(event -> {
            chosenImageFiles[1] = imageChooser.showOpenDialog(getParentStage());
            screenshotDlDoneHandler.run(chosenImageFiles[1]);
        });
        Label orLabel = new Label(Main.getString("or"));

        Button screenshotIGDBButton = new Button(Main.getString("IGDB"));
        screenshotIGDBButton.setOnAction(new EventHandler<ActionEvent>() {
                                             @Override
                                             public void handle(ActionEvent event) {
                                                 if (entry.getIgdb_id() != -1) {
                                                     GameEntry gameEntry = entry;
                                                     try {
                                                         JSONObject gameData = IGDBScraper.getGameData(gameEntry.getIgdb_id());
                                                         if (gameData != null) {
                                                             gameEntry.setIgdb_imageHashs(IGDBScraper.getScreenshotHash(gameData));
                                                             openImageSelector(gameEntry);
                                                         } else {
                                                             GameRoomAlert.info(Main.getString("error_no_screenshot_igdb"));
                                                         }
                                                     } catch (JSONException jse) {
                                                         if (jse.toString().contains("[\"screenshots\"] not found")) {
                                                             GameRoomAlert.error(Main.getString("no_screenshot_for_this_game"));
                                                         } else {
                                                             jse.printStackTrace();
                                                         }
                                                     } catch (UnirestException e) {
                                                         GameRoomAlert.errorIGDB();
                                                         LOGGER.error(e.getMessage());
                                                     }
                                                 } else {
                                                     SearchDialog dialog = new SearchDialog(createDoNotUpdateFielsMap(), entry.getName());
                                                     Optional<ButtonType> result = dialog.showAndWait();
                                                     result.ifPresent(val -> {
                                                         if (!val.getButtonData().isCancelButton()) {
                                                             GameEntry gameEntry = dialog.getSelectedEntry();
                                                             if (val != null) {
                                                                 openImageSelector(gameEntry);
                                                             }
                                                         }
                                                     });
                                                 }
                                             }
                                         }

        );

        screenShotButtonsBox.getChildren().addAll(screenshotFileButton, orLabel, screenshotIGDBButton);

        contentPane.add(screenShotButtonsBox, 1, row_count);
        row_count++;

        /**************************DESCRIPTION*********************************************/
        contentPane.add(createTitleLabel("game_description", false), 0, row_count);
        TextArea gameDescriptionField = new TextArea(entry.getDescription());
        gameDescriptionField.setWrapText(true);
        gameDescriptionField.setId("game_description");
        gameDescriptionField.setPrefRowCount(4);
        gameDescriptionField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                entry.setDescription(newValue);
            }
        });
        contentPane.add(gameDescriptionField, 1, row_count);
        row_count++;

        /**************************YOUTUBE*********************************************/
        if (!settings().getBoolean(PredefinedSetting.DISABLE_GAME_MAIN_THEME)) {
            contentPane.add(createTitleLabel("youtube_soundtrack", true), 0, row_count);
            TextField youtubeSoundtrackField = new TextField(entry.getYoutubeSoundtrackHash().equals("") ? "" : YoutubeSoundtrackScrapper.toYoutubeUrl(entry.getYoutubeSoundtrackHash()));
            youtubeSoundtrackField.setId("youtube_soundtrack");
            youtubeSoundtrackField.textProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                    if (newValue.equals("")) {
                        entry.setYoutubeSoundtrackHash("");
                    }
                    String hash = YoutubeSoundtrackScrapper.hashFromYoutubeUrl(newValue);
                    if (hash != null) {
                        entry.setYoutubeSoundtrackHash(hash);
                    }
                }
            });
            contentPane.add(youtubeSoundtrackField, 1, row_count);
            row_count++;
        }


        /**************************CMD & ARGS*********************************************/
        if (settings().getBoolean(PredefinedSetting.ADVANCED_MODE)) {
            contentPane.add(createTitleLabel("args", true), 0, row_count);
            TextField argsField = new TextField(entry.getArgs());
            argsField.setId("args");
            argsField.textProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                    entry.setArgs(newValue);
                }
            });
            contentPane.add(argsField, 1, row_count);
            row_count++;

            contentPane.add(createTitleLabel("cmd_before", true), 0, row_count);
            CMDTextField cmdBeforeField = new CMDTextField(entry.getCmd(GameEntry.CMD_BEFORE_START));
            cmdBeforeField.setWrapText(true);
            cmdBeforeField.setId("cmd_before");
            cmdBeforeField.textProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                    entry.setCmd(GameEntry.CMD_BEFORE_START, newValue);
                }
            });
            contentPane.add(cmdBeforeField, 1, row_count);
            row_count++;

            contentPane.add(createTitleLabel("cmd_after", true), 0, row_count);
            CMDTextField cmdAfterField = new CMDTextField(entry.getCmd(GameEntry.CMD_AFTER_END));
            cmdAfterField.setWrapText(true);
            cmdAfterField.setId("cmd_after");
            cmdAfterField.textProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                    entry.setCmd(GameEntry.CMD_AFTER_END, newValue);
                }
            });
            contentPane.add(cmdAfterField, 1, row_count);
            row_count++;
        }

        /********************END FOR PROPERTIES********************************************/

        GridPane coverAndPropertiesPane = new GridPane();

        coverAndPropertiesPane.setVgap(20 * SCREEN_WIDTH / 1920);
        coverAndPropertiesPane.setHgap(60 * SCREEN_WIDTH / 1920);

        Pane coverPane = createLeft(coverImage);
        coverAndPropertiesPane.add(coverPane, 0, 0);
        coverAndPropertiesPane.setPadding(new Insets(50 * SCREEN_HEIGHT / 1080, 50 * SCREEN_WIDTH / 1920, 20 * SCREEN_HEIGHT / 1080, 50 * SCREEN_WIDTH / 1920));

        contentPane.setPadding(new Insets(30 * SCREEN_HEIGHT / 1080, 30 * SCREEN_WIDTH / 1920, 30 * SCREEN_HEIGHT / 1080, 30 * SCREEN_WIDTH / 1920));
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setContent(contentPane);
        coverAndPropertiesPane.add(scrollPane, 1, 0);

        wrappingPane.setCenter(coverAndPropertiesPane);
    }

    private TextField createLineForProperty(String property, String initialValue, ChangeListener<String> changeListener) {
        Node titleNode = createTitleLabel(property, false);
        contentPane.add(titleNode, 0, row_count);
        TextField textField = new TextField(initialValue);
        textField.setPrefColumnCount(50);
        textField.setId(property);
        if (changeListener != null) {
            textField.textProperty().addListener(changeListener);
        }
        contentPane.add(textField, 1, row_count);
        row_count++;
        return textField;
    }

    private void setLineInvalid(String property_key) {
        String style = "-fx-text-inner-color: red;\n";
        for (Node node : contentPane.getChildren()) {
            if (node.getId() != null && node.getId().equals(property_key)) {
                node.setStyle(style);
                node.focusedProperty().addListener(new ChangeListener<Boolean>() {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                        if (newValue) {
                            node.setStyle("");
                        }
                    }
                });
                break;
            }
        }
    }

    private void updateLineProperty(String property, Object newValue, HashMap<String, Boolean> doNotUpdateFieldsMap) {
        if (doNotUpdateFieldsMap.get(property) == null || !doNotUpdateFieldsMap.get(property))
            if (newValue != null && !newValue.equals("")) {
                for (Node node : contentPane.getChildren()) {
                    if (node.getId() != null && node.getId().equals(property)) {
                        if (newValue instanceof String) {
                            if (node instanceof TextField) {
                                ((TextField) node).setText((String) newValue);
                            } else if (node instanceof TextArea) {
                                ((TextArea) node).setText((String) newValue);
                            } else if (node instanceof PathTextField) {
                                ((PathTextField) node).setText((String) newValue);
                            }
                        } else if (property.equals("genre")) {
                            ((CheckComboBox) node).getCheckModel().clearChecks();
                            if (node instanceof CheckComboBox) {
                                for (GameGenre genre : (ArrayList<GameGenre>) newValue) {
                                    ((CheckComboBox) node).getCheckModel().check(genre);
                                }
                            }
                        } else if (property.equals("theme")) {
                            ((CheckComboBox) node).getCheckModel().clearChecks();
                            if (node instanceof CheckComboBox) {
                                for (GameTheme theme : (ArrayList<GameTheme>) newValue) {
                                    ((CheckComboBox) node).getCheckModel().check(theme);
                                }
                            }
                        } else if (property.equals("developer")) {
                            if (node instanceof CheckComboBox) {
                                ((CheckComboBox) node).getCheckModel().clearChecks();
                                for (Company dev : (ArrayList<Company>) newValue) {
                                    ((CheckComboBox) node).getCheckModel().check(dev);
                                }
                            }
                        } else if (property.equals("publisher")) {
                            if (node instanceof CheckComboBox) {
                                ((CheckComboBox) node).getCheckModel().clearChecks();
                                for (Company pub : (ArrayList<Company>) newValue) {
                                    ((CheckComboBox) node).getCheckModel().check(pub);
                                }
                            }
                        } else if (property.equals("serie")) {
                            if (node instanceof ComboBox) {
                                ((ComboBox) node).getSelectionModel().clearSelection();
                                ((ComboBox<Serie>) node).getSelectionModel().select((Serie) newValue);
                            }
                        } else if (newValue instanceof LocalDateTime) {
                            if (node instanceof TextField) {
                                ((TextField) node).setText(newValue != null ? GameEntry.DATE_DISPLAY_FORMAT.format((LocalDateTime) newValue) : "");
                            }
                        }
                        break;
                    }
                }
            }
    }

    private Pane createLeft(Image coverImage) {
        StackPane pane = new StackPane();
        double coverWidth = settings().getWindowHeight() * 2 / (3 * GameButton.COVER_HEIGHT_WIDTH_RATIO);
        double coverHeight = settings().getWindowHeight() * 2 / 3;

        coverView = new ImageView();
        coverView.setFitWidth(coverWidth);
        coverView.setFitHeight(coverHeight);

        Image defaultImage = new Image("res/defaultImages/cover1024.jpg", coverWidth, coverHeight, false, true);

        ImageView defaultImageView = new ImageView(defaultImage);
        defaultImageView.setFitWidth(coverWidth);
        defaultImageView.setFitHeight(coverHeight);


        if (coverImage != null) {
            ImageUtils.transitionToCover(coverImage, coverView);
        } else {
            ImageUtils.getExecutorService().submit(() -> {
                Platform.runLater(() -> {
                    ImageUtils.transitionToCover(entry.getImagePath(0), coverWidth, coverHeight, coverView);
                });
            });
        }

        double imgSize = settings().getWindowWidth() / 15;
        //ImageButton changeImageButton = new ImageButton(new Image("res/ui/folderButton.png", settings().getWindowWidth() / 12, settings().getWindowWidth() / 12, false, true));
        ImageButton changeImageButton = new ImageButton("folder-button", imgSize, imgSize);
        changeImageButton.setOpacity(0);
        changeImageButton.setFocusTraversable(false);
        changeImageButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                chosenImageFiles[0] = imageChooser.showOpenDialog(getParentStage());

                ImageUtils.transitionToCover(chosenImageFiles[0],
                        settings().getWindowHeight() * 2 / (3 * GameButton.COVER_HEIGHT_WIDTH_RATIO),
                        settings().getWindowHeight() * 2 / 3,
                        coverView);
            }
        });
        //COVER EFFECTS
        DropShadow dropShadowBG = new DropShadow();
        dropShadowBG.setOffsetX(6.0 * SCREEN_WIDTH / 1920);
        dropShadowBG.setOffsetY(4.0 * SCREEN_HEIGHT / 1080);
        ColorAdjust colorAdjustBG = new ColorAdjust();
        colorAdjustBG.setBrightness(0.0);
        colorAdjustBG.setInput(dropShadowBG);
        GaussianBlur blurBG = new GaussianBlur(0.0);
        blurBG.setInput(colorAdjustBG);

        ColorAdjust colorAdjustIMG = new ColorAdjust();
        colorAdjustIMG.setBrightness(0.0);
        GaussianBlur blurIMG = new GaussianBlur(0.0);
        blurIMG.setInput(colorAdjustIMG);

        coverView.setEffect(blurIMG);
        defaultImageView.setEffect(blurBG);


        pane.setOnMouseEntered(e -> {
            // playButton.setVisible(true);
            //infoButton.setVisible(true);
            //coverPane.requestFocus();
            Timeline fadeInTimeline = new Timeline(
                    new KeyFrame(Duration.seconds(0),
                            new KeyValue(dropShadowBG.offsetXProperty(), dropShadowBG.offsetXProperty().getValue(), Interpolator.LINEAR),
                            new KeyValue(dropShadowBG.offsetYProperty(), dropShadowBG.offsetYProperty().getValue(), Interpolator.LINEAR),
                            new KeyValue(blurBG.radiusProperty(), blurBG.radiusProperty().getValue(), Interpolator.LINEAR),
                            new KeyValue(blurIMG.radiusProperty(), blurIMG.radiusProperty().getValue(), Interpolator.LINEAR),
                            new KeyValue(changeImageButton.opacityProperty(), changeImageButton.opacityProperty().getValue(), Interpolator.EASE_OUT),
                            new KeyValue(colorAdjustIMG.brightnessProperty(), colorAdjustIMG.brightnessProperty().getValue(), Interpolator.LINEAR),
                            new KeyValue(colorAdjustBG.brightnessProperty(), colorAdjustBG.brightnessProperty().getValue(), Interpolator.LINEAR)),
                    new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                            new KeyValue(dropShadowBG.offsetXProperty(), dropShadowBG.offsetXProperty().getValue() / COVER_SCALE_EFFECT_FACTOR, Interpolator.LINEAR),
                            new KeyValue(dropShadowBG.offsetYProperty(), dropShadowBG.offsetYProperty().getValue() / COVER_SCALE_EFFECT_FACTOR, Interpolator.LINEAR),
                            new KeyValue(blurBG.radiusProperty(), COVER_BLUR_EFFECT_RADIUS, Interpolator.LINEAR),
                            new KeyValue(blurIMG.radiusProperty(), COVER_BLUR_EFFECT_RADIUS, Interpolator.LINEAR),
                            new KeyValue(changeImageButton.opacityProperty(), 1, Interpolator.EASE_OUT),
                            new KeyValue(colorAdjustIMG.brightnessProperty(), -COVER_BRIGHTNESS_EFFECT_FACTOR, Interpolator.LINEAR),
                            new KeyValue(colorAdjustBG.brightnessProperty(), -COVER_BRIGHTNESS_EFFECT_FACTOR, Interpolator.LINEAR)
                    ));
            fadeInTimeline.setCycleCount(1);
            fadeInTimeline.setAutoReverse(false);

            fadeInTimeline.play();

        });

        pane.setOnMouseExited(e -> {
            //playButton.setVisible(false);
            //infoButton.setVisible(false);
            Timeline fadeOutTimeline = new Timeline(
                    new KeyFrame(Duration.seconds(0),
                            new KeyValue(dropShadowBG.offsetXProperty(), dropShadowBG.offsetXProperty().getValue(), Interpolator.LINEAR),
                            new KeyValue(dropShadowBG.offsetYProperty(), dropShadowBG.offsetYProperty().getValue(), Interpolator.LINEAR),
                            new KeyValue(blurBG.radiusProperty(), blurBG.radiusProperty().getValue(), Interpolator.LINEAR),
                            new KeyValue(blurIMG.radiusProperty(), blurIMG.radiusProperty().getValue(), Interpolator.LINEAR),
                            new KeyValue(changeImageButton.opacityProperty(), changeImageButton.opacityProperty().getValue(), Interpolator.EASE_OUT),
                            new KeyValue(colorAdjustIMG.brightnessProperty(), colorAdjustIMG.brightnessProperty().getValue(), Interpolator.LINEAR),
                            new KeyValue(colorAdjustBG.brightnessProperty(), colorAdjustBG.brightnessProperty().getValue(), Interpolator.LINEAR)),
                    new KeyFrame(Duration.seconds(FADE_IN_OUT_TIME),
                            new KeyValue(dropShadowBG.offsetXProperty(), 6.0 * SCREEN_WIDTH / 1920, Interpolator.LINEAR),
                            new KeyValue(dropShadowBG.offsetYProperty(), 4.0 * SCREEN_WIDTH / 1080, Interpolator.LINEAR),
                            new KeyValue(blurBG.radiusProperty(), 0, Interpolator.LINEAR),
                            new KeyValue(blurIMG.radiusProperty(), 0, Interpolator.LINEAR),
                            new KeyValue(changeImageButton.opacityProperty(), 0, Interpolator.EASE_OUT),
                            new KeyValue(colorAdjustBG.brightnessProperty(), 0, Interpolator.LINEAR),
                            new KeyValue(colorAdjustIMG.brightnessProperty(), 0, Interpolator.LINEAR)
                    ));
            fadeOutTimeline.setCycleCount(1);
            fadeOutTimeline.setAutoReverse(false);

            fadeOutTimeline.play();
        });
        pane.getChildren().addAll(defaultImageView, coverView, changeImageButton);
        wrappingPane.setLeft(pane);
        BorderPane.setMargin(pane, new Insets(50 * SCREEN_HEIGHT / 1080, 50 * SCREEN_WIDTH / 1920, 50 * SCREEN_HEIGHT / 1080, 50 * SCREEN_WIDTH / 1920));
        return pane;
    }

    private void initTop() {
        EventHandler<ActionEvent> backButtonHandler = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                GameRoomAlert alert = new GameRoomAlert(Alert.AlertType.CONFIRMATION);
                alert.setContentText(Main.getString("ignore_changes?"));

                Optional<ButtonType> result = alert.showAndWait();
                result.ifPresent(buttonType -> {
                    if (buttonType.equals(ButtonType.OK)) {
                        switch (mode) {
                            case MODE_ADD:
                                entry.reloadFromDB();
                                break;
                            case MODE_EDIT:
                                entry.reloadFromDB();
                                //was previously entry.loadEntry();
                                break;
                            default:
                                break;
                        }
                        fadeTransitionTo(previousScene, getParentStage());
                    } else {
                        // ... user chose CANCEL or closed the dialog
                    }
                });
            }
        };
        String title = Main.getString("add_a_game");
        if (mode == MODE_EDIT) {
            title = Main.getString("edit_a_game");
        }

        wrappingPane.setTop(createTop(backButtonHandler, title));
    }

    @Override
    public Pane getWrappingPane() {
        return wrappingPane;
    }

    @Override
    void initAndAddWrappingPaneToRoot() {
        wrappingPane = new BorderPane();
        getRootStackPane().getChildren().add(wrappingPane);
    }

    private HashMap<String, Boolean> createDoNotUpdateFielsMap() {
        HashMap<String, Boolean> map = new HashMap<>();
        /*map.put("game_name", false);
        map.put("serie", entry.getSerie() != null && !entry.getSerie().equals(""));
        map.put("release_date", entry.getReleaseDate() != null);
        map.put("developer", entry.getDeveloper() != null && !entry.getDeveloper().equals(""));
        map.put("game_description", entry.getDescription() != null && !entry.getDescription().equals(""));
        map.put("publisher", entry.getPublisher() != null && !entry.getPublisher().equals(""));
        map.put("genre", entry.getGenres() != null);
        map.put("theme", entry.getThemes() != null);
        map.put("cover", mode != MODE_ADD && entry.getImagePath(0) != null);*/
        return map;
    }

    //called when user selected a igdb game or when a steam game is added
    private void onNewEntryData(GameEntry gameEntry, HashMap<String, Boolean> doNotUpdateFieldsMap) {
        updateLineProperty("game_name", gameEntry.getName(), doNotUpdateFieldsMap);
        updateLineProperty("serie", gameEntry.getSerie(), doNotUpdateFieldsMap);
        updateLineProperty("release_date", gameEntry.getReleaseDate(), doNotUpdateFieldsMap);
        updateLineProperty("developer", gameEntry.getDevelopers(), doNotUpdateFieldsMap);
        updateLineProperty("publisher", gameEntry.getPublishers(), doNotUpdateFieldsMap);
        updateLineProperty("game_description", StringEscapeUtils.unescapeHtml(gameEntry.getDescription()), doNotUpdateFieldsMap);
        updateLineProperty("genre", gameEntry.getGenres(), doNotUpdateFieldsMap);
        updateLineProperty("theme", gameEntry.getThemes(), doNotUpdateFieldsMap);
        entry.setIgdb_id(gameEntry.getIgdb_id());
        entry.setAggregated_rating(gameEntry.getAggregated_rating());

        /*****************COVER DOWNLOAD***************************/
        if (!doNotUpdateFieldsMap.containsKey("cover") || !doNotUpdateFieldsMap.get("cover")) {
            GeneralToast.displayToast(Main.getString("downloading_images"), getParentStage(), GeneralToast.DURATION_SHORT);
            ImageUtils.downloadIGDBImageToCache(gameEntry.getIgdb_id()
                    , gameEntry.getIgdb_imageHash(0)
                    , ImageUtils.IGDB_TYPE_COVER
                    , ImageUtils.IGDB_SIZE_BIG_2X
                    , outputfile -> {
                        ImageUtils.transitionToCover(outputfile,
                                settings().getWindowHeight() * 2 / (3 * GameButton.COVER_HEIGHT_WIDTH_RATIO),
                                settings().getWindowHeight() * 2 / 3,
                                coverView);

                        chosenImageFiles[0] = outputfile;
                    });
        }
        openImageSelector(gameEntry);
    }

    private void openImageSelector(GameEntry gameEntry) {
        IGDBImageSelector screenshotSelector = new IGDBImageSelector(gameEntry, new OnItemSelectedHandler() {
            @Override
            public void handle(SelectListPane.ListItem item) {
                ImageUtils.downloadIGDBImageToCache(gameEntry.getIgdb_id()
                        , (java.lang.String) item.getValue()
                        , ImageUtils.IGDB_TYPE_SCREENSHOT
                        , ImageUtils.IGDB_SIZE_MED
                        , outputFile -> ImageUtils.transitionToWindowBackground(outputFile, backgroundView));
            }
        });
        Optional<ButtonType> screenShotSelectedHash = screenshotSelector.showAndWait();
        screenShotSelectedHash.ifPresent(button -> {
            if (!button.getButtonData().isCancelButton()) {
                GeneralToast.displayToast(Main.getString("downloading_images"), getParentStage(), GeneralToast.DURATION_SHORT);

                Task donwloadTask = ImageUtils.downloadIGDBImageToCache(gameEntry.getIgdb_id()
                        , screenshotSelector.getSelectedImageHash()
                        , ImageUtils.IGDB_TYPE_SCREENSHOT
                        , ImageUtils.IGDB_SIZE_BIG_2X
                        , outputfile -> {
                            ImageUtils.transitionToWindowBackground(outputfile, backgroundView);
                            chosenImageFiles[1] = outputfile;
                        });
                validEntriesConditions.add(new ValidEntryCondition() {
                    @Override
                    public boolean isValid() {
                        if (donwloadTask != null && !donwloadTask.isDone()) {
                            message.replace(0, message.length(), Main.getString("background_picture_still_downloading"));
                            return false;
                        }
                        return true;
                    }

                    @Override
                    public void onInvalid() {

                    }
                });

                if (gameEntry.getIgdb_id() != -1) {
                    entry.setIgdb_id(gameEntry.getIgdb_id());
                }
            } else {
                ImageUtils.transitionToWindowBackground(chosenImageFiles[1], backgroundView);
            }
        });
    }

    void setOnExitAction(ExitAction onExitAction) {
        this.onExitAction = onExitAction;
    }


    void addCancelAllButton() {
        addCancelButton(new ClassicExitAction(GameEditScene.this, getParentStage(), previousScene), "cancel_all_button", "ignore_changes_all");
    }

    void addCancelButton(ExitAction onAction) {
        addCancelButton(onAction, "cancel_button", "ignore_changes?");
    }

    private void addCancelButton(ExitAction action, String idAndTitleKey, String warningMessageKey) {
        boolean alreadyExists = false;
        for (Node n : buttonsBox.getChildren()) {
            if (n.getId() != null && n.getId().equals(idAndTitleKey)) {
                alreadyExists = true;
                break;
            }
        }
        if (!alreadyExists) {
            Button cancelButton = new Button(Main.getString(idAndTitleKey));
            cancelButton.setId(idAndTitleKey);
            cancelButton.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    GameRoomAlert alert = new GameRoomAlert(Alert.AlertType.CONFIRMATION);
                    alert.setContentText(Main.getString(warningMessageKey));

                    Optional<ButtonType> result = alert.showAndWait();
                    result.ifPresent(buttonType -> {
                        if (buttonType.equals(ButtonType.OK)) {
                            switch (mode) {
                                case MODE_ADD:
                                case MODE_EDIT:
                                    entry.reloadFromDB();
                                    break;
                                default:
                                    break;
                            }
                            action.run();
                        } else {
                            // ... user chose CANCEL or closed the dialog
                        }
                        //onAction.run();
                    });
                }
            });
            buttonsBox.getChildren().add(cancelButton);
        }
    }

    private static boolean checkPowershellAvailable() {
        Terminal t = new Terminal(false);
        try {
            String[] output = t.execute("powershell", "/?");
            if (output != null) {
                for (String s : output) {
                    if (s.contains("PowerShell[.exe]")) {
                        t.getProcess().destroy();
                        return true;
                    }
                }
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    private Node createTitleLabel(String key, boolean advanced) {
        Node node = null;
        String title = Main.getString(key);
        Label label = new Label(title + " :");
        if (advanced) {
            //label.setStyle(ADVANCE_MODE_LABEL_STYLE);
            label.setId("advanced-setting-label");
        }

        String tooltip = Main.getString(key + "_tooltip");
        if (!tooltip.equals(title) && !tooltip.equals(Main.NO_STRING)) {
            HBox hBox = new HBox();
            hBox.setAlignment(Pos.CENTER_LEFT);
            hBox.setSpacing(5 * SCREEN_WIDTH / 1920);
            hBox.getChildren().addAll(label, new HelpButton(tooltip));
            node = hBox;
        } else {
            label.setTooltip(new Tooltip(title));
            node = label;
        }
        return node;
    }
}
