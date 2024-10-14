package org.atg.realwearappbackend.room;

public class RoomManager {
    private final KurentoClient kurento;

    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();

    public RoomManager(KurentoClient kurento) {
        this.kurento = kurento;
    }
}
