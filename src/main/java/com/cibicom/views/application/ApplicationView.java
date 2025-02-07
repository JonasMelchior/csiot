package com.cibicom.views.application;

import com.cibicom.data.internal.Api;
import com.cibicom.data.internal.Url;
import com.cibicom.views.components.device.DeviceGrid;
import com.cibicom.views.components.notification.InternalErrorNotification;
import com.cibicom.views.components.notification.SuccessNotification;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinSession;
import io.chirpstack.api.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;

public class ApplicationView extends VerticalLayout {

    Tabs tabs;

    private final Tab[] applicationTabs = {
            new Tab(new Text("Devices"), new Icon(VaadinIcon.THIN_SQUARE)),
            new Tab(new Text("Integration"), new Icon(VaadinIcon.ARROW_RIGHT)),
            new Tab(new Text("Settings"), new Icon(VaadinIcon.TOOLS))
    };
    TextField applicationNameField = new TextField("Name");
    TextArea applicationDescriptionField = new TextArea("Description");
    Button editApplicationDetailsButton = new Button("Edit", new Icon(VaadinIcon.EDIT));
    Button saveApplicationDetailsButton = new Button("Save");
    Button cancelEditApplicationDetailsButton = new Button("Cancel");
    ApplicationServiceGrpc.ApplicationServiceBlockingStub applicationServiceBlockingStub;
    DeviceServiceGrpc.DeviceServiceBlockingStub deviceServiceBlockingStub;

    VerticalLayout devicesLayout;
    VerticalLayout appIntegrationLayout;
    VerticalLayout appSettingsLayout;
    String applicationUrlPrefix = Url.getUrlPrefix() + "/applications/";


    public ApplicationView(Application application, ManagedChannel channel) {
        tabs = new Tabs(applicationTabs);
        this.applicationServiceBlockingStub = ApplicationServiceGrpc
                .newBlockingStub(channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(VaadinSession.getCurrent().getAttribute(Metadata.class)));
        this.deviceServiceBlockingStub = DeviceServiceGrpc
                .newBlockingStub(channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(VaadinSession.getCurrent().getAttribute(Metadata.class)));

        setDevicesLayout(application);
        setAppSettingsLayout(application);
        setAppIntegrationLayout(application);

        tabs.addSelectedChangeListener( change -> {
            setView(change.getSelectedTab(), change.getPreviousTab());
        });

        add(
                tabs,
                devicesLayout
        );
        setHeightFull();
    }

    private void setAppIntegrationLayout(Application application) {
        appIntegrationLayout = new VerticalLayout(new Text("Integrations ..."));
    }

    private void setView(Tab selectedTab, Tab oldTab) {
        if (oldTab.equals(applicationTabs[0])) {
            remove(devicesLayout);
        }
        else if (oldTab.equals(applicationTabs[1])) {
            remove(appIntegrationLayout);
        }
        else if (oldTab.equals(applicationTabs[2])) {
            remove(appSettingsLayout);
        }

        if (selectedTab.equals(applicationTabs[0])) {
            add(devicesLayout);
        }
        else if (selectedTab.equals(applicationTabs[1])) {
            add(appIntegrationLayout);
        }
        else if (selectedTab.equals(applicationTabs[2])) {
            add(appSettingsLayout);
        }
    }

    private void setDevicesLayout(Application application) {

        DeviceGrid deviceGrid = new DeviceGrid(application.getId());
        ListDevicesRequest listDevicesRequest = ListDevicesRequest
                .newBuilder()
                .setApplicationId(application.getId())
                .setLimit(10)
                .build();

        ListDevicesResponse listDevicesResponse = deviceServiceBlockingStub.list(listDevicesRequest);
        deviceGrid.setItems(listDevicesResponse.getResultList());

        Button addDeviceButton = new Button("Add Device", new Icon(VaadinIcon.PLUS));
        addDeviceButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        addDeviceButton.addClickListener( click -> {
            UI.getCurrent().navigate(applicationUrlPrefix + application.getId() + "/devices/create");
        });

        devicesLayout = new VerticalLayout(addDeviceButton, deviceGrid);
        devicesLayout.setHeightFull();
    }


    private void setAppSettingsLayout(Application application) {
        applicationNameField.setWidth("300px");
        applicationNameField.setValue(application.getName());
        applicationDescriptionField.setWidth("500px");
        applicationDescriptionField.setValue(application.getDescription());
        applicationNameField.setReadOnly(true);
        applicationDescriptionField.setReadOnly(true);

        saveApplicationDetailsButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        cancelEditApplicationDetailsButton.addThemeVariants(ButtonVariant.LUMO_ERROR);

        HorizontalLayout applicationDetailsButtonLayout = new HorizontalLayout(editApplicationDetailsButton);

        editApplicationDetailsButton.addClickListener( click -> {
            applicationDetailsButtonLayout.removeAll();
            applicationDetailsButtonLayout.add(saveApplicationDetailsButton, cancelEditApplicationDetailsButton);

            applicationNameField.setReadOnly(false);
            applicationDescriptionField.setReadOnly(false);
        });

        saveApplicationDetailsButton.addClickListener( click -> {
            try {
                UpdateApplicationRequest updateApplicationRequest = UpdateApplicationRequest.newBuilder()
                        .setApplication(
                                application.toBuilder()
                                        .setName(applicationNameField.getValue())
                                        .setDescription(applicationDescriptionField.getValue())
                        )
                        .build();
                applicationServiceBlockingStub.update(updateApplicationRequest);
                new SuccessNotification("Application " + application.getId() + " Saved successfully").open();
            } catch (Exception e){
                e.printStackTrace();
                new InternalErrorNotification().open();
                applicationNameField.setValue(application.getName());
                applicationDescriptionField.setValue(application.getDescription());
            }


            applicationDetailsButtonLayout.removeAll();
            applicationDetailsButtonLayout.add(editApplicationDetailsButton);

            applicationNameField.setReadOnly(true);
            applicationDescriptionField.setReadOnly(true);
        });

        cancelEditApplicationDetailsButton.addClickListener( click -> {
            applicationDetailsButtonLayout.removeAll();
            applicationDetailsButtonLayout.add(editApplicationDetailsButton);

            applicationNameField.setValue(application.getName());
            applicationDescriptionField.setValue(application.getDescription());

            applicationNameField.setReadOnly(true);
            applicationDescriptionField.setReadOnly(true);
        });

        VerticalLayout applicationDetailsLayout = new VerticalLayout(
                applicationNameField,
                applicationDescriptionField,
                applicationDetailsButtonLayout
        );
        applicationDetailsLayout.setWidthFull();
        applicationDetailsLayout.setDefaultHorizontalComponentAlignment(Alignment.BASELINE);

        appSettingsLayout = new VerticalLayout(
                applicationDetailsLayout
        );
    }
}
