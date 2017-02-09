package ui.control.drawer;

import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

/**
 * Created by LM on 09/02/2017.
 */
public class SubMenu extends BorderPane {
    private Label titleLabel;
    private String menuId;
    private boolean active = true;
    private VBox optionBox = new VBox();
    private Timeline openAnim;
    private Timeline closeAnim;


    public SubMenu(String menuId){
        super();
        titleLabel  = new Label(menuId);
        this.menuId = menuId;
        setTop(titleLabel);

        getStyleClass().add("drawer-submenu");
        setFocusTraversable(false);
        setManaged(false);
        setVisible(false);
    }

    public void addOption(String option){
        optionBox.getChildren().add(new Label(option));
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getMenuId() {
        return menuId;
    }

    public Timeline getOpenAnim() {
        return openAnim;
    }

    public void setOpenAnim(Timeline openAnim) {
        this.openAnim = openAnim;
    }

    public Timeline getCloseAnim() {
        return closeAnim;
    }

    public void setCloseAnim(Timeline closeAnim) {
        this.closeAnim = closeAnim;
    }
}
