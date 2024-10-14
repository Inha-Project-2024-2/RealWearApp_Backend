package org.atg.realwearappbackend.handler;

import org.springframework.web.socket.handler.TextWebSocketHandler;

public class GroupCallHandler extends TextWebSocketHandler {
    private static final Gson gson = new GsonBuilder().create();

    private final RoomManager roomManager;

    private final UserRegistry registry;


    public GroupCallHandler(RoomManager roomManager, UserRegistry registry) {
        this.roomManager = roomManager;
        this.registry = registry;
    }
}
