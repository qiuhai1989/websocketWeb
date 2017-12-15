package com.gzcss.kurento.websocket;

import com.gzcss.kurento.bean.UserSession;
import org.kurento.client.IceCandidate;
import org.kurento.client.WebRtcEndpoint;

import java.io.IOException;

/**
 * 封装不同来源流端点获取相关操作
 * Created by qiu on 2017/12/14.
 */
public interface MediaEndpointOperate {

    /**
     * 视频流推送人与客户端 并非一定同一对象 分以下情况：
     * 1.当视频推送人调用该方法时 两者为同一对象
     * 2.当观众调用该方法适合 两者不为相同对象
     * 响应客户端建立连接请求
     * @param sender 视频流推送人
     * @param sdpOffer 客户端生成sdp
     * @throws IOException
     */
    void receiveVideoFrom(UserSession sender, String sdpOffer) throws IOException ;

    /**
     * 返回与指定sender相关的Endpoint
     * @param sender
     * @return
     */
    WebRtcEndpoint getEndpointForUser(final UserSession sender) ;

    void close();

    void cancelVideoFrom(final String senderName);

    void addCandidate(IceCandidate candidate, String name) ;
}
