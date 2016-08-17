package data.game;

import data.game.entry.GameEntry;
import data.game.scrapper.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONArray;
import system.application.settings.PredefinedSetting;
import ui.Main;
import ui.control.button.gamebutton.GameButton;
import ui.dialog.GameRoomAlert;
import ui.dialog.SteamIgnoredSelector;
import ui.scene.GameEditScene;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

import static ui.Main.GENERAL_SETTINGS;
import static ui.Main.MAIN_SCENE;
import static ui.Main.RESSOURCE_BUNDLE;

/**
 * Created by LM on 17/08/2016.
 */
public class GameLooker {
    private OnGameFoundHandler onGameFoundHandler;
    private ArrayList<SteamPreEntry> ownedSteamApps = new ArrayList<>();
    private ArrayList<SteamPreEntry> installedSteamApps = new ArrayList<>();

    private ArrayList<GameEntry> entriesToAdd = new ArrayList<>();
    private int foundGames = 0;

    public GameLooker(OnGameFoundHandler onGameFoundHandler) {
        this.onGameFoundHandler = onGameFoundHandler;

    }

    public void startService() {
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                while (Main.KEEP_THREADS_RUNNING) {
                    Main.LOGGER.info("Starting game watch routine");
                    foundGames = entriesToAdd.size();
                    initFolderWatchTask()/*.run()*/;
                    initSteamWatchTask().run();
                    initSteamUpdateStatusTask();

                    if (foundGames > entriesToAdd.size()) {
                        onGameFoundHandler.onAllGamesFound();
                    }

                    try {
                        Thread.sleep(5 * 60 * 100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        th.setDaemon(true);
        th.start();
    }

    private Task initSteamUpdateStatusTask() {
        //TODO implement steam update watcher here
        /*
        Status flag :
        4 : fully installed
        6,1030 : downloading update



         */
        return null;
    }

    private Task initFolderWatchTask() {
        //TODO implement folder watch task here
        return null;
    }

    private Task initSteamWatchTask() {
        Task<ArrayList<SteamPreEntry>> steamTask = new Task() {
            @Override
            protected Object call() throws Exception {
                ArrayList<SteamPreEntry> steamEntriesToAdd = new ArrayList<SteamPreEntry>();
                ownedSteamApps.clear();
                ownedSteamApps.addAll(SteamOnlineScrapper.getOwnedSteamGamesPreEntry());
                installedSteamApps.clear();
                installedSteamApps.addAll(SteamLocalScrapper.getSteamAppsInstalledPreEntries());

                for (SteamPreEntry steamEntry : ownedSteamApps) {
                    boolean alreadyAddedToLibrary = false;
                    for (GameEntry entry : AllGameEntries.ENTRIES_LIST) {
                        alreadyAddedToLibrary = steamEntry.getId() == entry.getSteam_id();
                        if (alreadyAddedToLibrary) {
                            for (SteamPreEntry installedEntry : installedSteamApps) {
                                if (installedEntry.getId() == entry.getSteam_id()) {
                                    //games is installed!
                                    //if(entry.isNotInstalled()) {
                                    entry.setNotInstalled(false);
                                    Platform.runLater(() -> {
                                        MAIN_SCENE.updateGame(entry);
                                    });
                                    break;
                                    //}
                                }
                            }
                            break;
                        }
                    }
                    if (!alreadyAddedToLibrary) {
                        SteamPreEntry[] ignoredSteamApps = GENERAL_SETTINGS.getSteamAppsIgnored();
                        for (SteamPreEntry ignoredEntry : ignoredSteamApps) {
                            alreadyAddedToLibrary = steamEntry.getId() == ignoredEntry.getId();
                            if (alreadyAddedToLibrary) {
                                break;
                            }
                        }
                    }
                    if (!alreadyAddedToLibrary && !alreadyWaitingToBeAdded(steamEntry)) {
                        Main.LOGGER.debug("To add : " + steamEntry.getName());
                        steamEntriesToAdd.add(steamEntry);
                    }
                }

                return steamEntriesToAdd;
            }
        };
        steamTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                ArrayList<SteamPreEntry> steamEntriesToAdd = steamTask.getValue();

                Main.LOGGER.info(steamEntriesToAdd.size() + " steam games to add");
                if (steamEntriesToAdd.size() != 0) {
                    for (SteamPreEntry preEntryToAdd : steamEntriesToAdd) {
                        Task getGameEntryTask = new Task<GameEntry>() {
                            @Override
                            protected GameEntry call() throws Exception {
                                GameEntry convertedEntry = new GameEntry(preEntryToAdd.getName());
                                convertedEntry.setSteam_id(preEntryToAdd.getId());

                                GameEntry fetchedEntry = null;
                                try {
                                    fetchedEntry = SteamOnlineScrapper.getEntryForSteamId(preEntryToAdd.getId(), installedSteamApps);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                GameEntry entryToAdd = fetchedEntry != null ? fetchedEntry : convertedEntry;
                                if (!alreadyWaitingToBeAdded(entryToAdd)) {
                                    foundGames++;
                                    GameEntry guessedEntry = tryGetFirstIGDBResult(entryToAdd.getName());
                                    if(guessedEntry!=null){
                                        guessedEntry.setName(entryToAdd.getName());
                                        guessedEntry.setSteam_id(entryToAdd.getSteam_id());
                                        guessedEntry.setPlayTimeSeconds(entryToAdd.getPlayTimeSeconds());
                                        guessedEntry.setNotInstalled(entryToAdd.isNotInstalled());
                                        guessedEntry.setDescription(entryToAdd.getDescription());
                                        ImageUtils.downloadIGDBImageToCache(guessedEntry.getIgdb_id()
                                                , guessedEntry.getIgdb_imageHash(0)
                                                , ImageUtils.IGDB_TYPE_COVER
                                                , ImageUtils.IGDB_SIZE_SMALL
                                                , new OnDLDoneHandler() {
                                                    @Override
                                                    public void run(File outputfile) {
                                                        guessedEntry.setImagePath(0, outputfile);
                                                        entriesToAdd.add(guessedEntry);

                                                        Platform.runLater(() -> {
                                                            onGameFoundHandler.gameToAddFound(guessedEntry);
                                                        });
                                                    }
                                                });
                                    }else {
                                        entriesToAdd.add(entryToAdd);

                                        Platform.runLater(() -> {
                                            onGameFoundHandler.gameToAddFound(entryToAdd);
                                        });
                                    }
                                }
                                return null;
                            }
                        };
                        Thread th = new Thread(getGameEntryTask);
                        th.setDaemon(true);
                        th.start();

                    }
                        /*GameRoomAlert alert = new GameRoomAlert(Alert.AlertType.CONFIRMATION, steamEntriesToAdd.size() + " " + Main.RESSOURCE_BUNDLE.getString("steam_games_to_add_detected"));
                        alert.getButtonTypes().add(new ButtonType(RESSOURCE_BUNDLE.getString("ignore") + "...", ButtonBar.ButtonData.LEFT));
                        Optional<ButtonType> result = alert.showAndWait();
                        result.ifPresent(buttonType -> {
                            if (buttonType.getButtonData().equals(ButtonBar.ButtonData.OK_DONE)) {
                                for (SteamPreEntry preEntryToAdd : steamEntriesToAdd) {
                                    GameEntry convertedEntry = new GameEntry(preEntryToAdd.getName());
                                    convertedEntry.setSteam_id(preEntryToAdd.getId());
                                    try {
                                        convertedEntry = SteamOnlineScrapper.getEntryForSteamId(preEntryToAdd.getId(), installedSteamApps);
                                    } catch (ConnectTimeoutException e) {
                                        e.printStackTrace();
                                    }
                                    onGameFoundHandler.gameToAddFound(convertedEntry);
                                }
                            } else if (buttonType.getButtonData().equals(ButtonBar.ButtonData.LEFT)) {
                                try {
                                    SteamIgnoredSelector selector = new SteamIgnoredSelector(ownedSteamApps);
                                    Optional<ButtonType> ignoredOptionnal = selector.showAndWait();
                                    ignoredOptionnal.ifPresent(pairs -> {
                                        if (pairs.getButtonData().equals(ButtonBar.ButtonData.OK_DONE)) {
                                            GENERAL_SETTINGS.setSettingValue(PredefinedSetting.IGNORED_STEAM_APPS, selector.getSelectedEntries());
                                        }
                                    });
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });*/
                }

            }
        });
        return steamTask;
    }

    private boolean alreadyWaitingToBeAdded(GameEntry entry) {
        return entriesToAdd.contains(entry);
    }

    private boolean alreadyWaitingToBeAdded(SteamPreEntry entry) {
        boolean already = false;
        for (GameEntry gameEntry : entriesToAdd) {
            already = gameEntry.getSteam_id() == entry.getId();
            if (already) {
                break;
            }
        }
        return already;
    }

    private GameEntry tryGetFirstIGDBResult(String name) {
        try {
            JSONArray bf4_results = null;
            bf4_results = IGDBScrapper.searchGame(name);
            ArrayList list = new ArrayList();
            list.add(bf4_results.getJSONObject(0).getInt("id"));
            JSONArray bf4_data = IGDBScrapper.getGamesData(list);
            return IGDBScrapper.getEntry(bf4_data.getJSONObject(0));

        } catch (Exception e) {
            Main.LOGGER.error(name + " not found on igdb first guess");
            return null;
        }

    }
}