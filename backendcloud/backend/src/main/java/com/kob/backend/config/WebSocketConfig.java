package com.kob.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@Configuration
public class WebSocketConfig {

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {

        return new ServerEndpointExporter();
    }
}

//这段代码用于注册WebSocket的端点，使得客户端（如浏览器）能够与服务器建立 WebSocket 连接
//当你在服务端通过 @ServerEndpoint 注解注册一个 WebSocket 端点后，客户端连接的 URL 就是这个端点定义的路径，每个独立的 URL 对应一个 WebSocket 连接
//@ServerEndpoint("/websocket/{userId}")  // 定义WebSocket连接路径
//@Component
//public class MyWebSocketEndpoint {
//    // 处理连接、消息等方法...
//}