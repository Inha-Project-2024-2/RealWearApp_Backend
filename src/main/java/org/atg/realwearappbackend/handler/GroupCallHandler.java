package org.atg.realwearappbackend.handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.atg.realwearappbackend.registry.UserRegistry;
import org.atg.realwearappbackend.room.RoomManager;
import org.atg.realwearappbackend.user.UserSession;
import org.kurento.client.IceCandidate;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.logging.Logger;

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

        final UserSession user = registry.getBySession(session);

        if(user != null){
            log.debug("참가자 {}로 부터 전달 받은 메시지 : {}", user.getName(), jsonMessage);
        }else{
            log.debug("새로운 참가자로 부터 전달 받은 메시지 : {}", jsonMessage);
        }

        switch (jsonMessage.get("id").getAsString()){
            case "joinRoom":
                joinRoom(jsonMessage, session);
                break;
            case "receiveVideoFrom":
                final String senderName = jsonMessage.get("sender").getAsString();
                final UserSession sender = registry.getByName(senderName);
                final String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
                if(sender == null){
                    log.debug("전송자를 찾을 수 없습니다.");
                    break;
                }
                user.receiveVideoFromOtherUser(sender, sdpOffer);
                break;
            case "leaveRoom":
                leaveRoom(user);
                break;
            case "onIceCandidate":
                JsonObject candidate = jsonMessage.get("candidate").getAsJsonObject();

                if(user != null){
                    IceCandidate cand = new IceCandidate(candidate.get("candidate").getAsString(),
                            candidate.get("sdpMid").getAsString(), candidate.get("sdpMLineIndex").getAsInt());
                    user.addCandidate(cand, jsonMessage.get("name").getAsString());
                }
                break;
            default:
                break;
        }
    }

    private void joinRoom(JsonObject jsonMessage, WebSocketSession session) {
        
    }

    private void leaveRoom(UserSession user) {

    }
}
