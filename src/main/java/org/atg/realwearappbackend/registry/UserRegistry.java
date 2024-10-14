package org.atg.realwearappbackend.registry;

import lombok.AllArgsConstructor;
import org.atg.realwearappbackend.user.UserSession;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;

public class UserRegistry {

    private final ConcurrentHashMap<String, UserSession> usersByName = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, UserSession> usersBySessionId = new ConcurrentHashMap<>();


    public void register(UserSession user) {
        usersByName.put(user.getName(), user);
        usersBySessionId.put(user.getSession().getId(), user);
    }

    public UserSession getByName(String name) { return usersByName.get(name); }

    public UserSession getBySession(WebSocketSession session) { return usersBySessionId.get(session.getId()); }

    public boolean exists(String name) { return usersByName.containsKey(name); } // TODO : 이미 방에 참가한 사용자이면 참가를 제한 하는 로직을 위해 미리 구현해둠

    public UserSession removeBySession(WebSocketSession session) {
        final UserSession userSession = getBySession(session);
        usersByName.remove(userSession.getName());
        usersBySessionId.remove(session.getId());
        return userSession;
    }
}
