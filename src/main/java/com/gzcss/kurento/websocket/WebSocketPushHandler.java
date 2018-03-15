package com.gzcss.kurento.websocket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**消息处理类
 * @author 丘海
 * @date 2017-11-23 16:03
 */
public class WebSocketPushHandler implements WebSocketHandler {
    private static Logger log = LoggerFactory.getLogger(WebSocketPushHandler.class);
    private static final List<WebSocketSession> users = new ArrayList<>();
    //private static final Map<String,UserSession>userSessionHashMap = new HashMap<>();
    private static final Map<String,UserSession>userSessionHashMapBySeesionId = new HashMap<>();
    private Map<String,Room> rooms = new HashMap<>();
    @Override
    public void afterConnectionEstablished(WebSocketSession webSocketSession) throws Exception {
        System.out.println("成功进入了系统。。。");
        users.add(webSocketSession);
    }

    @Override
    public void handleMessage(WebSocketSession webSocketSession, WebSocketMessage<?> webSocketMessage) throws Exception {
        //将消息进行转化，因为是消息是json数据，可能里面包含了发送给某个人的信息，所以需要用json相关的工具类处理之后再封装成TextMessage，我这儿并没有做处理，消息的封装格式一般有{from:xxxx,to:xxxxx,msg:xxxxx}，来自哪里，发送给谁，什么消息等等
        String msg = (String) webSocketMessage.getPayload();
        JSONObject jsonObject = JSON.parseObject(msg);
        //System.out.println(jsonObject.getString("name")+"--"+jsonObject.getString("sex"));
        //TextMessage textMessage = new TextMessage(msg);
        //给所有用户群发消息
        //sendMessagesToUsers(textMessage);
        //给指定用户群发消息
        //sendMessageToUser(userId,msg);
        String roomPk = jsonObject.getString("room");
        String sendToPk = jsonObject.getString("sendTo");
        UserSession sendToUser = null;
        switch (jsonObject.getString("type")){

            case "login":

                log.info("User logged in as "+ sendToPk+" in room "+roomPk);
                Room curRoom = rooms.get(roomPk);
                if(curRoom == null){
                    curRoom = new Room(roomPk);
                    rooms.put(roomPk,curRoom);
                }
                boolean isExists = false ;
                for(String usePk:curRoom.getUserSessionMap().keySet()){
                    if(usePk.equals(sendToPk)){
                        isExists = true;
                        break;
                    }
                }
                if(isExists){
                    JSONObject result = new JSONObject();
                    result.put("type","login");
                    result.put("success",false);
                    TextMessage textMessage = new TextMessage(result.toJSONString());
                    webSocketSession.sendMessage(textMessage);
                }else {
                    UserSession newUser = new UserSession(roomPk,sendToPk,webSocketSession);
                    curRoom.addUser(newUser);
                    JSONObject result = new JSONObject();
                    if(sendToPk.equals("v-hai")){
                        result.put("type","login");
                        result.put("success",true);
                        result.put("openCamera",false);
                        result.put("sendFrom",sendToPk);
                    }else{
                        result.put("type","login");
                        result.put("success",true);
                        result.put("openCamera",true);
                        result.put("sendFrom",sendToPk);

                    }
                    //userSessionHashMap.put(sendToPk,newUser);
                    userSessionHashMapBySeesionId.put(webSocketSession.getId(),newUser);
                    TextMessage textMessage = new TextMessage(result.toJSONString());
                    sendMessageToUser(newUser,textMessage);
                }

                break;
            case "offer":
                log.info("Sending offer to room "+roomPk+" name "+sendToPk);
                sendToUser = getUserSession(roomPk,sendToPk);
                if (sendToUser != null) {
                    JSONObject result = new JSONObject();
                    result.put("type","offer");
                    result.put("offer",jsonObject.getString("offer"));
                    result.put("sendFrom",getUserSessionBySession(webSocketSession).getUserPk());
                    TextMessage textMessage = new TextMessage(result.toJSONString());
                    sendMessageToUser(sendToUser,textMessage);
                }
                break;
            case "answer":
                log.info("Sending answer to room "+roomPk+" name "+sendToPk);
                sendToUser = getUserSession(roomPk,sendToPk);
                if(sendToUser != null){
                    JSONObject result = new JSONObject();
                    result.put("type","answer");
                    result.put("answer",jsonObject.getString("answer"));
                    result.put("sendFrom",getUserSessionBySession(webSocketSession).getUserPk());
                    TextMessage textMessage = new TextMessage(result.toJSONString());
                    sendMessageToUser(sendToUser,textMessage);
                }else{
                    log.info("room "+roomPk+" name"+sendToPk +" not found");
                }
                break;
            case "candidate":
                log.info("Sending candidate to room "+roomPk+" name "+sendToPk);
                sendToUser = getUserSession(roomPk,sendToPk);
                if(sendToUser != null){
                    JSONObject result = new JSONObject();
                    result.put("type","candidate");
                    result.put("candidate",jsonObject.getString("candidate"));
                    result.put("sendFrom",getUserSessionBySession(webSocketSession).getUserPk());
                    TextMessage textMessage = new TextMessage(result.toJSONString());
                    sendMessageToUser(sendToUser,textMessage);
                }else{
                    log.info("room "+roomPk+" name"+sendToPk +" not found");
                }
                break;
            case "leave":
                log.info("Disconnecting user from room "+roomPk+" name "+sendToPk);
                sendToUser = getUserSession(roomPk,sendToPk);
                if(sendToUser != null){
                    JSONObject result = new JSONObject();
                    result.put("type","leave");
                    result.put("sendFrom",getUserSessionBySession(webSocketSession).getUserPk());
                    TextMessage textMessage = new TextMessage(result.toJSONString());
                    sendMessageToUser(sendToUser,textMessage);
                }else{
                    log.info("room "+roomPk+" name"+sendToPk +" not found");
                }
                break;
            case "msg":
                log.info("Sending msg to room "+roomPk+" name "+sendToPk);
                sendToUser = getUserSession(roomPk,sendToPk);
                if(sendToUser != null){
                    JSONObject result = new JSONObject();
                    result.put("type","msg");
                    result.put("msg",jsonObject.getString("msg"));
                    result.put("sendFrom",getUserSessionBySession(webSocketSession).getUserPk());
                    TextMessage textMessage = new TextMessage(result.toJSONString());
                    sendMessageToUser(sendToUser,textMessage);
                }else{
                    log.info("room "+roomPk+" name"+sendToPk +" not found");
                }
                break;
            case "notification":
                log.info("Sending notification to room "+roomPk+" name "+sendToPk);
                sendToUser = getUserSession(roomPk,sendToPk);
                if(sendToUser != null){
                    JSONObject result = new JSONObject();
                    result.put("type","notification");
                    result.put("msg",jsonObject.getString("msg"));
                    result.put("sendFrom",getUserSessionBySession(webSocketSession).getUserPk());
                    TextMessage textMessage = new TextMessage(result.toJSONString());
                    sendMessageToUser(sendToUser,textMessage);
                }else{
                    log.info("room "+roomPk+" name"+sendToPk +" not found");
                }
                break;
            default:
                JSONObject result = new JSONObject();
                result.put("type","error");
                result.put("message","Unrecognized command: " + jsonObject.getString("type"));
                result.put("sendFrom",getUserSessionBySession(webSocketSession).getUserPk());
                TextMessage textMessage = new TextMessage(result.toJSONString());
                sendMessageToUser(sendToPk,roomPk,textMessage);
                break;
        }

    }

