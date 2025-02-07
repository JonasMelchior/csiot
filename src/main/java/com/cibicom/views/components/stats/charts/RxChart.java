package com.cibicom.views.components.stats.charts;

import com.storedobject.chart.*;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.chirpstack.api.Aggregation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class RxChart extends SOChart {

    public RxChart(List<Float> data, Aggregation aggregation) {

        AbstractData xValues;
        if (aggregation.equals(Aggregation.DAY)) {
            xValues = new TimeData();
        }
        else {
            xValues = new DateData();
        }
        Data yValues = new Data();

        for (int i = 0; i < data.size(); i++) {
            switch (aggregation) {
                case HOUR -> xValues.add(LocalDateTime.now().minusHours(i));
                case DAY -> xValues.add(LocalDate.now().minusDays(i));
                case MONTH -> xValues.add(LocalDate.now().minusMonths(i));
            }
            yValues.add(data.get(i));
        }

        LineChart rxChart = new LineChart(xValues, yValues);
        rxChart.setName("Received Frames");

        XAxis xAxis;
        if (aggregation.equals(Aggregation.DAY)) {
            xAxis = new XAxis(DataType.TIME);
        }
        else {
            xAxis = new XAxis(DataType.DATE);
        }
        xAxis.setName("Time");

        YAxis yAxis = new YAxis(DataType.NUMBER);
        yAxis.setName("Nr. of received frames");

        RectangularCoordinate rc = new RectangularCoordinate(xAxis, yAxis);
        rxChart.plotOn(rc);

        add(rxChart);
    }
}
