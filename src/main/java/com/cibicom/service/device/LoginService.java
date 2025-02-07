package com.cibicom.service.device;

import com.cibicom.views.application.ApplicationsView;
import com.cibicom.views.components.notification.UnauthenticatedNotification;
import com.google.protobuf.Empty;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;
import io.chirpstack.api.InternalServiceGrpc;
import io.chirpstack.api.LoginRequest;
import io.chirpstack.api.LoginResponse;
import io.chirpstack.api.ProfileResponse;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;

public class LoginService {
    public static void login(ManagedChannel channel, String email, String password) {
        InternalServiceGrpc.InternalServiceBlockingStub internalServiceBlockingStub = InternalServiceGrpc
                .newBlockingStub(channel);

        LoginRequest loginRequest = LoginRequest.newBuilder()
                .setEmail(email)
                .setPassword(password)
                .build();


        Empty empty = Empty.newBuilder().build();

        try {
            LoginResponse loginResponse = internalServiceBlockingStub.login(loginRequest);
            Metadata.Key<String> key = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
            String token = "Bearer " + loginResponse.getJwt();
            Metadata metadata = new Metadata();
            metadata.put(key, token);
            VaadinSession.getCurrent().setAttribute(Metadata.class, metadata);

            // Retrieve current user profile
            internalServiceBlockingStub = internalServiceBlockingStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));

            ProfileResponse profileResponse = internalServiceBlockingStub.profile(empty);
            VaadinSession.getCurrent().setAttribute(ProfileResponse.class, profileResponse);

            UI.getCurrent().navigate(ApplicationsView.class);
        } catch (StatusRuntimeException e) {
            e.printStackTrace();
            new UnauthenticatedNotification().open();
        }
    }
}
