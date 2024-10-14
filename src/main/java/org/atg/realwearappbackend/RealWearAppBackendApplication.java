package org.atg.realwearappbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

@SpringBootApplication
@EnableWebSocket
public class RealWearAppBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(RealWearAppBackendApplication.class, args);
    }

}
