package com.gameroom.ui.control.drawer;

import com.gameroom.ui.Main;

/**
 * Created by LM on 09/02/2017.
 */
public enum SortType {
    NAME("sortBy_name"),
    RATING("sortBy_rating"),
    PLAY_TIME("sortBy_playtime"),
    RELEASE_DATE("sortBy_release_date");

    private String id;

    SortType(String id){
        this.id = id;
    }

    public String getName(){
        return Main.getString(id);
    }

    public String getId() {
        return id;
    }

    public static SortType fromId(String id){
        for(SortType s : SortType.values()){
            if (s.id.equals(id)){
                return s;
            }
        }
        return NAME;
    }
}
