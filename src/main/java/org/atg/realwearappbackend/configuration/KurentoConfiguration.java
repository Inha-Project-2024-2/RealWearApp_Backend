package org.atg.realwearappbackend.configuration;


import org.atg.realwearappbackend.handler.GroupCallHandler;
import org.atg.realwearappbackend.registry.UserRegistry;
import org.atg.realwearappbackend.room.RoomManager;
import org.kurento.client.KurentoClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class KurentoConfiguration implements WebSocketConfigurer {
    @Bean
    public UserRegistry registry(){
        return new UserRegistry();
    }

    @Bean
    public GroupCallHandler groupCallHandler(){
        return new GroupCallHandler();
    }

    @Bean
    public KurentoClient kurentoClient(){
        return KurentoClient.create();
    }

    @Bean
    public RoomManager roomManager(){
        return new RoomManager();
    }

    @Bean
    public ServletServerContainerFactoryBean servletServerContainer(){
        ServletServerContainerFactoryBean servletServerContainer = new ServletServerContainerFactoryBean();
        servletServerContainer.setMaxTextMessageBufferSize(32768);
        return servletServerContainer;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(groupCallHandler(), "/groupcall");
    }
}
