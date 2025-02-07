package com.cibicom.views.components.misc;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public class EditControls extends HorizontalLayout {

    private Button editButton = new Button("Edit", new Icon(VaadinIcon.EDIT));
    private Button saveButton = new Button("Save");
    private Button cancelButton = new Button("Cancel", new Icon(VaadinIcon.CLOSE));

    public EditControls() {
        cancelButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        saveButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        configureButtons();

        add(editButton);
    }

    public void addEditListener(ComponentEventListener<ClickEvent<Button>> clickEvent) {
        editButton.addClickListener(clickEvent);
    }

    public void addSaveListener(ComponentEventListener<ClickEvent<Button>> clickEvent) {
        saveButton.addClickListener(clickEvent);
    }

    public void addCancelListener(ComponentEventListener<ClickEvent<Button>> clickEvent) {
        cancelButton.addClickListener(clickEvent);
    }

    private void configureButtons() {
        editButton.addClickListener( click -> {
            removeAll();
            add(saveButton, cancelButton);
        });
        saveButton.addClickListener( click -> {
            removeAll();
            add(editButton);
        });
        cancelButton.addClickListener( click -> {
            removeAll();
            add(editButton);
        });
    }
}
