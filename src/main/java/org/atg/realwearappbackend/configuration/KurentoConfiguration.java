package org.atg.realwearappbackend.configuration;


import lombok.extern.slf4j.Slf4j;
import org.atg.realwearappbackend.handler.GroupCallHandler;
import org.atg.realwearappbackend.registry.UserRegistry;
import org.atg.realwearappbackend.room.Room;
import org.kurento.client.KurentoClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableWebSocket
public class KurentoConfiguration implements WebSocketConfigurer {
    @Bean
    public UserRegistry registry(){
        return new UserRegistry();
    }

    @Bean
    public GroupCallHandler groupCallHandler(){
        return new GroupCallHandler(roomManager(), registry());
    }

    @Bean
    public KurentoClient kurentoClient(){
        return KurentoClient.create();
    }

    @Bean
    public RoomManager roomManager(){
        return new RoomManager(kurentoClient());
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

    @Slf4j
    public static class RoomManager {
        private final KurentoClient kurento;

        private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();

        public RoomManager(KurentoClient kurento) {
            this.kurento = kurento;
        }

        public Room getRoom(String roomName){
            log.debug("{} 라는 방이 있는지 탐색 중", roomName);
            Room room = rooms.get(roomName);
            if(room == null) {
                log.debug("{} 방이 존재하지 않기 때문에 생성함", roomName);
                room = new Room(roomName, kurento.createMediaPipeline());
                rooms.put(roomName, room);
                log.debug("{} 방을 생성 했습니다", roomName);
                return room;
            }

            log.debug("{} 방을 발견 했습니다.", roomName);
            return room;
        }

        public void removeRoom(Room room){
            this.rooms.remove(room.getRoomName());
            room.close();
            log.info("{} 방을 삭제했습니다", room.getRoomName());
        }
    }
}
