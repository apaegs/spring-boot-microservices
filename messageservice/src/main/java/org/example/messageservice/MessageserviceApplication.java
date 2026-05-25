package org.example.messageservice;

import org.example.userservice.grpc.UserServiceGrpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.grpc.client.ImportGrpcClients;

@SpringBootApplication
@ImportGrpcClients(basePackages = "org.example.userservice.grpc")
public class MessageserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessageserviceApplication.class, args);
    }
}
