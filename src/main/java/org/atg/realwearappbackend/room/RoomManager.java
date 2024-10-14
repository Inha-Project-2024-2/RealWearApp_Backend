package org.atg.realwearappbackend.room;

import lombok.extern.slf4j.Slf4j;
import org.kurento.client.KurentoClient;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RoomManager {
    private final KurentoClient kurento;

    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();

    public RoomManager(KurentoClient kurento) {
        this.kurento = kurento;
    }

    public Room getRoom(String roomName){
        log.debug("{} 라는 방이 있는지 탐색 중", roomName);
        Room room = rooms.get(roomName);
        if(room == null) {
            log.debug("{} 방이 존재하지 않기 때문에 생성함", roomName);
            room = new Room(roomName, kurento.createMediaPipeline());
            rooms.put(roomName, room);
            log.debug("{} 방을 생성 했습니다", roomName);
            return room;
        }

        log.debug("{} 방을 발견 했습니다.", roomName);
        return room;
    }

    public void removeRoom(Room room){
        this.rooms.remove(room.getRoomName());
        room.close();
        log.info("{} 방을 삭제했습니다", room.getRoomName());
    }
}
