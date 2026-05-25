package org.example.userservice;

import io.grpc.stub.StreamObserver;
import org.example.userservice.grpc.GetUserRequest;
import org.example.userservice.grpc.UserResponse;
import org.example.userservice.grpc.UserServiceGrpc;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final UserRepository userRepository;

    public UserGrpcService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void getUser(GetUserRequest request, StreamObserver<UserResponse> responseObserver) {
        userRepository.findById(request.getId())
                .ifPresentOrElse(
                        user -> {
                            UserResponse response = UserResponse.newBuilder()
                                    .setId(user.getId())
                                    .setUsername(user.getUsername())
                                    .setEmail(user.getEmail())
                                    .build();
                            responseObserver.onNext(response);
                            responseObserver.onCompleted();
                        },
                        () -> {
                            responseObserver.onError(
                                    io.grpc.Status.NOT_FOUND
                                            .withDescription("User not found: " + request.getId())
                                            .asRuntimeException()
                            );
                        }
                );
    }
}
