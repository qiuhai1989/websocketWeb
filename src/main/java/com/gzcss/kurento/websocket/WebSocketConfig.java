package com.gzcss.kurento.websocket;

import org.kurento.client.KurentoClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * @author 丘海
 * @date 2017-11-23 16:02
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig extends WebMvcConfigurerAdapter implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(WebSocketPushHandler(),"/call").addInterceptors(new MyWebSocketInterceptor());
        registry.addHandler(WebSocketPushHandler(), "/sockjs/webSocketServer").addInterceptors(new MyWebSocketInterceptor())
                .withSockJS();
    }

    @Bean
    public WebSocketHandler WebSocketPushHandler(){
        return new WebSocketPushHandler();
    }

    @Bean
    public KurentoClient kurentoClient() {
        return KurentoClient.create();
    }

}
