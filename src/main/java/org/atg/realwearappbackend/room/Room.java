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
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PreDestroy;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class Room implements Closeable {
    private final ConcurrentHashMap<String, UserSession> webParticipants = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, UserSession> androidParticipants = new ConcurrentHashMap<>();
    private final MediaPipeline pipeline;
    @Getter
    private final String roomName;

    public Room(String roomName, MediaPipeline pipeline) {
        this.roomName = roomName;
        this.pipeline = pipeline;

        log.info("방 {} 생성", roomName);
    }

    public Collection<UserSession> getWebParticipants() {
        return webParticipants.values();
    }

    public Collection<UserSession> getAndroidParticipants() {
        return androidParticipants.values();
    }

    @PreDestroy
    private void shutdown() {
        this.close();
    }

    public UserSession join(String username, WebSocketSession session, String device, Integer id) throws IOException {
        log.info("방 {} : 참가자 {} 추가", this.roomName, username);
        final UserSession participant = new UserSession(username, roomName, this.pipeline, session, device, id);

        joinRoom(participant);

        if(device.equals("web")) {
            sendParticipantNames(participant);
            webParticipants.put(username, participant);
        }else{
            androidParticipants.put(username, participant);
            responseConnect(username, participant);
        }
        return participant;
    }

    private void responseConnect(String username, UserSession participant) throws IOException {
        final JsonObject selfParticipantMsg = new JsonObject();
        selfParticipantMsg.addProperty("id", participant.getId());
        selfParticipantMsg.addProperty("jsonrpc", "2.0");

        final JsonObject result = new JsonObject();
        result.addProperty("success", true);
        selfParticipantMsg.add("result", result);
        participant.sendMessage(selfParticipantMsg);
    }

    private Collection<String> joinRoom(UserSession newParticipant) {
        final JsonObject newWebParticipantMsg = new JsonObject();
        newWebParticipantMsg.addProperty("id", "newParticipantArrived");
        newWebParticipantMsg.addProperty("name", newParticipant.getName());

        final List<String> participantsList = new ArrayList<>(webParticipants.values().size());

        log.debug("방 {}: 다른 참가자들에게 새로운 참가자 {}를 알림", this.roomName, newParticipant.getName());

        for(final UserSession participant : webParticipants.values()) {

            try {
                participant.sendMessage(newWebParticipantMsg);
            } catch (IOException e) {
                log.debug("방 {} : 참가자 {}가 방에 접속한 것을 알리지 못함", this.roomName, participant.getName(), e);
            }
            participantsList.add(participant.getName());
        }

        return participantsList;
    }

    private void sendParticipantNames(UserSession user) throws IOException {
        final JsonArray participantsArray = new JsonArray();
        for (final UserSession participant : this.getWebParticipants()) {
            if (!participant.equals(user)) {
                final JsonElement participantName = new JsonPrimitive(participant.getName());
                participantsArray.add(participantName);
            }
        }

        for (final UserSession participant : this.getAndroidParticipants()) {
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

    public void leave(UserSession user) throws IOException {
        log.debug("참가자 {}: 방 {}을 떠남", user.getName(), this.roomName);
        this.removeParticipant(user.getName());
        user.close();
    }

    private void removeParticipant(String name) throws IOException {
        webParticipants.remove(name);

        log.debug("방 {}: 모든 유저에게 참가자 {}가 방을 떠난다고 알림", this.roomName, name);

        final List<String> unNotifiedParticipants = new ArrayList<>();
        final JsonObject participantLeftJson = new JsonObject();
        participantLeftJson.addProperty("id", "participantLeft");
        participantLeftJson.addProperty("name", name);
        for (final UserSession participant : webParticipants.values()) {
            try {
                participant.cancelVideoFrom(name);
                participant.sendMessage(participantLeftJson);
            } catch (final IOException e) {
                unNotifiedParticipants.add(participant.getName());
            }
        }

        if (!unNotifiedParticipants.isEmpty()) {
            log.debug("방 {}: 참가자들이 {} 참가자 {}가 방을 떠났다는 알림을 받지 못함", this.roomName,
                    unNotifiedParticipants, name);
        }

    }




    @Override
    public void close() {
        for (final UserSession user : webParticipants.values()) {
            try {
                user.close();
            } catch (IOException e) {
                log.debug("방 {}: 참가자 {}의 닫기를 실행 하지 못함 ", this.roomName, user.getName(), e);
            }
        }

        webParticipants.clear();

        pipeline.release(new Continuation<Void>() {

            @Override
            public void onSuccess(Void result) throws Exception {
                log.trace("방 {}: 파이프라인 해제", Room.this.roomName);
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                log.warn("참가자 {}: 파이프라인을 해제하지 못함", Room.this.roomName);
            }
        });

        log.debug("방 {}이 닫아짐", this.roomName);

    }
}
