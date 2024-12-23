package org.atg.realwearappbackend;

import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class RealWearAppBackendApplication {
    @Autowired
    private SocketIOServer socketIOServer;
    public static void main(String[] args) {
        SpringApplication.run(RealWearAppBackendApplication.class, args);
    }

    @PostConstruct
    private void startSocketIOServer() {
        socketIOServer.start();
    }

    @PreDestroy
    private void stopSocketIOServer() {
        socketIOServer.stop();
    }
}
