package org.atg.realwearappbackend.room;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.atg.realwearappbackend.user.UserSession;
import org.kurento.client.Continuation;
import org.kurento.client.MediaPipeline;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PreDestroy;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

    public UserSession join(String username, WebSocketSession session) throws IOException {
        log.info("방 {} : 참가자 {} 추가", this.roomName, username);
        final UserSession participant = new UserSession(username, roomName, this.pipeline, session);

        joinRoom(participant);
        participants.put(username, participant);
        sendParticipantNames(participant);
        return participant;
    }

    private Collection<String> joinRoom(UserSession newParticipant) {
        final JsonObject newParticipantMsg = new JsonObject();
        newParticipantMsg.addProperty("id", "newParticipantArrived");
        newParticipantMsg.addProperty("name", newParticipant.getName());

        final List<String> participantsList = new ArrayList<>(participants.values().size());

        log.debug("방 {}: 다른 참가자들에게 새로운 참가자 {}를 알림", this.roomName, newParticipant.getName());

        for(final UserSession participant : participants.values()) {

            try {
                participant.sendMessage(newParticipantMsg);
            } catch (IOException e) {
                log.debug("방 {} : 참가자 {}가 방에 접속한 것을 알리지 못함", this.roomName, participant.getName(), e);
            }
            participantsList.add(participant.getName());
        }

        return participantsList;
    }

    private void sendParticipantNames(UserSession user) throws IOException {
        final JsonArray participantsArray = new JsonArray();
        for (final UserSession participant : this.getParticipants()) {
            if (!participant.equals(user)) {
                final JsonElement participantName = new JsonPrimitive(participant.getName());
                participantsArray.add(participantName);
            }
        }

        final JsonObject existingParticipantsMsg = new JsonObject();
        existingParticipantsMsg.addProperty("id", "existingParticipants");
        existingParticipantsMsg.add("data", participantsArray);
        log.debug("참가자 {}: 참가자 리스트(크기:{})를 전달 받음", user.getName(),
                participantsArray.size());
        user.sendMessage(existingParticipantsMsg);
    }

    public Collection<UserSession> getParticipants() {
        return participants.values();
    }

    @Override
    public void close() {

    }
}
