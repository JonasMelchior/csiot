package com.cibicom.views.auth;

import com.cibicom.data.internal.Api;
import com.cibicom.views.application.ApplicationsView;
import com.cibicom.views.components.notification.UnauthenticatedNotification;
import com.google.protobuf.Empty;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.VaadinSession;
import io.chirpstack.api.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;

@PageTitle("Cibicom IoT | Login")
@Route(value = "login")
@RouteAlias(value = "")
public class LoginView extends VerticalLayout {
    ManagedChannel channel = ManagedChannelBuilder
            .forAddress(Api.getHostName(), Api.getPort())
            .usePlaintext()
            .build();

    TextField emailField = new TextField("Email");
    PasswordField passwordField = new PasswordField("Password");
    Button loginButton = new Button("Login");

    public LoginView() {
        VerticalLayout layout = new VerticalLayout(
                emailField,
                passwordField,
                loginButton
        );

        layout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);

        loginButton.addClickListener( click -> {
            InternalServiceGrpc.InternalServiceBlockingStub internalServiceBlockingStub = InternalServiceGrpc
                    .newBlockingStub(channel);

            LoginRequest loginRequest = LoginRequest.newBuilder()
                    .setEmail(emailField.getValue())
                    .setPassword(passwordField.getValue())
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

        });

        add(layout);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
    }

}
