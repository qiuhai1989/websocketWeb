package com.gzcss.kurento.websocket;

import org.springframework.web.socket.WebSocketSession;

/**
 * Created by qiu on 2018/3/13.
 */
public class UserSession {

    private String roomPk;

    private String userPk;

    private WebSocketSession socketSession;

    public UserSession(String roomPk, String userPk, WebSocketSession socketSession) {
        this.roomPk = roomPk;
        this.userPk = userPk;
        this.socketSession = socketSession;
    }

    public String getRoomPk() {
        return roomPk;
    }

    public void setRoomPk(String roomPk) {
        this.roomPk = roomPk;
    }

    public String getUserPk() {
        return userPk;
    }

    public void setUserPk(String userPk) {
        this.userPk = userPk;
    }

    public WebSocketSession getSocketSession() {
        return socketSession;
    }

    public void setSocketSession(WebSocketSession socketSession) {
        this.socketSession = socketSession;
    }
}
