package com.cibicom.views.components.stats;

import com.cibicom.views.components.stats.charts.RxChart;
import com.google.protobuf.Timestamp;
import com.storedobject.chart.TimeData;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.server.VaadinSession;
import io.chirpstack.api.*;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class RxChartLayout extends VerticalLayout {
    ManagedChannel channel;
    String id;
    boolean isDevice;
    GatewayServiceGrpc.GatewayServiceBlockingStub gatewayStub;
    DeviceServiceGrpc.DeviceServiceBlockingStub deviceStub;
    Timestamp hourlyStartTimestamp;
    Timestamp hourlyEndTimestamp;
    Timestamp dailyStartTimestamp;
    Timestamp dailyEndTimestamp;
    Timestamp monthlyStartTimestamp;
    Timestamp monthlyEndTimestamp;
    RxChart rxChart;
    public RxChartLayout(ManagedChannel channel, String id, boolean isDevice) {
        this.channel = channel;
        this.id = id;
        this.isDevice = isDevice;
        setStubs();
        setTimeStamps();

        Metric metric;

        if (isDevice) {
            metric = deviceStub.getLinkMetrics(GetDeviceLinkMetricsRequest.newBuilder()
                    .setDevEui(id)
                    .setAggregation(Aggregation.HOUR)
                    .setStart(hourlyStartTimestamp)
                    .setEnd(hourlyEndTimestamp)
                    .build()
            ).getRxPackets();
        }
        else {
            metric = gatewayStub.getMetrics(GetGatewayMetricsRequest.newBuilder()
                    .setGatewayId(id)
                    .setAggregation(Aggregation.HOUR)
                    .setStart(hourlyStartTimestamp)
                    .setEnd(hourlyEndTimestamp)
                    .build()
            ).getRxPackets();
        }

        RadioButtonGroup<Aggregation> aggregationGroup = new RadioButtonGroup<>("Granularity");
        aggregationGroup.setItems(Aggregation.values());
        aggregationGroup.setItemLabelGenerator( aggregation -> {
            switch (aggregation) {
                case HOUR -> {
                    return "24";
                }
                case DAY -> {
                    return "31d";
                }
                case MONTH -> {
                    return "1y";
                }
                case UNRECOGNIZED -> {
                    return "";
                }
            }
            return "";
        });
        aggregationGroup.setValue(Aggregation.HOUR);

        aggregationGroup.addValueChangeListener( change -> {
            switch (change.getValue()) {
                case HOUR -> updateRxChart(Aggregation.HOUR, hourlyStartTimestamp, hourlyEndTimestamp);
                case DAY -> updateRxChart(Aggregation.DAY, dailyStartTimestamp, dailyEndTimestamp);
                case MONTH -> updateRxChart(Aggregation.MONTH, monthlyStartTimestamp, monthlyEndTimestamp);
            }
        });

        rxChart = new RxChart(metric.getDatasets(0).getDataList(), Aggregation.HOUR);
        add(aggregationGroup, rxChart);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
    }

    private void updateRxChart(Aggregation aggregation, Timestamp startTimestamp, Timestamp endTimestamp) {
        remove(rxChart);
        Metric metric;
        if (isDevice) {
            metric = deviceStub.getLinkMetrics(GetDeviceLinkMetricsRequest.newBuilder()
                    .setDevEui(id)
                    .setAggregation(aggregation)
                    .setStart(startTimestamp)
                    .setEnd(endTimestamp)
                    .build()
            ).getRxPackets();
        }
        else {
            metric = gatewayStub.getMetrics(GetGatewayMetricsRequest.newBuilder()
                    .setGatewayId(id)
                    .setAggregation(aggregation)
                    .setStart(startTimestamp)
                    .setEnd(endTimestamp)
                    .build()
            ).getRxPackets();
        }

        rxChart = new RxChart(metric.getDatasets(0).getDataList(), aggregation);
        add(rxChart);
    }

    private void setTimeStamps() {
        hourlyStartTimestamp = Timestamp.newBuilder()
                .setSeconds(LocalDateTime.now().minusHours(25).toEpochSecond(ZoneOffset.UTC))
                .setNanos(LocalDateTime.now().minusHours(25).getNano())
                .build();

        hourlyEndTimestamp = Timestamp.newBuilder()
                .setSeconds(LocalDateTime.now().minusHours(1).toEpochSecond(ZoneOffset.UTC))
                .setNanos(LocalDateTime.now().minusHours(1).getNano())
                .build();

        dailyStartTimestamp = Timestamp.newBuilder()
                .setSeconds(LocalDateTime.now().minusDays(30).toEpochSecond(ZoneOffset.UTC))
                .setNanos(LocalDateTime.now().minusDays(30).getNano())
                .build();

        dailyEndTimestamp = Timestamp.newBuilder()
                .setSeconds(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
                .setNanos(LocalDateTime.now().getNano())
                .build();

        monthlyStartTimestamp = Timestamp.newBuilder()
                .setSeconds(LocalDateTime.now().minusMonths(11).toEpochSecond(ZoneOffset.UTC))
                .setNanos(LocalDateTime.now().minusMonths(11).getNano())
                .build();

        monthlyEndTimestamp = Timestamp.newBuilder()
                .setSeconds(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
                .setNanos(LocalDateTime.now().getNano())
                .build();
    }

    private void setStubs() {
        deviceStub = DeviceServiceGrpc
                .newBlockingStub(channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(VaadinSession.getCurrent().getAttribute(Metadata.class)));
        gatewayStub = GatewayServiceGrpc
                .newBlockingStub(channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(VaadinSession.getCurrent().getAttribute(Metadata.class)));
    }
}
