package com.gzcss.kurento.websocket;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

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

    @Override
    public boolean equals(Object obj) {
        if(obj == null){
            return false;
        }
        if(obj.getClass() ==  this.getClass()){
            UserSession temp = (UserSession) obj;
            return this.userPk.equals(temp.getUserPk()) && this.roomPk.equals(temp.getRoomPk());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(userPk).append(roomPk).toHashCode();
    }

    /**
     * 传递信息给会话对应的客户端
     * @param message
     */
    public void sendMessageToClient(TextMessage message) throws IOException {
        this.socketSession.sendMessage(message);
    }

}
