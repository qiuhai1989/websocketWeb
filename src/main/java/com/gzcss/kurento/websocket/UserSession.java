/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.gzcss.kurento.websocket;

import com.google.gson.JsonObject;
import org.kurento.client.Continuation;
import org.kurento.client.IceCandidate;
import org.kurento.client.WebRtcEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.Closeable;
import java.io.IOException;

/**
 * User session.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.0
 */
public class UserSession implements Closeable {

  private static final Logger log = LoggerFactory.getLogger(UserSession.class);

  private final WebSocketSession session;
  private WebRtcEndpoint cameraEndpoint;
  private WebRtcEndpoint shareMediaEndpoint;
  private final String name;
  private final String roomName;
  private final Boolean isZhuBo;//主播标识

  public UserSession(WebSocketSession session, String name, String roomName,Boolean isZhuBo) {
    this.session = session;
    this.name = name;
    this.roomName = roomName;
    this.isZhuBo = isZhuBo;
  }



  public WebSocketSession getSession() {
    return session;
  }

  public void sendMessage(JsonObject message) throws IOException {
    log.debug("Sending message from user with session Id '{}': {}", session.getId(), message);
    session.sendMessage(new TextMessage(message.toString()));
  }

  public void closeCamera(){
    if(cameraEndpoint != null){
      cameraEndpoint.release(new Continuation<Void>() {
        @Override
        public void onSuccess(Void result) throws Exception {
          log.trace("cameraEndpoint {}: Released successfully",
                  UserSession.this.name);
        }

        @Override
        public void onError(Throwable cause) throws Exception {
          log.trace("cameraEndpoint {}: Released failed",
                  UserSession.this.name);
        }
      });
      cameraEndpoint = null;
    }
  }

  public void closeShareMedia(){
    if(shareMediaEndpoint != null){
      shareMediaEndpoint.release(new Continuation<Void>() {
        @Override
        public void onSuccess(Void result) throws Exception {
          log.trace("shareMediaEndpoint {}: Released successfully",
                  UserSession.this.name);
        }

        @Override
        public void onError(Throwable cause) throws Exception {
          log.trace("shareMediaEndpoint {}: Released failed",
                  UserSession.this.name);
        }
      });
      shareMediaEndpoint = null;
    }
  }


  @Override
  public void close() throws IOException {
      closeCamera();
      closeShareMedia();

  }

  public WebRtcEndpoint getCameraEndpoint() {
    return cameraEndpoint;
  }

  public void setCameraEndpoint(WebRtcEndpoint cameraEndpoint) {
    this.cameraEndpoint = cameraEndpoint;
  }

  public WebRtcEndpoint getShareMediaEndpoint() {
    return shareMediaEndpoint;
  }

  public void setShareMediaEndpoint(WebRtcEndpoint shareMediaEndpoint) {
    this.shareMediaEndpoint = shareMediaEndpoint;
  }

  public void addCandidate(IceCandidate candidate) {
    cameraEndpoint.addIceCandidate(candidate);
  }

  public void addCandidateShareMedia(IceCandidate candidate) {
    shareMediaEndpoint.addIceCandidate(candidate);
  }

  public String getName() {
    return name;
  }

  public String getRoomName() {
    return roomName;
  }

  public Boolean getZhuBo() {
    return isZhuBo;
  }

  @Override
  public int hashCode() {
    return (this.getName()+this.getRoomName()).hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null){
      return false;
    }
    if(obj instanceof UserSession){
       UserSession userSession = (UserSession)obj;
       return this.getName().equals(userSession.getName())
               && this.getRoomName().equals(userSession.getRoomName());
    }
    return super.equals(obj);
  }
}
