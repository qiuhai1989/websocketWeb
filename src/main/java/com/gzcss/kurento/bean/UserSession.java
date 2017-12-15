package com.gzcss.kurento.bean;

import com.google.gson.JsonObject;
import com.gzcss.kurento.websocket.CameraMedia;
import org.kurento.client.*;
import org.kurento.jsonrpc.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by qiu on 2017/12/14.
 */
public class UserSession implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(UserSession.class);

    private final String name;
    private final WebSocketSession session;
    private final MediaPipeline pipeline;
    private final String roomName;

    private CameraMedia cameraMedia;


    /**
     * 共享视频主播端点
     */
    private  WebRtcEndpoint shareVideoOutGoingMedia;
    /**
     * 共享桌面主播端点
     */
    private  WebRtcEndpoint shareDesktopOutGoingMedia;
    private ConcurrentMap<String,WebRtcEndpoint> shareVideoIncomingMedia = new ConcurrentHashMap<>();
    private ConcurrentMap<String,WebRtcEndpoint> shareDesktopIncomingMedia = new ConcurrentHashMap<>();

    public UserSession(final String name, String roomName, final WebSocketSession session,
                       MediaPipeline pipeline) {
        this.name = name;
        this.session = session;
        this.pipeline = pipeline;
        this.roomName = roomName;
    }


    public void receiveShareVideoFrom(UserSession sender, String sdpOffer) throws IOException {
        log.info("USER {}:receiveShareVideoFrom connecting with {} in room {}", this.name, sender.getName(), this.roomName);

        log.trace("USER {}:receiveShareVideoFrom SdpOffer for {} is {}", this.name, sender.getName(), sdpOffer);

        final String ipSdpAnswer = this.getShareVideoEndpointForUser(sender).processOffer(sdpOffer);
        final JsonObject scParams = new JsonObject();
        scParams.addProperty("id", "receiveShareVideoAnswer");
        scParams.addProperty("name", sender.getName());
        scParams.addProperty("sdpAnswer", ipSdpAnswer);

        log.trace("USER {}: SdpAnswer for {} is {}", this.name, sender.getName(), ipSdpAnswer);
        this.sendMessage(scParams);
        log.debug("gather candidates");
        this.getShareVideoEndpointForUser(sender).gatherCandidates();
    }

    private WebRtcEndpoint getShareVideoEndpointForUser(final UserSession sender) {
        if (sender.getName().equals(name)) {
            log.debug("PARTICIPANT {}: configuring loopback", this.name);
            return shareVideoOutGoingMedia;
        }

        log.debug("PARTICIPANT {}: receiving camera video from {}", this.name, sender.getName());

        WebRtcEndpoint incoming = shareVideoIncomingMedia.get(sender.getName());
        if (incoming == null) {
            log.debug("PARTICIPANT {}: creating new endpoint for {}", this.name, sender.getName());
            incoming = new WebRtcEndpoint.Builder(pipeline).build();

            incoming.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

                @Override
                public void onEvent(IceCandidateFoundEvent event) {
                    JsonObject response = new JsonObject();
                    response.addProperty("id", "iceCandidate");
                    response.addProperty("name", sender.getName());
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

            shareVideoIncomingMedia.put(sender.getName(), incoming);
        }

        log.debug("PARTICIPANT {}: obtained camera endpoint for {}", this.name, sender.getName());
        sender.getShareVideoOutGoingMedia().connect(incoming);

        return incoming;
    }

    public void sendMessage(JsonObject message) throws IOException {
        log.debug("USER {}: Sending message {}", name, message);
        synchronized (session) {
            session.sendMessage(new TextMessage(message.toString()));
        }
    }

    public String getName() {
        return name;
    }

    public WebSocketSession getSession() {
        return session;
    }

    public MediaPipeline getPipeline() {
        return pipeline;
    }

    public String getRoomName() {
        return roomName;
    }


    public WebRtcEndpoint getShareVideoOutGoingMedia() {
        return shareVideoOutGoingMedia;
    }

    public void setShareVideoOutGoingMedia(WebRtcEndpoint shareVideoOutGoingMedia) {
        this.shareVideoOutGoingMedia = shareVideoOutGoingMedia;
    }

    public WebRtcEndpoint getShareDesktopOutGoingMedia() {
        return shareDesktopOutGoingMedia;
    }

    public void setShareDesktopOutGoingMedia(WebRtcEndpoint shareDesktopOutGoingMedia) {
        this.shareDesktopOutGoingMedia = shareDesktopOutGoingMedia;
    }


    public ConcurrentMap<String, WebRtcEndpoint> getShareVideoIncomingMedia() {
        return shareVideoIncomingMedia;
    }

    public void setShareVideoIncomingMedia(ConcurrentMap<String, WebRtcEndpoint> shareVideoIncomingMedia) {
        this.shareVideoIncomingMedia = shareVideoIncomingMedia;
    }

    public ConcurrentMap<String, WebRtcEndpoint> getShareDesktopIncomingMedia() {
        return shareDesktopIncomingMedia;
    }

    public void setShareDesktopIncomingMedia(ConcurrentMap<String, WebRtcEndpoint> shareDesktopIncomingMedia) {
        this.shareDesktopIncomingMedia = shareDesktopIncomingMedia;
    }

    public void cancelVideoFrom(final String senderName) {
        log.debug("PARTICIPANT {}: canceling video reception from {}", this.name, senderName);
        if(cameraMedia != null){
            cameraMedia.cancelVideoFrom(senderName);
        }
    }

    /**
     * 根据音频来源不同做处理
     * @param sender 音频推送（创建）人
     * @param sdpOffer
     * @param type
     */
    public void receiveVideoFrom(UserSession sender,String sdpOffer,String type){
        try {
            switch (type){
                case "camera" :
                    cameraMedia.receiveVideoFrom(sender,sdpOffer);
                    break;
                case "shareVideo" :

                    break;
                case "shareDesktop" :

                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addCandidate(String mediaSourceType,IceCandidate candidate, String name) {
        try {
            switch (mediaSourceType){
                case "camera" :
                    cameraMedia.addCandidate(candidate,name);
                    break;
                case "shareVideo" :

                    break;
                case "shareDesktop" :

                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
        @Override
    public void close() throws IOException {
        log.debug("PARTICIPANT {}: Releasing resources", this.name);
        cameraMedia.close();
    }

    public CameraMedia getCameraMedia() {
        return cameraMedia;
    }

    public void setCameraMedia(CameraMedia cameraMedia) {
        this.cameraMedia = cameraMedia;
    }
}
