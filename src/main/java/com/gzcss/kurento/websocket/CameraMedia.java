package com.gzcss.kurento.websocket;

import com.google.gson.JsonObject;
import com.gzcss.kurento.bean.UserSession;
import org.kurento.client.*;
import org.kurento.jsonrpc.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 摄像头流操作
 * Created by qiu on 2017/12/14.
 */
public class CameraMedia implements MediaEndpointOperate {
    private static final Logger log = LoggerFactory.getLogger(CameraMedia.class);

    private final UserSession userSession;

    public CameraMedia(UserSession userSession) {
        this.userSession = userSession;
        this.outgoingMedia = new WebRtcEndpoint.Builder(userSession.getPipeline()).build();
    }

    /**
     * 摄像头视频共享主播端点
     */
    private  WebRtcEndpoint outgoingMedia;
    /**
     * 摄像头视频共享观众端点集合
     */
    private ConcurrentMap<String,WebRtcEndpoint> incomingMedia = new ConcurrentHashMap<>();

    @Override
    public void receiveVideoFrom(UserSession sender, String sdpOffer) throws IOException {
        log.info("USER {}:receiveVideoFrom connecting with {} in room {}", this.userSession.getName(), sender.getName(), this.userSession.getRoomName());

        log.trace("USER {}:receiveVideoFrom SdpOffer for {} is {}", this.userSession.getName(), sender.getName(), sdpOffer);

        final String ipSdpAnswer = this.getEndpointForUser(sender).processOffer(sdpOffer);
        final JsonObject scParams = new JsonObject();
        scParams.addProperty("id", "receiveCameraVideoAnswer");
        scParams.addProperty("name", sender.getName());
        scParams.addProperty("sdpAnswer", ipSdpAnswer);

        log.trace("USER {}: SdpAnswer for {} is {}", this.userSession.getName(), sender.getName(), ipSdpAnswer);
        this.userSession.sendMessage(scParams);
        log.debug("gather candidates");
        this.getEndpointForUser(sender).gatherCandidates();
    }

    @Override
    public WebRtcEndpoint getEndpointForUser(final UserSession sender) {
        if (sender.getName().equals(this.userSession.getName())) {
            log.debug("PARTICIPANT {}: configuring loopback", this.userSession.getName());
            return outgoingMedia;
        }

        log.debug("PARTICIPANT {}: receiving video from {}",this.userSession.getName(), sender.getName());

        WebRtcEndpoint incoming = this.incomingMedia.get(sender.getName());
        if (incoming == null) {
            log.debug("PARTICIPANT {}: creating new endpoint for {}", this.userSession.getName(), sender.getName());
            incoming = new WebRtcEndpoint.Builder(this.userSession.getPipeline()).build();

            incoming.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

                @Override
                public void onEvent(IceCandidateFoundEvent event) {
                    JsonObject response = new JsonObject();
                    response.addProperty("id", "iceCandidate");
                    response.addProperty("name", sender.getName());
                    response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
                    try {
                        synchronized (userSession.getPipeline()) {
                            userSession.getSession().sendMessage(new TextMessage(response.toString()));
                        }
                    } catch (IOException e) {
                        log.debug(e.getMessage());
                    }
                }
            });

            incomingMedia.put(sender.getName(), incoming);
        }

        log.debug("PARTICIPANT {}: obtained endpoint for {}", this.userSession.getName(), sender.getName());
        sender.getCameraMedia().getOutgoingMedia().connect(incoming);

        return incoming;

    }

    @Override
    public void close() {
        for (final String remoteParticipantName : incomingMedia.keySet()) {

            log.trace("PARTICIPANT {}: Released incoming EP for {}", this.userSession.getName(), remoteParticipantName);

            final WebRtcEndpoint ep = this.incomingMedia.get(remoteParticipantName);

            ep.release(new Continuation<Void>() {

                @Override
                public void onSuccess(Void result) throws Exception {
                    log.trace("PARTICIPANT {}: Released successfully incoming EP for {}",
                            userSession.getName(), remoteParticipantName);
                }

                @Override
                public void onError(Throwable cause) throws Exception {
                    log.warn("PARTICIPANT {}: Could not release incoming EP for {}", userSession.getName(),
                            remoteParticipantName);
                }
            });
        }
    }

    @Override
    public void cancelVideoFrom(final String senderName) {
        log.debug("PARTICIPANT {}: canceling video reception from {}", this.userSession.getName(), senderName);
        final WebRtcEndpoint incoming = incomingMedia.remove(senderName);

        log.debug("PARTICIPANT {}: removing endpoint for {}", this.userSession.getName(), senderName);
        incoming.release(new Continuation<Void>() {
            @Override
            public void onSuccess(Void result) throws Exception {
                log.trace("PARTICIPANT {}: Released successfully incoming EP for {}",
                        userSession.getName(),senderName);
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                log.warn("PARTICIPANT {}: Could not release incoming EP for {}", userSession.getName(),
                        senderName);
            }
        });
    }

    @Override
    public void addCandidate(IceCandidate candidate, String name) {
        if (this.userSession.getName().compareTo(name) == 0) {
            outgoingMedia.addIceCandidate(candidate);
        } else {
            WebRtcEndpoint webRtc = incomingMedia.get(name);
            if (webRtc != null) {
                webRtc.addIceCandidate(candidate);
            }
        }
    }

    public WebRtcEndpoint getOutgoingMedia() {
        return outgoingMedia;
    }

    public void setOutgoingMedia(WebRtcEndpoint outgoingMedia) {
        this.outgoingMedia = outgoingMedia;
    }

    public UserSession getUserSession() {
        return userSession;
    }



    public ConcurrentMap<String, WebRtcEndpoint> getIncomingMedia() {
        return incomingMedia;
    }

    public void setIncomingMedia(ConcurrentMap<String, WebRtcEndpoint> incomingMedia) {
        this.incomingMedia = incomingMedia;
    }
}
