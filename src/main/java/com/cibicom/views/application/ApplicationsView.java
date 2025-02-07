package com.cibicom.views.application;

import com.cibicom.data.internal.Api;
import com.cibicom.data.internal.Url;
import com.cibicom.service.device.LoginService;
import com.cibicom.views.MainLayout;
import com.cibicom.views.components.application.ApplicationGrid;
import com.cibicom.views.device.CreateDeviceView;
import com.cibicom.views.components.notification.ErrorNotification;
import com.cibicom.views.components.notification.InternalErrorNotification;
import com.cibicom.views.components.pagination.Pagination;
import com.cibicom.views.device.DeviceView;
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

@Route(value = "applications", layout = MainLayout.class)
public class ApplicationsView extends VerticalLayout implements HasUrlParameter<String>, HasDynamicTitle {
    VerticalLayout layout = new VerticalLayout();
    private String title = "";

    ManagedChannel channel = ManagedChannelBuilder
            .forAddress(Api.getHostName(), Api.getPort())
            .usePlaintext()
            .build();
    ApplicationServiceGrpc.ApplicationServiceBlockingStub applicationStub;
    DeviceServiceGrpc.DeviceServiceBlockingStub deviceStub;

    String applicationUrlPrefix = Url.getUrlPrefix() + "/applications/";


    public ApplicationsView() {
        //TODO: IMPORTANT!!! Remember to delete this when done testing
        //LoginService.login(channel, "jonasjensen04@gmail.com", "majo1230192");
        setStubs();

        layout.setHeightFull();
        add(layout);
        setHeightFull();
    }

    private void setStubs() {
        applicationStub = ApplicationServiceGrpc.
                newBlockingStub(channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(VaadinSession.getCurrent().getAttribute(Metadata.class)));
        deviceStub = DeviceServiceGrpc
                .newBlockingStub(channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(VaadinSession.getCurrent().getAttribute(Metadata.class)));
    }

    @Override
    public String getPageTitle() {
        return title;
    }

    @Override
    public void setParameter(BeforeEvent beforeEvent, @WildcardParameter String parameter) {
        layout.removeAll();
        if (parameter.isEmpty()) {
            setApplicationListView();
        }
        else if (parameter.equals("create")) {
            setCreateApplicationView();
        }
        else if (parameter.split("/").length == 1 && parameter.length() == 36) {
            if (Api.isIdValid(parameter)) {
                try {
                    GetApplicationRequest getApplicationRequest = GetApplicationRequest.newBuilder()
                            .setId(parameter)
                            .build();

                    GetApplicationResponse getApplicationResponse = applicationStub.get(getApplicationRequest);
                    if (!getApplicationResponse.hasApplication()) {
                        setApplicationListView();
                        new ErrorNotification("No such application").open();
                    }
                    else {
                        setApplicationView(getApplicationResponse.getApplication());
                    }
                } catch (Exception e) {
                    setApplicationListView();
                    new InternalErrorNotification().open();
                }

            }
            else {
                setApplicationListView();
                new ErrorNotification("Application ID invalid").open();
            }
        }
        else {
            String[] parameters = parameter.split("/");
            if (Api.isIdValid(parameters[0])) {
                GetApplicationResponse getApplicationResponse;
                try {
                    GetApplicationRequest getApplicationRequest = GetApplicationRequest.newBuilder()
                            .setId(parameters[0])
                            .build();

                    getApplicationResponse = applicationStub.get(getApplicationRequest);
                } catch (Exception e) {
                    e.printStackTrace();
                    setApplicationListView();
                    new InternalErrorNotification().open();
                    return;
                }
                if (parameters[1].equals("devices") && parameters[2].equals("create")) {
                    setCreateDeviceView(getApplicationResponse.getApplication());
                }
                else if (parameters[1].equals("devices") && parameters[2].length() == 16) {
                    try {
                        GetDeviceRequest getDeviceRequest = GetDeviceRequest.newBuilder()
                                .setDevEui(parameters[2])
                                .build();

                        GetDeviceResponse getDeviceResponse = deviceStub.get(getDeviceRequest);
                        setDeviceView(channel, getDeviceResponse.getDevice(), getApplicationResponse.getApplication().getName());
                    } catch (Exception e) {
                        e.printStackTrace();
                        setApplicationView(getApplicationResponse.getApplication());
                        new InternalErrorNotification().open();
                    }
                }
            }
            else {
                setApplicationListView();
                new ErrorNotification("Application ID invalid").open();
            }
        }
    }

