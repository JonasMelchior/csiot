package com.cibicom.views.device.profile;

import com.cibicom.data.internal.Api;
import com.cibicom.data.internal.Url;
import com.cibicom.views.MainLayout;
import com.cibicom.views.components.device.DeviceProfileGrid;
import com.cibicom.views.components.notification.ErrorNotification;
import com.cibicom.views.components.notification.InternalErrorNotification;
import com.cibicom.views.components.pagination.Pagination;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinSession;
import io.chirpstack.api.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;

@Route(value = "device-profiles", layout = MainLayout.class)
public class DeviceProfilesView extends VerticalLayout implements HasUrlParameter<String>, HasDynamicTitle {

    VerticalLayout layout = new VerticalLayout();

    ManagedChannel channel = ManagedChannelBuilder
            .forAddress(Api.getHostName(), Api.getPort())
            .usePlaintext()
            .build();

    DeviceProfileServiceGrpc.DeviceProfileServiceBlockingStub stub = DeviceProfileServiceGrpc
            .newBlockingStub(channel)
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(VaadinSession.getCurrent().getAttribute(Metadata.class)));
    private String title = "";

    String deviceProfilesUrlPrefix = Url.getUrlPrefix() + "/device-profiles/";

    public DeviceProfilesView() {
        layout.setHeightFull();
        add(layout);
        setHeightFull();
    }

    @Override
    public String getPageTitle() {
        return this.title;
    }

    @Override
    public void setParameter(BeforeEvent beforeEvent, @WildcardParameter String parameter) {
        layout.removeAll();
        if (parameter.isEmpty()) {
            setDeviceProfilesView();
        }
        else if (parameter.equals("create")) {
            setCreateDeviceProfileView();
        }
        else if (parameter.split("/").length == 1 && parameter.length() == 36) {
            if (Api.isIdValid(parameter)) {
                GetDeviceProfileRequest getDeviceProfileRequest = GetDeviceProfileRequest.newBuilder()
                        .setId(parameter)
                        .build();

                GetDeviceProfileResponse getDeviceProfileResponse = stub.get(getDeviceProfileRequest);
                if (!getDeviceProfileResponse.hasDeviceProfile()) {
                    setDeviceProfilesView();
                    new ErrorNotification("No such application").open();
                }
                else {
                    setDeviceProfileView(getDeviceProfileResponse.getDeviceProfile());
                }
            }
        }
    }

    ListDeviceProfilesRequest listDeviceProfilesRequest;
    private void setDeviceProfilesView() {
        title = "Device Profiles";
        VaadinSession.getCurrent().setAttribute("title", title);
        DeviceProfileGrid deviceProfileGrid = new DeviceProfileGrid(stub);

        Button createDeviceProfileButton = new Button("Create Device Profile", new Icon(VaadinIcon.PLUS));
        createDeviceProfileButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createDeviceProfileButton.addClickListener( click -> {
            UI.getCurrent().navigate(deviceProfilesUrlPrefix + "create");
        });

        Pagination pagination = new Pagination();

        try {
            listDeviceProfilesRequest = ListDeviceProfilesRequest.newBuilder()
                    .setTenantId(VaadinSession.getCurrent().getAttribute(ProfileResponse.class).getTenantsList().get(0).getTenantId())
                    .setLimit(10)
                    .build();
            ListDeviceProfilesResponse listDeviceProfilesResponse = stub.list(listDeviceProfilesRequest);

            pagination.setPageCounter(1);
            pagination.setMaxPages((int) Math.ceil((double) listDeviceProfilesResponse.getTotalCount() / 10));

            deviceProfileGrid.setItems(listDeviceProfilesResponse.getResultList());
        } catch (Exception e) {
            e.printStackTrace();
            new InternalErrorNotification().open();
        }

        TextField searchField = new TextField();
        searchField.setPlaceholder("Search");
        searchField.setSuffixComponent(new Icon(VaadinIcon.SEARCH));

        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener( change -> {
            listDeviceProfilesRequest = ListDeviceProfilesRequest.newBuilder()
                    .setTenantId(VaadinSession.getCurrent().getAttribute(ProfileResponse.class).getTenantsList().get(0).getTenantId())
                    .setLimit(10)
                    .setSearch(searchField.getValue())
                    .build();
            ListDeviceProfilesResponse listDeviceProfilesResponse = stub.list(listDeviceProfilesRequest);

            pagination.setMaxPages((int) Math.ceil((double) listDeviceProfilesResponse.getTotalCount() / 10));
            pagination.setPageCounter(1);

            deviceProfileGrid.setItems(listDeviceProfilesResponse.getResultList());
        });
        searchField.setWidthFull();

        pagination.addPageChangeListener( pageChangeEvent -> {
            listDeviceProfilesRequest = ListDeviceProfilesRequest.newBuilder()
                    .setTenantId(VaadinSession.getCurrent().getAttribute(ProfileResponse.class).getTenantsList().get(0).getTenantId())
                    .setLimit(10)
                    .setOffset(10 * (pageChangeEvent.getNewPageCounterValue() - 1))
                    .setSearch(searchField.getValue())
                    .build();
            ListDeviceProfilesResponse listDeviceProfilesResponse = stub.list(listDeviceProfilesRequest);

            deviceProfileGrid.setItems(listDeviceProfilesResponse.getResultList());
        });

        VerticalLayout deviceProfilesLayout = new VerticalLayout(
                createDeviceProfileButton,
                searchField,
                deviceProfileGrid,
                pagination
        );

        deviceProfilesLayout.setDefaultHorizontalComponentAlignment(Alignment.BASELINE);
        deviceProfilesLayout.setHeightFull();

        layout.add(deviceProfilesLayout);
    }

    private void setDeviceProfileView(DeviceProfile deviceProfile) {
        title = "Device Profile | " + deviceProfile.getId();
        VaadinSession.getCurrent().setAttribute("title", title);
        layout.add(new DeviceProfileView(this.channel, deviceProfile));
    }

    private void setCreateDeviceProfileView() {
        title = "Create Device Profile";
        VaadinSession.getCurrent().setAttribute("title", title);
        layout.add(new DeviceProfileView(this.channel));
    }
}
