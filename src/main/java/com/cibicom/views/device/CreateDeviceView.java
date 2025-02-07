package com.cibicom.views.device;

import com.cibicom.data.internal.Url;
import com.cibicom.views.components.notification.ErrorNotification;
import com.cibicom.views.components.notification.InternalErrorNotification;
import com.cibicom.views.components.notification.SuccessNotification;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinSession;
import io.chirpstack.api.*;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import org.atmosphere.interceptor.AtmosphereResourceStateRecovery;

public class CreateDeviceView extends FormLayout {
    String appId;
    ManagedChannel channel;
    DeviceServiceGrpc.DeviceServiceBlockingStub deviceStub;
    DeviceProfileServiceGrpc.DeviceProfileServiceBlockingStub deviceProfileStub;
    TextField nameField = new TextField("Name");
    TextArea descriptionField = new TextArea("Description");
    TextField deviceEuiField = new TextField("Device EUI");
    TextField joinEuiField = new TextField("Join EUI");
    ComboBox<DeviceProfileListItem> deviceProfileComboBox = new ComboBox<>("Device Profile");
    Checkbox disableDeviceCheckBox = new Checkbox("Disable Device");
    Checkbox frameCounterValidationCheckBox = new Checkbox("Disable frame-counter validation");
    Button createDeviceButton = new Button("Create Device");

    Device.Builder deviceBuilder = Device.newBuilder();
    VerticalLayout deviceLayout;

    public CreateDeviceView(ManagedChannel channel, String appId) {
        this.channel = channel;
        this.appId = appId;

        setStubs();
        setDeviceLayout();

        add(deviceLayout);
    }

    private void setDeviceLayout() {
        FormLayout deviceFormLayout = new FormLayout();

        nameField.setRequired(true);
        deviceProfileComboBox.setRequired(true);
        deviceEuiField.setRequired(true);
        disableDeviceCheckBox.setValue(false);
        frameCounterValidationCheckBox.setValue(false);
        deviceEuiField.setAllowedCharPattern("[0-9A-Fa-f]");
        joinEuiField.setAllowedCharPattern("[0-9A-Fa-f]");
        deviceEuiField.setMaxLength(16);
        joinEuiField.setMaxLength(16);
        createDeviceButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        ListDeviceProfilesRequest listDeviceProfilesRequest = ListDeviceProfilesRequest.newBuilder()
                        .setTenantId(VaadinSession.getCurrent().getAttribute(ProfileResponse.class).getTenantsList().get(0).getTenantId())
                                .setLimit(100)
                                        .build();
        deviceProfileComboBox.setItems(deviceProfileStub.list(listDeviceProfilesRequest).getResultList());
        deviceProfileComboBox.setItemLabelGenerator(DeviceProfileListItem::getName);

        deviceFormLayout.add(nameField, 2);
        deviceFormLayout.add(descriptionField, 2);
        deviceFormLayout.add(deviceProfileComboBox, 2);
        deviceFormLayout.add(deviceEuiField, joinEuiField);
        deviceFormLayout.add(disableDeviceCheckBox, frameCounterValidationCheckBox);
        deviceFormLayout.add(createDeviceButton, 2);

        createDeviceButton.addClickListener( click -> {
            if (nameField.isEmpty() || deviceProfileComboBox.isEmpty() || deviceEuiField.isEmpty()) {
                new ErrorNotification("You must fill out all required fields !").open();
            }
            else if (deviceEuiField.getValue().length() != 16) {
                new ErrorNotification("Device EUI must be of length 16").open();
            }
            else if (!joinEuiField.isEmpty() && joinEuiField.getValue().length() != 16) {
                new ErrorNotification("If specifying JoinEUI, it must be of length 16").open();
            }
            else {
                deviceBuilder
                        .setApplicationId(appId)
                        .setName(nameField.getValue())
                        .setDeviceProfileId(deviceProfileComboBox.getValue().getId())
                        .setDevEui(deviceEuiField.getValue())
                        .setJoinEui(joinEuiField.getValue())
                        .setIsDisabled(disableDeviceCheckBox.getValue())
                        .setSkipFcntCheck(frameCounterValidationCheckBox.getValue());

                try {
                    CreateDeviceRequest createDeviceRequest = CreateDeviceRequest
                            .newBuilder()
                            .setDevice(deviceBuilder)
                            .build();
                    deviceStub.create(createDeviceRequest);
                    new SuccessNotification("Device Created Successfully").open();
                    UI.getCurrent().navigate(Url.getUrlPrefix() + "/applications/" + this.appId);
                } catch (Exception e) {
                    e.printStackTrace();
                    new InternalErrorNotification().open();
                }
            }
        });

        deviceLayout = new VerticalLayout(deviceFormLayout);
    }

    private void setStubs() {
        deviceStub = DeviceServiceGrpc
                .newBlockingStub(channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(VaadinSession.getCurrent().getAttribute(Metadata.class)));
        deviceProfileStub = DeviceProfileServiceGrpc
                .newBlockingStub(channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(VaadinSession.getCurrent().getAttribute(Metadata.class)));
    }
}
