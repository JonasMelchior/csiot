package com.cibicom;

import com.cibicom.data.internal.Api;
import com.cibicom.views.application.ApplicationsView;
import com.cibicom.views.components.notification.UnauthenticatedNotification;
import com.google.protobuf.Empty;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.internal.VaadinContextInitializer;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.Theme;
import io.chirpstack.api.InternalServiceGrpc;
import io.chirpstack.api.LoginRequest;
import io.chirpstack.api.LoginResponse;
import io.chirpstack.api.ProfileResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * The entry point of the Spring Boot application.
 *
 * Use the @PWA annotation make the application installable on phones, tablets
 * and some desktop browsers.
 *
 */
@SpringBootApplication
@Push
@EnableAsync
@Theme(value = "csiot")
public class Application implements AppShellConfigurator{

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
