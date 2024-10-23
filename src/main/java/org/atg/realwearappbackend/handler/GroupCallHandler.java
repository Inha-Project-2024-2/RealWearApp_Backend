package org.atg.realwearappbackend.handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.atg.realwearappbackend.registry.UserRegistry;
import org.atg.realwearappbackend.room.Room;
import org.atg.realwearappbackend.room.RoomManager;
import org.atg.realwearappbackend.user.UserSession;
import org.kurento.client.IceCandidate;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

@Slf4j
public class GroupCallHandler extends TextWebSocketHandler {
    private static final Gson gson = new GsonBuilder().create();

    private final RoomManager roomManager;

    private final UserRegistry registry;


    public GroupCallHandler(RoomManager roomManager, UserRegistry registry) {
        this.roomManager = roomManager;
        this.registry = registry;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        final JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);
        final String method = jsonMessage.get("method").getAsString();
        final Integer id = jsonMessage.get("id").getAsInt();
        final UserSession user = registry.getBySession(session);
        final JsonObject params = jsonMessage.getAsJsonObject("params");

        if (user != null) {
            log.info("참가자 {}로 부터 전달 받은 메시지 : {}", user.getName(), jsonMessage);
        } else {
            log.info("새로운 참가자로 부터 전달 받은 메시지 : {}", jsonMessage);
        }

        switch (method) {
            case "joinRoom":
                joinRoom(id, params, session);
                break;
            case "receiveVideoFrom":
                final String senderName = params.get("sender").getAsString();
                final UserSession sender = registry.getByName(senderName);
                final String sdpOffer = params.get("sdpOffer").getAsString();
                if (sender == null) {
                    log.debug("전송자를 찾을 수 없습니다.");
                    break;
                }
                user.receiveVideoFromOtherUser(sender, sdpOffer);
                break;
            case "leaveRoom":
                leaveRoom(user);
                break;
            case "onIceCandidate":
                JsonObject candidate = params.get("candidate").getAsJsonObject();

                if (user != null) {
                    IceCandidate cand = new IceCandidate(candidate.get("candidate").getAsString(),
                            candidate.get("sdpMid").getAsString(), candidate.get("sdpMLineIndex").getAsInt());
                    user.addCandidate(cand, params.get("name").getAsString());
                }
                break;
            default:
                break;
        }
    }

    private void joinRoom(Integer id, JsonObject params, WebSocketSession session) throws IOException {
        final String roomName = params.get("room").getAsString();
        final String name = params.get("user").getAsString();
        final String device;
        if (params.has("device")) {
            device = params.get("device").getAsString();
        } else {
            device = "android";
        }
        log.info("참가자 {} : {} 방에 입장 시도", name, roomName);

        Room room = roomManager.getRoom(roomName);
        final UserSession user = room.join(name, session, device, id);
        registry.register(user);
    }

    private void leaveRoom(UserSession user) throws IOException {
        final Room room = roomManager.getRoom(user.getRoomName());
        room.leave(user);
        if (room.getWebParticipants().isEmpty()) {
            roomManager.removeRoom(room);
        }
    }
}
