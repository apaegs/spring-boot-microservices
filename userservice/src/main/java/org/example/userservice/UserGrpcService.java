package org.example.userservice;

import io.grpc.stub.StreamObserver;
import org.example.userservice.grpc.GetUserByUsernameRequest;
import org.example.userservice.grpc.GetUserRequest;
import org.example.userservice.grpc.UserResponse;
import org.example.userservice.grpc.UserServiceGrpc;
import org.springframework.grpc.server.service.GrpcService;

/**
 * gRPC service exposing user data for internal services.
 * Used by Message Service to fetch user information without going through REST.
 */
@GrpcService
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final UserRepository userRepository;

    public UserGrpcService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Fetches a user by ID.
     * Returns NOT_FOUND if the user does not exist.
     *
     * @param request          contains the user ID to look up
     * @param responseObserver used to send back the response or an error
     */
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
                        () -> responseObserver.onError(
                                io.grpc.Status.NOT_FOUND
                                        .withDescription("User not found: " + request.getId())
                                        .asRuntimeException()
                        )
                );
    }

    /**
     * Fetches a user by username.
     * Returns NOT_FOUND if the user does not exist.
     *
     * @param request          contains the username to look up
     * @param responseObserver used to send back the response or an error
     */
    @Override
    public void getUserByUsername(GetUserByUsernameRequest request, StreamObserver<UserResponse> responseObserver) {
        userRepository.findByUsername(request.getUsername())
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
                        () -> responseObserver.onError(
                                io.grpc.Status.NOT_FOUND
                                        .withDescription("User not found: " + request.getUsername())
                                        .asRuntimeException()
                        )
                );
    }
}
