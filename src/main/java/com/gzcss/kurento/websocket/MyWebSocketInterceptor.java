package com.gzcss.kurento.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author 丘海
 * @date 2017-11-23 16:07
 */
public class MyWebSocketInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse serverHttpResponse, WebSocketHandler webSocketHandler, Map<String, Object> attributes) throws Exception {
        //将ServerHttpRequest转换成request请求相关的类，用来获取request域中的用户信息
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest  = (ServletServerHttpRequest) request;
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            //Constants.CURRENT_USER这个是我定义的常量，是request域的key，通过key就可以获取到用户信息了
            //TsUser user = (TsUser)httpRequest.getAttribute(Constants.CURRENT_USER);
            //Constants.CURRENT_WEBSOCKET_USER也是常量，用来存储WebsocketSession的key值
            //attributes.put(Constants.CURRENT_WEBSOCKET_USER,user.getUserid());
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse, WebSocketHandler webSocketHandler, Exception e) {

    }
}
