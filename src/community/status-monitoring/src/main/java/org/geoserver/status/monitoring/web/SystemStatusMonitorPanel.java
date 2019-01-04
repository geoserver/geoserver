/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.status.monitoring.web;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.datetime.markup.html.basic.DateLabel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.time.Duration;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.status.monitoring.collector.MetricValue;
import org.geoserver.status.monitoring.collector.Metrics;
import org.geoserver.status.monitoring.collector.SystemInfoCollector;

/**
 * Panel to visualize system informations
 *
 * <p>Retrieves and updates system information metrics every second
 *
 * @author sandr
 */
public class SystemStatusMonitorPanel extends Panel {

    private static final long serialVersionUID = -5616622546856772557L;

    public static final String datePattern = "yyyy-MM-dd HH:mm:ss.SSS";

    public SystemStatusMonitorPanel(String id) {
        super(id);

        /*
         * Configure panel
         */

        final SystemInfoCollector systemInfoCollector =
                GeoServerExtensions.bean(SystemInfoCollector.class);
        final IModel<Date> timeMdl = Model.of(new Date());
        final IModel<List<MetricValue>> metricMdl = Model.ofList(Collections.emptyList());

        Label time = DateLabel.forDatePattern("time", timeMdl, datePattern);
        time.setOutputMarkupId(true);
        add(time);

        ListView<MetricValue> list =
                new ListView<MetricValue>("metrics", metricMdl) {
                    private static final long serialVersionUID = -5654700538264617274L;

                    private int counter;

                    @Override
                    protected void onBeforeRender() {
                        super.onBeforeRender();
                        counter = 0;
                    }

                    @Override
                    protected void populateItem(ListItem<MetricValue> item) {
                        item.add(
                                new Label(
                                        "info",
                                        new PropertyModel<MetricValue>(
                                                item.getModel(), "description")),
                                new Label(
                                        "value",
                                        new PropertyModel<MetricValue>(
                                                item.getModel(), "valueUnit")));
                        if (counter % 2 == 0) {
                            item.add(new AttributeModifier("class", "odd"));
                        } else {
                            item.add(new AttributeModifier("class", "even"));
                        }
                        counter++;
                    }
                };
        list.setOutputMarkupId(true);
        add(list);

        /*
         * Refresh every seconds
         */
        add(
                new AjaxSelfUpdatingTimerBehavior(Duration.seconds(1)) {
                    private static final long serialVersionUID = -7009847252782601466L;

                    @Override
                    public void onConfigure(org.apache.wicket.Component component) {
                        Metrics metrics = systemInfoCollector.retrieveAllSystemInfo();
                        metricMdl.setObject(metrics.getMetrics());
                        timeMdl.setObject(new Date());
                    }
                });
    }
}
