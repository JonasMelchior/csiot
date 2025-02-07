package com.cibicom.views.components.log;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import org.checkerframework.checker.units.qual.A;

public class LogItemBody extends VerticalLayout {
    public LogItemBody(String jsonBody) {
        // Replace your JSON string here

        AceEditor aceEditor = new AceEditor();
        aceEditor.setMode(AceMode.json);

        aceEditor.setValue(formatJson(jsonBody));
        setSizeFull();

        aceEditor.setHeight("800px");
        aceEditor.setWidth("500px");

        // Add components to the layout
        add(aceEditor);
    }

    // Helper method to format JSON string
    private String formatJson(String jsonString) {
        // You can use a library like Jackson or Gson for better formatting
        // Here, we are using a simple approach for illustration purposes
        return jsonString.replace(",", ",\n").replace("{", "{\n").replace("}", "\n}");
    }
}
