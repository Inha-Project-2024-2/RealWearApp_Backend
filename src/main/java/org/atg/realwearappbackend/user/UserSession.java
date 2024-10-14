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
}
