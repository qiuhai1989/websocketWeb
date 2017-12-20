package com.gzcss.kurento.websocket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import org.kurento.client.*;
import org.kurento.jsonrpc.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**消息处理类
 * @author 丘海
 * @date 2017-11-23 16:03
 */
public class WebSocketPushHandler implements WebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(WebSocketPushHandler.class);
    private static final List<WebSocketSession> users = new ArrayList<>();
//    private final ConcurrentHashMap<String, UserSession> viewers = new ConcurrentHashMap<>();

    @Autowired
    private KurentoClient kurento;

//    private MediaPipeline pipeline;
//    private UserSession presenterUserSession;

    @Autowired
    private RoomManager roomManager;

    @Autowired
    private UserRegistry registry;

    @Override
    public void afterConnectionEstablished(WebSocketSession webSocketSession) throws Exception {
        log.info("成功进入了系统:"+webSocketSession.getLocalAddress()+"--"+webSocketSession.getId());
        users.add(webSocketSession);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> webSocketMessage) throws Exception {
        //将消息进行转化，因为是消息是json数据，可能里面包含了发送给某个人的信息，所以需要用json相关的工具类处理之后再封装成TextMessage，我这儿并没有做处理，消息的封装格式一般有{from:xxxx,to:xxxxx,msg:xxxxx}，来自哪里，发送给谁，什么消息等等
        String msg = (String) webSocketMessage.getPayload();
        JSONObject jsonMessage = JSON.parseObject(msg);
        log.info("Incoming message from session '{}': {}", session.getId(), jsonMessage);
        switch (jsonMessage.getString("id")) {
            case "presenter":
                try {
                    presenter(session, jsonMessage);
                } catch (Throwable t) {
                    handleErrorResponse(t, session, "presenterResponse");
                }
                break;
            case "shareMedia":
                try {
                    shareMedia(session, jsonMessage);
                } catch (Throwable t) {
                    handleErrorResponse(t, session, "shareMediaResponse");
                }
                break;
            case "viewer":
                try {
                    viewer(session, jsonMessage);
                } catch (Throwable t) {
                    handleErrorResponse(t, session, "viewerResponse");
                }
                break;
            case "viewerShareMedia":
                try {
                    viewerShareMedia(session, jsonMessage);
                } catch (Throwable t) {
                    handleErrorResponse(t, session, "viewerShareMediaResponse");
                }
                break;
            case "onIceCandidate": {
                onIceCandidate(session, jsonMessage);
                break;
            }
            case "stop":
                stop(session);
                break;
            case "stopShareMedia":
                stopShareMedia(session);
                break;
            case "noticeToViewShareMedia":
                noticeToViewShareMedia(session);
                break;
            case "login":
                log.info("-----------------------User login as "+ jsonMessage.getString("sendTo")+" in room "+jsonMessage.getString("room"));
                joinRoom(jsonMessage, session);
                break;
            default:
                break;
        }
    }

    private void joinRoom(JSONObject params, WebSocketSession session) throws IOException {
        final String roomName = params.getString("room");
        String name = params.getString("name");
        final Boolean isZhuBo = params.getBoolean("isZhuBo") == null?Boolean.FALSE:params.getBoolean("isZhuBo");
        log.info("PARTICIPANT {}: trying to join room {}", name, roomName);
        //如果游客登录则使用ip作为用户名
        if(name == null){
            name = session.getLocalAddress().getAddress().getHostAddress();
        }
        Room room = roomManager.getRoom(roomName);
        boolean isJoinSuccess ;
        if(room.isUserExisted(name,isZhuBo)){
            isJoinSuccess = false;
        }else{
            final UserSession user = room.join(name,isZhuBo,session);
            registry.register(user);
            isJoinSuccess = true;
        };
        JsonObject response = new JsonObject();
        response.addProperty("id", "joinRoom");
        response.addProperty("name", name);
        response.addProperty("result",isJoinSuccess);

        session.sendMessage(new TextMessage(response.toString() ));

    }

    private synchronized void noticeToViewShareMedia(final WebSocketSession session) throws IOException{
        UserSession currentUser = registry.getBySession(session);
        Room currentRoom = roomManager.getRoom(currentUser.getRoomName());
        UserSession presenterUserSession = currentRoom.getPresenterUserSession();
        ConcurrentHashMap<String, UserSession> viewers = currentRoom.getViewers();
        for (UserSession viewer : viewers.values()) {
            if (viewer != null && viewer.getSession() != null) {
                JsonObject response = new JsonObject();
                response.addProperty("id", "toSeeShareMedia");
                viewer.getSession().sendMessage(new TextMessage(response.toString()));
            }
        }
    }

    private synchronized void onIceCandidate(final WebSocketSession session, JSONObject jsonMessage){
        UserSession currentUser = registry.getBySession(session);
        Room currentRoom = roomManager.getRoom(currentUser.getRoomName());
        UserSession presenterUserSession = currentRoom.getPresenterUserSession();
        JSONObject candidate = jsonMessage.getJSONObject("candidate");
        String mediaType = jsonMessage.getString("mediaType");
        ConcurrentHashMap<String, UserSession> viewers = currentRoom.getViewers();
        String sessionId = session.getId();
        UserSession user = null;
        if (presenterUserSession != null) {
            if (presenterUserSession.equals(currentUser)) {
                user = presenterUserSession;
            } else {
                user = currentUser;
            }
        }
        if (user != null) {
            IceCandidate cand =
                    new IceCandidate(candidate.getString("candidate"), candidate.getString("sdpMid")
                            , candidate.getInteger("sdpMLineIndex"));

            if("camera".equals(mediaType)){
                user.addCandidate(cand);
            }else if("shareMedia".equals(mediaType)){
                user.addCandidateShareMedia(cand);
            }
        }
    }

    //后台错误信息处理方法
    @Override
    public void handleTransportError(WebSocketSession webSocketSession, Throwable throwable) throws Exception {
        log.error(throwable.getMessage());
    }

    //用户退出后的处理，不如退出之后，要将用户信息从websocket的session中remove掉，这样用户就处于离线状态了，也不会占用系统资源
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        stop(session);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }



    private void handleErrorResponse(Throwable throwable, WebSocketSession session, String responseId)
            throws IOException {
        stop(session);
        log.error(throwable.getMessage(), throwable);
        JsonObject response = new JsonObject();
        response.addProperty("id", responseId);
        response.addProperty("response", "rejected");
        response.addProperty("message", throwable.getMessage());
        session.sendMessage(new TextMessage(response.toString()));
    }

    private synchronized void presenter(final WebSocketSession session, JSONObject jsonMessage)
            throws IOException {
            UserSession presenterUserSession = registry.getBySession(session);
            Room currentRoom = roomManager.getRoom(presenterUserSession.getRoomName());
            WebRtcEndpoint presenterWebRtc = new WebRtcEndpoint.Builder(currentRoom.getPipeline()).build();
            presenterWebRtc.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

                @Override
                public void onEvent(IceCandidateFoundEvent event) {
                    JsonObject response = new JsonObject();
                    response.addProperty("id", "iceCandidate");
                    response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
                    try {
                        synchronized (session) {
                            session.sendMessage(new TextMessage(response.toString()));
                        }
                    } catch (IOException e) {
                        log.debug(e.getMessage());
                    }
                }
            });
            presenterUserSession.setWebRtcEndpoint(presenterWebRtc);
            String sdpOffer = jsonMessage.getString("sdpOffer");
            String sdpAnswer = presenterWebRtc.processOffer(sdpOffer);

            JsonObject response = new JsonObject();
            response.addProperty("id", "presenterResponse");
            response.addProperty("response", "accepted");
            response.addProperty("sdpAnswer", sdpAnswer);

            synchronized (session) {
                presenterUserSession.sendMessage(response);
            }
            presenterWebRtc.gatherCandidates();

    }

    private synchronized void shareMedia(final WebSocketSession session, JSONObject jsonMessage)
            throws IOException {
            UserSession presenterUserSession = registry.getBySession(session);
            Room currentRoom = roomManager.getRoom(presenterUserSession.getRoomName());
            WebRtcEndpoint shareMediaWebRtc = new WebRtcEndpoint.Builder(currentRoom.getPipeline()).build();
            presenterUserSession.setShareMediaEndpoint(shareMediaWebRtc);
            shareMediaWebRtc.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

                @Override
                public void onEvent(IceCandidateFoundEvent event) {
                    JsonObject response = new JsonObject();
                    response.addProperty("id", "iceCandidateShareMedia");
                    response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
                    try {
                        synchronized (session) {
                            session.sendMessage(new TextMessage(response.toString()));
                        }
                    } catch (IOException e) {
                        log.debug(e.getMessage());
                    }
                }
            });

            String sdpOffer = jsonMessage.getString("sdpOffer");
            String sdpAnswer = shareMediaWebRtc.processOffer(sdpOffer);

            JsonObject response = new JsonObject();
            response.addProperty("id", "shareMediaResponse");
            response.addProperty("response", "accepted");
            response.addProperty("sdpAnswer", sdpAnswer);

            synchronized (session) {
                presenterUserSession.sendMessage(response);
            }
            shareMediaWebRtc.gatherCandidates();


    }

    private synchronized void viewer(final WebSocketSession session, JSONObject jsonMessage)
            throws IOException {
        UserSession currentUser = registry.getBySession(session);
        if(currentUser != null){
            Room currentRoom = roomManager.getRoom(currentUser.getRoomName());
            UserSession presenterUserSession = currentRoom.getPresenterUserSession();
            if (presenterUserSession == null || presenterUserSession.getWebRtcEndpoint() == null) {
                JsonObject response = new JsonObject();
                response.addProperty("id", "viewerResponse");
                response.addProperty("response", "rejected");
                response.addProperty("message",
                        "No active sender now. Become sender or . Try again later ...");
                session.sendMessage(new TextMessage(response.toString()));
            } else {
                if (currentRoom.getViewers().containsKey(session.getId())) {
                    JsonObject response = new JsonObject();
                    response.addProperty("id", "viewerResponse");
                    response.addProperty("response", "rejected");
                    response.addProperty("message", "You are already viewing in this session. "
                            + "Use a different browser to add additional viewers.");
                    session.sendMessage(new TextMessage(response.toString()));
                    return;
                }


                WebRtcEndpoint nextWebRtc = new WebRtcEndpoint.Builder(currentRoom.getPipeline()).build();

                nextWebRtc.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

                    @Override
                    public void onEvent(IceCandidateFoundEvent event) {
                        JsonObject response = new JsonObject();
                        response.addProperty("id", "iceCandidate");
                        response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
                        try {
                            synchronized (session) {
                                session.sendMessage(new TextMessage(response.toString()));
                            }
                        } catch (IOException e) {
                            log.debug(e.getMessage());
                        }
                    }
                });

                currentUser.setWebRtcEndpoint(nextWebRtc);
                presenterUserSession.getWebRtcEndpoint().connect(nextWebRtc);//此处将主播端与观众端进行关联？
                String sdpOffer = jsonMessage.getString("sdpOffer");
                String sdpAnswer = nextWebRtc.processOffer(sdpOffer);

                JsonObject response = new JsonObject();
                response.addProperty("id", "viewerResponse");
                response.addProperty("response", "accepted");
                response.addProperty("sdpAnswer", sdpAnswer);

                synchronized (currentUser) {
                    currentUser.sendMessage(response);
                }
                nextWebRtc.gatherCandidates();
            }
        }
    }

    private synchronized void viewerShareMedia(final WebSocketSession session, JSONObject jsonMessage)
            throws IOException {
        UserSession currentUser = registry.getBySession(session);
        Room currentRoom = roomManager.getRoom(currentUser.getRoomName());
        UserSession presenterUserSession = currentRoom.getPresenterUserSession();
        if (presenterUserSession == null || presenterUserSession.getShareMediaEndpoint() == null) {
            JsonObject response = new JsonObject();
            response.addProperty("id", "viewerShareMediaResponse");
            response.addProperty("response", "rejected");
            response.addProperty("message",
                    "No active shareMedia. Try again later ...");
            session.sendMessage(new TextMessage(response.toString()));
        } else {
            WebRtcEndpoint nextWebRtc = new WebRtcEndpoint.Builder(currentRoom.getPipeline()).build();

            nextWebRtc.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

                @Override
                public void onEvent(IceCandidateFoundEvent event) {
                    JsonObject response = new JsonObject();
                    response.addProperty("id", "iceCandidateShareMedia");
                    response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
                    try {
                        synchronized (session) {
                            session.sendMessage(new TextMessage(response.toString()));
                        }
                    } catch (IOException e) {
                        log.debug(e.getMessage());
                    }
                }
            });

            currentUser.setShareMediaEndpoint(nextWebRtc);
            presenterUserSession.getShareMediaEndpoint().connect(nextWebRtc);//此处将主播端与观众端进行关联？
            String sdpOffer = jsonMessage.getString("sdpOffer");
            String sdpAnswer = nextWebRtc.processOffer(sdpOffer);

            JsonObject response = new JsonObject();
            response.addProperty("id", "viewerShareMediaResponse");
            response.addProperty("response", "accepted");
            response.addProperty("sdpAnswer", sdpAnswer);

            synchronized (currentUser) {
                currentUser.sendMessage(response);
            }
            nextWebRtc.gatherCandidates();
        }
    }

    private synchronized void stop(WebSocketSession session) throws IOException {
        UserSession currentUser = registry.getBySession(session);
        if(currentUser != null){
            Room currentRoom = roomManager.getRoom(currentUser.getRoomName());
            UserSession presenterUserSession = currentRoom.getPresenterUserSession();
            ConcurrentHashMap<String, UserSession> viewers = currentRoom.getViewers();
            String sessionId = session.getId();
            if (currentUser.equals(presenterUserSession)) {//主播关闭摄像头
                for (UserSession viewer : viewers.values()) {
                    viewer.closeCamera();
                    JsonObject response = new JsonObject();
                    response.addProperty("id", "stopCommunication");
                    viewer.sendMessage(response);
                }
                presenterUserSession.closeCamera();
                currentRoom.clearZhuBo();
                log.info("Releasing media pipeline");
            } else {//如果是观众清除对应连接对象
                currentUser.closeCamera();
                currentRoom.clearSpecifiedViewer(currentUser);
            }
        }
    }

    private synchronized void stopShareMedia(WebSocketSession session) throws IOException {
        UserSession currentUser = registry.getBySession(session);
        Room currentRoom = roomManager.getRoom(currentUser.getRoomName());
        UserSession presenterUserSession = currentRoom.getPresenterUserSession();
        ConcurrentHashMap<String, UserSession> viewers = currentRoom.getViewers();
        String sessionId = session.getId();
        if (currentUser.equals(presenterUserSession)) {//主播关闭共享视频
            for (UserSession viewer : viewers.values()) {
                viewer.closeShareMedia();
                JsonObject response = new JsonObject();
                response.addProperty("id", "stopCommunicationShareMedia");
                viewer.sendMessage(response);
            }

            log.info("Releasing shareMedia.");
            presenterUserSession.closeShareMedia();
        } else if (viewers.containsKey(sessionId)) {//观众关闭共享视频

        }
    }
}
