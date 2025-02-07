package com.cibicom.views.components.notification;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

public class ErrorNotification extends Notification {

    public ErrorNotification(String notificationText) {
        setText(notificationText);
        addThemeVariants(NotificationVariant.LUMO_PRIMARY, NotificationVariant.LUMO_ERROR);
        setDuration(5000);
        setPosition(Position.TOP_CENTER);
    }
}
