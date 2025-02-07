package com.cibicom.views.components.device;

import com.cibicom.data.internal.Url;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import io.chirpstack.api.DeviceProfileListItem;
import io.chirpstack.api.DeviceProfileServiceGrpc;

public class DeviceProfileGrid extends Grid<DeviceProfileListItem> {

    DeviceProfileServiceGrpc.DeviceProfileServiceBlockingStub stub;

    public DeviceProfileGrid(DeviceProfileServiceGrpc.DeviceProfileServiceBlockingStub stub) {
        this.stub = stub;
        String deviceProfileUrlPrefix = Url.getUrlPrefix() + "/device-profiles/";

        addComponentColumn( deviceProfile -> {
            Anchor deviceProfileLink = new Anchor(deviceProfileUrlPrefix + deviceProfile.getId());
            deviceProfileLink.setText(deviceProfile.getName());
            deviceProfileLink.getElement().addEventListener("click", e -> {
                UI.getCurrent().navigate(deviceProfileLink.getHref());
            });
            deviceProfileLink.getStyle().set("color", "orange");
            return deviceProfileLink;
        }).setHeader("Name");
        addComponentColumn(deviceProfile -> {
            switch (deviceProfile.getMacVersion()) {
                case LORAWAN_1_0_0 -> {
                    return new Text("1.0.0");
                }
                case LORAWAN_1_0_1 -> {
                    return new Text("1.0.1");
                }
                case LORAWAN_1_0_2 -> {
                    return new Text("1.0.2");
                }
                case LORAWAN_1_0_3 -> {
                    return new Text("1.0.3");
                }
                case LORAWAN_1_0_4 -> {
                    return new Text("1.0.4");
                }
                case LORAWAN_1_1_0 -> {
                    return new Text("1.1.0");
                }
                default -> {
                    return new Text("Invalid");
                }
            }
        }).setHeader("LoRaWAN Version");
        addColumn(DeviceProfileListItem::getRegion).setHeader("Region");
        addComponentColumn( deviceProfile -> {
            Icon icon;
            if (deviceProfile.getSupportsOtaa()) {
                icon = new Icon(VaadinIcon.CHECK);
                icon.setColor("green");
            }
            else {
                icon = new Icon(VaadinIcon.CLOSE);
                icon.setColor("red");
            }
            return icon;
        }).setHeader("OTAA");
        addComponentColumn( deviceProfile -> {
            Icon icon;
            if (deviceProfile.getSupportsClassB()) {
                icon = new Icon(VaadinIcon.CHECK);
                icon.setColor("green");
            }
            else {
                icon = new Icon(VaadinIcon.CLOSE);
                icon.setColor("red");
            }
            return icon;
        }).setHeader("Class B");
        addComponentColumn( deviceProfile -> {
            Icon icon;
            if (deviceProfile.getSupportsClassC()) {
                icon = new Icon(VaadinIcon.CHECK);
                icon.setColor("green");
            }
            else {
                icon = new Icon(VaadinIcon.CLOSE);
                icon.setColor("red");
            }
            return icon;
        }).setHeader("Class C");


    }
}