    private void setDeviceView(ManagedChannel channel, Device device, String appName) {
        title = "Application | " + appName + " | Device | " + device.getName();
        VaadinSession.getCurrent().setAttribute("title", title);
        layout.add(new DeviceView(channel, device));
    }

    private void setCreateDeviceView(Application application) {
        title = "Application | " + application.getName() + " | Create Device";
        VaadinSession.getCurrent().setAttribute("title", title);
        layout.add(new CreateDeviceView(this.channel, application.getId()));
    }

    ListApplicationsRequest listApplicationsRequest;
    private void setApplicationListView() {

        title = "Applications";
        VaadinSession.getCurrent().setAttribute("title", title);
        ApplicationGrid applicationGrid = new ApplicationGrid(applicationStub);

        Button createApplicationButton = new Button("Create Application", new Icon(VaadinIcon.PLUS));
        createApplicationButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createApplicationButton.addClickListener( click -> {
            UI.getCurrent().navigate(applicationUrlPrefix + "create");
        });

        Pagination pagination = new Pagination();


        try {
            listApplicationsRequest = ListApplicationsRequest.newBuilder().
                    setTenantId(VaadinSession.getCurrent().getAttribute(ProfileResponse.class).getTenantsList().get(0).getTenantId())
                    .setLimit(10)
                    .build();
            ListApplicationsResponse listApplicationsResponse = applicationStub.list(listApplicationsRequest);

            pagination.setPageCounter(1);
            pagination.setMaxPages((int) Math.ceil((double) listApplicationsResponse.getTotalCount() / 10));

            applicationGrid.setItems(listApplicationsResponse.getResultList());
        } catch (Exception e) {
            e.printStackTrace();
            new InternalErrorNotification().open();
        }

        TextField searchField = new TextField();
        searchField.setPlaceholder("Search");
        searchField.setSuffixComponent(new Icon(VaadinIcon.SEARCH));

        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener( change -> {
            listApplicationsRequest = ListApplicationsRequest.newBuilder()
                    .setTenantId(VaadinSession.getCurrent().getAttribute(ProfileResponse.class).getTenantsList().get(0).getTenantId())
                    .setLimit(10)
                    .setSearch(searchField.getValue())
                    .build();
            ListApplicationsResponse listApplicationsResponse = applicationStub.list(listApplicationsRequest);

            pagination.setMaxPages((int) Math.ceil((double) listApplicationsResponse.getTotalCount() / 10));
            pagination.setPageCounter(1);

            applicationGrid.setItems(listApplicationsResponse.getResultList());
        });
        searchField.setWidthFull();

        pagination.addPageChangeListener( pageChangeEvent -> {
            listApplicationsRequest = ListApplicationsRequest.newBuilder().setTenantId(VaadinSession.getCurrent().getAttribute(ProfileResponse.class).getTenantsList().get(0).getTenantId())
                    .setLimit(10)
                    .setOffset(10 * (pageChangeEvent.getNewPageCounterValue() - 1))
                    .setSearch(searchField.getValue())
                    .build();
            ListApplicationsResponse listApplicationsResponse = applicationStub.list(listApplicationsRequest);

            applicationGrid.setItems(listApplicationsResponse.getResultList());
        });

        VerticalLayout applicationListLayout = new VerticalLayout(
                createApplicationButton,
                searchField,
                applicationGrid,
                pagination
        );
        applicationListLayout.setDefaultHorizontalComponentAlignment(Alignment.BASELINE);
        applicationListLayout.setHeightFull();

        layout.add(applicationListLayout);
    }

    private void setApplicationView(Application application) {
        title = "Application | " + application.getName();
        VaadinSession.getCurrent().setAttribute("title", title);

        layout.add(new ApplicationView(application, this.channel));
    }

    private void setCreateApplicationView() {
        title = "Create Application";
        VaadinSession.getCurrent().setAttribute("title", title);

        ApplicationServiceGrpc.ApplicationServiceBlockingStub applicationServiceBlockingStub = ApplicationServiceGrpc
                .newBlockingStub(channel)
                        .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(VaadinSession.getCurrent().getAttribute(Metadata.class)));
        layout.add(new CreateApplicationView(applicationServiceBlockingStub, VaadinSession.getCurrent().getAttribute(ProfileResponse.class).getTenantsList().get(0).getTenantId()));
    }

}
