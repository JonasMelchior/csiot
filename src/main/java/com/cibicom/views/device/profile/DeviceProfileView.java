package com.cibicom.views.device.profile;

import com.cibicom.data.internal.Url;
import com.cibicom.views.components.misc.EditControls;
import com.cibicom.views.components.notification.ErrorNotification;
import com.cibicom.views.components.notification.InternalErrorNotification;
import com.cibicom.views.components.notification.SuccessNotification;
import com.google.protobuf.Empty;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinSession;
import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.enums.AceTheme;
import io.chirpstack.api.*;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;

import java.util.Arrays;
import java.util.Optional;

public class DeviceProfileView extends VerticalLayout {
    ManagedChannel channel;
    Empty empty = Empty.newBuilder().build();


    Tabs tabs;

    private final Tab[] deviceProfileTabs = {
            new Tab(new Text("General")),
            new Tab(new Text("Class")),
            //new Tab(new Text("Relay")),
            new Tab("Codec")
    };

    VerticalLayout generalLayout;
    VerticalLayout classLayout;
    VerticalLayout relayLayout;
    VerticalLayout codecLayout;

    DeviceProfile.Builder deviceProfileBuilder = DeviceProfile.newBuilder();
    DeviceProfileServiceGrpc.DeviceProfileServiceBlockingStub deviceProfileStub;
    InternalServiceGrpc.InternalServiceBlockingStub interServiceStub;
    String deviceProfilesUrlPrefix = Url.getUrlPrefix() + "/device-profiles/";



    TextField nameField = new TextField("Name");
    TextArea descriptionField = new TextArea("Description");
    RadioButtonGroup<String> activationGroup = new RadioButtonGroup<>();
    IntegerField rx1DelayField = new IntegerField("RX1 delay");
    IntegerField rx1DataRateOffsetField = new IntegerField("RX1 data rate offset");
    IntegerField rx2DataRateField = new IntegerField("RX2 data rate");
    IntegerField rx2Frequency = new IntegerField("RX2 frequency [Hz]");
    ComboBox<Region> regionComboBox = new ComboBox<>("Region");
    ComboBox<MacVersion> macVersionComboBox = new ComboBox<>("LoRaWAN Version");
    ComboBox<RegParamsRevision> regionalParamsRevComboBox = new ComboBox<>("Regional Parameters Revision");
    ComboBox<AdrAlgorithmListItem> adrAlgorithmComboBox = new ComboBox<>("ADR Algorithm");
    IntegerField expectedUplinkIntervalField = new IntegerField("Expected Uplink Interval [seconds]");
    IntegerField deviceStatusReqsField = new IntegerField("Device status requests per day");
    Checkbox flushQueOnActivateCheckBox = new Checkbox("Flush Queue when device joins network");
    Checkbox allowRoamingCheckBox = new Checkbox("Allow Roaming");
    Checkbox supportsClassBCheckBox = new Checkbox("Supports Class-B");
    Checkbox supportsClassCCheckBox = new Checkbox("Supports Class-C");
    IntegerField confirmedDownLinkTimeoutBField = new IntegerField("Timeout for confirmed downlink message");
    ComboBox<Integer> pingSlotPeriodComboBox = new ComboBox<>("Period for ping-slot");
    IntegerField pingSlotDataRateField = new IntegerField("Ping-slot data rate");
    IntegerField pingSlotFrequencyField = new IntegerField("Ping-slot frequency [Hz]");
    IntegerField confirmedDownLinkTimeoutCField = new IntegerField("Timeout for confirmed downlink message");
    ComboBox<CodecRuntime> codecRuntimeComboBox = new ComboBox<>("Codec Runtime");
    AceEditor aceEditor = new AceEditor();
    Button createDeviceProfileButton = new Button("Create Device Profile", new Icon(VaadinIcon.PLUS));

    private boolean isEditing = false;
    // Only applicable when editing
    String id = null;

    public DeviceProfileView(ManagedChannel channel) {
        this.channel = channel;
        deviceProfileStub = DeviceProfileServiceGrpc
                .newBlockingStub(this.channel)
                        .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(VaadinSession.getCurrent().getAttribute(Metadata.class)));
        setStubs();

