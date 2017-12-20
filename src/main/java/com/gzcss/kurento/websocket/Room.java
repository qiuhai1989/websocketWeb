package com.gzcss.kurento.websocket;

import com.google.gson.JsonObject;
import org.kurento.client.*;
import org.kurento.jsonrpc.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PreDestroy;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by qiu on 2017/12/19.
 */
public class Room  implements Closeable {
    private final Logger log = LoggerFactory.getLogger(Room.class);
    /**
     * 参与人不区分主播，观众
     */
    private final ConcurrentMap<String, UserSession> participants = new ConcurrentHashMap<>();
    private final MediaPipeline pipeline;
    private final String name;

    /**
     * 主播会话
     */
    private UserSession presenterUserSession;
    /**
     * 观众会话列表 key（userSession.name）/value(userSession)
     */
    private final ConcurrentHashMap<String, UserSession> viewers = new ConcurrentHashMap<>();

    public Room(String roomName, MediaPipeline pipeline) {
        this.name = roomName;
        this.pipeline = pipeline;
        log.info("ROOM {} has been created", roomName);
    }

    /**
     * 检查用户名是否已经存在,同一个房间同一个用户名的主播或观众只能存在一个
     * @param userName
     * @param isZhuBo
     * @return
     */
    public boolean isUserExisted(String userName,Boolean isZhuBo){
        if(isZhuBo){
            if(this.presenterUserSession != null){
                log.error("Room {} 已经存在主播",this.name);
                return true;
            }
        }else {
            for(UserSession userSession:viewers.values()){
                if(userSession.getName().equals(userName)){
                    log.error("Room {} 用户名已被使用",this.name);
                    return true;
                }
            }
        }
        return false;
    }

    public UserSession join(String userName,Boolean isZhuBo, WebSocketSession session) throws IOException {
        log.info("ROOM {}: adding participant {}", userName, userName);
        final UserSession participant = new UserSession(session,userName,this.name,isZhuBo);
        joinRoom(participant);

        return participant;
    }

    private void joinRoom(final UserSession newParticipant) throws IOException {
        if(newParticipant.getZhuBo()){
            this.presenterUserSession = newParticipant;
        }else {
            this.viewers.put(newParticipant.getName(),newParticipant);
        }
        participants.put(newParticipant.getName(), newParticipant);
    }



    @Override
    public void close() {
       for (final UserSession user : viewers.values()) {
            try {
                user.close();
            } catch (IOException e) {
                log.debug("ROOM {}: Could not invoke close on participant {}", this.name, user.getName(),
                        e);
            }
        }

        participants.clear();

        pipeline.release(new Continuation<Void>() {

            @Override
            public void onSuccess(Void result) throws Exception {
                log.trace("ROOM {}: Released Pipeline", Room.this.name);
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                log.warn("PARTICIPANT {}: Could not release Pipeline", Room.this.name);
            }
        });

        log.debug("Room {} closed", this.name);
    }
    @PreDestroy
    private void shutdown() {
        this.close();
    }

    public MediaPipeline getPipeline() {
        return pipeline;
    }

    public ConcurrentMap<String, UserSession> getParticipants() {
        return participants;
    }

    public UserSession getPresenterUserSession() {
        return presenterUserSession;
    }

    public void setPresenterUserSession(UserSession presenterUserSession) {
        this.presenterUserSession = presenterUserSession;
    }

    public String getName() {
        return name;
    }

    public ConcurrentHashMap<String, UserSession> getViewers() {
        return viewers;
    }

    /**
     * 清空主播信息
     */
    public void clearZhuBo(){
        this.presenterUserSession = null;
    }

    public void clearSpecifiedViewer(UserSession removeUser){
        this.viewers.remove(removeUser.getName());
    }

}
