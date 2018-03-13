package com.gzcss.kurento.websocket;

import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;

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

    public List<UserSession> getUserSessionList() {
        return userSessionList;
    }

    public void setUserSessionList(List<UserSession> userSessionList) {
        this.userSessionList = userSessionList;
    }

    private List<UserSession> userSessionList = new ArrayList<>();

    public void removeSpecifiedUser(UserSession userSession){
        userSessionList.remove(userSession);
    }

}