    //后台错误信息处理方法
    @Override
    public void handleTransportError(WebSocketSession webSocketSession, Throwable throwable) throws Exception {
        //log.error(throwable.getMessage(),throwable);
    }

    //用户退出后的处理，不如退出之后，要将用户信息从websocket的session中remove掉，这样用户就处于离线状态了，也不会占用系统资源
    @Override
    public void afterConnectionClosed(WebSocketSession webSocketSession, CloseStatus closeStatus) throws Exception {
        UserSession userSession = getUserSessionBySession(webSocketSession);
        if(userSession ==null){
            return ;
        }
        String userPk = userSession.getUserPk();
        String roomPk = userSession.getRoomPk();
        log.info("close pk:"+userPk+",room:"+roomPk);
        Room currentRoom = rooms.get(roomPk);
        if(currentRoom != null){
            for(UserSession user:currentRoom.getUserSessionMap().values()){
                if(user.getUserPk().equals(userPk)){
                    continue;
                }
                JSONObject result = new JSONObject();
                result.put("type","leave");
                result.put("sendFrom",getUserSessionBySession(webSocketSession).getUserPk());
                TextMessage textMessage = new TextMessage(result.toJSONString());
                sendMessageToUser(user,textMessage);
            }
        }
        removeConnection(webSocketSession);

    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 给所有的用户发送消息
     */
    public void sendMessagesToUsers(TextMessage message){
        for(WebSocketSession user : users){
            try {
                //isOpen()在线就发送
                if(user.isOpen()){
                    user.sendMessage(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 发送消息给指定的用户
     */
    public void sendMessageToUser(String userPK,String roomPk,TextMessage message){
/*        for(WebSocketSession user : users){
            if(user.getAttributes().get(Constants.CURRENT_WEBSOCKET_USER).equals(userId)){
                try {
                    //isOpen()在线就发送
                    if(user.isOpen()){
                        user.sendMessage(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }*/
        Room currentRoom = rooms.get(roomPk);
        UserSession userSession = currentRoom.getUser(userPK);
        try {
//            userSession.getSocketSession().sendMessage(message);
            userSession.sendMessageToClient(message);
        } catch (IOException e) {
//            e.printStackTrace();
            log.error(e.getMessage(),e);
        }
    }

    /**
     * 发送消息给指定的用户
     */
    public void sendMessageToUser(UserSession userSession,TextMessage message){


        try {
            userSession.getSocketSession().sendMessage(message);
        } catch (IOException e) {
            log.error(e.getMessage(),e);
        }
    }

    private UserSession getUserSession(String room ,String pk){
        Room currentRoom = rooms.get(room);
        if(currentRoom != null){
            return currentRoom.getUser(pk);
        }

        return null;
    }

    private WebSocketSession getConnection(String room ,String pk){
        UserSession userSession = getUserSession(room,pk);
        if(userSession == null){
            return null;
        }
        return  userSession.getSocketSession();
    }

    private UserSession getUserSessionBySession(WebSocketSession webSocketSession){
        return userSessionHashMapBySeesionId.get(webSocketSession.getId());
    }

    private void removeConnection(WebSocketSession socketSession){
        UserSession userSession = getUserSessionBySession(socketSession);
        //String userPk = userSession.getUserPk();
        String roomPk = userSession.getRoomPk();
        Room currentRoom = rooms.get(roomPk);
        if(currentRoom !=null){
            currentRoom.removeSpecifiedUser(userSession);
        }
        userSessionHashMapBySeesionId.remove(socketSession.getId());
    }

}
