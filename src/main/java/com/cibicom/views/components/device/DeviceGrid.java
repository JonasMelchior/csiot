package com.cibicom.views.components.device;

import com.cibicom.data.internal.Url;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinSession;
import io.chirpstack.api.DeviceListItem;
import io.chirpstack.api.DeviceServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

public class DeviceGrid extends Grid<DeviceListItem> {

    public DeviceGrid(String appId) {
        String deviceUrlPrefix = Url.getUrlPrefix() + "/applications/" + appId + "/devices/";

        addComponentColumn( deviceListItem -> {
            Anchor deviceLink = new Anchor(deviceUrlPrefix + deviceListItem.getDevEui());
            deviceLink.setText(deviceListItem.getDevEui());
            deviceLink.getElement().addEventListener("click", e -> {
                UI.getCurrent().navigate(deviceLink.getHref());
            });
            deviceLink.getStyle().set("color", "orange");
            return deviceLink;
        }).setHeader("DevEUI");
        addColumn(DeviceListItem::getName).setHeader("Name");
        addColumn(DeviceListItem::getDeviceStatus).setHeader("Status");
        addComponentColumn( deviceListItem -> {
            Instant instant = Instant.ofEpochSecond(deviceListItem.getLastSeenAt().getSeconds());
            LocalDate localDate = LocalDate.ofInstant(instant, ZoneOffset.UTC);
            return new Text(localDate.toString());
        }).setHeader("Last Seen");
    }
}
