package org.atg.realwearappbackend.user;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.EventListener;
import org.kurento.client.IceCandidateFoundEvent;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.jsonrpc.JsonUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class UserSession {
    @Getter
    private final String name;
    @Getter
    private final WebSocketSession session;


    private final MediaPipeline pipeline;

    @Getter
    private final String roomName;

    @Getter
    private final WebRtcEndpoint outboundEndpoint;
    private final ConcurrentHashMap<String, WebRtcEndpoint> inboundEndPoint = new ConcurrentHashMap<>();

    public UserSession(String name, WebSocketSession session, MediaPipeline pipeline, String roomName, WebRtcEndpoint outboundEndpoint) {
        this.name = name;
        this.session = session;
        this.pipeline = pipeline;
        this.roomName = roomName;
        this.outboundEndpoint = outboundEndpoint;

        this.outboundEndpoint.addIceCandidateFoundListener(event -> {
            JsonObject response = new JsonObject();
            response.addProperty("id", "iceCandidate");
            response.addProperty("name", name);
            response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
            try {
                synchronized (session) {
                    session.sendMessage(new TextMessage(response.toString()));
                }
            } catch (IOException e) {
                log.debug(e.getMessage());
            }
        });
    }
    private WebRtcEndpoint getEndpointForUser(final UserSession sender){
        if(sender.getName().equals(name)){
            log.debug("참가자 {} : loopback", this.name); // 자기 자신의 화면을 보기 위한 설정 및 추후 필터 적용을 위한 도구
            return outboundEndpoint;
        }

        log.debug("참가자 {} : 참가자 {}의 비디오를 받는 중", this.name, sender.getName());

        WebRtcEndpoint inbound = inboundEndPoint.get(sender.getName());
        if(inbound == null){
            log.debug("참가자 {} : 기존의 엔드포인트가 없기 때문에 참가자 {} 를 위한 새로운 엔드 포인트 생성", this.name, sender.getName());
            inbound = new WebRtcEndpoint.Builder(pipeline).build();

            inbound.addIceCandidateFoundListener(event -> {
                JsonObject response = new JsonObject();
                response.addProperty("id", "iceCandidate");
                response.addProperty("name", sender.getName());
                response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
                try {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(response.toString()));
                    }
                } catch (IOException e) {
                    log.debug(e.getMessage());
                }
            });
            inboundEndPoint.put(sender.getName(), inbound);
        }
        log.debug("참가자 {} : 참가자 {} 의 엔드 포인트 생성", this.name, sender.getName());

        sender.getOutboundEndpoint().connect(inbound);
        return inbound;
    }
}