        tabs = new Tabs(deviceProfileTabs);

        setGeneralLayout();
        setClassLayout();
        setRelayLayout();
        setCodecLayout();
        setCreateDeviceProfileButton();

        tabs.addSelectedChangeListener( change -> {
           setView(change.getSelectedTab(), change.getPreviousTab());
        });

        add(tabs, generalLayout);
    }

    public DeviceProfileView(ManagedChannel channel, DeviceProfile deviceProfile) {
        this.channel = channel;
        deviceProfileStub = DeviceProfileServiceGrpc
                .newBlockingStub(this.channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(VaadinSession.getCurrent().getAttribute(Metadata.class)));
        setStubs();

        isEditing = true;
        this.id = deviceProfile.getId();

        tabs = new Tabs(deviceProfileTabs);

        setGeneralLayout();
        setClassLayout();
        setRelayLayout();
        setCodecLayout();
        setCreateDeviceProfileButton();
        initEdit(deviceProfile);

        tabs.addSelectedChangeListener( change -> {
            setView(change.getSelectedTab(), change.getPreviousTab());
        });

        add(tabs, generalLayout);
    }

    private void initEdit(DeviceProfile deviceProfile) {
        setAllFieldsReadOnly();
        initFields(deviceProfile);
    }

    private void initFields(DeviceProfile deviceProfile) {
        nameField.setValue(deviceProfile.getName());
        descriptionField.setValue(deviceProfile.getDescription());
        if (deviceProfile.getSupportsOtaa()) {
            activationGroup.setValue("OTAA");
        }
        else {
            activationGroup.setValue("ABP");
        }
        rx1DelayField.setValue(deviceProfile.getAbpRx1Delay());
        rx1DataRateOffsetField.setValue(deviceProfile.getAbpRx1DrOffset());
        rx2DataRateField.setValue(deviceProfile.getAbpRx2Dr());
        rx2Frequency.setValue(deviceProfile.getAbpRx2Freq());
        regionComboBox.setValue(deviceProfile.getRegion());
        macVersionComboBox.setValue(deviceProfile.getMacVersion());
        regionalParamsRevComboBox.setValue(deviceProfile.getRegParamsRevision());
        Optional<AdrAlgorithmListItem> adrAlgorithm = deviceProfileStub.listAdrAlgorithms(empty)
                .getResultList().stream()
                .filter( adrAlgorithmListItem -> adrAlgorithmListItem.getId().equals(deviceProfile.getAdrAlgorithmId()))
                .findFirst();
        adrAlgorithm.ifPresent(adrAlgorithmListItem -> adrAlgorithmComboBox.setValue(adrAlgorithmListItem));
        expectedUplinkIntervalField.setValue(deviceProfile.getUplinkInterval());
        deviceStatusReqsField.setValue(deviceProfile.getDeviceStatusReqInterval());
        flushQueOnActivateCheckBox.setValue(deviceProfile.getFlushQueueOnActivate());
        allowRoamingCheckBox.setValue(deviceProfile.getAllowRoaming());
        supportsClassBCheckBox.setValue(deviceProfile.getSupportsClassB());
        supportsClassCCheckBox.setValue(deviceProfile.getSupportsClassC());
        confirmedDownLinkTimeoutBField.setValue(deviceProfile.getClassBTimeout());
        pingSlotPeriodComboBox.setValue(deviceProfile.getClassBPingSlotNbK());
        pingSlotDataRateField.setValue(deviceProfile.getClassBPingSlotDr());
        pingSlotFrequencyField.setValue(deviceProfile.getClassBPingSlotFreq());
        confirmedDownLinkTimeoutCField.setValue(deviceProfile.getClassCTimeout());
        codecRuntimeComboBox.setValue(deviceProfile.getPayloadCodecRuntime());
        aceEditor.setValue(deviceProfile.getPayloadCodecScript());
    }


    private void setCreateDeviceProfileButton() {
        createDeviceProfileButton.addClickListener( click -> {
            setDeviceProfile(false);
        });
    }

    private void setDeviceProfile(boolean isEditing) {
        if (nameField.isEmpty() ||
                regionComboBox.isEmpty() ||
                macVersionComboBox.isEmpty() ||
                regionalParamsRevComboBox.isEmpty() ||
                adrAlgorithmComboBox.isEmpty() ||
                expectedUplinkIntervalField.isEmpty()
        ) {
            new ErrorNotification("You need to fill out all required fields").open();
        } else if (activationGroup.getValue().equals("ABP") &&
                (rx1DelayField.isEmpty() || rx1DataRateOffsetField.isEmpty() ||
                        rx2DataRateField.isEmpty() || rx2Frequency.isEmpty())
        ) {
            new ErrorNotification("With ABP selected, you need to specify RX1 and RX2 information").open();
        }
        else if (supportsClassBCheckBox.getValue() && (confirmedDownLinkTimeoutBField.isEmpty() ||
                pingSlotPeriodComboBox.isEmpty() || pingSlotDataRateField.isEmpty() ||
                pingSlotFrequencyField.isEmpty())) {
            new ErrorNotification("For Class B, you need to specify timeout for confirmed downlinks and ping slot configuration").open();
        }
        else if (supportsClassCCheckBox.getValue() && confirmedDownLinkTimeoutCField.isEmpty()) {
            new ErrorNotification("For Class C, you need to specify timeout for confirmed downlinks").open();
        }
        else {
            deviceProfileBuilder
                    .setTenantId(VaadinSession.getCurrent().getAttribute(ProfileResponse.class).getTenantsList().get(0).getTenantId())
                    .setName(nameField.getValue())
                    .setDescription(descriptionField.getValue())
                    .setRegion(regionComboBox.getValue())
                    .setMacVersion(macVersionComboBox.getValue())
                    .setRegParamsRevision(regionalParamsRevComboBox.getValue())
                    .setAdrAlgorithmId(adrAlgorithmComboBox.getValue().getId())
                    .setFlushQueueOnActivate(flushQueOnActivateCheckBox.getValue())
                    .setUplinkInterval(expectedUplinkIntervalField.getValue())
                    .setAllowRoaming(allowRoamingCheckBox.getValue());
            if (supportsClassBCheckBox.getValue()) {
                deviceProfileBuilder
                    .setSupportsClassB(supportsClassBCheckBox.getValue())
                    .setClassBTimeout(confirmedDownLinkTimeoutBField.getValue())
                    .setClassBPingSlotNbK(pingSlotPeriodComboBox.getValue())
                    .setClassBPingSlotDr(pingSlotDataRateField.getValue())
                    .setClassBPingSlotFreq(pingSlotFrequencyField.getValue());
            }
            if (supportsClassCCheckBox.getValue()) {
                deviceProfileBuilder
                    .setSupportsClassC(supportsClassCCheckBox.getValue())
                    .setClassCTimeout(confirmedDownLinkTimeoutCField.getValue());
            }
            if (!deviceStatusReqsField.isEmpty()) {
                deviceProfileBuilder.setDeviceStatusReqInterval(deviceStatusReqsField.getValue());
            }
            if (activationGroup.getValue().equals("OTAA")) {
                deviceProfileBuilder.setSupportsOtaa(true);
            }
            else {
                deviceProfileBuilder.
                        setSupportsOtaa(false)
                        .setAbpRx1Delay(rx1DelayField.getValue())
                        .setAbpRx1DrOffset(rx1DataRateOffsetField.getValue())
                        .setAbpRx2Dr(rx2DataRateField.getValue())
                        .setAbpRx2Freq(rx2Frequency.getValue());
            }
            if (codecRuntimeComboBox.isEmpty()) {
                codecRuntimeComboBox.setValue(CodecRuntime.NONE);
            }
            deviceProfileBuilder.setPayloadCodecRuntime(codecRuntimeComboBox.getValue());
            if (codecRuntimeComboBox.getValue().equals(CodecRuntime.JS)) {
                deviceProfileBuilder.setPayloadCodecScript(aceEditor.getValue());
            }

            try {
                if (!isEditing) {
                    CreateDeviceProfileRequest createDeviceProfileRequest = CreateDeviceProfileRequest
                            .newBuilder()
                            .setDeviceProfile(deviceProfileBuilder)
                            .build();

                    deviceProfileStub.create(createDeviceProfileRequest);

                    UI.getCurrent().navigate(deviceProfilesUrlPrefix);

                    new SuccessNotification("Device Profile Created Successfully").open();
                }
                else {
                    deviceProfileBuilder.setId(this.id);
                    UpdateDeviceProfileRequest updateDeviceProfileRequest = UpdateDeviceProfileRequest
                            .newBuilder()
                            .setDeviceProfile(deviceProfileBuilder)
                            .build();

                    deviceProfileStub.update(updateDeviceProfileRequest);

                    new SuccessNotification("Device Profile Updated Successfully").open();
                }

            } catch (Exception e) {
                e.printStackTrace();
                new InternalErrorNotification().open();
            }
        }
    }

    private void setGeneralLayout() {
        FormLayout formLayout = new FormLayout();

        nameField.setRequired(true);

        activationGroup.setLabel("Activation method");
        activationGroup.setItems("OTAA", "ABP");
        activationGroup.setValue("OTAA");

        HorizontalLayout rx1AbpConfigLayout = new HorizontalLayout(rx1DelayField, rx1DataRateOffsetField);
        HorizontalLayout rx2AbpConfigLayout = new HorizontalLayout(rx2DataRateField, rx2Frequency);
        rx1DelayField.setRequired(true);
        rx1DataRateOffsetField.setRequired(true);
        rx2DataRateField.setRequired(true);
        rx2Frequency.setRequired(true);
        VerticalLayout abpConfigurationLayout = new VerticalLayout(rx1AbpConfigLayout, rx2AbpConfigLayout);
        abpConfigurationLayout.setVisible(false);
        activationGroup.addValueChangeListener( change -> {
            abpConfigurationLayout.setVisible(change.getValue().equals("ABP"));
        });

        ListRegionsResponse listRegionsResponse = interServiceStub.listRegions(empty);

        regionComboBox.setRequired(true);
        regionComboBox.setItems(listRegionsResponse.getRegionsList()
                .stream().filter( region -> region.getId().equals("eu868") ||
                        region.getId().equals("eu433") ||
                        region.getId().equals("ism2400"))
                .map(RegionListItem::getRegion)
                .toList());

        macVersionComboBox.setItems(Arrays.stream(MacVersion.values())
                .filter( macVersion -> !macVersion.equals(MacVersion.UNRECOGNIZED)).toList());
        macVersionComboBox.setRequired(true);

        regionalParamsRevComboBox.setItems(Arrays.stream(RegParamsRevision.values()).
                filter( regParamsRevision -> !regParamsRevision.equals(RegParamsRevision.UNRECOGNIZED)).toList());
        regionalParamsRevComboBox.setRequired(true);

        adrAlgorithmComboBox.setItems(deviceProfileStub.listAdrAlgorithms(empty).getResultList());
        adrAlgorithmComboBox.setItemLabelGenerator(AdrAlgorithmListItem::getName);
        adrAlgorithmComboBox.setRequired(true);


        expectedUplinkIntervalField.setRequired(true);

        flushQueOnActivateCheckBox.setValue(true);

        allowRoamingCheckBox.setValue(false);

        createDeviceProfileButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        formLayout.add(nameField, 2);
        formLayout.add(descriptionField, 2);
        formLayout.add(activationGroup, 2);
        formLayout.add(abpConfigurationLayout, 2);
        formLayout.add(regionComboBox, 2);
        formLayout.add(macVersionComboBox, regionalParamsRevComboBox);
        formLayout.add(adrAlgorithmComboBox, 2);
        formLayout.add(expectedUplinkIntervalField, deviceStatusReqsField);
        formLayout.add(flushQueOnActivateCheckBox, allowRoamingCheckBox);
        if (!isEditing) {
            formLayout.add(createDeviceProfileButton, 2);
        }

        generalLayout = new VerticalLayout(formLayout);

        if (isEditing) {
            EditControls editControls = new EditControls();
            editControls.addEditListener( edit -> {
                unSetGeneralLayoutReadOnly();
            });
            editControls.addCancelListener( cancel -> {
                setAllFieldsReadOnly();
            });
            editControls.addSaveListener( save -> {
                setDeviceProfile(true);
                setAllFieldsReadOnly();
            });

            generalLayout.add(editControls);
        }
    }

    private void setClassLayout() {
        confirmedDownLinkTimeoutBField.setRequired(true);
        pingSlotPeriodComboBox.setRequired(true);
        pingSlotDataRateField.setRequired(true);
        pingSlotFrequencyField.setRequired(true);

        pingSlotPeriodComboBox.setItems(0, 1, 2, 3, 4, 5, 6, 7);
        pingSlotPeriodComboBox.setItemLabelGenerator( index -> switch (index) {
            case 0 -> "1 Second";
            case 1 -> "2 Second";
            case 2 -> "4 Second";
            case 3 -> "8 Second";
            case 4 -> "16 Second";
            case 5 -> "32 Second";
            case 6 -> "64 Second";
            case 7 -> "128 Second";
            default -> "Invalid";
        });

        FormLayout classBDetailsLayout = new FormLayout(
                confirmedDownLinkTimeoutBField,
                pingSlotPeriodComboBox,
                pingSlotDataRateField,
                pingSlotFrequencyField
        );
        FormLayout classCDetailsLayout = new FormLayout(confirmedDownLinkTimeoutCField);

        confirmedDownLinkTimeoutCField.setRequired(true);

        classBDetailsLayout.setVisible(false);
        classCDetailsLayout.setVisible(false);

        supportsClassBCheckBox.setValue(false);
        supportsClassCCheckBox.setValue(false);

        supportsClassBCheckBox.addValueChangeListener( change -> classBDetailsLayout.setVisible(change.getValue()));
        supportsClassCCheckBox.addValueChangeListener( change -> classCDetailsLayout.setVisible(change.getValue()));

        classLayout = new VerticalLayout(
                supportsClassBCheckBox,
                classBDetailsLayout,
                supportsClassCCheckBox,
                classCDetailsLayout
        );

        if (isEditing) {
            EditControls editControls = new EditControls();
            editControls.addEditListener( edit -> {
                unSetClassLayoutReadOnly();
            });
            editControls.addCancelListener( cancel -> {
                setAllFieldsReadOnly();
            });
            editControls.addSaveListener( save -> {
                setDeviceProfile(true);
                setAllFieldsReadOnly();
            });

            classLayout.add(editControls);
        }
    }

    private void setRelayLayout() {
        relayLayout = new VerticalLayout(

        );
    }

    private void setCodecLayout() {
        codecRuntimeComboBox.setItems(Arrays.stream(CodecRuntime.values()).
                filter( codecRuntime -> codecRuntime.equals(CodecRuntime.NONE) ||
                        codecRuntime.equals(CodecRuntime.JS) ||
                        codecRuntime.equals(CodecRuntime.CAYENNE_LPP)).toList());
        codecRuntimeComboBox.setItemLabelGenerator( codecRuntime -> switch (codecRuntime) {
            case NONE -> "None";
            case CAYENNE_LPP -> "Cayenne LLP";
            case JS -> "User-defined JavaScript Functions";
            default -> "Invalid";
        });

        codecRuntimeComboBox.setWidthFull();
        codecRuntimeComboBox.setMaxWidth("500px");

        codecRuntimeComboBox.addValueChangeListener( change -> aceEditor.setVisible(change.getValue().equals(CodecRuntime.JS)));

        aceEditor.setTheme(AceTheme.terminal);
        aceEditor.setMode(AceMode.javascript);
        aceEditor.setVisible(false);

        codecLayout = new VerticalLayout(
                codecRuntimeComboBox,
                aceEditor
        );

        if (isEditing) {
            EditControls editControls = new EditControls();
            editControls.addEditListener( edit -> {
                unSetCodecLayoutReadOnly();
            });
            editControls.addCancelListener( cancel -> {
                setAllFieldsReadOnly();
            });
            editControls.addSaveListener( save -> {
                setDeviceProfile(true);
                setAllFieldsReadOnly();
            });

            codecLayout.add(editControls);
        }
    }

    private void setStubs() {
        deviceProfileStub = DeviceProfileServiceGrpc
                .newBlockingStub(channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(VaadinSession.getCurrent().getAttribute(Metadata.class)));
        interServiceStub = InternalServiceGrpc
                .newBlockingStub(channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(VaadinSession.getCurrent().getAttribute(Metadata.class)));


    }

    private void setView(Tab selectedTab, Tab oldTab) {
        if (oldTab.equals(deviceProfileTabs[0])) {
            remove(generalLayout);
        }
        else if (oldTab.equals(deviceProfileTabs[1])) {
            remove(classLayout);
        }
        else if (oldTab.equals(deviceProfileTabs[2])) {
            remove(codecLayout);
        }
//        else if (oldTab.equals(applicationTabs[2])) {
//            remove(relayLayout);
//        }

        if (selectedTab.equals(deviceProfileTabs[0])) {
            add(generalLayout);
        }
        else if (selectedTab.equals(deviceProfileTabs[1])) {
            add(classLayout);
        }
        else if (selectedTab.equals(deviceProfileTabs[2])) {
            add(codecLayout);
        }
//        else if (selectedTab.equals(applicationTabs[2])) {
//            add(relayLayout);
//        }
    }

    private void setAllFieldsReadOnly() {
        nameField.setReadOnly(true);
        descriptionField.setReadOnly(true);
        activationGroup.setReadOnly(true);
        rx1DelayField.setReadOnly(true);
        rx1DataRateOffsetField.setReadOnly(true);
        rx2DataRateField.setReadOnly(true);
        rx2Frequency.setReadOnly(true);
        regionComboBox.setReadOnly(true);
        macVersionComboBox.setReadOnly(true);
        regionalParamsRevComboBox.setReadOnly(true);
        adrAlgorithmComboBox.setReadOnly(true);
        expectedUplinkIntervalField.setReadOnly(true);
        deviceStatusReqsField.setReadOnly(true);
        flushQueOnActivateCheckBox.setReadOnly(true);
        allowRoamingCheckBox.setReadOnly(true);
        supportsClassBCheckBox.setReadOnly(true);
        supportsClassCCheckBox.setReadOnly(true);
        confirmedDownLinkTimeoutBField.setReadOnly(true);
        pingSlotPeriodComboBox.setReadOnly(true);
        pingSlotDataRateField.setReadOnly(true);
        pingSlotFrequencyField.setReadOnly(true);
        confirmedDownLinkTimeoutCField.setReadOnly(true);
        codecRuntimeComboBox.setReadOnly(true);
        aceEditor.setReadOnly(true);
    }

    private void unSetGeneralLayoutReadOnly() {
        nameField.setReadOnly(false);
        descriptionField.setReadOnly(false);
        activationGroup.setReadOnly(false);
        rx1DelayField.setReadOnly(false);
        rx1DataRateOffsetField.setReadOnly(false);
        rx2DataRateField.setReadOnly(false);
        rx2Frequency.setReadOnly(false);
        regionComboBox.setReadOnly(false);
        macVersionComboBox.setReadOnly(false);
        regionalParamsRevComboBox.setReadOnly(false);
        adrAlgorithmComboBox.setReadOnly(false);
        expectedUplinkIntervalField.setReadOnly(false);
        deviceStatusReqsField.setReadOnly(false);
        flushQueOnActivateCheckBox.setReadOnly(false);
        allowRoamingCheckBox.setReadOnly(false);
    }

    private void unSetClassLayoutReadOnly() {
        supportsClassBCheckBox.setReadOnly(false);
        supportsClassCCheckBox.setReadOnly(false);
        confirmedDownLinkTimeoutBField.setReadOnly(false);
        pingSlotPeriodComboBox.setReadOnly(false);
        pingSlotDataRateField.setReadOnly(false);
        pingSlotFrequencyField.setReadOnly(false);
        confirmedDownLinkTimeoutCField.setReadOnly(false);
    }

    private void unSetCodecLayoutReadOnly() {
        codecRuntimeComboBox.setReadOnly(false);
        aceEditor.setReadOnly(false);
    }
}
