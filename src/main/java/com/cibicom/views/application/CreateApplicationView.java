package com.cibicom.views.application;

import com.cibicom.data.internal.Url;
import com.cibicom.views.components.notification.ErrorNotification;
import com.cibicom.views.components.notification.InternalErrorNotification;
import com.cibicom.views.components.notification.SuccessNotification;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import io.chirpstack.api.Application;
import io.chirpstack.api.ApplicationServiceGrpc;
import io.chirpstack.api.CreateApplicationRequest;
import io.chirpstack.api.CreateApplicationResponse;

public class CreateApplicationView extends VerticalLayout {

    TextField applicationNameField = new TextField("Name");
    TextArea applicationDescriptionField = new TextArea("Description");
    Button createApplicationButton = new Button("Create");
    ApplicationServiceGrpc.ApplicationServiceBlockingStub applicationServiceBlockingStub;

    public CreateApplicationView(ApplicationServiceGrpc.ApplicationServiceBlockingStub applicationServiceBlockingStub, String tenantId) {
        this.applicationServiceBlockingStub = applicationServiceBlockingStub;

        applicationNameField.setWidth("300px");
        applicationDescriptionField.setWidth("500px");

        applicationNameField.setRequired(true);

        createApplicationButton.addClickListener( click -> {
           if (applicationNameField.isEmpty()) {
               new ErrorNotification("You must fill out all required fields").open();
           }
           else {

               CreateApplicationRequest createApplicationRequest = CreateApplicationRequest
                       .newBuilder()
                       .setApplication(
                               Application
                                       .newBuilder()
                                       .setTenantId(tenantId)
                                       .setName(applicationNameField.getValue())
                                       .setDescription(applicationDescriptionField.getValue())
                       )
                       .build();

               try {
                   CreateApplicationResponse createApplicationResponse = applicationServiceBlockingStub.create(createApplicationRequest);
                   new SuccessNotification("Application created successfully").open();
                   String applicationUrlPrefix = Url.getUrlPrefix() +  "/applications/";
                   UI.getCurrent().navigate(applicationUrlPrefix + createApplicationResponse.getId());
               } catch (Exception e) {
                   e.printStackTrace();
                   new InternalErrorNotification().open();
               }
           }
        });
        createApplicationButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(applicationNameField, applicationDescriptionField, createApplicationButton);
    }
}
