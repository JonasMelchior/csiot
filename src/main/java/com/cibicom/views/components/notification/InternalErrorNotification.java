package com.cibicom.views.components.notification;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

public class InternalErrorNotification extends Notification {

    public InternalErrorNotification() {
        setText("An internal error occurred - contact system administrator");
        addThemeVariants(NotificationVariant.LUMO_PRIMARY, NotificationVariant.LUMO_ERROR);
        setPosition(Position.BOTTOM_END);
        setDuration(5000);
    }
}
