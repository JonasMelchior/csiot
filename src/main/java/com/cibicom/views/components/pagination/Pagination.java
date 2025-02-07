package com.cibicom.views.components.pagination;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.shared.Registration;

public class Pagination extends HorizontalLayout {
    private Button leftButton = new Button(new Icon(VaadinIcon.ARROW_LEFT));
    private Button rightButton = new Button(new Icon(VaadinIcon.ARROW_RIGHT));
    private int pageCounter = 1;
    private Text pageNoText = new Text("1");
    private int maxPages = 0;
    private Text maxPagesText =  new Text("");

    // Custom event for counter value change
    public static class PageChangeEvent extends ComponentEvent<Pagination> {
        private int newPageCounterValue;

        public PageChangeEvent(Pagination source, boolean fromClient, int newPageCounterValue) {
            super(source, fromClient);
            this.newPageCounterValue = newPageCounterValue;
        }

        public int getNewPageCounterValue() {
            return newPageCounterValue;
        }
    }

    public Pagination() {

        leftButton.addClickListener(event -> {
            if (getPageCounter() != 1) {
                setPageCounter(getPageCounter() - 1);
            }
        });

        rightButton.addClickListener(event -> {
            if (getPageCounter() != this.maxPages) {
                setPageCounter(getPageCounter() + 1);
            }
        });

        add(leftButton, pageNoText, rightButton, maxPagesText);
        setDefaultVerticalComponentAlignment(Alignment.CENTER);
    }

    public int getPageCounter() {
        return pageCounter;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
        maxPagesText.setText("of " + maxPages);
    }

    public void setPageCounter(int pageCounter) {
        if (pageCounter != this.pageCounter) {
            pageNoText.setText(String.valueOf(pageCounter));
            this.pageCounter = pageCounter;
            fireEvent(new PageChangeEvent(this, false, pageCounter));
        }
    }

    public Registration addPageChangeListener(ComponentEventListener<PageChangeEvent> listener) {
        return addListener(PageChangeEvent.class, listener);
    }
}
