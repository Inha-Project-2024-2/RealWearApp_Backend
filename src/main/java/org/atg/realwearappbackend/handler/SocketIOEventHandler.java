package org.atg.realwearappbackend.handler;

import com.corundumstudio.socketio.SocketIOServer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class SocketIOEventHandler {
    private final SocketIOServer server;
    private final Map<String, String> connectedUsers = new ConcurrentHashMap<>();
    private final Map<String, CallInfo> activeCallMap = new ConcurrentHashMap<>();

    @Data
    @AllArgsConstructor
    private static class CallInfo {
        private String caller;
        private String callee;
    }

    public SocketIOEventHandler(SocketIOServer server) {
        this.server = server;
        this.initializeEventListeners();
    }

    private void initializeEventListeners() {
        // 연결 이벤트 처리
        server.addConnectListener(client -> {
            log.info("Client connected: {}", client.getSessionId());
        });

        // 사용자 등록
        server.addEventListener("register", String.class, (client, userId, ackRequest) -> {
            log.info("User registered: {} with socket id: {}", userId, client.getSessionId());
            client.set("userId", userId); // 소켓 세션에 userId 저장
            connectedUsers.put(userId, client.getSessionId().toString());
            
            // 연결된 사용자 목록 브로드캐스트
            server.getBroadcastOperations().sendEvent("userList", 
                new ArrayList<>(connectedUsers.keySet()));
        });

        // 통화 요청
        server.addEventListener("call-request", Map.class, (client, data, ackRequest) -> {
            String targetUserId = (String) data.get("targetUserId");
            String targetSocketId = connectedUsers.get(targetUserId);
            
            if (targetSocketId != null) {
                String callerId = (String) client.get("userId");
                String callId = callerId + "-" + targetUserId;
                
                activeCallMap.put(callId, new CallInfo(callerId, targetUserId));
                
                Map<String, Object> response = new HashMap<>();
                response.put("callerId", callerId);
                
                server.getClient(UUID.fromString(targetSocketId))
                      .sendEvent("call-received", response);
                
                log.info("Call request from {} to {}", callerId, targetUserId);
            }
        });

        // WebRTC Offer 처리
        server.addEventListener("offer", Map.class, (client, data, ackRequest) -> {
            String targetUserId = (String) data.get("targetUserId");
            Object offer = data.get("offer");
            String targetSocketId = connectedUsers.get(targetUserId);
            
            if (targetSocketId != null) {
                String senderId = (String) client.get("userId");
                Map<String, Object> response = new HashMap<>();
                response.put("offer", offer);
                response.put("callerId", senderId);
                
                server.getClient(UUID.fromString(targetSocketId))
                      .sendEvent("offer", response);
                
                log.info("Offer forwarded from {} to {}", senderId, targetUserId);
            }
        });

        // WebRTC Answer 처리
        server.addEventListener("answer", Map.class, (client, data, ackRequest) -> {
            String targetUserId = (String) data.get("targetUserId");
            Object answer = data.get("answer");
            String targetSocketId = connectedUsers.get(targetUserId);
            
            if (targetSocketId != null) {
                String senderId = (String) client.get("userId");
                Map<String, Object> response = new HashMap<>();
                response.put("answer", answer);
                response.put("answererId", senderId);
                
                server.getClient(UUID.fromString(targetSocketId))
                      .sendEvent("answer", response);
                
                log.info("Answer forwarded from {} to {}", senderId, targetUserId);
            }
        });

        // ICE Candidate 처리
        server.addEventListener("ice-candidate", Map.class, (client, data, ackRequest) -> {
            String targetUserId = (String) data.get("targetUserId");
            Object candidate = data.get("candidate");
            String targetSocketId = connectedUsers.get(targetUserId);
            
            if (targetSocketId != null) {
                String senderId = (String) client.get("userId");
                Map<String, Object> response = new HashMap<>();
                response.put("candidate", candidate);
                response.put("senderId", senderId);
                
                server.getClient(UUID.fromString(targetSocketId))
                      .sendEvent("ice-candidate", response);
                
                log.info("ICE candidate forwarded from {} to {}", senderId, targetUserId);
            }
        });

        // 통화 수락
        server.addEventListener("call-accepted", Map.class, (client, data, ackRequest) -> {
            String callerId = (String) data.get("callerId");
            String accepterId = (String) client.get("userId");
            String callerSocketId = connectedUsers.get(callerId);
            
            if (callerSocketId != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("accepterId", accepterId);
                
                server.getClient(UUID.fromString(callerSocketId))
                      .sendEvent("call-accepted", response);
                
                log.info("Call accepted by {} for caller {}", accepterId, callerId);
            }
        });

        // 통화 종료
        server.addEventListener("end-call", Map.class, (client, data, ackRequest) -> {
            String targetUserId = (String) data.get("targetUserId");
            String senderId = (String) client.get("userId");
            String targetSocketId = connectedUsers.get(targetUserId);
            
            String callId1 = senderId + "-" + targetUserId;
            String callId2 = targetUserId + "-" + senderId;
            
            activeCallMap.remove(callId1);
            activeCallMap.remove(callId2);
            
            if (targetSocketId != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("enderId", senderId);
                
                server.getClient(UUID.fromString(targetSocketId))
                      .sendEvent("call-ended", response);
                
                log.info("Call ended by {} to {}", senderId, targetUserId);
            }
        });

        // 연결 해제
        server.addDisconnectListener(client -> {
            String userId = (String) client.get("userId");
            if (userId != null) {
                log.info("Client disconnected: {}", userId);
                connectedUsers.remove(userId);
                
                // 진행 중인 통화 정리
                activeCallMap.forEach((callId, call) -> {
                    if (call.getCaller().equals(userId) || call.getCallee().equals(userId)) {
                        String otherUserId = call.getCaller().equals(userId) ? 
                            call.getCallee() : call.getCaller();
                        String otherSocketId = connectedUsers.get(otherUserId);
                        
                        if (otherSocketId != null) {
                            Map<String, Object> response = new HashMap<>();
                            response.put("enderId", userId);
                            
                            server.getClient(UUID.fromString(otherSocketId))
                                  .sendEvent("call-ended", response);
                        }
                        activeCallMap.remove(callId);
                    }
                });
                
                // 연결된 사용자 목록 업데이트
                server.getBroadcastOperations().sendEvent("userList", 
                    new ArrayList<>(connectedUsers.keySet()));
                server.getBroadcastOperations().sendEvent("user-disconnected", userId);
            }
        });
    }
}