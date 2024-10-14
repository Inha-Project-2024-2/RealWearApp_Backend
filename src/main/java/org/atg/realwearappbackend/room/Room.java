package org.atg.realwearappbackend.room;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.atg.realwearappbackend.user.UserSession;
import org.kurento.client.MediaPipeline;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PreDestroy;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class Room implements Closeable {
    private final ConcurrentHashMap<String, UserSession> participants = new ConcurrentHashMap<>();
    private final MediaPipeline pipeline;
    @Getter
    private final String roomName;

    public Room(String roomName, MediaPipeline pipeline) {
        this.roomName = roomName;
        this.pipeline = pipeline;

        log.info("방 {} 생성", roomName);
    }

    @PreDestroy
    private void shutdown() {
        this.close();
    }

    public UserSession join(String username, WebSocketSession session) {
        log.info("방 {} : 참가자 {} 추가", this.roomName, username);
        final UserSession participant = new UserSession(username, roomName, this.pipeline, session);

        joinRoom(participant);
        participants.put(username, participant);
        sendParticipantNames(participant);
        return participant;
    }

    private void joinRoom(UserSession participant) {

    }

    private void sendParticipantNames(UserSession participant) {
    }

    @Override
    public void close() {

    }
}
