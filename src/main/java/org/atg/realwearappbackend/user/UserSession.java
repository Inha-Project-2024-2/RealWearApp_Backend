package org.atg.realwearappbackend.user;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.*;
import org.kurento.jsonrpc.JsonUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class UserSession implements Closeable {
    @Getter
    private final String name;
    @Getter
    private final WebSocketSession session;

    private final MediaPipeline pipeline;

    @Getter
    private final String roomName;

    // 안드로이드 참가자를 위한 속성
    @Getter
    private final String deviceType;

    @Getter
    private final Integer id;

    @Getter
    private final WebRtcEndpoint outboundEndpoint;
    private final ConcurrentHashMap<String, WebRtcEndpoint> inboundEndPoint = new ConcurrentHashMap<>();

    public UserSession(String name, String roomName, MediaPipeline pipeline, WebSocketSession session, String deviceType, Integer id) {
        this.name = name;
        this.session = session;
        this.pipeline = pipeline;
        this.roomName = roomName;
        this.outboundEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
        this.deviceType = deviceType;
        this.id = id;

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

    public void receiveVideoFromOtherUser(UserSession sender, String sdpOffer) throws IOException {
        log.info("참가자 {}:{} 방에 있는 참가자 {} 와 연결 중", this.name, this.roomName, sender.getName());

        log.trace("참가자 {}: 참가자 {}에 대한 SdpOffer {}", this.name, sender.getName(), sdpOffer);

        final String ipSdpAnswer = this.getEndpointForOtherUser(sender).processOffer(sdpOffer);
        final JsonObject scParams = new JsonObject();
        scParams.addProperty("id", "receiveVideoAnswer");
        scParams.addProperty("name", sender.getName());
        scParams.addProperty("sdpAnswer", ipSdpAnswer);

        log.trace("참가자 {}: 참가자 {}에 대한 SdpAnswer {}", this.name, sender.getName(), ipSdpAnswer);
        this.sendMessage(scParams);
        log.debug("후보 모으는 중");
        this.getEndpointForOtherUser(sender).gatherCandidates();
    }

    public void sendMessage(JsonObject message) throws IOException {
        log.debug("참가자 {}: 메시지 전송 {}", name, message);
        synchronized (session) {
            session.sendMessage(new TextMessage(message.toString()));
        }
    }

    private WebRtcEndpoint getEndpointForOtherUser(final UserSession sender){
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

    public void cancelVideoFrom(final String senderName) {
        log.debug("참가자 {}: 참가자 {}의 비디오 수신 취소", this.name, senderName);
        final WebRtcEndpoint incoming = inboundEndPoint.remove(senderName);

        log.debug("참가자 {}: 참가자 {}의 비디오 엔드포인트 삭제", this.name, senderName);
        incoming.release(new Continuation<Void>() {
            @Override
            public void onSuccess(Void result) throws Exception {
                log.trace("참가자 {}: 성공적으로 참가자 {}의 엔드포인트 삭제",
                        UserSession.this.name, senderName);
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                log.warn("참가자 {}:  참가자 {}의 엔드포인트 삭제 실패", UserSession.this.name,
                        senderName);
            }
        });
    }

    @Override
    public void close() throws IOException {
        log.debug("참가자 {}: 리소스 해제", this.name);
        for (final String remoteParticipantName : inboundEndPoint.keySet()) {

            log.trace("참가자 {}: 참가자{}의 incoming 엔드 포인트 삭제 ", this.name, remoteParticipantName);

            final WebRtcEndpoint ep = this.inboundEndPoint.get(remoteParticipantName);

            ep.release(new Continuation<Void>() {

                @Override
                public void onSuccess(Void result) throws Exception {
                    log.trace("참가자 {}: 성공적으로 참가자 {}의 엔드포인트 삭제",
                            UserSession.this.name, remoteParticipantName);
                }

                @Override
                public void onError(Throwable cause) throws Exception {
                    log.warn("참가자 {}:  참가자 {}의 엔드포인트 삭제 실패", UserSession.this.name,
                            remoteParticipantName);
                }
            });
        }

        outboundEndpoint.release(new Continuation<Void>() {

            @Override
            public void onSuccess(Void result) throws Exception {
                log.trace("참가자 {}: outgoing 엔드 포인트 삭제", UserSession.this.name);
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                log.warn("참가자 {}: outgoing 엔드 포인트 삭제 실패", UserSession.this.name);
            }
        });
    }

    public void addCandidate(IceCandidate candidate, String name) {
        if (this.name.equals(name)) {
            outboundEndpoint.addIceCandidate(candidate);
        } else {
            WebRtcEndpoint webRtc = inboundEndPoint.get(name);
            if (webRtc != null) {
                webRtc.addIceCandidate(candidate);
            }
        }
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (!(obj instanceof UserSession other)) {
            return false;
        }
        boolean eq = name.equals(other.name);
        eq &= roomName.equals(other.roomName);
        return eq;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + name.hashCode();
        result = 31 * result + roomName.hashCode();
        return result;
    }
}
