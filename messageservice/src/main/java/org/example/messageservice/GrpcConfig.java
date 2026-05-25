package org.example.messageservice;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.example.userservice.grpc.UserServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcConfig {

    @Bean
    public UserServiceGrpc.UserServiceBlockingStub userServiceStub() {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", 9091)
                .usePlaintext()
                .build();
        return UserServiceGrpc.newBlockingStub(channel);
    }
}
