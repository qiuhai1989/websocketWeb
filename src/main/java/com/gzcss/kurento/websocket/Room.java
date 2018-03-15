package com.gzcss.kurento.websocket;

import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by qiu on 2018/3/13.
 */
public class Room {

    private String pk;

    public Room(String pk) {
        this.pk = pk;
    }

    public String getPk() {
        return pk;
    }

    public void setPk(String pk) {
        this.pk = pk;
    }

    public Map<String, UserSession> getUserSessionMap() {
        return userSessionMap;
    }

    public void setUserSessionMap(Map<String, UserSession> userSessionMap) {
        this.userSessionMap = userSessionMap;
    }

    private Map<String,UserSession> userSessionMap = new HashMap<>();

    public void removeSpecifiedUser(UserSession userSession){
        userSessionMap.remove(userSession.getUserPk());
    }

    public void addUser(UserSession userSession){
        userSessionMap.put(userSession.getUserPk(),userSession);
    }

    public UserSession getUser(String userPk){
        return userSessionMap.get(userPk);
    }
}
