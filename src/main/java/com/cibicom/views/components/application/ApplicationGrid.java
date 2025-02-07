package com.cibicom.views.components.application;

import com.cibicom.data.internal.Url;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.server.VaadinSession;
import io.chirpstack.api.*;
import org.vaadin.klaudeta.PaginatedGrid;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

public class ApplicationGrid extends Grid<ApplicationListItem> {

    ApplicationServiceGrpc.ApplicationServiceBlockingStub stub;
    public ApplicationGrid(ApplicationServiceGrpc.ApplicationServiceBlockingStub stub) {
        this.stub = stub;

        String applicationUrlPrefix = Url.getUrlPrefix() + "/applications/";

        addComponentColumn( applicationListItem -> {
            Anchor applicationLink = new Anchor(applicationUrlPrefix + applicationListItem.getId());
            applicationLink.setText(applicationListItem.getName());
            applicationLink.getElement().addEventListener("click", e -> {
                UI.getCurrent().navigate(applicationLink.getHref());
            });
            applicationLink.getStyle().set("color", "orange");
            return applicationLink;
        }).setHeader("Name");
        addComponentColumn( applicationListItem -> {
            Instant instant = Instant.ofEpochSecond(applicationListItem.getCreatedAt().getSeconds());
            LocalDate localDate = LocalDate.ofInstant(instant, ZoneOffset.UTC);
            DatePicker datePicker = new DatePicker();
            datePicker.setReadOnly(true);
            datePicker.setValue(localDate);
            return datePicker;
        }).setHeader("Created At");
        addComponentColumn( applicationListItem -> {
            Instant instant = Instant.ofEpochSecond(applicationListItem.getUpdatedAt().getSeconds());
            LocalDate localDate = LocalDate.ofInstant(instant, ZoneOffset.UTC);
            DatePicker datePicker = new DatePicker();
            datePicker.setReadOnly(true);
            datePicker.setValue(localDate);
            return datePicker;
        }).setHeader("Updated At");


        ListApplicationsRequest listApplicationsRequest = ListApplicationsRequest.newBuilder()
                .setTenantId(VaadinSession.getCurrent().getAttribute(ProfileResponse.class).getTenantsList().get(0).getTenantId())
                .setLimit(15)
                .build();

        ListApplicationsResponse listApplicationsResponse = stub.list(listApplicationsRequest);
        setItems(listApplicationsResponse.getResultList());

    }
}
