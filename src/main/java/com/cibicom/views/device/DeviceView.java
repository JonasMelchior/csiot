package com.cibicom.views.device;

import com.cibicom.views.components.log.LogItemBody;
import com.cibicom.views.components.misc.EditControls;
import com.cibicom.views.components.notification.ErrorNotification;
import com.cibicom.views.components.notification.InternalErrorNotification;
import com.cibicom.views.components.notification.SuccessNotification;
import com.cibicom.views.components.stats.RxChartLayout;
import com.cibicom.views.components.stats.charts.RxChart;
import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import com.sun.jna.platform.win32.Sspi;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinSession;
import io.chirpstack.api.*;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class DeviceView extends VerticalLayout {
    ManagedChannel channel;
    Device device;
    Tabs tabs;
    DeviceProfile deviceProfile;
    InternalServiceGrpc.InternalServiceStub internalStub;
    DeviceProfileServiceGrpc.DeviceProfileServiceBlockingStub deviceProfileStub;
    DeviceServiceGrpc.DeviceServiceBlockingStub deviceStub;


    private final Tab[] deviceTabs = {
            new Tab(new Text("Overview")),
            new Tab(new Text("Live Data")),
            new Tab("Stats"),
            new Tab(new Text("Keys"), new Icon(VaadinIcon.KEY)),
            new Tab(new Text("Settings"), new Icon(VaadinIcon.SCREWDRIVER))
    };

    VerticalLayout overviewLayoutWrapper;
    VerticalLayout liveDataLayout;
    VerticalLayout statsLayout;
    VerticalLayout keysLayout;
    VerticalLayout settingsLayout;
    LocalDateTime enterTime;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");



    public DeviceView(ManagedChannel channel, Device device) {
        this.channel = channel;
        this.device = device;
        setStubs();
        this.enterTime = LocalDateTime.now();

        tabs = new Tabs(deviceTabs);
        tabs.addSelectedChangeListener( change -> {
            setView(change.getSelectedTab(), change.getPreviousTab());
        });

        setOverviewLayout();
        setLiveDataLayout();
        setStatsLayout();
        setKeysLayout();
        setSettingsLayout();

        add(tabs, overviewLayoutWrapper);
        setSizeFull();
    }


    private void setSettingsLayout() {
        settingsLayout = new VerticalLayout(new Text("Settings Layout"));
    }


    GetDeviceKeysResponse getDeviceKeysResponse;
    GetDeviceActivationResponse getDeviceActivationResponse;
    private void setKeysLayout() {
        GetDeviceKeysRequest getDeviceKeysRequest = GetDeviceKeysRequest.newBuilder()
                .setDevEui(device.getDevEui())
                .build();
        GetDeviceActivationRequest getDeviceActivationRequest = GetDeviceActivationRequest.newBuilder()
                .setDevEui(device.getDevEui())
                .build();

        try {
            getDeviceKeysResponse = deviceStub.getKeys(getDeviceKeysRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            getDeviceActivationResponse = deviceStub.getActivation(getDeviceActivationRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }

        TextField appKeyField = new TextField("Application Key");
        appKeyField.setAllowedCharPattern("[0-9A-Fa-f]");
        appKeyField.setMaxLength(32);
        TextField nwkKeyField = new TextField("Network Key");
        nwkKeyField.setAllowedCharPattern("[0-9A-Fa-f]");
        nwkKeyField.setMaxLength(32);
        appKeyField.setReadOnly(true);
        nwkKeyField.setReadOnly(true);

        VerticalLayout rootKeysLayout = new VerticalLayout(new H4("Root Key(s)"));

        if (this.deviceProfile.getMacVersion().equals(MacVersion.LORAWAN_1_1_0)) {
            if (getDeviceKeysResponse != null) {
                appKeyField.setValue(getDeviceKeysResponse.getDeviceKeys().getAppKey());
                nwkKeyField.setValue(getDeviceKeysResponse.getDeviceKeys().getNwkKey());
            }
            rootKeysLayout.add(new FormLayout(appKeyField, nwkKeyField));
        }
        else {
            if (getDeviceKeysResponse != null) {
                appKeyField.setValue(getDeviceKeysResponse.getDeviceKeys().getNwkKey());
            }
            rootKeysLayout.add(new FormLayout(appKeyField));
        }
        if (deviceProfile.getSupportsOtaa()) {
            EditControls editControls = new EditControls();
            rootKeysLayout.add(editControls);
            editControls.addEditListener( edit -> {
                appKeyField.setReadOnly(false);
                nwkKeyField.setReadOnly(false);
            });

            editControls.addCancelListener( cancel -> {
                if (deviceProfile.getMacVersion().equals(MacVersion.LORAWAN_1_1_0)) {
                    appKeyField.setValue(getDeviceKeysResponse.getDeviceKeys().getAppKey());
                    nwkKeyField.setValue(getDeviceKeysResponse.getDeviceKeys().getNwkKey());
                }
                else {
                    appKeyField.setValue(getDeviceKeysResponse.getDeviceKeys().getNwkKey());
                }
                appKeyField.setReadOnly(true);
                nwkKeyField.setReadOnly(true);
            });

            editControls.addSaveListener( save -> {
                boolean isValid = false;
                DeviceKeys.Builder deviceKeysBuilder = DeviceKeys.newBuilder().setDevEui(device.getDevEui());
                if (deviceProfile.getMacVersion().equals(MacVersion.LORAWAN_1_1_0)) {
                    if (appKeyField.getValue().length() != 32 || nwkKeyField.getValue().length() != 32) {
                        new ErrorNotification("Keys must be of length 32 (16 bytes)").open();
                    }
                    else {
                        isValid = true;
                        deviceKeysBuilder
                                .setAppKey(appKeyField.getValue())
                                .setNwkKey(nwkKeyField.getValue());
                    }

                }
                else {
                    if (appKeyField.getValue().length() != 32) {
                        new ErrorNotification("Key must be of length 32 (16 bytes)").open();
                    }
                    else {
                        isValid = true;
                        deviceKeysBuilder.setNwkKey(appKeyField.getValue());
                    }
                }

                if ((getDeviceKeysResponse == null || !getDeviceKeysResponse.hasDeviceKeys()) && isValid) {
                    CreateDeviceKeysRequest createDeviceKeysRequest = CreateDeviceKeysRequest.newBuilder()
                            .setDeviceKeys(deviceKeysBuilder)
                            .build();

                    try {
                        deviceStub.createKeys(createDeviceKeysRequest);
                        getDeviceKeysResponse = deviceStub.getKeys(getDeviceKeysRequest);
                        new SuccessNotification("Device Root Key(s) successfully saved").open();
                    } catch (Exception e) {
                        e.printStackTrace();
                        new InternalErrorNotification().open();
                    }
                }
                else if (isValid && Objects.requireNonNull(getDeviceKeysResponse).hasDeviceKeys()){
                    UpdateDeviceKeysRequest createDeviceKeysRequest = UpdateDeviceKeysRequest.newBuilder()
                            .setDeviceKeys(deviceKeysBuilder)
                            .build();

                    try {
                        deviceStub.updateKeys(createDeviceKeysRequest);
                        new SuccessNotification("Device Root Key(s) successfully saved").open();
                    } catch (Exception e) {
                        e.printStackTrace();
                        new InternalErrorNotification().open();
                    }
                }
                appKeyField.setReadOnly(true);
                nwkKeyField.setReadOnly(true);
            });
        }


        // LoRaWAN 1.1
        TextField devAddressField= new TextField("Device Address");
        TextField nwkSEncKeyField = new TextField("Network Session Encryption Key");
        TextField sNwkSIKeyField = new TextField("Serving Network Session Integrity Key");
        TextField fNwkSIKeyField = new TextField("Forwarding Network Session Integrity Key");
        TextField appSKeyField = new TextField("Application Session Key");
        IntegerField uplinkFCntField = new IntegerField("Uplink Frame-counter");
        IntegerField downLinkFCntNwkField = new IntegerField("Downlink Frame-counter (Network)");
        IntegerField downLinkFCntAppField = new IntegerField("Downlink Frame-counter (Application)");
        devAddressField.setRequired(true);
        nwkSEncKeyField.setRequired(true);
        sNwkSIKeyField.setRequired(true);
        fNwkSIKeyField.setRequired(true);
        appSKeyField.setRequired(true);
        uplinkFCntField.setRequired(true);
        downLinkFCntNwkField.setRequired(true);
        downLinkFCntAppField.setRequired(true);
        devAddressField.setAllowedCharPattern("[0-9A-Fa-f]");
        devAddressField.setMaxLength(8);
        nwkSEncKeyField.setAllowedCharPattern("[0-9A-Fa-f]");
        nwkSEncKeyField.setMaxLength(32);
        sNwkSIKeyField.setAllowedCharPattern("[0-9A-Fa-f]");
        sNwkSIKeyField.setMaxLength(32);
        fNwkSIKeyField.setAllowedCharPattern("[0-9A-Fa-f]");
        fNwkSIKeyField.setMaxLength(32);
        appSKeyField.setAllowedCharPattern("[0-9A-Fa-f]");
        appSKeyField.setMaxLength(32);

        // LoRaWAN 1.0.x
        TextField nwkSKeyField = new TextField("Network Session Key");
        IntegerField downLinkFCntField = new IntegerField("Downlink Framecounter");
        nwkSKeyField.setReadOnly(true);
        downLinkFCntField.setReadOnly(true);
        nwkSKeyField.setAllowedCharPattern("[0-9A-Fa-f]");
        nwkSKeyField.setMaxLength(32);

        if (getDeviceActivationResponse != null) {
            devAddressField.setValue(getDeviceActivationResponse.getDeviceActivation().getDevAddr());
            appSKeyField.setValue(getDeviceActivationResponse.getDeviceActivation().getAppSKey());
            nwkSEncKeyField.setValue(getDeviceActivationResponse.getDeviceActivation().getNwkSEncKey());
            sNwkSIKeyField.setValue(getDeviceActivationResponse.getDeviceActivation().getSNwkSIntKey());
            fNwkSIKeyField.setValue(getDeviceActivationResponse.getDeviceActivation().getFNwkSIntKey());
            nwkSKeyField.setValue(getDeviceActivationResponse.getDeviceActivation().getNwkSEncKey());
            uplinkFCntField.setValue(getDeviceActivationResponse.getDeviceActivation().getFCntUp());
            downLinkFCntField.setValue(getDeviceActivationResponse.getDeviceActivation().getNFCntDown());
            downLinkFCntNwkField.setValue(getDeviceActivationResponse.getDeviceActivation().getNFCntDown());
            downLinkFCntAppField.setValue(getDeviceActivationResponse.getDeviceActivation().getAFCntDown());
        }


        VerticalLayout sessionDetailsLayout = new VerticalLayout();

        if (!deviceProfile.getSupportsOtaa()) {
            String abpNoteString = "<div>Device root keys are not applicable for ABP devices<br>and session keys along with framecounter must be specified</div>"; // wrapping <div> tags are required here
            Html abpNoteText = new Html(abpNoteString);
            abpNoteText.getStyle().set("color", "gray");
            sessionDetailsLayout.add(abpNoteText);
        }

        sessionDetailsLayout.add(new H4("Session Details"), devAddressField);

        FormLayout sessionDetailsFormLayout = new FormLayout();
        if (deviceProfile.getMacVersion().equals(MacVersion.LORAWAN_1_1_0)) {
            sessionDetailsFormLayout.add(appSKeyField, nwkSEncKeyField, sNwkSIKeyField, fNwkSIKeyField);
            sessionDetailsLayout.add(
                    sessionDetailsFormLayout,
                    new HorizontalLayout(uplinkFCntField, downLinkFCntNwkField, downLinkFCntAppField)
            );
        }
        else {
            sessionDetailsFormLayout.add(appSKeyField, nwkSKeyField);
            sessionDetailsLayout.add(
                    sessionDetailsFormLayout,
                    new HorizontalLayout(uplinkFCntField, downLinkFCntField)
            );
        }

        List<AbstractField<?, ?>> sessionFields = List.of(
                devAddressField,
                nwkSEncKeyField,
                sNwkSIKeyField,
                fNwkSIKeyField,
                appSKeyField,
                uplinkFCntField,
                downLinkFCntNwkField,
                downLinkFCntAppField,
                nwkSKeyField,
                downLinkFCntField
        );

        setFieldsReadOnly(sessionFields);

        if (!deviceProfile.getSupportsOtaa()) {
            EditControls editControls = new EditControls();
            editControls.addEditListener( edit -> {
                unSetFieldsReadOnly(sessionFields);
            });

            editControls.addCancelListener( cancel -> {
                setFieldsReadOnly(sessionFields);
            });

            editControls.addSaveListener( save -> {
                boolean isValid = false;
                DeviceActivation.Builder deviceActivationBuilder = DeviceActivation.newBuilder().setDevEui(device.getDevEui());
                if (deviceProfile.getMacVersion().equals(MacVersion.LORAWAN_1_1_0)) {
                    if (devAddressField.isEmpty() ||
                            nwkSEncKeyField.isEmpty() ||
                            sNwkSIKeyField.isEmpty() ||
                            fNwkSIKeyField.isEmpty() ||
                            appSKeyField.isEmpty() ||
                            uplinkFCntField.isEmpty() || downLinkFCntNwkField.isEmpty() || downLinkFCntAppField.isEmpty()) {
                        new ErrorNotification("You must fill out all required sessionFields").open();
                        setFieldsReadOnly(sessionFields);
                    }
                    else if (devAddressField.getValue().length() != 8 ||
                        nwkSEncKeyField.getValue().length() != 32 ||
                        sNwkSIKeyField.getValue().length() != 32 ||
                        fNwkSIKeyField.getValue().length() != 32 ||
                        appSKeyField.getValue().length() != 32 ||
                        uplinkFCntField.isEmpty() || downLinkFCntNwkField.isEmpty() || downLinkFCntAppField.isEmpty()) {
                        new ErrorNotification("Device Address must be of length 8, and all keys must be of length 32").open();
                        setFieldsReadOnly(sessionFields);
                    }
                    else {
                        deviceActivationBuilder
                                .setDevAddr(devAddressField.getValue())
                                .setNwkSEncKey(nwkSEncKeyField.getValue())
                                .setFNwkSIntKey(fNwkSIKeyField.getValue())
                                .setSNwkSIntKey(sNwkSIKeyField.getValue())
                                .setAppSKey(appSKeyField.getValue())
                                .setFCntUp(uplinkFCntField.getValue())
                                .setNFCntDown(downLinkFCntNwkField.getValue())
                                .setAFCntDown(downLinkFCntAppField.getValue());

                        try {
                            ActivateDeviceRequest activateDeviceRequest = ActivateDeviceRequest.newBuilder()
                                    .setDeviceActivation(deviceActivationBuilder)
                                    .build();

                            deviceStub.activate(activateDeviceRequest);
                            setFieldsReadOnly(sessionFields);

                            new SuccessNotification("Device activated successfully").open();
                        } catch (Exception e) {
                            e.printStackTrace();
                            new InternalErrorNotification().open();
                        }
                    }
                }
                else {
                    if (devAddressField.isEmpty() ||
                            nwkSKeyField.isEmpty() ||
                            appSKeyField.isEmpty() ||
                            uplinkFCntField.isEmpty() || downLinkFCntField.isEmpty()) {
                        new ErrorNotification("You must fill out all required sessionFields").open();
                        setFieldsReadOnly(sessionFields);
                    }
                    else if (devAddressField.getValue().length() != 8 ||
                            nwkSKeyField.getValue().length() != 32 ||
                            appSKeyField.getValue().length() != 32 ||
                            uplinkFCntField.isEmpty() || downLinkFCntField.isEmpty()) {
                        new ErrorNotification("Device Address must be of length 8, and all keys must be of length 32").open();
                        setFieldsReadOnly(sessionFields);
                    }
                    else {
                        deviceActivationBuilder
                                .setDevAddr(devAddressField.getValue())
                                .setAppSKey(appSKeyField.getValue())
                                .setNwkSEncKey(nwkSKeyField.getValue())
                                .setSNwkSIntKey(nwkSKeyField.getValue())
                                .setFNwkSIntKey(nwkSKeyField.getValue())
                                .setFCntUp(uplinkFCntField.getValue())
                                .setNFCntDown(downLinkFCntField.getValue());
                        try {
                            ActivateDeviceRequest activateDeviceRequest = ActivateDeviceRequest.newBuilder()
                                    .setDeviceActivation(deviceActivationBuilder)
                                    .build();

                            deviceStub.activate(activateDeviceRequest);
                            setFieldsReadOnly(sessionFields);
                            new SuccessNotification("Device activated successfully").open();
                        } catch (Exception e) {
                            setFieldsReadOnly(sessionFields);
                            e.printStackTrace();
                            new InternalErrorNotification().open();
                        }
                    }
                }
            });
            sessionDetailsLayout.add(editControls);
        }

        keysLayout = new VerticalLayout();

        if (this.deviceProfile.getSupportsOtaa()) {
            keysLayout.add(rootKeysLayout);
        }

        keysLayout.setMaxWidth("50%");
        keysLayout.add(sessionDetailsLayout);
    }

    public static void setFieldsReadOnly(Collection<? extends AbstractField<?, ?>> fields) {
        for (AbstractField<?, ?> field : fields) {
            field.setReadOnly(true);
        }
    }

    public static void unSetFieldsReadOnly(Collection<? extends AbstractField<?, ?>> fields) {
        for (AbstractField<?, ?> field : fields) {
            field.setReadOnly(false);
        }
    }

    private void setStatsLayout() {
        statsLayout = new VerticalLayout(new Text("Stats Layout"));
    }

    private void setLiveDataLayout() {
        RadioButtonGroup<String> liveDataTypeGroup = new RadioButtonGroup<>("Data types");
        liveDataTypeGroup.setItems("LoRaWAN Frames", "Events");

        Grid<LogItem> framesGrid = new Grid<>();
        Grid<LogItem> eventsGrid = new Grid<>();

        framesGrid.getStyle().set("border-color", "orange");
        framesGrid.addComponentColumn( deviceEvent -> {
            LocalDateTime deviceEventTime = Instant.ofEpochSecond(deviceEvent.getTime().getSeconds(), deviceEvent.getTime().getNanos())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            Span timeSpan = new Span(deviceEventTime.format(formatter));
            if (deviceEventTime.isAfter(enterTime)) {
                Icon icon = new Icon(VaadinIcon.CIRCLE);
                icon.setSize("5%");
                icon.setColor("#1E90FF");
                HorizontalLayout timeLayout = new HorizontalLayout(timeSpan, icon);
                timeLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
                return timeLayout;
            }
            return timeSpan;
        }).setHeader("Time");
        framesGrid.addColumn(LogItem::getDescription).setHeader("Message Type");
        framesGrid.addColumn( deviceEvent -> deviceEvent.getPropertiesMap().get("DevAddr")).setHeader("Device Address");
        framesGrid.addComponentColumn( deviceEvent -> {
            return new Button(new Icon(VaadinIcon.INFO), click -> {
                Dialog dialog = new Dialog(new LogItemBody(deviceEvent.getBody()));
                dialog.open();
            });
        }).setHeader("Inspect");

        eventsGrid.getStyle().set("border-color", "orange");
        eventsGrid.addComponentColumn( deviceEvent -> {
            LocalDateTime deviceEventTime = Instant.ofEpochSecond(deviceEvent.getTime().getSeconds(), deviceEvent.getTime().getNanos())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            Span timeSpan = new Span(deviceEventTime.format(formatter));
            if (deviceEventTime.isAfter(enterTime)) {
                Icon icon = new Icon(VaadinIcon.CIRCLE);
                icon.setSize("5%");
                icon.setColor("#1E90FF");
                HorizontalLayout timeLayout = new HorizontalLayout(timeSpan, icon);
                timeLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
                return timeLayout;
            }
            return timeSpan;
        }).setHeader("Time");
        eventsGrid.addColumn(LogItem::getDescription).setHeader("Type");
        eventsGrid.addColumn( deviceEvent -> deviceEvent.getPropertiesMap().get("Data")).setHeader("Data");
        eventsGrid.addColumn( deviceEvent -> deviceEvent.getPropertiesMap().get("DR")).setHeader("DR");
        eventsGrid.addColumn( deviceEvent -> deviceEvent.getPropertiesMap().get("FCnt")).setHeader("FCnt");
        eventsGrid.addColumn( deviceEvent -> deviceEvent.getPropertiesMap().get("FPort")).setHeader("FPort");
        eventsGrid.addComponentColumn( deviceEvent -> {
            return new Button(new Icon(VaadinIcon.INFO), click -> {
                Dialog dialog = new Dialog(new LogItemBody(deviceEvent.getBody()));
                dialog.open();
            });
        }).setHeader("Inspect");

        StreamDeviceFramesRequest streamDeviceFramesRequest = StreamDeviceFramesRequest.newBuilder()
                .setDevEui(device.getDevEui())
                .build();

        StreamDeviceEventsRequest streamDeviceEventsRequest = StreamDeviceEventsRequest.newBuilder()
                .setDevEui(device.getDevEui())
                .build();

        internalStub.streamDeviceFrames(streamDeviceFramesRequest, configureStreamObserver(framesGrid));
        internalStub.streamDeviceEvents(streamDeviceEventsRequest, configureStreamObserver(eventsGrid));


        liveDataTypeGroup.setValue("LoRaWAN Frames");

        liveDataTypeGroup.addValueChangeListener( change -> {
            if (change.getValue().equals("LoRaWAN Frames")) {
                liveDataLayout.remove(eventsGrid);
                liveDataLayout.add(framesGrid);
            }
            else {
                liveDataLayout.remove(framesGrid);
                liveDataLayout.add(eventsGrid);
            }
        });

        liveDataLayout = new VerticalLayout(
                liveDataTypeGroup,
                framesGrid
        );

        liveDataLayout.setHeightFull();
    }


    private void setOverviewLayout() {
        VerticalLayout deviceDetailsLayout = new VerticalLayout();


        TextField devEuiField = new TextField("Device EUI");
        TextField joinEuiField = new TextField("Join EUI");
        devEuiField.setReadOnly(true);
        joinEuiField.setReadOnly(true);
        devEuiField.setWidthFull();
        joinEuiField.setWidthFull();
        devEuiField.setValue(device.getDevEui());
        joinEuiField.setValue(device.getJoinEui());
        HorizontalLayout euiLayout = new HorizontalLayout(devEuiField, joinEuiField);
        VerticalLayout addressesLayout = new VerticalLayout(new H4("Addresses"), euiLayout);

        TextField regionField = new TextField("Region");
        regionField.setWidthFull();
        TextField loraWanVersion =  new TextField("LoRaWAN Version");
        loraWanVersion.setWidthFull();
        TextField expectedUplinkField = new TextField("Expected Uplink Interval [seconds]");
        expectedUplinkField.setWidthFull();
        regionField.setReadOnly(true);
        loraWanVersion.setReadOnly(true);
        expectedUplinkField.setReadOnly(true);
        GetDeviceProfileRequest getDeviceProfileRequest = GetDeviceProfileRequest
                .newBuilder()
                .setId(device.getDeviceProfileId())
                .build();
        GetDeviceProfileResponse getDeviceProfileResponse = deviceProfileStub.get(getDeviceProfileRequest);
        this.deviceProfile = getDeviceProfileResponse.getDeviceProfile();
        regionField.setValue(getDeviceProfileResponse.getDeviceProfile().getRegion().name());
        loraWanVersion.setValue(getDeviceProfileResponse.getDeviceProfile().getMacVersion().name());
        expectedUplinkField.setValue(String.valueOf(getDeviceProfileResponse.getDeviceProfile().getUplinkInterval()));
        String deviceProfileNoteString = "<div>Details below are device profile specific. <br>In order to change them, the device profile of the device has to be changed</div>"; // wrapping <div> tags are required here
        Html deviceProfileNoteText = new Html(deviceProfileNoteString);
        deviceProfileNoteText.getStyle().set("color", "gray");
        VerticalLayout deviceProfileLayout = new VerticalLayout(
                new H4("Device Profile"),
                deviceProfileNoteText,
                regionField,
                loraWanVersion,
                expectedUplinkField
        );
        deviceDetailsLayout.add(addressesLayout, deviceProfileLayout);

        Grid<LogItem> deviceEventGrid = new Grid<>();

        deviceEventGrid.getStyle().set("border-color", "orange");
        deviceEventGrid.addComponentColumn( deviceEvent -> {
            LocalDateTime deviceEventTime = Instant.ofEpochSecond(deviceEvent.getTime().getSeconds(), deviceEvent.getTime().getNanos())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            Span timeSpan = new Span(deviceEventTime.format(formatter));
            if (deviceEventTime.isAfter(enterTime)) {
                Icon icon = new Icon(VaadinIcon.CIRCLE);
                icon.setSize("5%");
                icon.setColor("#1E90FF");
                HorizontalLayout timeLayout = new HorizontalLayout(timeSpan, icon);
                timeLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
                return timeLayout;
            }
            return timeSpan;
        }).setHeader("Time");
        deviceEventGrid.addColumn(LogItem::getDescription).setHeader("Type");
        deviceEventGrid.addColumn( deviceEvent -> deviceEvent.getPropertiesMap().get("FCnt")).setHeader("FCnt");
        deviceEventGrid.addComponentColumn( deviceEvent -> {
            return new Button(new Icon(VaadinIcon.INFO), click -> {
                Dialog dialog = new Dialog(new LogItemBody(deviceEvent.getBody()));
                dialog.open();
            });
        }).setHeader("Inspect");

        StreamDeviceEventsRequest streamDeviceEventsRequest = StreamDeviceEventsRequest.newBuilder()
                .setDevEui(device.getDevEui())
                .build();

        internalStub.streamDeviceEvents(streamDeviceEventsRequest, configureStreamObserver(deviceEventGrid));

        VerticalLayout liveDataLayout = new VerticalLayout(
                new H4("Live Data"),
                deviceEventGrid
        );
        liveDataLayout.setMaxHeight("500px");

        FlexLayout overviewLayout = new FlexLayout();
        overviewLayout.setFlexDirection(FlexLayout.FlexDirection.ROW);
        overviewLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);

        RxChartLayout rxChartLayout = new RxChartLayout(channel, device.getDevEui(), true);

        overviewLayout.setFlexBasis("700px", deviceDetailsLayout);
        overviewLayout.add(deviceDetailsLayout);
        overviewLayout.setFlexBasis("700px", rxChartLayout);
        overviewLayout.add(rxChartLayout);
        overviewLayout.setFlexBasis("1000px", liveDataLayout);
        overviewLayout.add(liveDataLayout);

        overviewLayout.setSizeFull();

        overviewLayoutWrapper = new VerticalLayout(
                overviewLayout
        );

        overviewLayoutWrapper.setSizeFull();
    }

    private StreamObserver<LogItem> configureStreamObserver(Grid<LogItem> logItemGrid) {
        return new StreamObserver<>() {
            UI ui = UI.getCurrent();

            @Override
            public void onNext(LogItem logItem) {
                ui.access(() -> {
                    System.out.println(logItem);
                    LinkedList<LogItem> logItems = new LinkedList<>(logItemGrid.getListDataView().getItems().toList());
                    logItems.addFirst(logItem);
                    logItemGrid.setItems(logItems);
                });
            }

            @Override
            public void onError(Throwable throwable) {
                ui = UI.getCurrent();
            }

            @Override
            public void onCompleted() {
                System.out.println("Completed");
            }
        };
    }

    private void setView(Tab selectedTab, Tab oldTab) {
        if (oldTab.equals(deviceTabs[0])) {
            remove(overviewLayoutWrapper);
        }
        else if (oldTab.equals(deviceTabs[1])) {
            remove(liveDataLayout);
        }
        else if (oldTab.equals(deviceTabs[2])) {
            remove(statsLayout);
        }
        else if (oldTab.equals(deviceTabs[3])) {
            remove(keysLayout);
        }
        else if (oldTab.equals(deviceTabs[4])) {
            remove(settingsLayout);
        }

        if (selectedTab.equals(deviceTabs[0])) {
            add(overviewLayoutWrapper);
        }
        else if (selectedTab.equals(deviceTabs[1])) {
            add(liveDataLayout);
        }
        else if (selectedTab.equals(deviceTabs[2])) {
            add(statsLayout);
        }
        else if (selectedTab.equals(deviceTabs[3])) {
            add(keysLayout);
        }
        else if (selectedTab.equals(deviceTabs[4])) {
            add(settingsLayout);
        }
    }

    private void setStubs() {
        internalStub = InternalServiceGrpc
                .newStub(channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(VaadinSession.getCurrent().getAttribute(Metadata.class)));
        deviceProfileStub = DeviceProfileServiceGrpc
                .newBlockingStub(channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(VaadinSession.getCurrent().getAttribute(Metadata.class)));
        deviceStub = DeviceServiceGrpc
                .newBlockingStub(channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(VaadinSession.getCurrent().getAttribute(Metadata.class)));
    }
}
